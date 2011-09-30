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

package org.teatrove.trove.persist;

import java.io.*;
import org.teatrove.trove.io.FastBufferedInputStream;
import org.teatrove.trove.io.FastBufferedOutputStream;
import org.teatrove.trove.io.FastDataOutputStream;
import org.teatrove.trove.io.DataIO;
import org.teatrove.trove.file.*;
import org.teatrove.trove.util.ReadWriteLock;

/**
 * 
 * @author Brian S O'Neill
 */
public class MultiplexFileStorageStrategy implements BTree.StorageStrategy {
    private static final int
        OBJECT_TYPE = 1,
        STRING_TYPE = 2,
        INT_TYPE = 3,
        LONG_TYPE = 4,
        BYTE_TYPE = 5,
        SHORT_TYPE = 6,
        CHAR_TYPE = 7,
        FLOAT_TYPE = 8,
        DOUBLE_TYPE = 9,
        BOOLEAN_TYPE = 10,
        STRING_ARRAY_TYPE = 11,
        INT_ARRAY_TYPE = 12,
        LONG_ARRAY_TYPE = 13,
        BYTE_ARRAY_TYPE = 14,
        SHORT_ARRAY_TYPE = 15,
        CHAR_ARRAY_TYPE = 16,
        FLOAT_ARRAY_TYPE = 17,
        DOUBLE_ARRAY_TYPE = 18,
        BOOLEAN_ARRAY_TYPE = 19;

    public static int calculateMaxNodeSize(int blockSize,
                                           int keyType,
                                           int valueType,
                                           int maxKeyLength,
                                           int maxValueLength)
    {
        int keySize = calculateSize(keyType, maxKeyLength);
        int valueSize = calculateSize(valueType, maxValueLength);
        return (blockSize - 8) / (keySize + valueSize + 4);
    }

    public static int calculateBlockSize(int maxNodeSize,
                                         int keyType,
                                         int valueType,
                                         int maxKeyLength,
                                         int maxValueLength)
    {
        int keySize = calculateSize(keyType, maxKeyLength);
        int valueSize = calculateSize(valueType, maxValueLength);
        return maxNodeSize * (keySize + valueSize + 4) + 8;
    }

    private static int calculateSize(int type, int length) {
        switch (type) {
        default:
            if (length <= 0) {
                throw new IllegalArgumentException
                    ("Cannot calculate expected max node size");
            }
            return length;
        case STRING_TYPE:
            return 2 + length;
        case INT_TYPE:
            return 4;
        case LONG_TYPE:
            return 8;
        case BYTE_TYPE:
            return 1;
        case SHORT_TYPE:
            return 2;
        case CHAR_TYPE:
            return 2;
        case FLOAT_TYPE:
            return 4;
        case DOUBLE_TYPE:
            return 8;
        case BOOLEAN_TYPE:
            return 1;
        case INT_ARRAY_TYPE:
            return 2 + 4 * length;
        case BYTE_ARRAY_TYPE:
            return 2 + length;
        }
    }

    private static int selectType(Class type) {
        if (String.class.isAssignableFrom(type)) {
            return STRING_TYPE;
        }
        else if (Integer.class.isAssignableFrom(type)) {
            return INT_TYPE;
        }
        else if (Long.class.isAssignableFrom(type)) {
            return LONG_TYPE;
        }
        else if (Byte.class.isAssignableFrom(type)) {
            return BYTE_TYPE;
        }
        else if (Short.class.isAssignableFrom(type)) {
            return SHORT_TYPE;
        }
        else if (Character.class.isAssignableFrom(type)) {
            return CHAR_TYPE;
        }
        else if (Float.class.isAssignableFrom(type)) {
            return FLOAT_TYPE;
        }
        else if (Double.class.isAssignableFrom(type)) {
            return DOUBLE_TYPE;
        }
        else if (Boolean.class.isAssignableFrom(type)) {
            return BOOLEAN_TYPE;
        }
        else if (String[].class == type) {
            return STRING_ARRAY_TYPE;
        }
        else if (int[].class == type) {
            return INT_ARRAY_TYPE;
        }
        else if (long[].class == type) {
            return LONG_ARRAY_TYPE;
        }
        else if (byte[].class == type) {
            return BYTE_ARRAY_TYPE;
        }
        else if (short[].class == type) {
            return SHORT_ARRAY_TYPE;
        }
        else if (char[].class == type) {
            return CHAR_ARRAY_TYPE;
        }
        else if (float[].class == type) {
            return FLOAT_ARRAY_TYPE;
        }
        else if (double[].class == type) {
            return DOUBLE_ARRAY_TYPE;
        }
        else if (boolean[].class == type) {
            return BOOLEAN_ARRAY_TYPE;
        }

        return OBJECT_TYPE;
    }

