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
import java.net.URL;

/**
 * ClassLoader that delegates class loading requests if the parent ClassLoader
 * (and its ancestors) can't fulfill the request. Class loading requests are
 * handed off to scouts, which are called upon in sequence to find the class,
 * until the class is found. If neither the parent or scouts can find a
 * requested class, a normal ClassNotFoundException is thrown.
 *
 * @author Brian S O'Neill
 * @version
 */
public class DelegateClassLoader extends ClassLoader {
    private ClassLoader[] mScouts;

    /**
     * @param parent ClassLoader that gets first chance to load a class
     * @param scouts ClassLoaders that get next chances to load a class, if the
     * parent couldn't find it.
     */
    public DelegateClassLoader(ClassLoader parent, ClassLoader[] scouts) {
        super(parent);
        mScouts = (ClassLoader[])scouts.clone();
    }

    protected synchronized Class findClass(String name)
        throws ClassNotFoundException
    {
        for (int i=0; i<mScouts.length; i++) {
            try {
                return mScouts[i].loadClass(name);
            }
            catch (ClassNotFoundException e) {
            }
        }
        throw new ClassNotFoundException(name);
    }
    
    protected URL findResource(String name) {
        URL resource = null;

        try {
            for (int i=0; i<mScouts.length; i++) {
                resource = mScouts[i].getResource(name);
                if (resource != null) {
                    return resource;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return resource;
    }
}
