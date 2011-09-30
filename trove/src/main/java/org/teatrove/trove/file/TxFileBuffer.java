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

import java.io.*;

/**
 * TxFileBuffer supports transactions.
 *
 * @author Brian S O'Neill
 */
public interface TxFileBuffer extends FileBuffer {
    /**
     * It is not required for TxFileBuffer implementations to support
     * rollback. If rollback isn't supported, an UnsupportedOperationException
     * is thrown when calling rollback.
     */
    boolean isRollbackSupported();

    /**
     * Returns true if this TxFileBuffer is in a known clean state. If false is
     * returned, the file may be corrupt. Exactly how a TxFileBuffer gets in a
     * dirty state depends on the implementaion. In general, isClean should
     * be called before any data is written to a re-opened file.
     */
    boolean isClean() throws IOException;

    /**
     * Begins a transaction. Transactions may be nested, and each call to
     * begin must be matched by a call to commit or rollback.
     */
    void begin() throws IOException;

    /**
     * Commits any changes to the file since the last transaction began, but
     * does not necessarily force those changes to disk. If no transaction is
     * in progress, false is returned.
     *
     * @return true if and only if a transaction was is progress and has been
     * commited.
     */
    boolean commit() throws IOException;

    /**
     * Undos all changes made to the file since the last transaction began,
     * but does not necessarily force those changes to disk. If no transaction
     * is in progress, false is returned. If this TxFileBuffer doesn't support
     * rollback, an UnsupportedOperationException is thrown.
     *
     * @return true if and only if a transaction was is progress and has been
     * rolled back.
     */
    boolean rollback() throws IOException, UnsupportedOperationException;

    /**
     * Forces unwritten data to the physical disk, but does not commit or
     * rollback any transactions.
     *
     * @return false if partially forced.
     */
    boolean force() throws IOException;

    /**
     * Immediately closes the file, with any current transactions left open.
     * This is equivalent to calling close(0).
     */
    void close() throws IOException;

    /**
     * Waits for all transactions to commit or rollback before closing file.
     * A timeout must be specified for the wait. If the value is -1, the
     * timeout is infinite.
     *
     * @param timeout maximum milliseconds to wait to close
     */
    void close(long timeout) throws IOException;
}