    private int mKeyType;
    private int mValueType;

    private final TxFileBuffer mFile;
    private final int mReserved;
    private final int mMaxNodeSize;
    private int mBlockSize;

    private FileRepository mRepository;
    private FileBufferInputStream mMetaIn;
    private FileBufferOutputStream mMetaOut;

    private final ObjectStreamBuilder mOSBuilder;

    /**
     * If file passed in implements TxFileBuffer, its transaction features
     * will be used.
     *
     * One of block size or max node size may be omitted of max key length and
     * max value length are specified. To omit one of those parameters, specify
     * 0 for it. Max key length and max value length may be omitted if block
     * size and max node size are specified or if associated data type is
     * primitive.
     *
     * @param file file for storing b-tree
     * @param reserved Number of bytes to reserve before headers
     * @param blockSize block size to use on file
     * @param keyType expected key type
     * @param valueType expected value type
     * @param maxKeyLength if key type is array or string, maximum expected
     * length, which can be exceeded
     * @param maxValueLength if value type is array or string, maximum expected
     * length, which can be exceeded
     * @param maxNodeSize maximum entries per b-tree node
     * @param builder optional ObjectStreamBuilder
     */                                     
    public MultiplexFileStorageStrategy(FileBuffer file, int reserved,
                                        int blockSize,
                                        Class keyType, Class valueType,
                                        int maxKeyLength, int maxValueLength,
                                        int maxNodeSize,
                                        ObjectStreamBuilder builder)
        throws IOException
    {
        if (file instanceof TxFileBuffer) {
            mFile = (TxFileBuffer)file;
        }
        else {
            mFile = new NonTxFileBuffer(file);
        }

        mReserved = reserved;
        mKeyType = selectType(keyType);
        mValueType = selectType(valueType);

        if (maxNodeSize <= 0) {
            maxNodeSize =
                calculateMaxNodeSize(blockSize, mKeyType, mValueType,
                                     maxKeyLength, maxValueLength);
        }
        else if (blockSize <= 0) {
            blockSize =
                calculateBlockSize(maxNodeSize, mKeyType, mValueType,
                                   maxKeyLength, maxValueLength);
        }

        if (maxNodeSize < 2) {
            throw new IllegalArgumentException
                ("Max node size must be at least 2: " + maxNodeSize);
        }

        mMaxNodeSize = maxNodeSize;
        mBlockSize = blockSize;

        if (builder == null) {
            builder = new ObjectStreamBuilder();
        }
        mOSBuilder = builder;

        initFile(false);
    }

    public int getBlockSize() {
        return mBlockSize;
    }

    public int getMaxNodeSize() {
        return mMaxNodeSize;
    }

    public int loadTotalSize() throws IOException {
        mMetaIn.position(mReserved);
        try {
            return mMetaIn.readInt();
        }
        catch (EOFException e) {
            return 0;
        }
    }
    
    public void saveTotalSize(int size) throws IOException {
        mMetaOut.position(mReserved);
        mMetaOut.writeInt(size);
    }

    public long loadRootNodeId() throws IOException {
        mMetaIn.position(mReserved + 4);
        try {
            return mMetaIn.readLong();
        }
        catch (EOFException e) {
            return -1;
        }
    }

    public void saveRootNodeId(long id) throws IOException {
        mMetaOut.position(mReserved + 4);
        mMetaOut.writeLong(id);
    }
    
    public void loadNodeExceptEntries(long id, BTree.NodeData data)
        throws IOException
    {
        InputStream in = openNodeInput(id);
        DataInput din = new DataInputStream(in);
        
        int size = din.readInt();
        
        if (size < 0) {
            size = ~size;
        }
        else {
            long[] childrenIds = new long[mMaxNodeSize + 1];
            data.childrenIds = childrenIds;
            int limit = size + 1;
            for (int i=0; i<limit; i++) {
                childrenIds[i] = din.readInt() & 0xffffffffL;
            }
        }
        
        data.size = size;
        in.close();
    }

