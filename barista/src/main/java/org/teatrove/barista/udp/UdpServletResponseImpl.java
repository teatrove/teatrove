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

/**
 * Response implementation that wraps an ServletResponse.
 *
 * @author Tammy Wang
 */
public class UdpServletResponseImpl implements ServletResponse {
    private static final int NONE = 0, STREAM = 1, WRITER = 2;
    
    private UdpServerConnection mServerConnection;  
    private Map mCharsetAliases;
    
    private BufferedOutputStream mRawStream;
    private ServletOutputStream mStream;
    private ResettableWriter mWriter;


	private final DatagramPacket mResponsePacket;
    // True if mStream or mWriter have data to be committed.
    boolean mShouldCommit;

    private int mOutputType = NONE;
	private int mBufferSize = 1024;//default to 1K

    public UdpServletResponseImpl(UdpServerConnection udpCon) {
        this(udpCon, null);
    }

    public UdpServletResponseImpl(UdpServerConnection udpCon,
                                   Map charsetAliases) {
        mServerConnection = udpCon;
        mCharsetAliases = charsetAliases;
		mResponsePacket = mServerConnection.getResponsePacket();
		mShouldCommit = false;
		mBufferSize = udpCon.getReponseLen();
		mStream = new ServletOutput();
    }

    // ServletResponse methods:
    
    public String getCharacterEncoding() {
        
        return null;
    }
    
  

    public PrintWriter getWriter() throws IOException {
             

        return null;
    }

    public void setContentLength(int length) {
        mResponsePacket.setLength(length);
	
    }

    public void setContentType(String type) {
 
    }

    public void setBufferSize(int size) {
        commitCheck();
        if (size > mBufferSize) {
            mBufferSize = size;
           
            resetBuffer();
        }
    }

    public int getBufferSize() {
        return mBufferSize;
    }    
    
    public void flushBuffer() throws IOException {
        if (mStream != null) {
            if (mWriter != null) {
                mWriter.flush();
            }
            mStream.flush();
        }
        else if (mWriter != null) {
            mWriter.flush();
        }
        
    }

    public void resetBuffer() {
        commitCheck();
        
    }

    public boolean isCommitted() {
        return mServerConnection.isResponseCommitted();
    } 

    public void reset() throws IllegalStateException {
        resetBuffer();
        
        mOutputType = NONE;
    }

    public void setLocale(Locale locale) {
        
    }
	
	public ServletOutputStream getOutputStream() throws IOException {
 
        if (mStream == null) {
            mStream = new ServletOutput();
        }

        return mStream;
		
    }

    public Locale getLocale() {
            return null;
    }
    /**
     * @deprecated use encodeURL
     */
    public String encodeUrl(String url) {
        return null;
    }

    /**
     * @deprecated use encodeRedirectUrl
     */
    public String encodeRedirectUrl(String url) {
        return null;
    }
   
    // Useful methods not in the Servlet API.

    public void resetOutputType() {
        mOutputType = NONE;
    }

   public void close() throws IOException {
        if (mStream != null) {
            if (mWriter != null) {
                mWriter.close();
                mWriter = null;
            }
            mStream.close();
            mStream = null;
        }
        else if (mWriter != null) {
            mWriter.close();
            mWriter = null;
        }
       
        
        mOutputType = NONE;
    }

    void commitCheck() throws IllegalStateException {
        if (isCommitted()) {
            throw new IllegalStateException
                ("Response has been committed: "); 
        }
    }

    private class ResettableWriter extends PrintWriter {
        ResettableWriter(Writer writer) {
            super(writer);
        }

        ResettableWriter(Writer writer, boolean autoFlush) {
            super(writer, autoFlush);
        }

        public void write(int c) {
            mShouldCommit = true;
            super.write(c);
        }

        public void write(char[] cbuf) {
            write(cbuf, 0, cbuf.length);
        }

        public void write(char[] cbuf, int off, int len) {
            mShouldCommit = true;
            super.write(cbuf, off, len);
        }

        public void write(String str) {
            mShouldCommit = true;
            super.write(str);
        }

        public void write(String str, int off, int len) {
            mShouldCommit = true;
            super.write(str, off, len);
        }

        void resetWriter() throws IOException {
            String enc = getCharacterEncoding();
        }
    }

    private class ServletOutput extends ServletOutputStream {
        private OutputStream mOutput;
		private byte[] mBuffer; 

        ServletOutput(OutputStream out) {
            mOutput = out;
        }
		
		ServletOutput()
		{
			mShouldCommit = false;
			//mBuffer = new byte[mBufferSize];
			
		}

        public void write(int b) throws IOException {
            mShouldCommit = true;
       
			Integer itg = new Integer(b);
			String stritg = itg.toString();
			mBuffer = stritg.getBytes();
			mResponsePacket.setData(mBuffer);

			
        }

        public void write(byte[] b) throws IOException {
            mShouldCommit = true;
			mBuffer = b;
			mResponsePacket.setData(mBuffer);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            mShouldCommit = true;
			String str = new String(b, off, len);
			mBuffer = str.getBytes();
			mResponsePacket.setData(mBuffer);
        }

        // Override default print method to not throw CharConversionExceptions.
        public void print(String str) throws IOException {
            if (str == null) {
                str = "null";
            }
            //write(str.getBytes("ISO-8859-1"));
			mBuffer = str.getBytes();
			mResponsePacket.setData(mBuffer);
        }

        public void flush() throws IOException {
            
			mResponsePacket.setData(mBuffer);
			mServerConnection.getSocket().send(mResponsePacket);
        }

        public void close() throws IOException {
            
			
        }
    }
}
