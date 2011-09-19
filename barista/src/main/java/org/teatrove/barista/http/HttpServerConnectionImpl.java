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
import org.teatrove.trove.net.*;
import org.teatrove.trove.io.CharToByteBuffer;
import org.teatrove.trove.io.DefaultByteBuffer;
import org.teatrove.trove.io.FastCharToByteBuffer;
import org.teatrove.trove.io.InternedCharToByteBuffer;
import org.teatrove.trove.log.Log;

/**
 * @author Brian S O'Neill
 */
public class HttpServerConnectionImpl implements HttpServerConnection {
    private static final byte[] CRLF = {'\r', '\n'};
    
    private final HttpSocket mSocket;
    private final Recycler mRecycler;
    private final String mScheme;
    private final boolean mSingleCookieMode;
    private final Log mImpressionLog;
    private final long mStartTime;
    private final String mMethod;
    private final String mProtocol;
    private final String mURI;
    private final String mPath;
    private final String mQuery;
    private final HttpHeaderMap mRequestHeaders;
    
    private Map mParameters;
    private Map mCookies;
    private Map mAttributes;

    private final HttpHeaderMap mResponseHeaders;
    private final Map mResponseCookies;

    /**
     * @param socket Buffered socket whose request has not yet been read from.
     * @param recycler Gets the socket back when response is complete and if
     * socket is persistent.
     * @param scheme i.e. http or https
     */
    public HttpServerConnectionImpl(SocketFace socket,
                                    Recycler recycler,
                                    String scheme,
                                    boolean singleCookieMode,
                                    Log impressionLog,
                                    int lineLimit)
        throws IOException, ProtocolException
    {
        mRecycler = recycler;
        mScheme = scheme;
        mSingleCookieMode = singleCookieMode;
        mImpressionLog = impressionLog;

        CountingSocket csocket = new CountingSocket(socket);
        InputStream in = csocket.getInputStream();

        char[] buffer = new char[80];
        String requestStr = HttpUtils.readLine(in, buffer, lineLimit);
        
        if (requestStr == null) {
            throw new SocketException("closed");
        }

        // Record start time immediately after the start of the request has
        // been read. For impression logging, time recorded almost represents
        // total time to process transaction. Don't record start time before
        // reading first line because it may take some time for it to be read,
        // especially if socket is persistent.
        mStartTime = System.currentTimeMillis();

        String uri;

        int index = requestStr.indexOf(' ');
        if (index < 0) {
            throw new ProtocolException("Invalid request line: " + requestStr);
        }

        mMethod = requestStr.substring(0, index);
        
        int index2 = requestStr.indexOf(' ', index + 1);
        if (index2 < 0) {
            mURI = uri = requestStr.substring(index + 1);
            mProtocol = "HTTP/0.9";
        }
        else {
            mURI = uri = requestStr.substring(index + 1, index2);
            
            String ver = requestStr.substring(index2 + 1);
            if (ver.length() > 0) {
                mProtocol = ver;
            }
            else {
                mProtocol = "HTTP/0.9";
            }
        }

        // Now separate the query string from the uri
        index = uri.indexOf('?');
        String path;
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

        mRequestHeaders = new HttpHeaderMap();
        mRequestHeaders.readFrom(in, buffer);

        // Make sure path is relative: no scheme or net path.
        index = path.indexOf('/');
        if (index >= 0) {
            try {
                if (path.charAt(index + 1) == '/') {
                    index2 = path.indexOf('/', index + 2);
                    // Strip out the host from the URI, but save it in the Host
                    // request header unless one is already supplied.
                    if (!mRequestHeaders.containsKey("Host")) {
                        mRequestHeaders.put("Host", path.substring(index + 2, index2));
                    }
                    path = path.substring(index2);
                }
            }
            catch (IndexOutOfBoundsException e) {
            }
        }

        mPath = path;

        mResponseHeaders = new HttpHeaderMap();

        if (mSingleCookieMode) {
            mResponseCookies = new HashMap();
        }
        else {      
            mResponseCookies = null;
        }

        String responseProtocol = mProtocol;
        if (!"HTTP/1.1".equals(responseProtocol)) {
            responseProtocol = "HTTP/1.0";
        }

        mSocket = new HttpSocket(this, csocket, recycler,
                                 mMethod, responseProtocol,
                                 mRequestHeaders, mResponseHeaders,
                                 mResponseCookies);
    }

