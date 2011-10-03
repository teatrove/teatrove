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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javassist.bytecode.ClassFile;

import javax.servlet.ServletContext;

import org.scannotation.ClasspathUrlFinder;
import org.scannotation.archiveiterator.Filter;
import org.scannotation.archiveiterator.IteratorFactory;
import org.scannotation.archiveiterator.StreamIterator;

public class ClassDB {

    /** Set of packages to ignore or allow. */
    private Set<String> ignoredPackages = new HashSet<String>();
    private Set<String> allowedPackages = new HashSet<String>();

    /** Mapping of superclass to set of direct subclasses. */
    private Map<String, Set<String>> classIndex =
        new HashMap<String, Set<String>>();

    public ClassDB() {
        super();
    }

    public void scanAll() throws IOException {
        this.scanArchives(ClasspathUrlFinder.findClassPaths());
    }

    public void scanAll(ServletContext scontext) throws IOException {
        this.scanAll();
        this.scanArchives(ClassUtils.findServletClasspath(scontext));
    }

    public Map<String, Set<String>> getClassIndex() {
        return classIndex;
    }

    public Set<String> getSubclasses(Class<?> superclass) {
        return getSubclasses(superclass.getName());
    }

    public Set<String> getSubclasses(String superclass) {
        return classIndex.get(superclass);
    }

    public Set<String> getAllSubclasses(Class<?> superclass) {
        return getAllSubclasses(superclass.getName());
    }

    public Set<String> getAllSubclasses(String superclass) {
        Set<String> all = new HashSet<String>();

        Set<String> subclasses = getSubclasses(superclass);
        if (subclasses != null) {
            all.addAll(subclasses);
            for (String subclass : subclasses) {
                all.addAll(getAllSubclasses(subclass));
            }
        }

        return all;
    }

    public Set<String> getIgnoredPackages() {
       return ignoredPackages;
    }

    public void setIgnoredPackages(Set<String> ignoredPackages) {
        if (!this.allowedPackages.isEmpty()) {
            throw new IllegalStateException("cannot have both ignored and allowed packages");
        }

        this.ignoredPackages = ignoredPackages;
    }

    public void addIgnoredPackages(String... packages) {
        if (!this.allowedPackages.isEmpty()) {
            throw new IllegalStateException("cannot have both ignored and allowed packages");
        }

        for (String ignoredPackage : packages) {
            this.ignoredPackages.add(ignoredPackage);
        }
    }

    public Set<String> getAllowedPackages() {
        return allowedPackages;
     }

     public void setAllowedPackages(Set<String> allowedPackages) {
         if (!this.ignoredPackages.isEmpty()) {
             throw new IllegalStateException("cannot have both ignored and allowed packages");
         }

         this.allowedPackages = allowedPackages;
     }

     public void addAllowedPackages(String... packages) {
         if (!this.ignoredPackages.isEmpty()) {
             throw new IllegalStateException("cannot have both ignored and allowed packages");
         }

         for (String allowedPackage : packages) {
             this.allowedPackages.add(allowedPackage);
         }
     }

    public void scanArchives(URL... urls)
        throws IOException {

        int i = 0;
        for (URL url : urls) {
            i++;

            // setup filter to scan classes
            Filter filter = new Filter() {
                @Override
                public boolean accepts(String filename) {
                    if (filename.endsWith(".class")) {
                        if (filename.startsWith("/")) {
                            filename = filename.substring(1);
                        }

                        if (!ignoreScan(filename.replace('/', '.'))) {
                            return true;
                        }
                    }

                    return false;
                }
            };

            // search for all found classes
            StreamIterator it = IteratorFactory.create(url, filter);

            // stream each class instance
            InputStream stream;
            while ((stream = it.next()) != null) {
                scanClass(stream);
            }
        }
    }

    protected boolean ignoreScan(String className) {
        if (!ignoredPackages.isEmpty()) {
            for (String ignored : ignoredPackages) {
                if (className.startsWith(ignored + ".")) {
                    return true;
                }
            }

            return false;
        }
        else if (!allowedPackages.isEmpty()) {
            for (String allowed : allowedPackages) {
                if (className.startsWith(allowed + ".")) {
                    return false;
                }
            }

            return true;
        }
        else {
            return false;
        }
    }

    protected void scanClass(InputStream bits)
        throws IOException {

        DataInputStream dstream =
            new DataInputStream(new BufferedInputStream(bits));

        ClassFile cf = null;
        try {
            cf = new ClassFile(dstream);
            // System.out.println("SCANNED: " + cf.getName());

            String superclass = cf.getSuperclass();
            if (superclass != null) {
                addIndex(superclass, cf.getName());
            }

            String[] interfaces = cf.getInterfaces();
            if (interfaces != null) {
                for (String iface : interfaces) {
                    addIndex(iface, cf.getName());
                }
            }
        }
        finally {
            dstream.close();
            bits.close();
        }
    }

    protected void addIndex(String superclass, String subclass) {
        Set<String> subclasses = classIndex.get(superclass);
        if (subclasses == null) {
            subclasses = new HashSet<String>();
            classIndex.put(superclass, subclasses);
        }

        subclasses.add(subclass);
    }
}
