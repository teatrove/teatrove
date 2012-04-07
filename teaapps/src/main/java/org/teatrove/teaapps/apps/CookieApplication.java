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
package org.teatrove.teaapps.apps;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.teatrove.teaservlet.Application;
import org.teatrove.teaservlet.ApplicationConfig;
import org.teatrove.teaservlet.ApplicationRequest;
import org.teatrove.teaservlet.ApplicationResponse;
import org.teatrove.trove.util.PropertyMap;

/**
 * The CookieApplication is a tea-based application for handling HTTP-based
 * cookies including setting, retrieving, and clearing.
 * 
 * @author Scott Jappinen
 * 
 * @see CookieContext
 */
public class CookieApplication implements Application {
    
    private String mDomain;
    private String mPath;
    private boolean mIsSecure;
    
    /**
     * Initializes the application with the given configuration.  The following
     * properties are supported:
     * 
     * <dl>
     * <dt>domain</dt>
     * <dd>The static domain to use in setting and retrieving cookies</dd>
     * 
     * <dt>path</dt>
     * <dd>The static path to use in setting and retrieving cookies</dd>
     * 
     * <dt>isSecure</dt>
     * <dd>The boolean state of setting secure or insecure cookies</dd>
     * </dl>
     * 
     * @param config an ApplicationConfig object containing config. info.
     */
    public void init(ApplicationConfig config) throws ServletException {
        PropertyMap properties = config.getProperties();
        mDomain = properties.getString("domain");
        mPath = properties.getString("path");
        mIsSecure = properties.getBoolean("isSecure", false);
        
        /*
        String className = config.getProperties().getString("encoderClass");
        if (className != null && !className.equals("")) {
            try {
                Class clazz = Class.forName(className);
                mTranscoder = (StringTranscoder) clazz.newInstance();
            } catch (ClassNotFoundException e) {
                throw new ServletException(e);
            } catch (InstantiationException e) {
                throw new ServletException(e);
            } catch (IllegalAccessException e) {
                throw new ServletException(e);
            }
        } else {
            mTranscoder = new Base64StringTranscoder();
        }
        */
    }
    
    /**
     * Called before the application is closed down.
     */
    public void destroy() {
        // nothing to do
    }
    
    /**
     * Creates a context for the templates.
     * 
     * @param request The user's http request
     * @param response The user's http response
     * 
     * @return the context for the templates
     */
    public Object createContext(ApplicationRequest request,
                                ApplicationResponse response) {
        
        return new CookieContext(
            request, response, mDomain, mPath, mIsSecure);
    }
    
    /**
     * The class of the object that the createContext method will return.
     * 
     * @return the class that the createContext method will return
     */
    public Class<?> getContextType() {
        return CookieContext.class;
    }

    /**
     * Custom context that gets and decodes cookies as well as sets and encodes 
     * cookies.
     *
     * @author Scott Jappinen
     */
    public class CookieContext {

        private HttpServletResponse mResponse;
        private HttpServletRequest mRequest;
        private String mDomain;
        private String mPath;
        private boolean mIsSecure;

        /**
         * Create a context using the given request/response and the given
         * cookie settings.
         * 
         * @param request The current request
         * @param response The current response
         * @param domain The domain to encode cookies with
         * @param path The path to set within cookies
         * @param isSecure Whether to use secure cookies or not
         */
        public CookieContext(HttpServletRequest request,
                             HttpServletResponse response,
                             String domain, String path, boolean isSecure) {
            mResponse = response;
            mRequest =  request;
            mDomain = domain;
            mPath = path;
            mIsSecure = isSecure;
        }

        /**
         * Get the cookie with the given name.  This will return the cookie
         * within the current request with the matching name. If no matching
         * cookie is found, <code>null</code> will be returned. If the 
         * {@link CookieApplication} was preconfigured with a domain and path, 
         * then the cookie domain and path will be replaced with those values.
         * 
         * @param name The name of the cookie
         * 
         * @return The matching cookie or <code>null</code>
         * 
         * @see #getCookie(String, boolean)
         * @see CookieApplication#init(ApplicationConfig)
         */
        public Cookie getCookie(String name) {
            return getCookie(name, false);
        }
        
