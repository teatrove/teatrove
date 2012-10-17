/*
 *  Copyright 1997-2011 teatrove.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teatrove.teaservlet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.AbstractList;
import java.util.Date;
import java.util.Enumeration;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

import org.teatrove.tea.runtime.OutputReceiver;
import org.teatrove.tea.runtime.Substitution;
import org.teatrove.teaservlet.management.HttpContextManagement;
import org.teatrove.teaservlet.util.DecodedRequest;
import org.teatrove.trove.io.ByteBufferOutputStream;
import org.teatrove.trove.io.ByteData;
import org.teatrove.trove.io.CharToByteBuffer;
import org.teatrove.trove.io.FileByteData;
import org.teatrove.trove.log.Log;
import org.teatrove.trove.net.HttpClient;

/**
 * The context that is used by the template to return its data. This class 
 * provides some additional HTTP-specific template functions.
 *
 * @author Reece Wilton, Brian S O'Neill
 */
class HttpContextImpl extends org.teatrove.tea.runtime.DefaultContext
implements HttpContext {

    private static final int FILE_SPILLOVER = 65000;

    protected final ServletContext mServletContext;

    protected final Log mLog;

    /** The client's HTTP request */
    protected final HttpServletRequest mRequest;

    /** The client's HTTP response */
    protected final HttpServletResponse mResponse;

    /** Buffer that receives template output */
    protected final CharToByteBuffer mBuffer;

    private Request mReq;

    private OutputReceiver mOutputReceiver;

    private boolean mOutputOverridePermitted;

    // Default is 10,000 milliseconds.
    private long mURLTimeout = 10000;
    
    private HttpContextManagement mHttpContextMBean;  

    /**
     * Constructs the HttpContext which provides HTTP-specific template
     * functions.
     *
     * @param request the client's HTTP request
     * @param response the client's HTTP response
     * @param buffer receives the template output
     */
    public HttpContextImpl(ServletContext context,
                           Log log,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           CharToByteBuffer buffer,
                           OutputReceiver outputReceiver,
                           HttpContextManagement httpContextMBean) {
        mServletContext = context;
        mLog = log;
        mRequest = request;
        mResponse = response;
        mBuffer = buffer;
        mOutputReceiver = outputReceiver;
        mOutputOverridePermitted = (outputReceiver != null);
        mHttpContextMBean = httpContextMBean;
    }

    public void write(int c) throws IOException {
        if ((mOutputOverridePermitted || mBuffer == null) 
            && mOutputReceiver != null) { 
            mOutputReceiver.write(c);
        }
        else if (mBuffer != null) {
            mBuffer.append((char)c);
        }
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        if ((mOutputOverridePermitted || mBuffer == null) 
            && mOutputReceiver != null) { 
            mOutputReceiver.write(cbuf, off, len);
        }
        else if (mBuffer != null) {
            mBuffer.append(cbuf, off, len);
        }
    }

    public void write(String str) throws IOException {
        if ((mOutputOverridePermitted || mBuffer == null) 
            && mOutputReceiver != null) { 
            mOutputReceiver.write(str);
        }
        else if (mBuffer != null) {
            mBuffer.append(str);
        }
    }

    public void write(String str, int off, int len) throws IOException {
        if ((mOutputOverridePermitted || mBuffer == null) 
            && mOutputReceiver != null) { 
            mOutputReceiver.write(str, off, len);
        }
        else if (mBuffer != null) {
            mBuffer.append(str, off, len);
        }
    }

    /**
     * This method is called when the template outputs data. This
     * implementation calls this.toString(Object) on the object and then
     * appends the result to the internal CharToByteBuffer.
     *
     * @param obj the object to output
     *
     * @hidden
     */
    public final void print(Object obj) throws Exception {
        if ((mOutputOverridePermitted || mBuffer == null) 
            && mOutputReceiver != null) { 
            mOutputReceiver.print(obj);
        }
        else if (mBuffer != null) {
            mBuffer.append(toString(obj));
        }
    }

    public void overrideOutput(boolean overridePermitted) {
        mOutputOverridePermitted = overridePermitted;
    }
    
    public HttpContext.Request getRequest() {
        if (mReq == null) {
            mReq = new Request(mRequest);
        }
        return mReq;
    }

    public HttpContext.Request getRequest(String encoding) {
        return new Request(new DecodedRequest(mRequest, encoding));
    }

    public void setStatus(int code) {
        mResponse.setStatus(code);
    }

    public void sendError(int code, String message)
        throws AbortTemplateException, IOException
    {
        mResponse.sendError(code, message);
        throw new AbortTemplateException();
    }

    public void sendError(int code)
        throws AbortTemplateException, IOException
    {
        mResponse.sendError(code);
        throw new AbortTemplateException();
    }

    public void sendRedirect(String url)
        throws AbortTemplateException, IOException
    {
        mResponse.sendRedirect(url);
        throw new AbortTemplateException();
    }

    public void setContentType(String type) throws IOException {
        mResponse.setContentType(type);
    }

    public void setHeader(String name, String value) {
        mResponse.setHeader(name, value);
    }

    public void setHeader(String name, int value) {
        mResponse.setIntHeader(name, value);
    }

    public void setHeader(String name, Date value) {
        mResponse.setDateHeader(name, value.getTime());
    }
    
    public void flushBuffer() throws IOException {
        mResponse.flushBuffer();
    }

    public String encodeParameter(String str, String encoding) {
        try {
            return java.net.URLEncoder.encode(str, encoding);
        }
        catch (UnsupportedEncodingException ex) { mLog.error(ex); return null; }
    }

    public String encodeParameter(String str) {
        String enc = mRequest.getCharacterEncoding() != null ? mRequest.getCharacterEncoding() : "iso-8859-1";
        try {
            return java.net.URLEncoder.encode(str, enc);
        }
        catch (UnsupportedEncodingException ex) { mLog.error(ex); return null; }
    }

    public String decodeParameter(String str, String encoding) {
        try {
            return java.net.URLDecoder.decode(str, encoding);
        }
        catch (UnsupportedEncodingException ex) { mLog.error(ex); return null; }
    }

    public String decodeParameter(String str) {
        String enc = mRequest.getCharacterEncoding() != null ? mRequest.getCharacterEncoding() : "iso-8859-1";
        try {
            return java.net.URLDecoder.decode(str, enc);
        }
        catch (UnsupportedEncodingException ex) { mLog.error(ex); return null; }
    }

    public boolean fileExists(String path) {
        if (path != null) {
            return absoluteFile(path).exists();
        }
        return false;
    }
    
    public void insertFile(String path) throws IOException {
        if (path != null) {
            File file = absoluteFile(path);
            ByteData data = null;
            try {
                data = new FileByteData(file);
                long length = data.getByteCount();
                if (length > FILE_SPILLOVER) {
                    // If its big enough, don't save contents.
                    mBuffer.appendSurrogate(data);
                    data = null;
                }
                else {
                    data.writeTo(new ByteBufferOutputStream(mBuffer));
                }
            }
            catch (IOException e) {
                mLog.warn(e);
            }
            finally {
                if (data != null) {
                    data.reset();
                }
            }
        }
    }

    public String readFile(String path) throws IOException {
        if (path != null) {
            File file = absoluteFile(path);
            ByteData data = null;
            try {
                data = new FileByteData(file);
                long length = data.getByteCount();
                if (length > Integer.MAX_VALUE) {
                    throw new IOException
                        ("File is too long: " + file + ", " + length);
                }
                ByteArrayOutputStream baos = 
                    new ByteArrayOutputStream((int)length);
                data.writeTo(baos);
                return baos.toString();
            }
            catch (IOException e) {
                mLog.warn(e);
            }
            finally {
                if (data != null) {
                    data.reset();
                }
            }
        }
        return "";
    }

    public String readFile(String path, String encoding) throws IOException {
        if (path != null) {
            File file = absoluteFile(path);
            ByteData data = null;
            try {
                data = new FileByteData(file);
                long length = data.getByteCount();
                if (length > Integer.MAX_VALUE) {
                    throw new IOException
                        ("File is too long: " + file + ", " + length);
                }
                ByteArrayOutputStream baos = 
                    new ByteArrayOutputStream((int)length);
                data.writeTo(baos);
                return baos.toString(encoding);
            }
            catch (IOException e) {
                mLog.warn(e);
            }
            finally {
                if (data != null) {
                    data.reset();
                }
            }
        }
        return "";
    }

    private File absoluteFile(String path) {
        String originalPath = path;

        if (path.length() > 0 && path.charAt(0) != '/') {
            // Relative path.
            String requestURI = mRequest.getRequestURI();
            int index = requestURI.lastIndexOf('/');
            if (index > 0) {
                if (index < requestURI.length() - 1) {
                    path = requestURI.substring(0, index + 1) + path;
                }
                else {
                    path = requestURI + path;
                }
            }
            else {
                path = '/' + path;
            }
        }
        else {
            String servletPath = mRequest.getServletPath();
            if (servletPath != null) {
                int length = servletPath.length();
                if (length > 0 && servletPath.charAt(length - 1) == '/') {
                    path = servletPath.substring(0, length - 1) + path;
                }
                else {
                    path = servletPath + path;
                }
            }
        }

        String realPath = mServletContext.getRealPath(path);
        if (realPath != null) {
            return new File(realPath);
        }

        return new File(originalPath);
    }

    public boolean URLExists(String url) {
        if (url == null) {
            return false;
        }

        try {
            return HttpResource.get(absoluteURL(url)).exists(mURLTimeout);
        }
        catch (UnknownHostException e) {
        }
        catch (IOException e) {
            mLog.warn(e);
        }

        return false;
    }

    public void insertURL(String url) throws IOException {
        if (url == null) {
            return;
        }

        // Unlike insertFile, the URL contents are read into the buffer
        // immediately. Not all resources report a content length, and
        // I can't lock the resource to guarantee the length remains fixed.

        try {
            HttpResource resource = HttpResource.get(absoluteURL(url));
            HttpClient.Response response = resource.getResponse(mURLTimeout);
            if (response == null) {
                return;
            }

            Integer contentLength =
                response.getHeaders().getInteger("Content-Length");

            byte[] buffer;
            if (contentLength == null || contentLength.intValue() >= 1024) {
                buffer = new byte[1024];
            }
            else {
                buffer = new byte[contentLength.intValue()];
            }

            InputStream in = response.getInputStream();

            int count;
            while ((count = in.read(buffer)) > 0) {
                mBuffer.append(buffer, 0, count);
            }
        }
        catch (UnknownHostException e) {
        }
        catch (IOException e) {
            mLog.warn(e);
        }
    }

    public String readURL(String url) throws IOException {
        return readURL(url, "iso-8859-1");
    }

    public String readURL(String url, String encoding) throws IOException {
        if (url == null) {
            return "";
        }
        
        // Add the URL to the mBean if it's configured.
        if (mHttpContextMBean != null) {
            mHttpContextMBean.addReadUrl(url);          
        }

        try {
            HttpResource resource = HttpResource.get(absoluteURL(url));
            HttpClient.Response response = resource.getResponse(mURLTimeout);
            if (response == null) {
                return "";
            }

            Reader in = new InputStreamReader
                (response.getInputStream(), encoding);

            StringBuffer sb = new StringBuffer(1024);
            char[] buffer = new char[1024];
            int count;
            while ((count = in.read(buffer, 0, 1024)) > 0) {
                sb.append(buffer, 0, count);
            }
            return new String(sb);
        }
        catch (UnknownHostException e) {
        }
        catch (IOException e) {
            mLog.warn(e);
        }

        return "";
    }

    public void setURLTimeout(long timeout) {
        mURLTimeout = timeout;
    }

    private URL absoluteURL(String path) throws MalformedURLException {
        int colonIndex = path.indexOf(':');
        if (colonIndex > 0) {
            if (path.lastIndexOf('/', colonIndex - 1) < 0) {
                // Protocol pattern detected, URL is already absolute.
                return new URL(path);
            }
        }

        if (path.length() > 0 && path.charAt(0) != '/') {
            // Relative path.
            String requestURI = mRequest.getRequestURI();
            int index = requestURI.lastIndexOf('/');
            if (index > 0) {
                if (index < requestURI.length() - 1) {
                    path = requestURI.substring(0, index + 1) + path;
                }
                else {
                    path = requestURI + path;
                }
            }
            else {
                path = '/' + path;
            }
        }

        return new URL(mRequest.getScheme(), mRequest.getServerName(), 
                       mRequest.getServerPort(), path);
    }

    public void stealOutput(OutputReceiver receiver, Substitution s)
        throws Exception
    {
        if (receiver == this) {
            // Avoid stack overflow if this method is accidentally misused.
            receiver = null;
        }

        OutputReceiver original = mOutputReceiver;
        mOutputReceiver = receiver;
        boolean override = mOutputOverridePermitted;
        overrideOutput(true);
        try {
            s.substitute();
        }
        finally {
            overrideOutput(override);
            mOutputReceiver = original;
        }
    }


    public void debug(String s) {
        mLog.debug(s);
    }
    
    public void debug(Throwable t) {
        mLog.debug(t);
    }

    public void error(String s) {
        mLog.error(s);
    }
    
    public void error(Throwable t) {
        mLog.error(t);
    }

    public void info(String s) {
        mLog.info(s);
    }
    
    public void info(Throwable t) {
        mLog.info(t);
    }

    public void warn(String s) {
        mLog.warn(s);
    }
    
    public void warn(Throwable t) {
        mLog.warn(t);
    }


    private static class Request implements HttpContext.Request {

        private final HttpServletRequest mRequest;

        private Parameters mParameters;
        private Headers mHeaders;
        private Cookies mCookies;
        private Attributes mAttributes;
        
        Request(HttpServletRequest request) {
            mRequest = request;
        }

        public String getProtocol() {
            return mRequest.getProtocol();
        }

        public String getScheme() {
            return mRequest.getScheme();
        }

        public String getServerName() {
            return mRequest.getServerName();
        }

        public int getServerPort() {
            return mRequest.getServerPort();
        }

        public String getRemoteAddr() {
            return mRequest.getRemoteAddr();
        }

        public String getRemoteHost() {
            return mRequest.getRemoteHost();
        }

        public String getAuthType() {
            return mRequest.getAuthType();
        }

        public String getMethod() {
            return mRequest.getMethod();
        }

        public String getRequestURI() {
            return mRequest.getRequestURI();
        }

        public String getContextPath() {
            return mRequest.getContextPath();
        }

        public String getServletPath() {
            return mRequest.getServletPath();
        }

        public String getPathInfo() {
            return mRequest.getPathInfo();
        }

        public String getQueryString() {
            return mRequest.getQueryString();
        }
        
        public String getRemoteUser() {
            return mRequest.getRemoteUser();
        }

        public String getRequestedSessionId() {
            return mRequest.getRequestedSessionId();
        }

        public boolean isRequestedSessionIdValid() {
            return mRequest.isRequestedSessionIdValid();
        }

        public HttpContext.Parameters getParameters() {
            if (mParameters == null) {
                mParameters = new Parameters(mRequest);
            }
            return mParameters;
        }

        public HttpContext.Headers getHeaders() {
            if (mHeaders == null) {
                mHeaders = new Headers(mRequest);
            }
            return mHeaders;
        }

        public HttpContext.Cookies getCookies() {
            if (mCookies == null) {
                mCookies = new Cookies(mRequest);
            }
            return mCookies;
        }

        public HttpContext.Attributes getAttributes() {
            if (mAttributes == null) {
                mAttributes = new Attributes(mRequest);
            }
            return mAttributes;
        }

        
        public HttpContext.Session getSession() {
            return new Session(mRequest);
        }
    }

    private static class Session implements HttpContext.Session {

         private final HttpServletRequest mRequest;

         Session(HttpServletRequest request) {
             mRequest = request; 
         }

         public HttpContext.Attributes getAttributes() {
             return new SessionAttributes(mRequest);
         }

    }

    private static class Parameters implements HttpContext.Parameters {

        private final HttpServletRequest mRequest;

        private HttpContext.StringArrayList mNames;
        
        Parameters(HttpServletRequest request) {
            mRequest = request;
        }
        
        public HttpContext.ParameterValues get(String name) {
            String value = mRequest.getParameter(name);
            return (value == null) ? null :
                new ParameterValues(mRequest, name, value);
        }
        
        @SuppressWarnings("unchecked")
        public HttpContext.StringArrayList getNames() {
            if (mNames == null) {
                mNames = new HttpContext.StringArrayList();
                Enumeration<String> e = mRequest.getParameterNames();
                while (e.hasMoreElements()) {
                    mNames.add(e.nextElement());
                }
            }
            return mNames;
        }
    }

    private static class ParameterValues 
        extends AbstractList<HttpContext.Parameter> 
        implements HttpContext.ParameterValues
    {
        private final HttpServletRequest mRequest;
        private final String mName;
        private final String mValue;
        private String[] mParameterValues;

        ParameterValues(HttpServletRequest request,
                        String name, String value) {
            mRequest = request;
            mName = name;
            mValue = value;
        }

        @Override
        public Parameter get(int index) {
            return new Parameter(getParameterValues()[index]);
        }

        @Override
        public int size() {
            return getParameterValues().length;
        }

        public Integer getAsInteger() {
            try {
                return new Integer(mValue);
            }
            catch (NumberFormatException e) {
                return null;
            }
        }

        public String getAsString() {
            return mValue;
        }

        public String toString() {
            return mValue;
        }

        private String[] getParameterValues() {
            if (mParameterValues == null) {
                mParameterValues = mRequest.getParameterValues(mName);
            }
            return mParameterValues;
        }
    }

    private static class Parameter implements HttpContext.Parameter {

        private final String mValue;

        Parameter(String value) {
            mValue = value;
        }

        public Integer getAsInteger() {
            try {
                return new Integer(mValue);
            }
            catch (NumberFormatException e) {
                return null;
            }
        }

        public String getAsString() {
            return mValue;
        }

        public String toString() {
            return mValue;
        }
    }

    private static class Headers implements HttpContext.Headers {

        private final HttpServletRequest mRequest;
        private HttpContext.StringArrayList mNames;
        
        Headers(HttpServletRequest request) {
            mRequest = request;
        }
        
        public HttpContext.Header get(String name) {
            return new Header(mRequest, name);
        }
        
        @SuppressWarnings("unchecked")
        public HttpContext.StringArrayList getNames() {
            if (mNames == null) {
                mNames = new HttpContext.StringArrayList();
                Enumeration<String> e = mRequest.getHeaderNames();
                while (e.hasMoreElements()) {
                    mNames.add(e.nextElement());
                }
            }
            return mNames;
        }
    }

    private static class Header implements HttpContext.Header {

        private final HttpServletRequest mRequest;
        private final String mName;

        Header(HttpServletRequest request, String name) {
            mRequest = request;
            mName = name;
        }

        public Integer getAsInteger() {
            try {
                int value = mRequest.getIntHeader(mName);
                if (value >= 0) {
                    return new Integer(value);
                }
            }
            catch (NumberFormatException e) {
            }
            return null;
        }

        public Date getAsDate() {
            try {
                long date = mRequest.getDateHeader(mName);
                if (date >= 0) {
                    return new Date(date);
                }
            }
            catch (IllegalArgumentException e) {
            }
            return null;
        }

        public String getAsString() {
            return mRequest.getHeader(mName);
        }

        public String toString() {
            return mRequest.getHeader(mName);
        }
    }

    private static class Cookies implements HttpContext.Cookies {

        private final HttpServletRequest mRequest;

        Cookies(HttpServletRequest request) {
            mRequest = request;
        }
        
        public Cookie get(String name) {
            Cookie[] cookies = mRequest.getCookies();
            for (int i=cookies.length - 1; i >= 0; i--) {
                Cookie cookie = cookies[i];
                if (cookie.getName().equals(name)) {
                    return cookie;
                }
            }
            return null;
        }
        
        public Cookie[] getAll() {
            return mRequest.getCookies();
        }
    }

    private static class Attributes implements HttpContext.Attributes {

        private final HttpServletRequest mRequest;
        private HttpContext.StringArrayList mNames;
        
        Attributes(HttpServletRequest request) {
            mRequest = request;
        }
        
        public Object get(String name) {
            return mRequest.getAttribute(name);
        }
        
        @SuppressWarnings("unchecked")
        public HttpContext.StringArrayList getNames() {
            if (mNames == null) {
                mNames = new HttpContext.StringArrayList();
                Enumeration<String> e = mRequest.getAttributeNames();
                while (e.hasMoreElements()) {
                    mNames.add(e.nextElement());
                }
            }
            return mNames;
        }
    }

    private static class SessionAttributes implements HttpContext.Attributes {

        private final HttpServletRequest mRequest;
        private HttpContext.StringArrayList mNames;
        
        SessionAttributes(HttpServletRequest request) {
            mRequest = request;
        }
        
        public Object get(String name) {
            HttpSession session = mRequest.getSession();
            return mRequest != null && session != null ? 
                session.getAttribute(name) : null;
        }
        
        @SuppressWarnings("unchecked")
        public HttpContext.StringArrayList getNames() {
            HttpSession session = mRequest.getSession();
            if (mRequest == null || session == null)
                return null;
            if (mNames != null) {
                mNames = new HttpContext.StringArrayList();
                Enumeration<String> e = session.getAttributeNames();
                while (e.hasMoreElements()) {
                    mNames.add(e.nextElement());
                }
            }
            return mNames;
        }
    }

    public void insertPath(String path) {
        try {
            if(path.trim().startsWith("/")==false) path = "/" + path.trim();

            RequestDispatcher disp = mServletContext.getRequestDispatcher(path);
            disp.include(mRequest, mResponse);
        } catch(Exception ex) {
            mLog.error(ex);
        }
    }

    public String readPath(String path) {
        
        try {
            if(path.trim().startsWith("/")==false) path = "/" + path.trim();
            
            RequestDispatcher disp = mServletContext.getRequestDispatcher(path);
            
            OutputCapturingResponse resp = new OutputCapturingResponse(mResponse);
            disp.include(mRequest, resp);
            
            return resp.getOutput();
            
        } catch(Exception ex) {
            mLog.error(ex);
        }
        
        return "";
    }
    
    public class OutputCapturingResponse extends HttpServletResponseWrapper {

	ByteArrayOutputStream mOutputStream;
	PrintWriter mWriter;

        public OutputCapturingResponse(HttpServletResponse resp) {
            super(resp);
            mOutputStream = new ByteArrayOutputStream();
            mWriter = new PrintWriter(mOutputStream);
        }
        
	public PrintWriter getWriter() throws IOException {
		return mWriter;
	}

	public String getOutput() {
		mWriter.flush();
		mWriter.close();
		return mOutputStream.toString();
	}        
    }    
}
