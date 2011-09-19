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

/** 
 * @author Tammy Wang
 */
public class UdpServerConnectionImpl implements UdpServerConnection {
    private static final byte[] CRLF = {'\r', '\n'};
	private static final int RESPONSE_LEN = 1024;
    
    private final DatagramSocket mSocket;
    private final Recycler mRecycler;
    private final long mStartTime;
    private final String mURI;
    private final String mPath;
    private final String mQuery;
	private  DatagramPacket mRequestPacket;
    
    private Map mParameters;
    private Map mAttributes;

    private final DatagramPacket mResponsePacket;
    private boolean mResponseCommitted;
    /**
     * @param socket Buffered socket whose request has not yet been read from.
     * @param recycler Gets the socket back when response is complete and if
     * socket is persistent.
     * 
     */
    public UdpServerConnectionImpl(DatagramSocket socket,
                                    Recycler recycler)
        throws IOException, ProtocolException
    {
        mRecycler = recycler;
		byte[] buff = new byte[RESPONSE_LEN];
		mResponseCommitted = false;
		
        

        // Record start time immediately after the start of the request has
        // been read. For impression logging, time recorded almost represents
        // total time to process transaction. Don't record start time before
        // reading first line because it may take some time for it to be read,
        // especially if socket is persistent.	
        mStartTime = System.currentTimeMillis();

        mRequestPacket = new DatagramPacket(buff,buff.length);
		
		
		String uri = new String();
		String path = new String();
	
		
		if (mRequestPacket!=null)
		{
		 	
		 	uri = new String(mRequestPacket.getData());
			//System.out.println(uri);
		    
			// Now separate the query string from the uri
		     int  index = uri.indexOf('?');
		     
		      if (index < 0) {	   
		          path = uri;
		          mQuery = null;
		      }
		      else {
		          path = uri.substring(0, index);
		          if (index + 1 < uri.length()) {
		              mQuery = uri.substring(index + 1);
		          }
		          else {
		              mQuery = "";
		          }
		      }
		
		}else
		{
			mQuery = null;
		}
		mPath = path;
		mURI = uri;
        mResponsePacket = new DatagramPacket(buff,buff.length);
       
		mSocket = socket;						
    }
     /**
     * @param socket Buffered socket whose request has not yet been read from.
     * @param recycler Gets the socket back when response is complete and if
     * socket is persistent.
     * 
     */
    public UdpServerConnectionImpl(DatagramSocket socket,
									DatagramPacket packet,
                                    Recycler recycler)
        throws IOException, ProtocolException
    {
        mRecycler = recycler;
		byte[] buff = new byte[RESPONSE_LEN];
		mResponseCommitted = false;
		
        

        // Record start time immediately after the start of the request has
        // been read. For impression logging, time recorded almost represents
        // total time to process transaction. Don't record start time before
        // reading first line because it may take some time for it to be read,
        // especially if socket is persistent.	
        mStartTime = System.currentTimeMillis();

        mRequestPacket = packet;
				
		String uri = new String();
		String path = new String();
	
		
		if (mRequestPacket!=null)
		{
		 	
		 	uri = new String(mRequestPacket.getData());
		    
			// Now separate the query string from the uri
		     int  index = uri.indexOf('?');
		     
		      if (index < 0) {
		          path = uri;
		          mQuery = null;
		      }
		      else {
		          path = uri.substring(0, index);
		          if (index + 1 < uri.length()) {
		              mQuery = uri.substring(index + 1);
		          }
		          else {
		              mQuery = "";
		          }
		      }
		
		}else
		{
			mQuery = null;
		}
		mPath = path;
		mURI = uri; 
        mResponsePacket = new DatagramPacket(buff,buff.length,mRequestPacket.getAddress(), mRequestPacket.getPort());
       
   
		mSocket = socket;		
		Map attributemap = getAttributeMap();
		String urikey = new String("uri");
		attributemap.put(urikey,mURI); 				
    }
 
  
    
    public DatagramPacket getRequestPacket() {
        return mRequestPacket;
    }
	
	
	public int getReponseLen() {
        return RESPONSE_LEN;
    }
	public void setRequestPacket(DatagramPacket requestPacket) {
         mRequestPacket = requestPacket;
    }

	public String getRequestURI() {
        return mURI;
    }

    public String getRequestPath() {
        return mPath;
    }

    public String getRequestQueryString() {
        return mQuery;
    }
  
    /*public boolean isResponseCommitted() {
        return mSocket.mOut.isResponseCommitted();
    }*/

    public DatagramPacket getResponsePacket() {
        return mResponsePacket;
    }
	
	public Map getRequestParameters() {
        if (mParameters == null) {
            mParameters = new HashMap(23);
            Utils.parseQueryString(mQuery, mParameters);
        }
        return mParameters;
    }
    public boolean isResponseCommitted() {
        return mResponseCommitted;
    }
   /*public void setResponseStatus(int code, String message) {
        if (!isResponseCommitted()) {
            mSocket.mStatusCode = code;
            mSocket.mStatusMessage = message;
        }
    }

   
    public int getResponseStatusCode() {
        return mSocket.mStatusCode;
    }

    public String getResponseStatusMessage() {
        return mSocket.mStatusMessage;
    }*/

    public DatagramSocket getSocket() {
        return mSocket;
    }

    /*public long getBytesRead() {
        return mSocket.getBytesRead();
    }

    public long getBytesWritten() {
        return mSocket.getBytesWritten();
    }*/

    public Map getAttributeMap() {
        if (mAttributes == null) {
            mAttributes = new HashMap();
        }
        return mAttributes;
    }

 
    public interface Recycler {
        /**
         * Called when all the criteria is met for persistent sockets and the
         * socket can now be recycled.
         */
        public void recycleSocket(DatagramSocket socket);
    }


}
