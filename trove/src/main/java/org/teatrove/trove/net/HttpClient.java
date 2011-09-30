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

package org.teatrove.trove.net;

import org.teatrove.trove.io.ByteBuffer;
import org.teatrove.trove.io.CharToByteBuffer;
import org.teatrove.trove.io.DefaultByteBuffer;
import org.teatrove.trove.io.FastBufferedInputStream;
import org.teatrove.trove.io.FastBufferedOutputStream;
import org.teatrove.trove.io.FastCharToByteBuffer;
import org.teatrove.trove.io.InternedCharToByteBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.ProtocolException;
import java.net.SocketException;

/**
 *
 * @author Brian S O'Neill
 */
public class HttpClient {
    private final SocketFactory mFactory;
    private final int mReadTimeout;

    private String mMethod = "GET";
    private String mURI = "";
    private String mProtocol = "HTTP/1.0";
    private HttpHeaderMap mHeaders;

    private Object mSession;

    /**
     * Constructs a HttpClient with a read timeout that matches the given
     * factory's connect timeout.
     *
     * @param factory source of socket connections
     */
    public HttpClient(SocketFactory factory) {
        this(factory, factory.getDefaultTimeout());
    }

    /**
     * @param factory source of socket connections
     * @param readTimeout timeout on socket read operations before throwing a
     * InterruptedIOException
     */
    public HttpClient(SocketFactory factory, long readTimeout) {
        mFactory = factory;
        if (readTimeout == 0) {
            mReadTimeout = 1;
        }
        else if (readTimeout < 0) {
            mReadTimeout = 0;
        }
        else if (readTimeout > Integer.MAX_VALUE) {
            mReadTimeout = Integer.MAX_VALUE;
        }
        else {
            mReadTimeout = (int)readTimeout;
        }
    }

    /**
     * Set the HTTP request method, which defaults to "GET".
     *
     * @return 'this', so that addtional calls may be chained together
     */
    public HttpClient setMethod(String method) {
        mMethod = method;
        return this;
    }

    /**
     * Set the URI to request, which can include a query string.
     *
     * @return 'this', so that addtional calls may be chained together
     */
    public HttpClient setURI(String uri) {
        mURI = uri;
        return this;
    }

    /**
     * Set the HTTP protocol string, which defaults to "HTTP/1.0".
     *
     * @return 'this', so that addtional calls may be chained together
     */
    public HttpClient setProtocol(String protocol) {
        mProtocol = protocol;
        return this;
    }

    /**
     * Set a header name-value pair to the request.
     *
     * @return 'this', so that addtional calls may be chained together
     */
    public HttpClient setHeader(String name, Object value) {
        if (mHeaders == null) {
            mHeaders = new HttpHeaderMap();
        }
        mHeaders.put(name, value);
        return this;
    }

    /**
     * Add a header name-value pair to the request in order for multiple values
     * to be specified.
     *
     * @return 'this', so that addtional calls may be chained together
     */
    public HttpClient addHeader(String name, Object value) {
        if (mHeaders == null) {
            mHeaders = new HttpHeaderMap();
        }
        mHeaders.add(name, value);
        return this;
    }

    /**
     * Set all the headers for this request, replacing any existing headers.
     * If any more headers are added to this request, they will be stored in
     * the given HttpHeaderMap.
     *
     * @return 'this', so that addtional calls may be chained together
     */
    public HttpClient setHeaders(HttpHeaderMap headers) {
        mHeaders = headers;
        return this;
    }

    /**
     * Convenience method for setting the "Connection" header to "Keep-Alive"
     * or "Close".
     *
     * @param b true for persistent connection
     * @return 'this', so that addtional calls may be chained together
     */
    public HttpClient setPersistent(boolean b) {
        if (b) {
            setHeader("Connection", "Keep-Alive");
        }
        else {
            setHeader("Connection", "Close");
        }
        return this;
    }

    /**
     * Convenience method for preparing a post to the server. This method sets
     * the method to "POST", sets the "Content-Length" header, and sets the
     * "Content-Type" header to "application/x-www-form-urlencoded". When
     * calling getResponse, PostData must be provided.
     *
     * @param contentLength number of bytes to be posted
     * @return 'this', so that addtional calls may be chained together
     */
    public HttpClient preparePost(int contentLength) {
        setMethod("POST");
        setHeader("Content-Type", "application/x-www-form-urlencoded");
        setHeader("Content-Length", new Integer(contentLength));
        return this;
    }

    /**
     * Optionally specify a session for getting connections. If SocketFactory
     * is distributed, then session helps to ensure the same server is routed
     * to on multiple requests.
     *
     * @param session Object whose hashcode might be used to select a specific
     * connection if factory is distributed. If null, then no session is used.
     * @return 'this', so that addtional calls may be chained together
     */
    public HttpClient setSession(Object session) {
        mSession = session;
        return this;
    }

    /**
     * Opens a connection, passes on the current request settings, and returns
     * the server's response.
     */
    public Response getResponse() throws ConnectException, SocketException {
        return getResponse(null);
    }