    public String getRequestScheme() {
        return mScheme;
    }
    
    public String getRequestMethod() {
        return mMethod;
    }

    public String getRequestProtocol() {
        return mProtocol;
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

    public Map getRequestParameters() {
        if (mParameters == null) {
            mParameters = new HashMap(23);
            Utils.parseQueryString(mQuery, mParameters);
        }
        return mParameters;
    }
    
    public HttpHeaderMap getRequestHeaders() {
        return mRequestHeaders;
    }

    public Map getRequestCookies() {
        if (mCookies == null) {
            String[][] fields = Utils.parseHeaderFields
                (getRequestHeaders().getString("Cookie"), ";");

            int length = fields.length;
            Map cookies = new HashMap(fields.length * 2 + 1);

            for (int i=0; i<length; i++) {
                String[] pair = fields[i];
                try {
                    cookies.put(pair[0], pair[1]);
                }
                catch (Exception e) {
                }
            }

            mCookies = cookies;
        }
        return mCookies;
    }

    public boolean isResponseCommitted() {
        return mSocket.mOut.isResponseCommitted();
    }

    public HttpHeaderMap getResponseHeaders() {
        return mResponseHeaders;
    }

    public Map getResponseCookies() {
        return mResponseCookies;
    }

    public void setResponseStatus(int code, String message) {
        if (!isResponseCommitted()) {
            mSocket.mStatusCode = code;
            mSocket.mStatusMessage = message;
        }
    }

    public String getResponseProtocol() {
        if (isResponseCommitted()) {
            return mSocket.mProtocol;
        }
        else {
            return null;
        }
    }
    
    public int getResponseStatusCode() {
        return mSocket.mStatusCode;
    }

    public String getResponseStatusMessage() {
        return mSocket.mStatusMessage;
    }

    public SocketFace getSocket() {
        return mSocket;
    }

    public long getBytesRead() {
        return mSocket.getBytesRead();
    }

    public long getBytesWritten() {
        return mSocket.getBytesWritten();
    }

    public Map getAttributeMap() {
        if (mAttributes == null) {
            mAttributes = new HashMap();
        }
        return mAttributes;
    }

    void logImpression() {
        Log log = mImpressionLog;
        if (log != null) {
            log.logMessage(new Impression(log, this, mStartTime));
        }
    }

    public interface Recycler {
        /**
         * Called when all the criteria is met for persistent sockets and the
         * socket can now be recycled.
         */
        public void recycleSocket(SocketFace socket);
    }

    private static class CountingSocket extends SocketFaceWrapper {
        long mReadCount;
        long mWriteCount;
        
        private InputStream mIn;
        private OutputStream mOut;
        
        CountingSocket(SocketFace socket) throws IOException {
            super(socket);
            mIn = new Input(socket.getInputStream());
            mOut = new Output(socket.getOutputStream());
        }
        
        public InputStream getInputStream() throws IOException {
            return mIn;
        }
        
        public OutputStream getOutputStream() throws IOException {
            return mOut;
        }

        SocketFace getRawSocket() {
            return super.mSocket;
        }
        
        private class Input extends FilterInputStream {
            Input(InputStream in) {
                super(in);
            }
            
            public int read() throws IOException {
                int b = in.read();
                if (b >= 0) {
                    mReadCount++;
                }
                return b;
            }
            
            public int read(byte[] b, int off, int len) throws IOException {
                int amt = in.read(b, off, len);
                if (amt > 0) {
                    mReadCount += amt;
                }
                return amt;
            }
            
            public long skip(long n) throws IOException {
                long amt = in.skip(n);
                if (amt > 0) {
                    mReadCount += amt;
                }
                return amt;
            }
        }
        
        private class Output extends FilterOutputStream {
            Output(OutputStream out) {
                super(out);
            }
            
            public void write(int b) throws IOException {
                out.write(b);
                mWriteCount++;
            }
            
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }

            public void write(byte[] b, int off, int len) throws IOException {
                out.write(b, off, len);
                mWriteCount += len;
            }
        }
    }       