    public void loadNode(long id, BTree.NodeData data) throws IOException {
        InputStream in = openNodeInput(id);
        DataInput din = new DataInputStream(in);
        
        int size = din.readInt();
        
        if (size < 0) {
            size = ~size;
        }
        else {
            long[] childrenIds = new long[mMaxNodeSize + 1];
            data.childrenIds = childrenIds;
            int limit = size + 1;
            for (int i=0; i<limit; i++) {
                childrenIds[i] = din.readInt() & 0xffffffffL;
            }
        }
        
        data.size = size;
        
        int keyType = mKeyType;
        int valueType = mValueType;
        
        ObjectInputStream oin;
        
        if (keyType == OBJECT_TYPE || valueType == OBJECT_TYPE) {
            oin = mOSBuilder.createInputStream(in);
            din = oin;
        }
        else {
            oin = null;
        }
        
        Object[] entries = new Object[mMaxNodeSize * 2];
        
        data.entries = entries;
        
        try {
            int limit = size * 2;
            for (int i = 0; i < limit; i += 2) {
                if (keyType == OBJECT_TYPE) {
                    entries[i] = oin.readObject();
                }
                else if (keyType == STRING_TYPE) {
                    entries[i] = readUTF(din);
                }
                else {
                    entries[i] = readData(din, keyType);
                }
                
                if (valueType == OBJECT_TYPE) {
                        entries[i + 1] = oin.readObject();
                }
                else if (valueType == STRING_TYPE) {
                    entries[i + 1] = readUTF(din);
                }
                else {
                    entries[i + 1] = readData(din, valueType);
                }
            }
        }
        catch (ClassNotFoundException e) {
            throw new IOException(e.toString());
        }
        
        if (oin != null) {
            oin.close();
        }
        else {
            in.close();
        }
    }
    
    public void saveNode(long id, BTree.NodeData data) throws IOException {
        int size = data.size;
        Object[] entries = data.entries;
        long[] childrenIds = data.childrenIds;

        int keyType = mKeyType;
        int valueType = mValueType;

        OutputStream out =
            new FileBufferOutputStream(mRepository.openFile(id));
        out = new FastBufferedOutputStream(out, mBlockSize);
        DataOutput dout = new FastDataOutputStream(out);
        
        if (childrenIds == null) {
            dout.writeInt(~size);
        }
        else {
            dout.writeInt(size);
            int limit = size + 1;
            for (int i=0; i<limit; i++) {
                long child = childrenIds[i];
                if (child > 0xffffffffL) {
                    throw new IOException("Node id too large: " + child);
                }
                dout.writeInt((int)child);
            }
        }
        
        ObjectOutputStream oout;
        
        if (keyType == OBJECT_TYPE || valueType == OBJECT_TYPE) {
            oout = mOSBuilder.createOutputStream(out);
            dout = oout;
        }
        else {
            oout = null;
        }
        
        char[] workspace;
        if (keyType == STRING_TYPE || valueType == STRING_TYPE) {
            workspace = new char[128];
        }
        else {
            workspace = null;
        }
        
        int limit = size * 2;
        for (int i = 0; i < limit; i += 2) {
            if (keyType == OBJECT_TYPE) {
                oout.writeObject(entries[i]);
            }
            else if (keyType == STRING_TYPE) {
                writeUTF(dout, (String)entries[i], workspace);
            }
            else {
                writeData(dout, keyType, entries[i]);
            }
            
            if (valueType == OBJECT_TYPE) {
                oout.writeObject(entries[i + 1]);
            }
            else if (valueType == STRING_TYPE) {
                writeUTF(dout, (String)entries[i + 1], workspace);
            }
            else {
                writeData(dout, valueType, entries[i + 1]);
            }
        }
        
        if (oout != null) {
            oout.close();
        }
        else {
            out.close();
        }
    }

    public long allocLeafNode() throws IOException {
        return mRepository.createFile();
    }

    public long allocNonLeafNode() throws IOException {
        return mRepository.createFile();
    }

    public void freeNode(long id) throws IOException {
        mRepository.deleteFile(id);
    }

    public boolean clear() throws IOException {
        initFile(true);
        return true;
    }

    public ReadWriteLock lock() {
        return mFile.lock();
    }

    public void begin() throws IOException {
        mFile.begin();
    }

    public boolean commit() throws IOException {
        return mFile.commit();
    }

    public boolean force() throws IOException {
        return mFile.force();
    }

