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

package org.teatrove.trove.util;

import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A special ClassLoader that allows classes to be defined by directly
 * injecting the bytecode. All classes other than those injected are loaded
 * from the parent ClassLoader, which is the ClassLoader that loaded this
 * class. If a directory is passed in, the ClassInjector looks there for 
 * non-injected class files before asking the parent ClassLoader for a class.
 *
 * @author Brian S O'Neill
 */
public class ClassInjector extends ClassLoader {
    
    @SuppressWarnings("unchecked")
    private static Map<ClassLoader, Reference<ClassInjector>> cShared = 
        new NullKeyMap(new IdentityMap());

    /**
     * Returns a shared ClassInjector instance.
     */
    public static ClassInjector getInstance() {
        return getInstance(null);
    }

    /**
     * Returns a shared ClassInjector instance for the given ClassLoader.
     */
    public static ClassInjector getInstance(ClassLoader loader) {
        ClassInjector injector = null;

        synchronized (cShared) {
            Reference<ClassInjector> ref = cShared.get(loader);
            if (ref != null) {
                injector = ref.get();
            }
            if (injector == null) {
                injector = new ClassInjector(loader);
                cShared.put(loader, new WeakReference<ClassInjector>(injector));
            }
            return injector;
        }
    }

    // Parent ClassLoader, used to load classes that aren't defined by this.
    private ClassLoader mSuperLoader;

    private File[] mRootClassDirs;
    private String mRootPackage;

    // A set of all the classes defined by the ClassInjector.
    private Map<String, String> mDefined = 
        Collections.synchronizedMap(new HashMap<String, String>());

    // A map to store raw bytecode for future use in getResourceAsStream().
    private Map<String, byte[]> mGZippedBytecode;

    private URLStreamHandler mFaker;

    /**
     * Construct a ClassInjector that uses the ClassLoader that loaded this
     * class as a parent, and it has no root class directory or root package.
     */
    public ClassInjector() {
        this(null, (File[])null, null);
    }

    /**
     * Construct a ClassInjector that has no root class directory or root
     * package.
     *
     * @param parent optional parent ClassLoader to default to when a class
     * cannot be loaded with this ClassInjector.
     */
    public ClassInjector(ClassLoader parent) {
        this(parent, (File[])null, null);
    }

    /**
     * Construct a ClassInjector that uses the ClassLoader that loaded this
     * class as a parent.
     *
     * @param rootClassDir optional directory to look for non-injected classes
     * @param rootPackage optional package name for the root directory
     */
    public ClassInjector(File rootClassDir, String rootPackage) {
        this(null, (rootClassDir == null) ? null : new File[]{rootClassDir},
             rootPackage);
    }

    /**
     * @param parent optional parent ClassLoader to default to when a class
     * cannot be loaded with this ClassInjector.
     * @param rootClassDir optional directory to look for non-injected classes
     * @param rootPackage optional package name for the root directory
     */
    public ClassInjector(ClassLoader parent,
                         File rootClassDir, String rootPackage) {
        this(parent, (rootClassDir == null) ? null : new File[]{rootClassDir},
             rootPackage);
    }

    /**
     * Construct a ClassInjector that uses the ClassLoader that loaded this
     * class as a parent.
     *
     * @param rootClassDirs optional directories to look for non-injected
     * classes
     * @param rootPackage optional package name for the root directory
     */
    public ClassInjector(File[] rootClassDirs, String rootPackage) {
        this(null, rootClassDirs, rootPackage);
    }

    /**
     * @param parent optional parent ClassLoader to default to when a class
     * cannot be loaded with this ClassInjector.
     * @param rootClassDirs optional directories to look for non-injected
     * classes
     * @param rootPackage optional package name for the root directory
     */
    public ClassInjector(ClassLoader parent,
                         File[] rootClassDirs,
                         String rootPackage) {
        this(parent, rootClassDirs, rootPackage, false);
    }

    /**
     * @param parent optional parent ClassLoader to default to when a class
     * cannot be loaded with this ClassInjector.
     * @param rootClassDirs optional directories to look for non-injected
     * classes
     * @param rootPackage optional package name for the root directory
     * @param keepRawBytecode if true, will cause the ClassInjector to store
     * the raw bytecode of defined classes.
     */
    public ClassInjector(ClassLoader parent,
                         File[] rootClassDirs,
                         String rootPackage,
                         boolean keepRawBytecode) {
        super();
        if (parent == null) {
            parent = getClass().getClassLoader();
        }
        mSuperLoader = parent;
        if (rootClassDirs != null) {
            mRootClassDirs = rootClassDirs.clone();
        }
        if (rootPackage != null && !rootPackage.endsWith(".")) {
            rootPackage += '.';
        }
        mRootPackage = rootPackage;

        if (keepRawBytecode) {
            mGZippedBytecode = 
                Collections.synchronizedMap(new HashMap<String, byte[]>());
        }
    }