        /**
         * Get the cookie with the given name.  This will return the cookie
         * within the current request with the matching name. If no matching
         * cookie is found, <code>null</code> will be returned. If a cookie is
         * found and <code>isEncoded</code> is <code>true</code>, then the
         * value of the cookie will base64 decoded into a binary stream.  If
         * the {@link CookieApplication} was preconfigured with a domain and
         * path, then the cookie domain and path will be replaced with those
         * values.
         * 
         * @param name The name of the cookie
         * @param isEncoded Whether the cookie value is encoded or not
         * 
         * @return The matching cookie or <code>null</code>
         * 
         * @see Base64#decodeBase64(String)
         * @see #setCookie(String, String, int, boolean)
         * @see CookieApplication#init(ApplicationConfig)
         */
        public Cookie getCookie(String name, boolean isEncoded) {
            // validate data
            if (name == null || name.isEmpty()) {
                return null;
            }
            
            Cookie result = null;
            String value = null;
            Cookie[] cookies = mRequest.getCookies();
            if (cookies != null) {
    	        for (int i = 0; i < cookies.length; i++) {
    	            if (name.equals(cookies[i].getName().trim())) {
    	                // retrieve and set the value
    	                result = cookies[i];
    	                value = result.getValue();
    	                if (value != null && isEncoded) {
	                        String decodedValue = 
	                            new String(Base64.decodeBase64(value));
	                        result.setValue(decodedValue);
    	                }
    	                
    	                // forcefully set the domain if configured
	                    if (mDomain != null) {
	                        result.setDomain(mDomain);
	                    }
    	                
    	                // forcefully set the path if configured
	                    if (mPath != null) {
	                        result.setPath(mPath);
	                    }
    	                
    	                // success
    	                break;
    	            }
    	        }
            }
            
            return result;
        }

        /**
         * Create a cookie without setting the cookie in the response. This
         * method allows the cookie to be changed before setting in the
         * response.  Use {@link #setCookie(Cookie)} to actually set the cookie
         * in the response. The cookie will use the preconfigured domain and
         * path if provided during application initialization. The given age 
         * will control the expiry time, in seconds, of the cookie. A negative 
         * value will cause the cookie to be a session-cookie and automatically
         * delete when the browser session is closed. A 0 value will cause the
         * cookie to be deleted.
         * 
         * @param name The name of the cookie
         * @param value The binary data of the cookie
         * @param age The expiry time of the cookie, in seconds
         * 
         * @return The initialized cookie
         * 
         * @see #setCookie(String, String, int)
         * @see #createCookie(String, String, int, boolean)
         */
        public Cookie createCookie(String name, String value, int age) {
            return createCookie(name, value, age, false);
        }
        
        /**
         * Create a cookie without setting the cookie in the response. This
         * method allows the cookie to be changed before setting in the
         * response.  Use {@link #setCookie(Cookie)} to actually set the cookie
         * in the response. The cookie will use the preconfigured domain and
         * path if provided. The given binary value will be Base64 encoded. The 
         * given age will control the expiry time, in seconds, of the cookie. A 
         * negative value will cause the cookie to be a session-cookie and 
         * automatically delete when the browser session is closed. A 0 value 
         * will cause the cookie to be deleted.
         * 
         * @param name The name of the cookie
         * @param value The binary data of the cookie
         * @param age The expiry time of the cookie, in seconds
         * 
         * @return The initialized cookie
         * 
         * @see #setCookie(String, byte[], int)
         * @see #createCookie(String, String, int, boolean)
         */
        public Cookie createCookie(String name, byte[] value, int age) {
            String encodedValue = null;
            if (value != null) {
                encodedValue = Base64.encodeBase64String(value); 
            }
            
            return createCookie(name, encodedValue, age, false);
        }

        /**
         * Create a cookie without setting the cookie in the response. This
         * method allows the cookie to be changed before setting in the
         * response.  Use {@link #setCookie(Cookie)} to actually set the cookie
         * in the response. The cookie will use the preconfigured domain and
         * path if provided. If <code>isEncoded</code> is <code>true</code>,
         * then the value will be Base64 encoded before being set as the value.
         * The given age will control the expiry time, in seconds, of the 
         * cookie. A negative value will cause the cookie to be a session-cookie 
         * and automatically delete when the browser session is closed. A 0 
         * value will cause the cookie to be deleted.
         * 
         * @param name The name of the cookie
         * @param value The value of the cookie
         * @param age The expiry time of the cookie, in seconds
         * @param isEncoded Whether to Base64 encode the value or not
         * 
         * @return The initialized cookie
         * 
         * @see #setCookie(String, String, int, boolean)
         */
        public Cookie createCookie(String name, String value, int age,
                                   boolean isEncoded) {
            return createCookie(name, value, age, mDomain, mPath, isEncoded);
        }

