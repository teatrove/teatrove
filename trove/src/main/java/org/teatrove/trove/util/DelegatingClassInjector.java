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

import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A ClassInjector that delegates responsibility to a list of ClassInjectors 
 * on a per-class basis.  This is an alternative to a monolithic Class Injector, 
 * which carries the risk of a single reference to a loaded class preventing
 * collection of the Loader and all of the associated bytecode, which can result
 * in overflow of the Permanent heap region.
 *
 * @author Josh Yockey
 * @version
 */
public class DelegatingClassInjector extends ClassInjector {
    private static Map cShared = new NullKeyMap(new IdentityMap());

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
            Reference ref = (Reference)cShared.get(loader);
            if (ref != null) {
                injector = (ClassInjector)ref.get();
            }
            if (injector == null) {
                injector = new ClassInjector(loader);
                cShared.put(loader, new WeakReference(injector));
            }
            return injector;
        }
    }

    // Parent ClassLoader, used to load classes that aren't defined by this.
    private ClassLoader mSuperLoader;
    
    private File[] mRootClassDirs;
    private String mRootPackage;
    private boolean mStoreBytecode;

    private Map m_loaderMap = Collections.synchronizedMap(new HashMap());

    /**
     * Construct a ClassInjector that uses the ClassLoader that loaded this
     * class as a parent, and it has no root class directory or root package.
     */
    public DelegatingClassInjector() {
        this(null, (File[])null, null);
    }

    /**
     * Construct a ClassInjector that has no root class directory or root
     * package.
     *
     * @param parent optional parent ClassLoader to default to when a class
     * cannot be loaded with this ClassInjector.
     */
    public DelegatingClassInjector(ClassLoader parent) {
        this(parent, (File[])null, null);
    }

    /**
     * Construct a ClassInjector that uses the ClassLoader that loaded this
     * class as a parent.
     *
     * @param rootClassDir optional directory to look for non-injected classes
     * @param rootPackage optional package name for the root directory
     */
    public DelegatingClassInjector(File rootClassDir, String rootPackage) {
        this(null, (rootClassDir == null) ? null : new File[]{rootClassDir},
             rootPackage);
    }
    
    /**
     * @param parent optional parent ClassLoader to default to when a class
     * cannot be loaded with this ClassInjector.
     * @param rootClassDir optional directory to look for non-injected classes
     * @param rootPackage optional package name for the root directory
     */
    public DelegatingClassInjector(ClassLoader parent, 
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
    public DelegatingClassInjector(File[] rootClassDirs, String rootPackage) {
        this(null, rootClassDirs, rootPackage);
    }
    
    /**
     * @param parent optional parent ClassLoader to default to when a class
     * cannot be loaded with this ClassInjector.
     * @param rootClassDirs optional directories to look for non-injected
     * classes
     * @param rootPackage optional package name for the root directory
     */
    public DelegatingClassInjector(ClassLoader parent, 
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
    public DelegatingClassInjector(ClassLoader parent, 
                         File[] rootClassDirs, 
                         String rootPackage,
                         boolean keepRawBytecode) {
        super();
        if (parent == null) {
            parent = getClass().getClassLoader();
        }
        mSuperLoader = parent;
        if (rootClassDirs != null) {
            mRootClassDirs = (File[])rootClassDirs.clone();
        }
        if (rootPackage != null && !rootPackage.endsWith(".")) {
            rootPackage += '.';
        }
        mRootPackage = rootPackage;

        mStoreBytecode = keepRawBytecode;
    }

    public URL getResource(String name) {
        return mSuperLoader.getResource(name);
    }
    
    private ClassInjector getSubLoader(String name) {
    	ClassInjector ci = (ClassInjector)m_loaderMap.get(name);
    	if (ci == null) {
    		ci = new ClassInjector(null, mRootClassDirs, mRootPackage, mStoreBytecode);
    		m_loaderMap.put(name, ci);
    	}
    	return ci;
    }

    protected Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException {

    	Class clazz = null;
    	try {
    		clazz = getSubLoader(name).loadClass(name, resolve);
    	} catch (ClassNotFoundException e) {
    	}
    	if (clazz == null && mSuperLoader != null) {
    		clazz = mSuperLoader.loadClass(name);
    	}
    	if (clazz == null) {
    		throw new ClassNotFoundException();
    	}
    	return clazz;
    }
    
    protected void define(String name, byte[] data) {
        getSubLoader(name).define(name, data);
    }
}