    /**
     * Opens a connection, passes on the current request settings, and returns
     * the server's response. The optional PostData parameter is used to
     * supply post data to the server. The Content-Length header specifies
     * how much data will be read from the PostData InputStream. If it is not
     * specified, data will be read from the InputStream until EOF is reached.
     *
     * @param postData additional data to supply to the server, if request
     * method is POST
     */
    public Response getResponse(PostData postData)
        throws ConnectException, SocketException
    {
        CheckedSocket socket = mFactory.getSocket(mSession);

        try {
            CharToByteBuffer request = new FastCharToByteBuffer
                (new DefaultByteBuffer(), "8859_1");
            request = new InternedCharToByteBuffer(request);

            request.append(mMethod);
            request.append(' ');
            request.append(mURI);
            request.append(' ');
            request.append(mProtocol);
            request.append("\r\n");
            if (mHeaders != null) {
                mHeaders.appendTo(request);
            }
            request.append("\r\n");

            Response response;
            try {
                response = sendRequest(socket, request, postData);
            }
            catch (InterruptedIOException e) {
                // If timed out, throw exception rather than risk double
                // posting.
                throw e;
            }
            catch (IOException e) {
                response = null;
            }

            if (response == null) {
                // Try again with new connection. Persistent connection may
                // have timed out and been closed by server.
                try {
                    socket.close();
                }
                catch (IOException e) {
                }

                socket = mFactory.createSocket(mSession);

                response = sendRequest(socket, request, postData);
                if (response == null) {
                    throw new ConnectException("No response from server " + socket.getInetAddress() + ":" +
                            socket.getPort());
                }
            }

            return response;
        }
        catch (SocketException e) {
            throw e;
        }
        catch (InterruptedIOException e) {
            throw new ConnectException("Read timeout expired: " +
                                       mReadTimeout + ", " + e);
        }
        catch (IOException e) {
            throw new SocketException(e.toString());
        }
    }

    private Response sendRequest(CheckedSocket socket,
                                 ByteBuffer request,
                                 PostData postData)
        throws SocketException, IOException
    {
        socket.setSoTimeout(mReadTimeout);

        OutputStream out =
            new FastBufferedOutputStream(socket.getOutputStream());
        request.writeTo(out);
        if (postData != null) {
            writePostData(out, postData);
        }
        out.flush();
        InputStream in = new FastBufferedInputStream(socket.getInputStream());

        char[] buf = new char[100];
        String line = HttpUtils.readLine(in, buf);

        if (line == null) {
            return null;
        }

        return new Response(socket, mMethod, in, buf, line);
    }