        /**
         * Create a cookie without setting the cookie in the response. This
         * method allows the cookie to be changed before setting in the
         * response.  Use {@link #setCookie(Cookie)} to actually set the cookie
         * in the response. The given age will control the expiry time, in 
         * seconds, of the cookie. A negative value will cause the cookie to be 
         * a session-cookie and automatically delete when the browser session is 
         * closed. A 0 value will cause the cookie to be deleted.
         * 
         * @param name The name of the cookie
         * @param value The value of the cookie
         * @param age The expiry time of the cookie, in seconds
         * @param domain The domain of the cookie
         * @param path The path of the cookie
         * 
         * @return The initialized cookie
         * 
         * @see #setCookie(String, String, int, String, String, boolean)
         */
        public Cookie createCookie(String name, String value, int age, 
                                   String domain, String path) {
            return createCookie(name, value, age, domain, path, false);
        }
        
        /**
         * Create a cookie without setting the cookie in the response. This
         * method allows the cookie to be changed before setting in the
         * response.  Use {@link #setCookie(Cookie)} to actually set the cookie
         * in the response. The given binary value will be Base64 encoded. The 
         * given age will control the expiry time, in seconds, of the cookie. A 
         * negative value will cause the cookie to be a session-cookie and 
         * automatically delete when the browser session is closed. A 0 value 
         * will cause the cookie to be deleted.
         * 
         * @param name The name of the cookie
         * @param value The binary value of the cookie
         * @param age The expiry time of the cookie, in seconds
         * @param domain The domain of the cookie
         * @param path The path of the cookie
         * 
         * @return The initialized cookie
         * 
         * @see #setCookie(String, String, int, String, String, boolean)
         */
        public Cookie createCookie(String name, byte[] value, int age, 
                                   String domain, String path) {
            String encodedValue = null;
            if (value != null) {
                encodedValue = Base64.encodeBase64String(value); 
            }
            
            return createCookie(name, encodedValue, age, domain, path, false);
        }
        
        /**
         * Create a cookie without setting the cookie in the response. This
         * method allows the cookie to be changed before setting in the
         * response.  Use {@link #setCookie(Cookie)} to actually set the cookie
         * in the response. If <code>isEncoded</code> is <code>true</code>,
         * then the value will be Base64 encoded before being set as the value.
         * The given age will control the expiry time, in seconds, of the 
         * cookie. A negative value will cause the cookie to be a session-cookie 
         * and automatically delete when the browser session is closed. A 0 
         * value will cause the cookie to be deleted.
         * 
         * @param name The name of the cookie
         * @param value The value of the cookie
         * @param age The expiry time of the cookie, in seconds
         * @param domain The domain of the cookie
         * @param path The path of the cookie
         * @param isEncoded Whether to Base64 encode the value or not
         * 
         * @return The initialized cookie
         * 
         * @see #setCookie(String, String, int, String, String, boolean)
         */
        public Cookie createCookie(String name, String value, int age, 
                                   String domain, String path, 
                                   boolean isEncoded) {
            Cookie oreo;
            if (isEncoded) {
                String encodedValue = 
                    Base64.encodeBase64String(value.getBytes());
                oreo = new Cookie(name, encodedValue);
            } else {
                oreo = new Cookie(name, value);
            }
            
            if (domain == null || domain.trim().isEmpty()){
                domain = mDomain;
            }
            
            if (path == null || path.trim().isEmpty()) {
                path = mPath;
            }
            
            oreo.setMaxAge(age);
            oreo.setDomain(domain);
            oreo.setPath(path);
            oreo.setSecure(mIsSecure);
            
            return oreo;
        }
        
        /**
         * Set the given cookie into the response so that it will be added to
         * the user's browser with the response to the current request.
         * 
         * @param cookie The cookie to set
         * 
         * @see #createCookie(String, String, int)
         */
        public void setCookie(Cookie oreo) {
            mResponse.addCookie(oreo);
        }
        
        /**
         * Create and set a cookie into the current response that will be added
         * to the user's browser with the response to the current request. The
         * domain and path will be automatically set to the pre-defined values
         * the {@link CookieApplication} was configured with. The given age will
         * control the expiry time, in seconds, of the cookie. A negative value
         * will cause the cookie to be a session-cookie and automatically
         * delete when the browser session is closed. A 0 value will cause the
         * cookie to be deleted.
         * 
         * @param name The name of to give to the cookie
         * @param value The value to include in the cookie
         * @param age The expiry time, in seconds, for the cookie
         * 
         * @see #setCookie(String, String, int, String, String, boolean)
         * @see CookieApplication#init(ApplicationConfig)
         */
        public void setCookie(String name, String value, int age) {
            setCookie(createCookie(name, value, age));
        }
        
