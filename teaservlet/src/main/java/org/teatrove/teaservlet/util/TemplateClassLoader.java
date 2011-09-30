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

package org.teatrove.teaservlet.util;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.StringTokenizer;

/**
 * 
 * @author Jonathan Colwell
 */
public class TemplateClassLoader extends URLClassLoader {

    private static URL[]  makeURLs(String classpath) {
        if (classpath == null) {
            return new URL[0];
        }
        StringTokenizer st = new StringTokenizer(classpath, ",; ");
        URL[] urls = new URL[st.countTokens()];
        for (int j = 0; st.hasMoreTokens();j++) {
            try {
                urls[j] = new URL(st.nextToken());
            }
            catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        return urls;
    }

    public TemplateClassLoader(ClassLoader parent, String classpath) {
        super(makeURLs(classpath), parent);
    }

    protected Class loadClass(String name, boolean resolve) 
        throws ClassNotFoundException {

        //System.out.println("\nLoading " + name);

        Class clazz = findLoadedClass(name);

        if (clazz == null) {
            synchronized (this) {
                try {
                    clazz = findClass(name);
                }   
                catch (Throwable wuteva) {
                    clazz = null;
                }
                    
                if (clazz == null) {
                    clazz = getParent().loadClass(name);
                    //   System.out.println("Parent Loaded " + name);
                }
                /*
                  else {
                  System.out.println("Loaded " + name + " successfully.");
                  }
                */
            }
        }
   
        
        if (resolve) {
            resolveClass(clazz);
        }
        
        return clazz;
        
    }
}
