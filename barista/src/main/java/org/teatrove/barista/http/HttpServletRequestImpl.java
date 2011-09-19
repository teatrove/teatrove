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

package org.teatrove.barista.http;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.Principal;
import javax.servlet.*;
import javax.servlet.http.*;
import org.teatrove.trove.net.*;
import org.teatrove.trove.util.SoftHashMap;

/**
 * @author Jonathan Colwell
 */
class HttpServletRequestImpl implements HttpServletRequest {
    // Maps InetAddresses to host names.
    private static final Map cHostCache;

    static {
        cHostCache = Collections.synchronizedMap(new SoftHashMap());
    }

    private static String getRemoteAddr(InetAddress address) {
        StringBuffer buf = new StringBuffer(15);
        byte[] bytes = address.getAddress();
        int len = bytes.length;
        for (int i=0; i<len; i++) {
            if (i > 0) {
                buf.append('.');
            }
            append3Digit(buf, bytes[i] & 0xff);
        }
        return buf.toString();
    }

    private static void append3Digit(StringBuffer buf, int value) {
        if (value < 10) {
            buf.append((char)(value + '0'));
        }
        else if (value < 100) {
            buf.append((char)(value / 10 + '0'));
            buf.append((char)(value % 10 + '0'));
        }
        else {
            buf.append((char)(value / 100 + '0'));
            buf.append((char)(value / 10 % 10 + '0'));
            buf.append((char)(value % 10 + '0'));
        }
    }

    private static String getHostName(InetAddress address) {
        String hostName = (String)cHostCache.get(address);
        if (hostName == null) {
            hostName = address.getHostName();
            cHostCache.put(address, hostName);
        }
        return hostName;
    }

    private HttpServerConnection mServerConnection;
    private List mCookies;
    private String mContextPath;
    private String mServletPath;
    private String mPathInfo;
    private String mPathTranslated;
    private String mEncoding;
    private ServletInputStream mStream;
    private ServletContext mContext;
    private Map mParameters;
    private HttpSessionStrategy.Support mSessionSupport;

    public HttpServletRequestImpl(HttpServerConnection httpCon) {
        mServerConnection = httpCon;
    }

    // Accessor Methods for hooking into the servlet context.
    protected void setServletContext(ServletContext context) {
        mContext = context;
    }

    protected ServletContext getServletContext() {
        return mContext;
    }

    // Session Support
    protected void setSessionSupport(HttpSessionStrategy.Support support) {
        mSessionSupport = support;
    }

    // ServletRequest Methods:
    public String getScheme() {
        return mServerConnection.getRequestScheme();
    }

    public String getProtocol() {
        return mServerConnection.getRequestProtocol();
    }

    public String getRequestURI() {
        return mServerConnection.getRequestPath();
    }

    public String getServerName() {
        return getHostName(mServerConnection.getSocket().getLocalAddress());
    }

    public int getServerPort() {
        return mServerConnection.getSocket().getLocalPort();
    }

    public String getRemoteAddr() {
        return getRemoteAddr(mServerConnection.getSocket().getInetAddress());
    }

    public String getRemoteHost() {
        return getHostName(mServerConnection.getSocket().getInetAddress());
    }

    public Object getAttribute(String name) {
        return mServerConnection.getAttributeMap().get(name);
    }

    public void removeAttribute(String name) {
        mServerConnection.getAttributeMap().remove(name);
    }

    public void setAttribute(String name, java.lang.Object o) {
        mServerConnection.getAttributeMap().put(name, o);
    }

    public Enumeration getAttributeNames() {
        return Collections.enumeration(mServerConnection.getAttributeMap().keySet());
    }

    public String getCharacterEncoding() {
        if (mEncoding != null) {
            return mEncoding;
        }
         
        String fileEncoding = "Cp1252".equals(System.getProperty("file.encoding")) ? 
            "iso-8859-1" : System.getProperty("file.encoding");

        if (mEncoding == null)
            mEncoding = parseCharacterEncoding(getContentType());

        if (mEncoding == null)
            mEncoding = fileEncoding;

        return mEncoding;
    }