    private void initFile(boolean clear) throws IOException {
        if (clear) {
            mFile.truncate(mReserved);
        }
        else if (mFile.size() <= mReserved) {
            clear = true;
        }

        // Additional 12 bytes reserved for meta information.
        MultiplexFile mf;
        if (clear) {
            mf = new MultiplexFile(mFile, mReserved + 12, mBlockSize, 4, 4);
        }
        else {
            mf = new MultiplexFile(mFile, mReserved + 12);
            mBlockSize = mf.getBlockSize();
        }
        
        // Reserve file 1 for future use.
        mRepository = new MultiplexFileRepository(mf, 2);
        
        mMetaIn = new FileBufferInputStream(mFile);
        mMetaOut = new FileBufferOutputStream(mFile);

        if (clear) {
            saveTotalSize(0);
            saveRootNodeId(-1);
        }
    }

    private InputStream openNodeInput(long id) throws IOException {
        FileBuffer nodeFile = mRepository.openFile(id);
        long fileSize = nodeFile.size();
        int bufferSize = (fileSize < mBlockSize) ? (int)fileSize : mBlockSize;
        return new FastBufferedInputStream
            (new FileBufferInputStream(nodeFile), bufferSize);
    }

    /**
     * Reads string length as number of characters. The DataInput.readUTF
     * method reads the length as number of encoded bytes.
     */
    private String readUTF(DataInput din) throws IOException {
        int length = din.readUnsignedShort();
        char[] chars = new char[length];
        length = DataIO.readUTF(din, chars, 0, length);
        return new String(chars, 0, length);
    }

    private Object readData(DataInput din, int type) throws IOException {
        switch (type) {
        default:
        case STRING_TYPE:
            return din.readUTF();
        case INT_TYPE:
            return new Integer(din.readInt());
        case LONG_TYPE:
            return new Long(din.readLong());
        case BYTE_TYPE:
            return new Byte(din.readByte());
        case SHORT_TYPE:
            return new Short(din.readShort());
        case CHAR_TYPE:
            return new Character(din.readChar());
        case FLOAT_TYPE:
            return new Float(din.readFloat());
        case DOUBLE_TYPE:
            return new Double(din.readDouble());
        case BOOLEAN_TYPE:
            return din.readBoolean() ? Boolean.TRUE : Boolean.FALSE;
        case INT_ARRAY_TYPE:
            int length = din.readUnsignedShort();
            int[] ints = new int[length];
            for (int i=0; i<length; i++) {
                ints[i] = din.readInt();
            }
            return ints;
        case BYTE_ARRAY_TYPE:
            length = din.readUnsignedShort();
            byte[] bytes = new byte[length];
            din.readFully(bytes);
            return bytes;
        }
    }

    /**
     * Writes string length as number of characters. The DataOutput.writeUTF
     * method writes the length as number of encoded bytes.
     */
    private void writeUTF(DataOutput dout, String str, char[] workspace)
        throws IOException
    {
        int length = str.length();
        if (length > 65535) {
            throw new IOException("String too long: " + length);
        }
        dout.writeShort(length);
        DataIO.writeUTF(dout, str, 0, length, workspace);
    }

    private void writeData(DataOutput dout, int type, Object obj)
        throws IOException
    {
        switch (type) {
        default:
        case STRING_TYPE:
            dout.writeUTF((String)obj);
            break;
        case INT_TYPE:
            dout.writeInt(((Integer)obj).intValue());
            break;
        case LONG_TYPE:
            dout.writeLong(((Long)obj).longValue());
            break;
        case BYTE_TYPE:
            dout.writeByte(((Byte)obj).byteValue());
            break;
        case SHORT_TYPE:
            dout.writeShort(((Short)obj).shortValue());
            break;
        case CHAR_TYPE:
            dout.writeChar(((Character)obj).charValue());
            break;
        case FLOAT_TYPE:
            dout.writeFloat(((Float)obj).floatValue());
            break;
        case DOUBLE_TYPE:
            dout.writeDouble(((Double)obj).doubleValue());
            break;
        case BOOLEAN_TYPE:
            dout.writeBoolean(((Boolean)obj).booleanValue());
            break;
        case INT_ARRAY_TYPE:
            int[] ints = (int[])obj;
            if (ints.length > 65535) {
                throw new IOException("Array too long: " + ints.length);
            }
            dout.writeShort(ints.length);
            for (int i=0; i<ints.length; i++) {
                dout.writeInt(ints[i]);
            }
            break;
        case BYTE_ARRAY_TYPE:
            byte[] bytes = (byte[])obj;
            if (bytes.length > 65535) {
                throw new IOException("Array too long: " + bytes.length);
            }
            dout.writeShort(bytes.length);
            for (int i=0; i<bytes.length; i++) {
                dout.writeByte(bytes[i]);
            }
            break;
        }
    }
}