    private void writePostData(OutputStream out, PostData postData)
        throws IOException
    {
        InputStream in = postData.getInputStream();

        int contentLength = -1;
        if (mHeaders != null) {
            Integer i = mHeaders.getInteger("Content-Length");
            if (i != null) {
                contentLength = i.intValue();
            }
        }

        byte[] buf;
        if (contentLength < 0 || contentLength > 4000) {
            buf = new byte[4000];
        }
        else {
            buf = new byte[contentLength];
        }

        try {
            int len;
            if (contentLength < 0) {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            else {
                while (contentLength > 0) {
                    len = buf.length;
                    if (contentLength < len) {
                        len = contentLength;
                    }
                    if ((len = in.read(buf, 0, len)) <= 0) {
                        break;
                    }
                    out.write(buf, 0, len);
                    contentLength -= len;
                }
            }
        }
        finally {
            in.close();
        }
    }

    /**
     * A factory for supplying data to be written to server in a POST request.
     */
    public static interface PostData {
        /**
         * Returns the actual data via an InputStream. If the client needs to
         * reconnect to the server, this method may be called again. The
         * InputStream is closed when all the post data has been read from it.
         */
        public InputStream getInputStream() throws IOException;
    }

    public class Response {
        private final CheckedSocket mSocket;
        private final String mRequestMethod;
        private final InputStream mRawIn;

        private int mStatusCode;
        private String mStatusMessage;
        private HttpHeaderMap mHeaders;

        private InputStream mIn;

        Response(CheckedSocket socket, String method,
                 InputStream in, char[] buf, String line) throws IOException
        {
            mSocket = socket;
            mRequestMethod = method;
            mRawIn = in;
            nextResponse(buf, line);
        }

        /**
         * Add method stub for use by GoPublish.
         */
        public void close() {
            try {
                mSocket.close();
            }
            catch (Exception ignore) { }
        }


        /**
         * Returns the server's status code, 200 for OK, 404 for not found,
         * etc.
         */
        public int getStatusCode() {
            return mStatusCode;
        }

        /**
         * Returns the server's status message accompanying the status code.
         * This message is intended for humans only.
         */
        public String getStatusMessage() {
            return mStatusMessage;
        }

        public HttpHeaderMap getHeaders() {
            return mHeaders;
        }

        /**
         * Returns true if status code is 100 (continue).
         */
        public boolean hasNextResponse() {
            return mStatusCode == 100;
        }

        /**
         * If status code is 100 (continue), then call nextResponse to read the
         * next response from the server. Returns false if current response
         * status code is not 100. Returns true if next response was read,
         * updating this response's status code and headers.
         *
         * If the getInputStream method is called before calling nextResponse,
         * then the response is automatically moved to the next non-100
         * response.
         */
        public boolean nextResponse() throws IOException {
            if (mStatusCode != 100) {
                return false;
            }
            char[] buf = new char[100];
            String line = HttpUtils.readLine(mRawIn, buf);
            if (line == null) {
                throw new ProtocolException("No next response");
            }
            nextResponse(buf, line);
            return true;
        }

        /**
         * Returns an InputStream supplying the body of the response. When all
         * of the response body has been read, the connection is either closed
         * or recycled, depending on if all the criteria is met for supporting
         * persistent connections. Further reads on the InputStream will
         * return EOF.
         */
        public InputStream getInputStream() throws IOException {
            if (mIn != null) {
                return mIn;
            }

            while (nextResponse()) {
            }

            // Used for controlling persistent connections.
            int contentLength;
            if ("Keep-Alive".equalsIgnoreCase
                (mHeaders.getString("Connection"))) {

                if ("HEAD".equals(mRequestMethod)) {
                    contentLength = 0;
                }
                else {
                    Integer i = mHeaders.getInteger("Content-Length");
                    if (i != null) {
                        contentLength = i.intValue();
                    }
                    else {
                        contentLength = -1;
                    }
                }
            }
            else {
                contentLength = -1;
            }

            return mIn = new ResponseInput(mSocket, mRawIn, contentLength);
        }

        private void nextResponse(char[] buf, String line) throws IOException {
            int statusCode = -1;
            String statusMessage = "";

            int space = line.indexOf(' ');
            if (space > 0) {
                int nextSpace = line.indexOf(' ', space + 1);
                String sub;
                if (nextSpace < 0) {
                    sub = line.substring(space + 1);
                }
                else {
                    sub = line.substring(space + 1, nextSpace);
                    statusMessage = line.substring(nextSpace + 1);
                }
                try {
                    statusCode = Integer.parseInt(sub);
                }
                catch (NumberFormatException e) {
                }
            }

            if (statusCode < 0) {
                throw new ProtocolException("Invalid HTTP response: " + line);
            }

            mStatusCode = statusCode;
            mStatusMessage = statusMessage;
            mHeaders = new HttpHeaderMap();
            mHeaders.readFrom(mRawIn, buf);
        }
    }

    private class ResponseInput extends InputStream {
        private CheckedSocket mSocket;
        private InputStream mIn;
        private int mContentLength;

        /**
         * @param contentLength Used for supporting persistent connections. If
         * negative, then close connection when EOF is read.
         */
        public ResponseInput(CheckedSocket socket,
                             InputStream in, int contentLength)
            throws IOException
        {
            mSocket = socket;
            mIn = in;
            if ((mContentLength = contentLength) == 0) {
                recycle();
            }
        }

        public int read() throws IOException {
            if (mContentLength == 0) {
                return -1;
            }

            int b = mIn.read();

            if (b < 0) {
                close();
            }
            else if (mContentLength > 0) {
                if (--mContentLength == 0) {
                    recycle();
                }
            }

            return b;
        }

        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (mContentLength == 0) {
                return -1;
            }

            if (mContentLength < 0) {
                len = mIn.read(b, off, len);
                if (len < 0) {
                    close();
                }
                else if (len == 0) {
                    close();
                    len = -1;
                }
                return len;
            }

            if (len > mContentLength) {
                len = mContentLength;
            }
            else if (len == 0) {
                return 0;
            }

            len = mIn.read(b, off, len);

            if (len < 0) {
                close();
            }
            else if (len == 0) {
                close();
                len = -1;
            }
            else {
                if ((mContentLength -= len) == 0) {
                    recycle();
                }
            }

            return len;
        }

        public long skip(long n) throws IOException {
            if (mContentLength == 0) {
                return 0;
            }

            if (mContentLength < 0) {
                return mIn.skip(n);
            }

            if (n > mContentLength) {
                n = mContentLength;
            }
            else if (n == 0) {
                return 0;
            }

            n = mIn.skip(n);

            if ((mContentLength -= n) == 0) {
                recycle();
            }

            return n;
        }

        public int available() throws IOException {
            if (mContentLength == 0) {
                return 0;
            }
            else {
                try {
                    return mIn.available();
                }
                catch (SocketException se) {
                    return 0;
                }
            }
        }

        public void close() throws IOException {
            if (mSocket != null) {
                mContentLength = 0;
                mSocket = null;
                mIn.close();
            }
        }

        private void recycle() throws IOException {
            if (mSocket != null) {
                if (mContentLength == 0) {
                    CheckedSocket s = mSocket;
                    mSocket = null;
                    mFactory.recycleSocket(s);
                }
                else {
                    mSocket = null;
                    mIn.close();
                }
            }
        }
    }
}