    /**
     * Parse the character encoding from the specified content type header.
     * If the content type is null, or there is no explicit character encoding,
     * <code>null</code> is returned.
     *
     * @param contentType a content type header
     */
    public static String parseCharacterEncoding(String contentType) {

        if (contentType == null)
            return (null);
        int start = contentType.indexOf("charset=");
        if (start < 0)
            return (null);
        String encoding = contentType.substring(start + 8);
        int end = encoding.indexOf(';');
        if (end >= 0)
            encoding = encoding.substring(0, end);
        encoding = encoding.trim();
        if ((encoding.length() > 2) && (encoding.startsWith("\""))
            && (encoding.endsWith("\"")))
            encoding = encoding.substring(1, encoding.length() - 1);
        return (encoding.trim());

    }

    public int getContentLength() {
        return getIntHeader("Content-Length");
    }

    public String getContentType() {
        return getHeader("Content-Type");
    }

    public ServletInputStream getInputStream() throws IOException {
        if (mStream == null) {
            mStream = new ServletInputStreamImpl
                (mServerConnection.getSocket().getInputStream());
        }
        return mStream;
    }

    public Locale getLocale() {
        // TODO: this is still kind of a lame implementation.
        // perhaps improve it some day. -jc 3/8/01
        Locale locale = null;
        String language = getHeader("Accept-Language");
        if (language != null) {
            if (language.length() == 2) {
                locale = new Locale(language,"");
            }
            else if (language.length() > 4) {
                locale = new Locale(language.substring(0,2),
                                    language.substring(3,5));
            }
        }
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return locale;
    }

    public Enumeration getLocales() {
        // TODO: this too is a lame implementation.
        // improvement might be a good idea. -jc 3/8/01
        Set localeSet = new HashSet();
        Enumeration languages = getHeaders("Accept-Language");
        if (languages != null) {
            while (languages.hasMoreElements()) {
                String language = languages.nextElement().toString();
                Locale locale = null;
                if (language.length() == 2) {
                    locale = new Locale(language,"");
                }
                else if (language.length() > 4) {
                    locale = new Locale(language.substring(0,2),
                                        language.substring(3,5));
                }

                if (locale != null) {
                    localeSet.add(locale);
                }
            }
        }
        if (localeSet.size() == 0) {
            localeSet.add(Locale.getDefault());
        }
        return Collections.enumeration(localeSet);
    }

    public String getParameter(String name) {
        Object param = getParameterMap().get(name);
        if (param != null) {
            if (param instanceof String[]) {
                if (((String[])param).length > 0) {
                    return ((String[])param)[0];
                }
            }
            else if (param instanceof String) {
                return (String)param;
            }
        }
        return null;
    }

    public Map getParameterMap() {
        if (mParameters == null) {
            mParameters = mServerConnection.getRequestParameters();
            parsePostParameters();
            Utils.ensureOnlyStringArrayValues(mParameters);

            String encoding = getCharacterEncoding();
            if ((!"iso-8859-1".equals(encoding)) &&
                (!"ISO-8859-1".equals(encoding)) &&
                (!"8859-1".equals(encoding)) &&
                (!"8859_1".equals(encoding))) {

                // Rebuild the parameters against a different character
                // encoding.

                try {
                    Iterator it = mParameters.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry entry = (Map.Entry)it.next();
                        String key = (String)entry.getKey();
                        String[] values = (String[])entry.getValue();

                        if (values != null) {
                            for (int i=values.length; --i>=0; ) {
                                values[i] = new String
                                    (values[i].getBytes("iso-8859-1"), encoding);
                            }
                        }

                        // TODO: Transform keys as well. If the new key is
                        // different, create a new map and populate it.
                    }
                }
                catch (UnsupportedEncodingException e) {
                }
            }

            mParameters = Collections.unmodifiableMap(mParameters);
        }

