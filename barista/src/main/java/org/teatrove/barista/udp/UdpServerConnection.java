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

import java.util.Map;
import java.net.DatagramSocket;
import java.net.DatagramPacket;

/**
 * Simple interface to the server-side implementation of an UDP connection.
 * 
 * @author tammy wang
 */
public interface UdpServerConnection {
        
       
     
    /**
     * Returns the undecoded request URI, complete with optional query
     * string and scheme. The value returned from this method returns the raw
     * request URI, and this value differs from the getRequestURI as provided
     * by the Servlet API. Call getRequestPath to get the value that matches
     * the Servlet API.
     */
    public String getRequestURI();
    
    /**
     * Returns the undecoded path from the request URI. Any query string is
     * stripped off and absolute URIs are converted to relative. The value
     * returned from this method matches the value returned by getRequestURI
     * in the Servlet API.
     */
    public String getRequestPath();
    
    /**
     * Returns the undecoded request query string, which is the part of the
     * URI that follows the optional '?'. Null is returned if the URI does
     * not end in '?'.
     */
    public String getRequestQueryString();
    
    /**
     * Returns a map of decoded request parameters. Keys are strings,
     * and values may be strings or string arrays. The map entries are not
     * stored in any particular order.
     * <p>
     * The request parameters do not initially include any that came from a
     * POST. The returned map is modifiable so that POSTed parameters may be
     * added later.
     */
    public Map getRequestParameters();
    
 
   
    /**
     * After the response is committed, changes to status code and response
     * headers will not affect the actual response. The response is usually
     * committed as soon as response output is provided.
     */
    public boolean isResponseCommitted();

    
   

    /**
     * Returns the socket used by this UdpServerConnection. The I/O streams in
     * the returned socket are designed to work with the UDP protocol. They
     * are not the actual raw socket I/O streams.
     * <p>
     * The InputStream provides access to additional request data, like from a
     * POST. If content length is provided by the request, the InputStream
     * returns EOF when all of the content has been read. Closing the request
     * input always closes down the socket connection.
     * <p>
     * The OutputStream is used to write the response body. As soon as data is
     * first written to the OutputStream, the response is committed. If no
     * status code has been set when the response is committed, an
     * IllegalStateException is thrown from the OutputStream write method.
     * <p>
     * If one of the response headers provides content length, any output that
     * is written beyond that length is discarded. If less bytes are written,
     * then closing the OutputStream will always close the socket.
     * <p>
     * Closing the response output does necessarily close the socket
     * connection. The socket is flushed, but it is kept if all the criteria is
     * met for supporting persistent connections. Closing the response output
     * is preferred over closing the socket because it allows the socket to be
     * recycled.
     */
    public DatagramSocket getSocket();
	
	public DatagramPacket getResponsePacket();
	
	public DatagramPacket getRequestPacket();
	

	
	public void setRequestPacket(DatagramPacket requestPacket);


    /**
     * Returns a modifiable map of user-defined attributes. Although not
     * restricted, attribute map keys should be strings, and they should follow
     * package naming conventions.
     */
    public Map getAttributeMap();
	
	/**
	 *Returns the length of the response
	 *
	 */
	public int getReponseLen();
}
