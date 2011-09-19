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
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.teatrove.trove.io.FastBufferedOutputStream;
import org.teatrove.trove.net.HttpHeaderMap;

/**
 * Response implementation that wraps an HttpServerConnection.
 *
 * @author Jonathan Colwell
 */
class HttpServletResponseImpl implements HttpServletResponse {
    private static final int NONE = 0, STREAM = 1, WRITER = 2;
    
    private HttpServerConnection mServerConnection;
    private HttpHeaderMap mHeaders;
    private Map mCookies;

    private Map mCharsetAliases;
    
    private int mBufferSize;

    private BufferedOutputStream mRawStream;
    private ServletOutputStream mStream;
    private ResettableWriter mWriter;

    // True if mStream or mWriter have data to be committed.
    boolean mShouldCommit;

    private int mOutputType = NONE;

    public HttpServletResponseImpl(HttpServerConnection httpCon) {
        this(httpCon, null);
    }

    public HttpServletResponseImpl(HttpServerConnection httpCon,
                                   Map charsetAliases) {
        mServerConnection = httpCon;
        mHeaders = httpCon.getResponseHeaders();
        mCookies = httpCon.getResponseCookies();
        setStatus(SC_OK);
        mCharsetAliases = charsetAliases;
    }

    // ServletResponse methods:
    
    public String getCharacterEncoding() {
        String encoding = getCharset();
        Map aliasMap;
        if ((aliasMap = mCharsetAliases) != null) {
            String alias;
            if ((alias = (String)aliasMap.get(encoding)) != null) {
                return alias;
            }
        }
        return encoding == null ? "ISO-8859-1" : encoding;
    }
    
    public ServletOutputStream getOutputStream() throws IOException {
        if (mOutputType == WRITER) {
            throw new IllegalStateException("Writer already requested");
        }
        else {
            mOutputType = STREAM;
        }

        if (mShouldCommit && mWriter != null) {
            mWriter.flush();
        }

        if (mStream == null) {
            mStream = new ServletOutput(getRawOutputStream());
        }

        return mStream;
    }

    public PrintWriter getWriter() throws IOException {
        if (mOutputType == STREAM) {
            throw new IllegalStateException("OutputStream already requested");
        }
        else {
            mOutputType = WRITER;
        }

        if (mShouldCommit && mStream != null) {
            mStream.flush();
        }

        if (mWriter == null) {
            String enc = getCharacterEncoding();
            OutputStreamWriter osw =
                new OutputStreamWriter(getRawOutputStream(), enc);
            mWriter = new ResettableWriter(osw, false);
        }

        return mWriter;
    }

    public void setContentLength(int length) {
        setIntHeader("Content-Length", length);
    }

    public void setContentType(String type) {
        setHeader("Content-Type", type);
    }