        return mParameters;
    }

    public Enumeration getParameterNames() {
        return Collections.enumeration(getParameterMap().keySet());
    }

    public String[] getParameterValues(java.lang.String name) {
        Object params = getParameterMap().get(name);
        if (params != null) {
            if (params instanceof String[]) {
                return (String[])params;
            }
            else if (params instanceof String) {
                String[] stringz = new String[1];
                stringz[0] = (String)params;
                return stringz;
            }
        }
        return null;
    }

    public BufferedReader getReader() throws IOException {
        Reader rawReader;
        String encoding = getCharacterEncoding();
        if (encoding != null) {
            rawReader = new InputStreamReader(getInputStream(), encoding);
        }
        else {
            rawReader = new InputStreamReader(getInputStream());
        }
        return new BufferedReader(rawReader);
    }

    /**
     * @deprecated As of Version 2.1 of the Java Servlet API,
     * use ServletContext.getRealPath(java.lang.String) instead.
     */
    public String getRealPath(String uri) {
        if (uri.length() > 0 && uri.charAt(0) != '/') {
            // Slap the servlet path on the front of the uri.
            uri = getServletPath() + "/" + uri;
        }
        return getServletContext().getRealPath(uri);
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        if (path.length() > 0 && path.charAt(0) != '/') {
            // Slap the servlet path on the front of the uri.
            path = getServletPath() + "/" + path;
        }
        return getServletContext().getRequestDispatcher(path);
    }

    /**
     * Returns true if the request was made using https.
     * this method also sets the SSL Attributes before returning true.
     */
    public boolean isSecure() {

        if ("https".equalsIgnoreCase(getScheme())) {
            try {
                javax.net.ssl.SSLSocket secureSocket =
                    (javax.net.ssl.SSLSocket)mServerConnection.getSocket();
                javax.net.ssl.SSLSession secureSession =
                    secureSocket.getSession();

                /*
                 * Since the Servlet API appears to be trying to match the
                 * Tomcat implementation rather than the other way around,
                 * I felt free to thug the keysize constants right out of
                 * Tomcat. -jc 3/14/01
                 */
                String cipherSuite = secureSession.getCipherSuite();
                if (cipherSuite != null) {
                    setAttribute("javax.servlet.request.cipher-suite",
                                 cipherSuite);
                    Integer keySize = null;
                    if (cipherSuite.indexOf("_WITH_NULL_") >= 0) {
                        keySize = new Integer(0);
                    }
                    else if ((cipherSuite.indexOf("_WITH_IDEA_CBC_") >= 0)
                             || (cipherSuite.indexOf("_WITH_RC4_128_") >= 0)) {
                        keySize = new Integer(128);
                    }
                    else if ((cipherSuite.indexOf("_WITH_RC2_CBC_40_") >= 0)
                             || (cipherSuite.indexOf("_WITH_RC4_40_") >= 0)
                             || (cipherSuite.indexOf("_WITH_DES40_CBC_") >= 0)) {
                        keySize = new Integer(40);
                    }
                    else if (cipherSuite.indexOf("_WITH_DES_CBC_") >= 0) {
                        keySize = new Integer(56);
                    }
                    else if (cipherSuite.indexOf("_WITH_3DES_EDE_CBC_") >= 0) {
                        keySize = new Integer(168);
                    }
                    if (keySize != null) {
                        setAttribute("javax.servlet.request.key-size",keySize);
                    }
                }

                javax.security.cert.X509Certificate[] certChain =
                    secureSession.getPeerCertificateChain();
                if (certChain != null && certChain.length > 0) {
                    setAttribute("javax.servlet.request.X509Certificate",
                                 certChain[0]);
                }

            }
            catch (Exception e) {
            }
            return true;
        }
        else {
            return false;
        }
    }

    public void setCharacterEncoding(String encoding) {
        mEncoding = encoding;
    }

    // HttpServletRequest Methods:

    protected void setContextPath(String contextPath) {
        mContextPath = contextPath;
    }

    public String getContextPath() {
        return mContextPath;
    }

    public Cookie[] getCookies() {
        if (mCookies == null) {
            Set entries = mServerConnection.getRequestCookies().entrySet();

            List cookies = new ArrayList(entries.size());
            for (Iterator it = entries.iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry)it.next();
                try {
                    Cookie cookie = new Cookie((String)entry.getKey(),
                                               (String)entry.getValue());
                    cookies.add(cookie);
                }
                // eat cookie exceptions
                catch (IllegalArgumentException e) {}
            }

            mCookies = cookies;
        }

        return (Cookie[])mCookies.toArray(new Cookie[mCookies.size()]);
    }

    public long getDateHeader(String name) {
        HttpHeaderMap headers = mServerConnection.getRequestHeaders();
        Date hoy = headers.getDate(name);
        if (hoy != null) {
            return hoy.getTime();
        }

        Object value = headers.get(name);
        if (value == null) {
            return -1;
        }

        throw new IllegalArgumentException("Cannot convert to date: " + value);
    }

    public String getHeader(String name) {
        return mServerConnection.getRequestHeaders().getString(name);
    }

    public Enumeration getHeaderNames() {
        return Collections.enumeration
            (mServerConnection.getRequestHeaders().keySet());
    }

    public Enumeration getHeaders(String name) {
        return Collections.enumeration
            (mServerConnection.getRequestHeaders().getAll(name));
    }

    public int getIntHeader(String name) {
        HttpHeaderMap headers = mServerConnection.getRequestHeaders();
        Integer num = headers.getInteger(name);
        if (num != null) {
            return num.intValue();
        }

        Object value = headers.get(name);
        if (value == null) {
            return -1;
        }

        throw new IllegalArgumentException("Cannot convert to int: " + value);
    }

    public String getMethod() {
        return mServerConnection.getRequestMethod();
    }

    protected void setPathInfo(String pathInfo) {
        mPathInfo = pathInfo;
    }

    public String getPathInfo() {
        return mPathInfo;
    }

    public String getPathTranslated() {
        if (getPathInfo() != null) {
            return getServletContext().getRealPath
                (getServletPath() + getPathInfo());
        }
        return null;
    }

    public String getQueryString() {
        return mServerConnection.getRequestQueryString();
    }

    public String getRequestedSessionId() {
        return mSessionSupport.getRequestedSessionId();
    }

    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        if (!getRequestURI().startsWith(getScheme())) {
            url.append(getScheme());
            url.append("://");

            // check if the "Host" header was specified.
            String server = getHeader("Host");
            if (server == null) {
                server = getServerName();
                if (server == null) {
                    server = mServerConnection.getSocket()
                        .getLocalAddress().getHostAddress();
                }
	            url.append(server);
				int port = getServerPort();
				if (!((port == 80 && "http".equalsIgnoreCase(getScheme())) ||
					  (port == 443 && "https".equalsIgnoreCase(getScheme())))) {
					url.append(':');
					url.append(port);
				}
			}
            else {
            	url.append(server);
			}

        }
        url.append(getRequestURI());
        return url;
    }

    protected void setServletPath(String servletPath) {
        mServletPath = servletPath;
    }

    public String getServletPath() {
        return mServletPath;
    }

    public HttpSession getSession() {
        return mSessionSupport.getSession(getServletContext());
    }

    public HttpSession getSession(boolean create) {
        return mSessionSupport.getSession(getServletContext(), create);
    }

    public boolean isRequestedSessionIdFromCookie() {
        return mSessionSupport.isRequestedSessionIdFromCookie();
    }

    /**
     * @deprecated
     */
    public boolean isRequestedSessionIdFromUrl() {
        return mSessionSupport.isRequestedSessionIdFromURL();
    }

    public boolean isRequestedSessionIdFromURL() {
        return mSessionSupport.isRequestedSessionIdFromURL();
    }

    public boolean isRequestedSessionIdValid() {
        return mSessionSupport.isRequestedSessionIdValid();
    }

    public String getAuthType() {
        // TODO: implement this method.
        return null;
    }

    public Principal getUserPrincipal() {
        // TODO: implement this method.
        return null;
    }

    public String getRemoteUser() {
        // TODO: implement this method.
        return null;
    }

    public boolean isUserInRole(String role) {
        // TODO: implement this method.
        return false;
    }

    // Additional methods.

    /**
     * Checks if the given socket matches one being used this request. This
     * method can be used to tell if a checked socket exception came from this
     * request.
     *
     * @param s may be null, in which case false is returned.
     */
    public boolean isMatchingSocket(SocketFace s) {
        if (s == null) {
            return false;
        }
        SocketFace thisSocket = mServerConnection.getSocket();
        return
            thisSocket.getInetAddress().equals(s.getInetAddress()) &&
            thisSocket.getPort() == s.getPort() &&
            thisSocket.getLocalPort() == s.getLocalPort();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());
        buf.append(':');
        buf.append('\n');

        Iterator entries = getPropertyMap().entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry)entries.next();

            buf.append(entry.getKey());
            buf.append('=');

            Object value = entry.getValue();

            if (value instanceof Object[]) {
                buf.append(Arrays.asList((Object[])value));
            }
            else {
                buf.append(value);
            }

            buf.append('\n');
        }

        return buf.toString();
    }

    // For use when converting this object to a string.
    private Map getPropertyMap() {
        Map map = new TreeMap();

        map.put("CharacterEncoding", getCharacterEncoding());
        map.put("ContentLength", String.valueOf(getContentLength()));
        map.put("ContentType", getContentType());
        map.put("Protocol", getProtocol());
        map.put("Scheme", getScheme());
        map.put("ServerName", getServerName());
        map.put("ServerPort", String.valueOf(getServerPort()));
        map.put("RemoteAddr", getRemoteAddr());
        map.put("RemoteHost", getRemoteHost());

        Enumeration enumeration = getAttributeNames();
        while (enumeration.hasMoreElements()) {
            String key = (String)enumeration.nextElement();
            map.put("Attribute:" + key, getAttribute(key));
        }

        enumeration = getParameterNames();
        while (enumeration.hasMoreElements()) {
            String key = (String)enumeration.nextElement();
            map.put("ParameterValues:" + key, getParameterValues(key));
        }

        map.put("AuthType", getAuthType());
        map.put("Cookies", getCookies());
        map.put("Method", getMethod());
        map.put("PathInfo", getPathInfo());
        map.put("PathTranslated", getPathTranslated());
        map.put("QueryString", getQueryString());
        map.put("RemoteUser", getRemoteUser());
        map.put("RequestURI", getRequestURI());
        map.put("ServletPath", getServletPath());
        map.put("ContextPath", getContextPath());

        enumeration = getHeaderNames();
        while (enumeration.hasMoreElements()) {
            String key = (String)enumeration.nextElement();
            map.put("Header:" + key, getHeader(key));
        }

        return map;
    }

    private void parsePostParameters() {
        int contentLength = getContentLength();

        if (contentLength > 0 && "POST".equals(getMethod())) {
            String contentType = getContentType();

            if (contentType == null ||
                contentType.equals("application/x-www-form-urlencoded")) {

                StringBuffer buf = new StringBuffer(contentLength);

                try {
                    InputStream in = getInputStream();
                    int c;
                    while ((c = in.read()) >= 0) {
                        buf.append((char)c);
                    }
                }
                catch (IOException e) {
                    //e.printStackTrace();
                }

                Map postData = new HashMap();
                Utils.parseQueryString(buf.toString(), postData);

                Iterator it = postData.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry entry = (Map.Entry)it.next();
                    Utils.addToParameterMap(mParameters,
                                            entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