    /**
     * Get a stream used to define a class. Close the stream to finish the
     * definition.
     *
     * @param the fully qualified name of the class to be defined.
     */
    public OutputStream getStream(String name) {
        return new Stream(name);
    }

    /**
     * Reset a stream removing any created definition.
     *
     * @param name the fully qualified name of the class to be undefined
     */
    public void resetStream(String name) {
        synchronized (mDefined) {
            mDefined.remove(name);
        }
    }

    public URL getResource(String name) {

        if (mGZippedBytecode != null) {
            if (mGZippedBytecode.containsKey(name)) {
                try {
                    return new URL("file", null, -1, name, getURLFaker());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("created URL for " + name);
            }
        }
        return mSuperLoader.getResource(name);
    }

    private URLStreamHandler getURLFaker() {
        if (mFaker == null) {
            mFaker = new URLFaker();
        }
        return mFaker;
    }

    protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException {

        Class<?> clazz = findLoadedClass(name);

        if (clazz == null) {
            synchronized (this) {
                clazz = findLoadedClass(name);

                if (clazz == null) {
                    clazz = loadFromFile(name);

                    if (clazz == null) {
                        if (mSuperLoader != null) {
                            clazz = mSuperLoader.loadClass(name);
                        }
                        else {
                            clazz = findSystemClass(name);
                        }

                        if (clazz == null) {
                            throw new ClassNotFoundException(name);
                        }
                    }
                }
            }
        }

        if (resolve) {
            resolveClass(clazz);
        }

        return clazz;
    }

    protected void define(String name, byte[] data) {
        defineClass(name, data, 0, data.length);
        if (mGZippedBytecode != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GZIPOutputStream gz = new GZIPOutputStream(baos);
                gz.write(data,0,data.length);
                gz.close();
                mGZippedBytecode.put(name.replace('.','/') + ".class",
                                     baos.toByteArray());
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private Class<?> loadFromFile(String name) throws ClassNotFoundException {
        if (mRootClassDirs == null) {
            return null;
        }

        String fileName = name;

        if (mRootPackage != null) {
            if (fileName.startsWith(mRootPackage)) {
                fileName = fileName.substring(mRootPackage.length());
            }
            else {
                return null;
            }
        }

        fileName = fileName.replace('.', File.separatorChar);
        ClassNotFoundException error = null;

        for (int i=0; i<mRootClassDirs.length; i++) {
            File file = new File(mRootClassDirs[i], fileName + ".class");

            if (file.exists()) {
                try {
                    byte[] buffer = new byte[(int)file.length()];
                    int avail = buffer.length;
                    int offset = 0;
                    InputStream in = new FileInputStream(file);

                    int len = -1;
                    while ( (len = in.read(buffer, offset, avail)) > 0 ) {
                        offset += len;

                        if ( (avail -= len) <= 0 ) {
                            avail = buffer.length;
                            byte[] newBuffer = new byte[avail * 2];
                            System.arraycopy(buffer, 0, newBuffer, 0, avail);
                            buffer = newBuffer;
                        }
                    }

                    in.close();

                    try {
                        return defineClass(name, buffer, 0, offset);
                    }
                    catch (Throwable ex) {
                        error = new ClassNotFoundException(ex.getMessage(), ex);
                        throw error;
                    }
                }
                catch (IOException e) {
                    if (error == null) {
                        error = new ClassNotFoundException
                            (fileName + ": " + e.toString());
                    }
                }
            }
        }

        if (error != null) {
            throw error;
        }
        else {
            return null;
        }
    }

    private class Stream extends ByteArrayOutputStream {
        private String mName;

        public Stream(String name) {
            super(1024);
            mName = name;
        }

        public void close() {
            synchronized (mDefined) {
                if (mDefined.get(mName) == null) {
                    define(mName, toByteArray());
                    mDefined.put(mName, mName);
                }
            }
        }
    }

    private class URLFaker extends URLStreamHandler {

        protected URLConnection openConnection(URL u) throws IOException {
            return new ClassInjector.ResourceConnection(u);
        }
    }

    private class ResourceConnection extends URLConnection {

        String resourceName;
        public ResourceConnection(URL u) {
            super(u);
            resourceName = u.getFile();
        }

        // not really needed here but it was abstract.
        public void connect() {}

        public InputStream getInputStream() throws IOException {

            try {
                if (mGZippedBytecode != null) {

                    if (mGZippedBytecode.get(resourceName) != null) {
                        return new GZIPInputStream(new ByteArrayInputStream
                                (mGZippedBytecode.get(resourceName)));
                    }
                    else {
                        System.out.println(resourceName + " not found in bytecode map.");
                    }
                }
                else {
                    System.out.println("no bytecode map configured in "+ ClassInjector.this);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