    private static class HttpSocket extends SocketFaceWrapper {
        int mStatusCode = -1;
        String mStatusMessage;
            
        private HttpServerConnectionImpl mCon;
        private Recycler mRecycler;
        private String mMethod;
        private String mProtocol;

        private HttpHeaderMap mRequestHeaders;
        private HttpHeaderMap mResponseHeaders;
        private Map mResponseCookies;

        private RequestInput mIn;
        private ResponseOutput mOut;

        public HttpSocket(HttpServerConnectionImpl con,
                          CountingSocket socket,
                          Recycler recycler,
                          String method,
                          String protocol,
                          HttpHeaderMap requestHeaders,
                          HttpHeaderMap responseHeaders,
                          Map responseCookies)
            throws IOException
        {
            super(socket);

            mCon = con;
            mRecycler = recycler;
            mMethod = method;
            mProtocol = protocol;
            mRequestHeaders = requestHeaders;
            mResponseHeaders = responseHeaders;
            mResponseCookies = responseCookies;

            int limit = -1;
            Integer contentLength =
                requestHeaders.getInteger("Content-Length");
            if (contentLength != null) {
                limit = contentLength.intValue();
            }
            mIn = new RequestInput(socket.getInputStream(), limit);

            mOut = new ResponseOutput(socket.getOutputStream());
        }

        public InputStream getInputStream() {
            return mIn;
        }

        public OutputStream getOutputStream() {
            return mOut;
        }

        public void close() throws IOException {
            try {
                super.close();
            }
            finally {
                logImpression();
            }
        }

        long getBytesRead() {
            return ((CountingSocket)super.mSocket).mReadCount;
        }

        long getBytesWritten() {
            return ((CountingSocket)super.mSocket).mWriteCount;
        }

        SocketFace getRawSocket() {
            return ((CountingSocket)super.mSocket).getRawSocket();
        }
        
        void logImpression() {
            HttpServerConnectionImpl con = mCon;
            mCon = null;
            if (con != null) {
                con.logImpression();
            }
        }

        private class RequestInput extends FilterInputStream {
            private int mLimit;
            private int mMarkedLimit;

            /**
             * @param limit if negative, no limit.
             */
            RequestInput(InputStream in, int limit) {
                super(in);
                mLimit = limit;
            }
            
            public int read() throws IOException {
                int limit = mLimit;
                if (limit == 0) {
                    return -1;
                }
                int c = in.read();
                if (limit > 0) {
                    mLimit = limit - 1;
                }
                return c;
            }
            
            public int read(byte[] b, int off, int len) throws IOException {
                int limit = mLimit;
                if (limit == 0) {
                    return -1;
                }
                if (len > limit && limit > 0) {
                    len = limit;
                }
                int amt = in.read(b, off, len);
                if (limit > 0 && amt > 0) {
                    mLimit = limit - amt;
                }
                return amt;
            }
            
            public long skip(long n) throws IOException {
                int limit = mLimit;
                if (limit == 0) {
                    return 0;
                }
                if (n > limit && limit > 0) {
                    n = limit;
                }
                long amt = in.skip(n);
                if (limit > 0 && amt > 0) {
                    mLimit = limit - (int)amt;
                }
                return amt;
            }
            
            public int available() throws IOException {
                int limit = mLimit;
                if (limit == 0) {
                    return 0;
                }
                int avail = in.available();
                if (avail > limit) {
                    return limit;
                }
                else {
                    return avail;
                }
            }
            
            public void mark(int readlimit) {
                in.mark(readlimit);
                mMarkedLimit = mLimit;
            }
            
            public void reset() throws IOException {
                in.reset();
                mLimit = mMarkedLimit;
            }

            public void close() throws IOException {
                try {
                    super.close();
                }
                finally {
                    logImpression();
                }
            }

            void disable() {
                mLimit = 0;
                mMarkedLimit = 0;
            }
        }

        private class ResponseOutput extends FilterOutputStream {
            private int mLimit = Integer.MIN_VALUE;

            ResponseOutput(OutputStream out) {
                super(out);
            }
            
