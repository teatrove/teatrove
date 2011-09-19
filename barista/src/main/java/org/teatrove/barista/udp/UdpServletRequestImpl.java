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

package org.teatrove.barista.udp;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.*;
import org.teatrove.trove.util.SoftHashMap;

/**
 * @author Tammy Wang
 */
public class UdpServletRequestImpl implements ServletRequest {
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

    private UdpServerConnection mServerConnection;
    private List mCookies;
    private String mContextPath;
    private String mServletPath;
    private String mPathInfo;
    private String mPathTranslated;
    private String mEncoding;
    private ServletInputStream mStream;
    private ServletContext mContext;
    private Map mParameters;
    

    public UdpServletRequestImpl(UdpServerConnection udpCon) {
        mServerConnection = udpCon;
    }

    // Accessor Methods for hooking into the servlet context.
    protected void setServletContext(ServletContext context) {
        mContext = context;
    }
	
	protected void setServletPath(String servletPath) {
        mServletPath = servletPath;
    }
	 public String getServletPath() {
        return mServletPath;
    }

    protected ServletContext getServletContext() {
        return mContext;
    }
 
    // ServletRequest Methods:
    public String getScheme() {
        return null;
    }

    public String getProtocol() {
        return null;
   }

    public String getRequestURI() {
        return mServerConnection.getRequestURI();
    }

    public String getServerName() {
        return getHostName(mServerConnection.getSocket().getLocalAddress());
    }

    public int getServerPort() {
        return mServerConnection.getSocket().getLocalPort();
    }

    public String getRemoteAddr() {
        return getRemoteAddr(mServerConnection.getRequestPacket().getAddress());
    }

    public String getRemoteHost() {
        return getHostName(mServerConnection.getRequestPacket().getAddress());
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
        
        return null;
    }

    public int getContentLength() {
	
		return mServerConnection.getRequestPacket().getLength();
      
    }

    public String getContentType() {
        return null;
    }

    public ServletInputStream getInputStream() throws IOException {
        
        return null;
    }
	
	public String getQueryString() {
        return mServerConnection.getRequestQueryString();
    }

    public Locale getLocale() {
        return null;
    }

    public Enumeration getLocales() {
        return null;
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
            Utils.ensureOnlyStringArrayValues(mParameters);
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
        
        rawReader = new InputStreamReader(getInputStream());
        
        return new BufferedReader(rawReader);
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
          return false;      
    }

    public void setCharacterEncoding(String encoding) {
        mEncoding = encoding;
    }

 
    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());
        buf.append(':');
        buf.append('\n');

       
        return buf.toString();
    }
	
	/*
	 *Deprecated. As of Version 2.1 of the Java Servlet API, use ServletContext.getRealPath(java.lang.String) instead. 
	 */
	
	public String getRealPath(String path)
	{
		return null;
	}

  

    
    
}