    public void setBufferSize(int size) {
        commitCheck();
        if (size > mBufferSize) {
            mBufferSize = size;
            // TODO: Reset it??? We want to keep any existing data!
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
        else {
            // Force the headers to be written out, but don't select an
            // OutputStream over a Writer.
            if (mRawStream == null) {
                mServerConnection.getSocket().getOutputStream().flush();
            }
            else {
                mRawStream.flush();
            }
        }
    }

    public void resetBuffer() {
        commitCheck();
        if (mRawStream != null) {
            try {
                mRawStream.resetBuffer();
                if (mWriter != null) {
                    mWriter.resetWriter();
                }
            }
            catch (IOException e) {
                Thread t = Thread.currentThread();
                t.getThreadGroup().uncaughtException(t, e);
            }
        }
    }

    public boolean isCommitted() {
        return mServerConnection.isResponseCommitted();
    } 

    public void reset() throws IllegalStateException {
        resetBuffer();
        setStatus(SC_OK);
        mHeaders.clear();
        mOutputType = NONE;
    }

    public void setLocale(Locale locale) {
        setHeader(HttpHeaders.CONTENT_LANGUAGE, locale.getLanguage());
    }

    public Locale getLocale() {
        String contentLanguage =
            mHeaders.getString(HttpHeaders.CONTENT_LANGUAGE);
        if (contentLanguage != null) {
            return (Locale)Utils.intern(new Locale(contentLanguage, ""));
        }
        else {
            return Locale.getDefault();
        }
    }
    
    // HttpServletResponse methods:

    public void addCookie(Cookie cookie) {
        if (mCookies == null) {
            Utils.addCookie(mHeaders, cookie);
        }
        else {
            mCookies.put(cookie.getName(), cookie);
        }
    }

    public boolean containsHeader(String name) {
        return mHeaders.containsKey(name);
    }

    public String encodeURL(String url) {
        return url;
    }

    public String encodeRedirectURL(String url) {
        return url;
    }

    /**
     * @deprecated use encodeURL
     */
    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    /**
     * @deprecated use encodeRedirectUrl
     */
    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url);
    }

    public void sendError(int sc, String msg) 
        throws IOException, IllegalStateException
    {
        if (mShouldCommit) {
            if (mWriter != null) {
                mWriter.flush();
            }
            if (mStream != null) {
                mStream.flush();
            }
        }

        commitCheck();

        setStatus(sc);
        String decoded = Utils.decodeStatusCode(sc);

        StringBuffer buf = new StringBuffer(500);
        buf.append("<HTML><HEAD><TITLE>");
        buf.append(decoded);
        buf.append("</TITLE></HEAD><BODY>");
        buf.append(sc);
        buf.append(' ');
        buf.append(decoded);

        if (msg != null && msg.length() > 0) {
            buf.append("<HR>");
            buf.append(msg);
        }

        buf.append("</BODY></HTML>");

        setContentLength(buf.length());

        try {
            setContentType("text/html");
            ServletOutputStream out = getOutputStream();
            out.print(buf.toString());
        }
        catch (IllegalStateException e) {
            setContentType("text/html");
            PrintWriter out = getWriter();
            out.print(buf.toString());
        }

        close();
    }

    public void sendError(int sc) throws IOException,IllegalStateException {
        sendError(sc, null);
    }

    public void sendRedirect(String location) 
        throws IOException, IllegalStateException
    {
        if (mShouldCommit) {
            if (mWriter != null) {
                mWriter.flush();
            }
            if (mStream != null) {
                mStream.flush();
            }
        }

        commitCheck();

        int sc = mServerConnection.getResponseStatusCode();
        if (sc < 300 || sc >= 400) {
            setStatus(sc = SC_MOVED_TEMPORARILY);
        }

        String newLoc = encodeRedirectURL(location);
        setHeader(HttpHeaders.LOCATION, newLoc);

        StringBuffer buf = new StringBuffer(200);
        buf.append("<HTML><HEAD><TITLE>");
        buf.append(Utils.decodeStatusCode(sc));
        buf.append("</TITLE></HEAD><BODY>");
        buf.append("This document has moved to <A HREF=\"");
        buf.append(newLoc);
        buf.append("\">");
        buf.append(newLoc);
        buf.append("</A>.<BODY></HTML>");

        setContentLength(buf.length());

        try {
            ServletOutputStream out = getOutputStream();
            setContentType("text/html");
            out.print(buf.toString());
        }
        catch (IllegalStateException e) {
            PrintWriter out = getWriter();
            setContentType("text/html");
            out.print(buf.toString());
        }

        close();
    }

    public void setDateHeader(String name, long date) {
        mHeaders.put(name, new Date(date));
    }

    public void addDateHeader(String name, long date) {
        mHeaders.add(name, new Date(date));
    }

    public void setHeader(String name, String value) {
        String encoding = "Cp1252".equals(System.getProperty("file.encoding")) ? 
            "iso-8859-1" : System.getProperty("file.encoding");

        if ("Content-Type".equals(name) && value == null)
            value = "text/html; charset=" + encoding;
        if ("Content-Type".equals(name) && value.indexOf("charset") == -1 && value.startsWith("text"))
            value += "; charset=" + encoding;
        mHeaders.put(name, value);
    }

    public void addHeader(String name, String value) {
        mHeaders.add(name, value);
    }

    public void setIntHeader(String name, int value) {
        mHeaders.put(name, new Integer(value));
    }

    public void addIntHeader(String name, int value) {
        mHeaders.add(name, new Integer(value));
    }

    /**
     * @deprecated use sendError(int, String)
     */
    public void setStatus(int sc, String msg) {
        if (sc < 0) {
            throw new IllegalArgumentException("Illegal status code: " + sc);
        }
        mServerConnection.setResponseStatus(sc, msg);
    }

    public void setStatus(int sc) {
        setStatus(sc, null);
    }
    
    // Useful methods not in the Servlet API.

    public void resetOutputType() {
        mOutputType = NONE;
    }

    public String getCharset() {
        String contentType = mHeaders.getString("Content-Type");
        if (contentType == null) {
            return "ISO-8859-1";
        }

        String[][] fields = Utils.parseHeaderFields(contentType, ";");
        if (fields.length > 1 && "charset".equalsIgnoreCase(fields[1][0])) {
            return fields[1][1];
        }
        return "ISO-8859-1";
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
        else {
            // Force the headers to be written out, but don't select an
            // OutputStream over a Writer.
            if (mRawStream == null) {
                mServerConnection.getSocket().getOutputStream().close();
            }
            else {
                mRawStream.close();
            }
        }
        
        mOutputType = NONE;
    }

    void commitCheck() throws IllegalStateException {
        if (isCommitted()) {
            throw new IllegalStateException
                ("Response has been committed: " +
                 mServerConnection.getResponseStatusCode());
        }
    }

    /**
     * Once the output stream has been obtained, the status code and headers 
     * for this response should not be modified.  
     */
    private BufferedOutputStream getRawOutputStream() throws IOException {
        if (mRawStream == null) {
            mRawStream = new BufferedOutputStream();
        }
        return mRawStream;
    }

    // Secret inner classes.

    private class BufferedOutputStream extends OutputStream {
        private OutputStream mOut;

        BufferedOutputStream() throws IOException {
            resetBuffer();
        }

        void resetBuffer() throws IOException {
            OutputStream out = mServerConnection.getSocket().getOutputStream();
            if (mBufferSize > 0) {
                out = new FastBufferedOutputStream(out, mBufferSize);
            }
            mOut = out;
        }

        public void write(int b) throws IOException {
            mOut.write(b);
        }

        public void write(byte[] b) throws IOException {
            mOut.write(b, 0, b.length);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            mOut.write(b, off, len);
        }

        public void flush() throws IOException {
            mOut.flush();
        }

        public void close() throws IOException {
            mOut.close();
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
            out = new OutputStreamWriter(getRawOutputStream(), enc);
        }
    }

    private class ServletOutput extends ServletOutputStream {
        private OutputStream mOutput;

        ServletOutput(OutputStream out) {
            mOutput = out;
        }

        public void write(int b) throws IOException {
            mShouldCommit = true;
            mOutput.write(b);
        }

        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            mShouldCommit = true;
            mOutput.write(b, off, len);
        }

        // Override default print method to not throw CharConversionExceptions.
        public void print(String str) throws IOException {
            if (str == null) {
                str = "null";
            }
            write(str.getBytes(getCharacterEncoding()));
        }

        public void flush() throws IOException {
            mOutput.flush();
        }

        public void close() throws IOException {
            mOutput.close();
        }
    }
}