            public void write(int b) throws IOException {
                int limit = mLimit;
                if (limit == Integer.MIN_VALUE) {
                    commit();
                    limit = mLimit;
                }
                if (limit > 0) {
                    out.write(b);
                    mLimit = limit - 1;
                }
                else if (limit < 0) {
                    out.write(b);
                }
            }
            
            public void write(byte[] b, int off, int len) throws IOException {
                int limit = mLimit;
                if (limit == Integer.MIN_VALUE) {
                    commit();
                    limit = mLimit;
                }
                if (limit > 0) {
                    if (len > limit) {
                        len = limit;
                    }
                    out.write(b, off, len);
                    mLimit = limit - len;
                }
                else if (limit < 0) {
                    out.write(b, off, len);
                }
            }

            public void flush() throws IOException {
                if (!isResponseCommitted()) {
                    commit();
                }
                out.flush();
            }
            
            public void close() throws IOException {
                if (mCon == null) {
                    // Already closed and logged.
                    return;
                }

                try {
                    if (!isResponseCommitted()) {
                        commit();
                    }
                    
                    if (mRecycler == null || mIn.mLimit > 0 || mLimit > 0) {
                        mLimit = 0;
                        out.close();
                        // Ensure this HttpSocket cannot be read anymore.
                        mIn.disable();
                    }
                    else {
                        mLimit = 0;
                        out.flush();
                        // Ensure this HttpSocket cannot be read anymore.
                        mIn.disable();
                        mRecycler.recycleSocket(getRawSocket());
                        mRecycler = null;
                    }
                }
                finally {
                    logImpression();
                }
            }
            
            boolean isResponseCommitted() {
                return mLimit != Integer.MIN_VALUE;
            }

            private void commit() throws IOException {
                if (mStatusCode < 0) {
                    throw new IllegalStateException
                        ("No HTTP status code supplied");
                }
                
                HttpHeaderMap headers = mResponseHeaders;
                Map cookies = mResponseCookies;

                if (cookies.size() > 0) {
                    Iterator cookieIt = cookies.values().iterator();
                    while (cookieIt.hasNext()) {
                        Utils.addCookie(headers, 
                                        (javax.servlet.http.Cookie)
                                        cookieIt.next());
                    }
                }

                if ("HEAD".equals(mMethod)) {
                    mLimit = 0;
                }
                else {
                    Integer contentLength =
                        headers.getInteger("Content-Length");
                    if (contentLength != null) {
                        mLimit = contentLength.intValue();
                    }
                    else {
                        mLimit = -1;
                    }
                }

                String resCon = headers.getString("Connection");

                if (mLimit >= 0) {
                    if (resCon == null || "Keep-Alive".equalsIgnoreCase(resCon)) {
                        String reqCon = mRequestHeaders.getString("Connection");
                        if ((reqCon == null && "HTTP/1.1".equals(mProtocol)) ||
                            "Keep-Alive".equalsIgnoreCase(reqCon)) {
                            headers.put("Connection", "Keep-Alive");
                        }
                        else {
                            if (resCon == null) {
                                headers.put("Connection", "Close");
                            }
                            mRecycler = null;
                        }
                    }
                    else {
                        mRecycler = null;
                    }
                }
                else {
                    if (resCon == null) {
                        headers.put("Connection", "Close");
                    }
                    mRecycler = null;
                }
                
                if (mStatusMessage == null) {
                    mStatusMessage = Utils.decodeStatusCode(mStatusCode);
                }

                CharToByteBuffer buffer = new FastCharToByteBuffer
                    (new DefaultByteBuffer(), "ISO-8859-1");
                buffer = new InternedCharToByteBuffer(buffer);

                buffer.append(mProtocol);
                buffer.append((byte)' ');
                int status = mStatusCode;
                if (status == 200) {
                    // Tiny optimization.
                    buffer.append("200");
                }
                else {
                    buffer.append(Integer.toString(status));
                }
                buffer.append((byte)' ');
                buffer.append(mStatusMessage);
                buffer.append(CRLF);
                headers.appendTo(buffer);
                buffer.append(CRLF);

                buffer.writeTo(out);

                // Don't need these anymore.
                mRequestHeaders = null;
                mResponseHeaders = null;
            }
        }
    }
}
