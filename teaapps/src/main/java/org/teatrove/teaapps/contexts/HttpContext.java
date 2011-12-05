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
package org.teatrove.teaapps.contexts;

import org.teatrove.trove.net.HttpClient;
import org.teatrove.trove.net.PlainSocketFactory;
import org.teatrove.trove.net.SocketFactory;
import org.teatrove.trove.net.SSLSocketFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Map;
import java.util.Iterator;

/**
 * @author Scott Jappinen
 */
public class HttpContext {
    
    public String doGet(String host, String path, 
                        int port, Map headers, int timeout)
        throws UnknownHostException, ConnectException, IOException
    {
        return doHttpCall(host, path, null, port, headers, timeout, false);
    }
    
    public String doSecureGet(String host, String path, 
                              int port, Map headers, int timeout)
        throws UnknownHostException, ConnectException, IOException
    {
        return doHttpCall(host, path, null, port, headers, timeout, true);
    }
    
    public String doPost(String host, String path, String postData, 
                         int port, Map headers, int timeout)
        throws UnknownHostException, ConnectException, IOException
    {
        return doHttpCall(host, path, postData, port, headers, timeout, false);
    }
    
    public String doSecurePost(String host, String path, String postData,
                               int port, Map headers, int timeout)
        throws UnknownHostException, ConnectException, IOException
    {
        return doHttpCall(host, path, postData, port, headers, timeout, true);
    }
    
    private String doHttpCall(String host, String path,  String postData,
                              int port, Map headers, int timeout, 
                              boolean isSecure)
        throws UnknownHostException, ConnectException, IOException
    { 
        String result = null;
        InetAddress inetAddress = InetAddress.getByName(host);
        SocketFactory socketFactory;
        if (isSecure) {
            socketFactory = getSecureSocketFactory
                (inetAddress, port, timeout);
        } else {
            socketFactory = getSocketFactory(inetAddress, port, timeout);               
        }
        HttpClient httpClient = new HttpClient(socketFactory);
        httpClient.setURI(path);
        if (headers != null && headers.size() > 0) {
            Iterator iter = headers.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                httpClient.addHeader(key, headers.get(key));
            }
        }
        HttpClient.Response response;
        if (postData != null) {
            httpClient.setMethod("POST");
            httpClient.preparePost(postData.getBytes().length);
            HttpClient.PostData pd = new PostData(postData);
            response = httpClient.getResponse(pd);
        } else {
            response = httpClient.getResponse();
        }
        Reader in = new InputStreamReader(response.getInputStream());
        StringBuffer buffer = new StringBuffer(1024);
        char[] charBuffer = new char[1024];
        int count;
        while ((count = in.read(charBuffer, 0, 1024)) > 0) {
            buffer.append(charBuffer, 0, count);
        }
        result = buffer.toString();
        return result;
    }   
    
    private SocketFactory getSocketFactory(InetAddress inetAddress,
                                           int port,  int timeout)
    {
        SocketFactory result;
        if (port == -1) {
            port = 80;
        }
        result = new PlainSocketFactory(inetAddress, port, timeout);
        return result;
    }

    private SocketFactory getSecureSocketFactory(InetAddress inetAddress,
                                                 int port,  int timeout)
    {
        SocketFactory result;
        if (port == -1) {
            port = 443;
        }
        result = new SSLSocketFactory(inetAddress, port, timeout);
        return result;
    }
    
    private class PostData implements HttpClient.PostData {
        private String mPostData;
        
        public PostData(String data) {
            mPostData = data;   
        }
        
        public InputStream getInputStream() {
            return new ByteArrayInputStream(mPostData.getBytes());
        }
    }   
}
