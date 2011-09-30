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

package org.teatrove.trove.file;

import java.io.IOException;
import java.io.EOFException;

/**
 * Extends the regular FileBuffer interface for providing I/O operations on
 * primitive data types.
 *
 * @author Brian S O'Neill
 */
public interface DataFileBuffer extends FileBuffer {
    /**
     * Read one signed byte at the given position.
     */
    byte readByte(long position) throws IOException, EOFException;

    /**
     * Read one unsigned byte at the given position.
     */
    int readUnsignedByte(long position) throws IOException, EOFException;

    /**
     * Read one signed short at the given position, big endian order.
     */
    short readShort(long position) throws IOException, EOFException;

    /**
     * Read one signed short at the given position, little endian order.
     */
    short readShortLE(long position) throws IOException, EOFException;

    /**
     * Read one unsigned short at the given position, big endian order.
     */
    int readUnsignedShort(long position) throws IOException, EOFException;

    /**
     * Read one unsigned short at the given position, little endian order.
     */
    int readUnsignedShortLE(long position) throws IOException, EOFException;

    /**
     * Read one char at the given position, big endian order.
     */
    char readChar(long position) throws IOException, EOFException;

    /**
     * Read one char at the given position, little endian order.
     */
    char readCharLE(long position) throws IOException, EOFException;

    /**
     * Read one int at the given position, big endian order.
     */
    int readInt(long position) throws IOException, EOFException;

    /**
     * Read one int at the given position, little endian order.
     */
    int readIntLE(long position) throws IOException, EOFException;

    /**
     * Read one long at the given position, big endian order.
     */
    long readLong(long position) throws IOException, EOFException;

    /**
     * Read one long at the given position, little endian order.
     */
    long readLongLE(long position) throws IOException, EOFException;

    /**
     * Write one byte at the given position.
     */
    void writeByte(long position, int value) throws IOException;

    /**
     * Write one short at the given position, big endian order.
     */
    void writeShort(long position, int value) throws IOException;

    /**
     * Write one short at the given position, little endian order.
     */
    void writeShortLE(long position, int value) throws IOException;

    /**
     * Write one char at the given position, big endian order.
     */
    void writeChar(long position, int value) throws IOException;

    /**
     * Write one char at the given position, little endian order.
     */
    void writeCharLE(long position, int value) throws IOException;

    /**
     * Write one int at the given position, big endian order.
     */
    void writeInt(long position, int value) throws IOException;

    /**
     * Write one int at the given position, little endian order.
     */
    void writeIntLE(long position, int value) throws IOException;

    /**
     * Write one long at the given position, big endian order.
     */
    void writeLong(long position, long value) throws IOException;

    /**
     * Write one int at the given position, little endian order.
     */
    void writeLongLE(long position, long value) throws IOException;
}
