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

package org.teatrove.teaservlet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

import org.teatrove.trove.io.ByteData;
import org.teatrove.trove.io.ByteBuffer;
import org.teatrove.trove.io.DefaultByteBuffer;
import org.teatrove.trove.io.ByteBufferOutputStream;
import org.teatrove.trove.util.Deflater;
import org.teatrove.trove.util.DeflaterPool;
import org.teatrove.trove.util.DeflaterOutputStream;

/**
 * 
 * @author Brian S O'Neill
 */
class DetachedResponseImpl extends ApplicationResponseImpl {
    // Minimum data size to compress.
    private static final int MINIMUM_SIZE = 100;

    static ByteData compressByteData(ByteData original, int level) {
        ByteBuffer compressed = new DefaultByteBuffer();
        OutputStream cout = new ByteBufferOutputStream(compressed);

        try {
            if (original.getByteCount() < MINIMUM_SIZE) {
                level = Deflater.NO_COMPRESSION;
            }
            
            Deflater d = DeflaterPool.get(level, true);
            
            DeflaterOutputStream dout = new DeflaterOutputStream(cout, d, 512);
            original.writeTo(dout);
            dout.fullFlush();

            DeflaterPool.put(d);
        }
        catch (IOException e) {
            throw new InternalError(e.toString());
        }

        return compressed;
    }

    private final SwappableBuffer mSwappableBuffer;
    private final Data mData;

    DetachedResponseImpl(HttpServletResponse response, 
                         TeaServletEngineImpl tse)
        throws IOException
    {
        this(response, tse, new SwappableBuffer());
    }

    private DetachedResponseImpl(HttpServletResponse response,
                                 TeaServletEngineImpl tse,
                                 SwappableBuffer sb)
        throws IOException
    {
        super(response, tse, sb);
        sb.setBuffer(new DefaultByteBuffer());
        mSwappableBuffer = sb;
        mData = new Data();
        mData.addCommand(new AddByteData(sb.mBuffer));
    }

    DetachedData getData() throws IOException {
        drain();
        return mData;
    }

    public void setContentType(String type) {
        try {
            mBuffer.setEncoding(getCharacterEncoding());
            mData.addCommand(new SetContentType(type));
        }
        catch (IOException e) {
            Thread t = Thread.currentThread();
            t.getThreadGroup().uncaughtException(t, e);
        }
    }

    public void reset() {
        // Ignore.
    }

    public void setLocale(Locale locale) {
        getHttpContext().setLocale(locale);
    }

    public void addCookie(Cookie cookie) {
        mData.addCommand(new AddCookie(cookie));
    }

    public void sendError(int statusCode, String msg) throws IOException {
        mState |= 1;
        mData.addCommand(new SendError(statusCode, msg));
    }

    public void sendError(int statusCode) throws IOException {
        mState |= 1;
        mData.addCommand(new SendError(statusCode, null));
    }

    public void sendRedirect(String location) throws IOException {
        mState |= 1;
        mData.addCommand(new SendRedirect(location));
    }

    public void setHeader(String name, String value) {
        mData.addCommand(new SetHeader(name, value));
    }

    public void setDateHeader(String name, long date) {
        mData.addCommand(new SetHeader(name, new Long(date)));
    }

    public void setIntHeader(String name, int value) {
        mData.addCommand(new SetHeader(name, new Integer(value)));
    }

    public void addHeader(String name, String value) {
        mData.addCommand(new AddHeader(name, value));
    }

    public void addDateHeader(String name, long date) {
        mData.addCommand(new AddHeader(name, new Long(date)));
    }

    public void addIntHeader(String name, int value) {
        mData.addCommand(new AddHeader(name, new Integer(value)));
    }

    public void setStatus(int sc) {
        mData.addCommand(new SetStatus(sc, null));
    }

    /**
     * @deprecated
     */
    public void setStatus(int sc, String msg) {
        mData.addCommand(new SetStatus(sc, msg));
    }

    public boolean insertCommand(Command command) throws Exception {
        mData.addCommand(command);
        addByteDataCommand();
        return true;
    }

    public void finish() {
        // Detached - nothing to finish to.
    }

