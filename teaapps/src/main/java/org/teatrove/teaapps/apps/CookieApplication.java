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
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.teatrove.teaservlet.Application;
import org.teatrove.teaservlet.ApplicationConfig;
import org.teatrove.teaservlet.ApplicationRequest;
import org.teatrove.teaservlet.ApplicationResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

import org.apache.commons.codec.binary.Base64;

import javax.servlet.ServletException;

/**
 * @author Scott Jappinen
 */
public class CookieApplication implements Application {
    
    private String mDomain;
    private String mPath;
    private boolean mIsSecure;
    
    /**
     * Initializes the application.
     * @param config an ApplicationConfig object containing config. info.
     */
    public void init(ApplicationConfig config) throws ServletException {
        mDomain = config.getProperties().getString("domain");
        mPath = config.getProperties().getString("path");
        mIsSecure = config.getProperties().getBoolean("isSecure", false);
        
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
    public void destroy() {}
    
    /**
     * Creates a context for the templates.
     * @param request The user's http request
     * @param response The user's http response
     * @return the context for the templates
     */
    public Object createContext(ApplicationRequest request,
                                ApplicationResponse response) {
        return new CookieContext
            (request, response, mDomain, mPath, mIsSecure);
    }
    
    /**
     * The class of the object that the createContext method will return.
     * @return the class that the createContext method will return
     */
    public Class getContextType() {
        return CookieContext.class;
    }
    


    /**
     * Gets and decodes cookies as well as sets and encodes a cookies.
     *
     * @author Scott Jappinen
     */
    public class CookieContext {

        private HttpServletResponse mResponse;
        private HttpServletRequest mRequest;
        private String mDomain;
        private String mPath;
        private boolean mIsSecure;

        public CookieContext(HttpServletRequest request,
                                 HttpServletResponse response,
                                 String domain,
                                 String path,
                                 boolean isSecure)
        {
            mResponse = response;
            mRequest =  request;
            mDomain = domain;
            mPath = path;
            mIsSecure = isSecure;
        }

        public Cookie getCookie(String name, boolean isEncoded) {
            Cookie result = null;
            String value = null;
            Cookie[] cookies = mRequest.getCookies();
            if (cookies != null) {
    	        for (int i = 0; i < cookies.length; i++) {
    	            if (cookies[i].getName().trim().equals(name)) {
    	                value = cookies[i].getValue();
    	                if (value != null) {
    	                    if (isEncoded) {
    	                        String decodedValue = new String(Base64.decodeBase64(value));
    	                        result = new Cookie(name, decodedValue);
    	                    } else {
    	                        result = cookies[i];
    	                    }
    	                    result.setDomain(mDomain);
    	                    result.setPath(mPath);
    	                    result.setMaxAge(-1);
    	                    break;
    	                }
    	            }
    	        }
            }
            return result;
        }

        public void setCookie(String name, String value,
                              int age, boolean isEncoded) {
            Cookie oreo;
            if (isEncoded) {
                String encodedValue = Base64.encodeBase64String(value.getBytes());
                oreo = new Cookie(name, encodedValue);
            } else {
                oreo = new Cookie(name, value);
            }
            oreo.setMaxAge(age);
            oreo.setDomain(mDomain);
            //oreo.setComment("a comment");
            oreo.setPath(mPath);
            oreo.setSecure(mIsSecure);
            mResponse.addCookie(oreo);
        }

        public void setCookie(String name, String value,
                              int age, String domain, String path, boolean isEncoded) {
            if (domain == null || domain.trim().equals("")){
                domain = mDomain;
            }
            if(path == null || path.trim().equals("")) {
                path = mPath;
            }

            Cookie oreo;
            if (isEncoded) {
                String encodedValue = Base64.encodeBase64String(value.getBytes());
                oreo = new Cookie(name, encodedValue);
            } else {
                oreo = new Cookie(name, value);
            }
            oreo.setMaxAge(age);
            oreo.setDomain(domain);
            //oreo.setComment("a comment");
            oreo.setPath(path);
            oreo.setSecure(mIsSecure);
            mResponse.addCookie(oreo);
        }

        public void clearCookie(String name) {
            Cookie oreo = new Cookie(name, "");
            oreo.setMaxAge(0); //cookie deletes itself
            oreo.setDomain(mDomain);
            oreo.setPath(mPath);
            mResponse.addCookie(oreo);
        }

        public void clearCookie(String name, String domain, String path) {
            if (domain == null || domain.trim().equals("")){
                domain = mDomain;
            }
            if(path == null || path.trim().equals("")) {
                path = mPath;
            }

            Cookie oreo = new Cookie(name, "");
            oreo.setMaxAge(0); //cookie deletes itself
            oreo.setDomain(domain);
            oreo.setPath(path);
            mResponse.addCookie(oreo);
        }
    }
}