        /**
         * Create and set a cookie into the current response that will be added
         * to the user's browser with the response to the current request. The
         * domain and path will be automatically set to the pre-defined values
         * the {@link CookieApplication} was configured with. The given binary 
         * value will be Base64 encoded. The given age will control the expiry 
         * time, in seconds, of the cookie. A negative value will cause the 
         * cookie to be a session-cookie and automatically delete when the 
         * browser session is closed. A 0 value will cause the cookie to be 
         * deleted.
         * 
         * @param name The name of to give to the cookie
         * @param value The binary value to include in the cookie
         * @param age The expiry time, in seconds, for the cookie
         * 
         * @see #setCookie(String, String, int, String, String, boolean)
         * @see CookieApplication#init(ApplicationConfig)
         */
        public void setCookie(String name, byte[] value, int age) {
            setCookie(createCookie(name, value, age));
        }
        
        /**
         * Create and set a cookie into the current response that will be added
         * to the user's browser with the response to the current request. The
         * domain and path will be automatically set to the pre-defined values
         * the {@link CookieApplication} was configured with. If the given
         * <code>isEncoded</code> value is <code>true</code>, then the given
         * value will be Base64 encoded first. The given age will control the 
         * expiry time, in seconds, of the cookie. A negative value will cause 
         * the cookie to be a session-cookie and automatically delete when the 
         * browser session is closed. A 0 value will cause the cookie to be 
         * deleted.
         * 
         * @param name The name of to give to the cookie
         * @param value The value to include in the cookie
         * @param age The expiry time, in seconds, for the cookie
         * @param isEncoded Whether to Base64 encode the value 
         * 
         * @see #setCookie(String, String, int, String, String, boolean)
         * @see CookieApplication#init(ApplicationConfig)
         */
        public void setCookie(String name, String value,
                              int age, boolean isEncoded) {
            setCookie(createCookie(name, value, age, isEncoded));
        }

        /**
         * Create and set a cookie into the current response that will be added
         * to the user's browser with the response to the current request. The 
         * given age will control the expiry time, in seconds, of the cookie. A 
         * negative value will cause the cookie to be a session-cookie and 
         * automatically delete when the browser session is closed. A 0 value 
         * will cause the cookie to be deleted.
         * 
         * @param name The name of to give to the cookie
         * @param value The value to include in the cookie
         * @param age The expiry time, in seconds, for the cookie
         * @param domain The configured domain for the cookie
         * @param path The configured path for the cookie
         */
        public void setCookie(String name, String value, int age, String domain, 
                              String path) {
            setCookie(createCookie(name, value, age, domain, path));
        }
        
        /**
         * Create and set a cookie into the current response that will be added
         * to the user's browser with the response to the current request. The
         * binary value will be Base64 encoded first. The given age will control 
         * the expiry time, in seconds, of the cookie. A negative value will 
         * cause the cookie to be a session-cookie and automatically delete when 
         * the browser session is closed. A 0 value will cause the cookie to be 
         * deleted.
         * 
         * @param name The name of to give to the cookie
         * @param value The binary value to include in the cookie
         * @param age The expiry time, in seconds, for the cookie
         * @param domain The configured domain for the cookie
         * @param path The configured path for the cookie
         */
        public void setCookie(String name, byte[] value, int age, String domain, 
                              String path) {
            setCookie(createCookie(name, value, age, domain, path));
        }
        
        /**
         * Create and set a cookie into the current response that will be added
         * to the user's browser with the response to the current request. If 
         * the given <code>isEncoded</code> value is <code>true</code>, then the 
         * given value will be Base64 encoded first. The given age will control 
         * the expiry time, in seconds, of the cookie. A negative value will 
         * cause the cookie to be a session-cookie and automatically delete when 
         * the browser session is closed. A 0 value will cause the cookie to be 
         * deleted.
         * 
         * @param name The name of to give to the cookie
         * @param value The value to include in the cookie
         * @param age The expiry time, in seconds, for the cookie
         * @param domain The configured domain for the cookie
         * @param path The configured path for the cookie
         * @param isEncoded Whether to Base64 encode the value 
         */
        public void setCookie(String name, String value, int age, String domain, 
                              String path, boolean isEncoded) {
            setCookie(createCookie(name, value, age, domain, path, isEncoded));
        }

        /**
         * Clear the cookie with the given name. This will use the preconfigured
         * domain and path provided during application initialization. The
         * cookie will be set in the brower with an expiry time of 0 to cause
         * the browser to clear the cookie.
         * 
         * @param name The name of the cookie to clear
         */
        public void clearCookie(String name) {
            // set expiry time to 0 to clear
            setCookie(name, "", 0);
        }

        /**
         * Clear the cookie with the given name, domain, and path. The cookie 
         * will be set in the brower with an expiry time of 0 to cause the 
         * browser to clear the cookie.
         * 
         * @param name The name of the cookie to clear
         * @param domain The domain of the cookie
         * @param path The path of the cookie
         */
        public void clearCookie(String name, String domain, String path) {
            // set expiry time to 0 to clear
            setCookie(name, "", 0, domain, path);
        }
    }
}