    private void addByteDataCommand() throws IOException {
        drain();
        ByteBuffer bb = new DefaultByteBuffer();
        mSwappableBuffer.setBuffer(bb);
        mData.addCommand(new AddByteData(bb));
    }

    private void drain() throws IOException {
        if (mBuffer != null) {
            mBuffer.drain();
        }
    }

    // A wrapper that allows another buffer to be swapped in.
    private static class SwappableBuffer implements ByteBuffer {
        private ByteBuffer mBuffer;

        SwappableBuffer() {
        }

        public void setBuffer(ByteBuffer bb) {
            mBuffer = bb;
        }

        public long getByteCount() throws IOException {
            return mBuffer.getByteCount();
        }

        public void writeTo(OutputStream out) throws IOException {
            mBuffer.writeTo(out);
        }

        public void reset() throws IOException {
            mBuffer.reset();
        }
        
        public void clear() throws IOException {
            mBuffer.clear();
        }

        public long getBaseByteCount() throws IOException {
            return mBuffer.getBaseByteCount();
        }

        public void append(byte b) throws IOException {
            mBuffer.append(b);
        }

        public void append(byte[] bytes) throws IOException {
            mBuffer.append(bytes);
        }

        public void append(byte[] bytes, int offset, int length)
            throws IOException
        {
            mBuffer.append(bytes, offset, length);
        }

        public void appendSurrogate(org.teatrove.trove.io.ByteData s)
            throws IOException
        {
            mBuffer.appendSurrogate(s);
        }

        public void addCaptureBuffer(org.teatrove.trove.io.ByteBuffer buffer)
            throws IOException 
        {
            mBuffer.addCaptureBuffer(buffer);
        }

        public void removeCaptureBuffer(org.teatrove.trove.io.ByteBuffer buffer)
            throws IOException
        {
            mBuffer.removeCaptureBuffer(buffer);
        }
    }

    private static class Data implements DetachedData, Serializable {
        private List mCommands;
        private boolean mCompressed;

        public void playback(ApplicationRequest request,
                             ApplicationResponse response) throws Exception {
            if (mCommands != null) {
                int size = mCommands.size();
                for (int i=0; i<size; i++) {
                    ((Command)mCommands.get(i)).execute(request, response);
                }
            }
        }

        public void compress() {
            compress(Deflater.DEFAULT_COMPRESSION);
        }

        public synchronized void compress(int level) {
            if (!mCompressed) {
                mCompressed = true;
                List commands = mCommands;
                int size = commands.size();

                // Check if enough to compress.
                enough: {
                    for (int i=0; i<size; i++) {
                        Object command = commands.get(i);
                        if (command instanceof AddByteData) {
                            ByteData bytes = ((AddByteData)command).mBytes;
                            try {
                                if (bytes.getByteCount() >= MINIMUM_SIZE) {
                                    break enough;
                                }
                            }
                            catch (IOException e) {
                                throw new InternalError(e.toString());
                            }
                        }
                    }
                    
                    // Not enough.
                    return;
                }

                for (int i=0; i<size; i++) {
                    Object command = commands.get(i);
                    if (command instanceof AddByteData) {
                        ByteData original = ((AddByteData)command).mBytes;
                        try {
                            if (original.getByteCount() > 0) {
                                command = new CompressedByteData
                                    (compressByteData(original, level),
                                     original);
                                mCommands.set(i, command);
                            }
                        }
                        catch (IOException e) {
                            throw new InternalError(e.toString());
                        }
                    }
                }
            }
        }

        void addCommand(Command c) {
            if (mCommands == null) {
                mCommands = new ArrayList();
            }
            mCommands.add(c);
        }
    }

    private static class SetContentType implements Command, Serializable {
        private final String mContentType;

        SetContentType(String type) {
            mContentType = type;
        }

        public void execute(ApplicationRequest request,
                            ApplicationResponse response) {
            response.setContentType(mContentType);
        }
    }

    private static class AddCookie implements Command, Serializable {
        private transient Cookie mCookie;

        AddCookie(Cookie cookie) {
            mCookie = cookie;
        }

        public void execute(ApplicationRequest request,
                            ApplicationResponse response) {
            response.addCookie(mCookie);
        }

