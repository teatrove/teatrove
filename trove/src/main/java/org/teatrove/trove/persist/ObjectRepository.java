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

import java.io.IOException;
import java.io.FileNotFoundException;
import org.teatrove.trove.file.FileRepository;

/**
 * Simple object persistence mechanism that saves objects using
 * ObjectOutputStream. Objects are cached, so calling retrieve repeatedly
 * on the same object doesn't keep hitting the underlying FileRepository.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 23 <!-- $-->, <!--$$JustDate:--> 02/03/06 <!-- $-->
 */
public interface ObjectRepository extends FileRepository {
    /**
     * Access the current thread's ObjectRepository during loading and saving
     * of objects. Objects being serialized can access the local repository
     * from their readObject or writeObject method. With direct access to the
     * repository, serialized objects may break themselves up into separately
     * saved objects.
     *
     * @see Link
     */
    public static final LocalRepository local = new LocalRepository();

    /**
     * Returns the Object saved in the given file. To check if it exists, call
     * the fileExists method. If the object has already been retrieved, the
     * existing instance is returned.
     *
     * @param id object file id, as returned from the save or replace method.
     * @throws FileNotFoundException if the file doesn't exist.
     */
    Object retrieveObject(long id)
        throws IOException, FileNotFoundException, ClassNotFoundException;

    /**
     * Returns the Object saved in the given file. To check if it exists, call
     * the fileExists method. If the object has already been retrieved, the
     * existing instance is returned.
     *
     * @param id object file id, as returned from the save or replace method.
     * @param reserved reserved bytes to skip in the head of the file
     * @throws FileNotFoundException if the file doesn't exist.
     */
    Object retrieveObject(long id, int reserved)
        throws IOException, FileNotFoundException, ClassNotFoundException;

    /**
     * Saves the given object and returns a file id for retrieving it. If the
     * given object instance was already saved, it is not replaced.
     *
     * @see #replaceObject
     */
    long saveObject(Object obj) throws IOException;

    /**
     * Saves the given object and returns a file id for retrieving it. If the
     * given object instance was already saved, it is not replaced.
     *
     * @param reserved reserved bytes to skip in the head of the file
     * @see #replaceObject
     */
    long saveObject(Object obj, int reserved) throws IOException;

    /**
     * Saves the given object and returns a file id for retrieving it. If the
     * given object instance was already saved, it is replaced, and the
     * original id is returned.
     *
     * @see #saveObject
     */
    long replaceObject(Object obj) throws IOException;

    /**
     * Saves the given object and returns a file id for retrieving it. If the
     * given object instance was already saved, it is replaced, and the
     * original id is returned.
     *
     * @param reserved reserved bytes to skip in the head of the file
     * @see #saveObject
     */
    long replaceObject(Object obj, int reserved) throws IOException;

    /**
     * Saves the given object and returns a file id for retrieving it. If the
     * given object instance was already saved, it is replaced, and the
     * original id is returned. If the object wasn't saved, it is written into
     * the identified file, only if it exists.
     *
     * @param reserved reserved bytes to skip in the head of the file
     * @see #saveObject
     */
    long replaceObject(Object obj, int reserved, long id) throws IOException;

    /**
     * Removes the referred object from the repository, but deletion may be
     * deferred until all in memory references are cleared or this repository
     * is closed. If the object is saved or replaced before it is deleted, the
     * deferred delete is canceled.
     * <p>
     * To immediately delete the object, call the deleteFile method.
     *
     * @return true if no in memory references and object was deleted
     */
    boolean removeObject(long id) throws IOException;

    public static final class LocalRepository {
        private final ThreadLocal mLocal = new ThreadLocal();

        private LocalRepository() {
        }

        /**
         * Get the current thread's ObjectRepository, or null if none.
         */
        public ObjectRepository get() {
            return (ObjectRepository)mLocal.get();
        }

        /**
         * Set the current thread's ObjectRepository, returning the previously
         * set one. This method should only be called by ObjectRepository
         * implementations.
         *
         * @return previously set ObjectRepository
         */
        public ObjectRepository set(ObjectRepository local) {
            ObjectRepository old = get();
            mLocal.set(local);
            return old;
        }
    }
}