        private void writeObject(java.io.ObjectOutputStream out)
            throws IOException
        {
            out.writeUTF(mCookie.getName());
            out.writeUTF(mCookie.getValue());
            out.writeUTF(mCookie.getComment());
            out.writeUTF(mCookie.getDomain());
            out.writeInt(mCookie.getMaxAge());
            out.writeUTF(mCookie.getPath());
            out.writeBoolean(mCookie.getSecure());
            out.writeInt(mCookie.getVersion());
        }

        private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
            mCookie = new Cookie(in.readUTF(), in.readUTF());
            mCookie.setComment(in.readUTF());
            mCookie.setDomain(in.readUTF());
            mCookie.setMaxAge(in.readInt());
            mCookie.setPath(in.readUTF());
            mCookie.setSecure(in.readBoolean());
            mCookie.setVersion(in.readInt());
        }
    }

    private static class SetHeader implements Command, Serializable {
        protected final String mName;
        protected final Object mValue;

        SetHeader(String name, Object value) {
            mName = name;
            mValue = value;
        }

        public void execute(ApplicationRequest request,
                            ApplicationResponse response) {
            if (mValue instanceof String) {
                response.setHeader(mName, (String)mValue);
            }
            else if (mValue instanceof Long) {
                response.setDateHeader(mName, ((Long)mValue).longValue());
            }
            else {
                response.setIntHeader(mName, ((Integer)mValue).intValue());
            }
        }
    }

    private static class AddHeader extends SetHeader
        implements Command, Serializable
    {
        AddHeader(String name, Object value) {
            super(name, value);
        }

        public void execute(ApplicationRequest request,
                            ApplicationResponse response) {
            if (mValue instanceof String) {
                response.addHeader(mName, (String)mValue);
            }
            else if (mValue instanceof Long) {
                response.addDateHeader(mName, ((Long)mValue).longValue());
            }
            else {
                response.addIntHeader(mName, ((Integer)mValue).intValue());
            }
        }
    }

    private static class SetStatus implements Command, Serializable {
        protected final int mCode;
        protected final String mMessage;

        SetStatus(int code, String message) {
            mCode = code;
            mMessage = message;
        }

        public void execute(ApplicationRequest request,
                            ApplicationResponse response) throws IOException {
            if (mMessage == null) {
                response.setStatus(mCode);
            }
            else {
                response.setStatus(mCode, mMessage);
            }
        }
    }

    private static class SendError extends SetStatus
        implements Command, Serializable
    {
        SendError(int code, String message) {
            super(code, message);
        }

        public void execute(ApplicationRequest request,
                            ApplicationResponse response) throws IOException {
            if (mMessage == null) {
                response.sendError(mCode);
            }
            else {
                response.sendError(mCode, mMessage);
            }
        }
    }

    private static class SendRedirect implements Command, Serializable {
        private final String mLocation;

        SendRedirect(String location) {
            mLocation = location;
        }

        public void execute(ApplicationRequest request,
                            ApplicationResponse response) throws IOException {
            response.sendRedirect(mLocation);
        }
    }

    private static class AddByteData implements Command, Serializable {
        final ByteData mBytes;

        AddByteData(ByteData bytes) {
            mBytes = bytes;
        }

        public void execute(ApplicationRequest request,
                            ApplicationResponse response) throws IOException {
            response.getResponseBuffer().appendSurrogate(mBytes);
        }
    }

    private static class CompressedByteData implements Command, Serializable {
        private final ByteData mCompressed;
        private final ByteData mOriginal;

        CompressedByteData(ByteData compressed, ByteData original) {
            mCompressed = compressed;
            mOriginal = original;
        }

        public void execute(ApplicationRequest request,
                            ApplicationResponse response) throws IOException {

            if (request.isCompressionAccepted()) {
                try {
                    ApplicationResponseImpl impl =
                        (ApplicationResponseImpl)response;
                    impl.appendCompressed(mCompressed, mOriginal);
                    return;
                }
                catch (ClassCastException e) {
                }
            }

            response.getResponseBuffer().appendSurrogate(mOriginal);
        }
    }
}
