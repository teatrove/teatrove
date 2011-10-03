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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;

import org.scannotation.ClasspathUrlFinder;

public class ClassUtils {

    public static URL[] findServletClasspath(ServletContext context) {
        Set<URL> urls = new HashSet<URL>();

        // get actual path in context
        String path = context.getRealPath("/");
        if (path == null) {
            context.log("unable to find context path");
            return new URL[0];
        }

        // lookup /WEB-INF/classes directory
        try {
            File classes = new File(path + "/WEB-INF/classes");
            if (classes.exists() && classes.isDirectory()) {
                urls.add(classes.toURI().toURL());
            }
        }
        catch (MalformedURLException e) {
            context.log("unable to resolve /WEB-INF/classes", e);
        }

        // lookup each JAR library
        File lib = new File(path + "/WEB-INF/lib");
        if (lib.exists() && lib.isDirectory()) {
            File[] jars = lib.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return (pathname.getName().endsWith(".jar"));
                }
            });

            for (File jar : jars) {
                try {
                    urls.add(jar.toURI().toURL());
                }
                catch (MalformedURLException e) {
                    context.log("unable to resolve " + jar, e);
                }
            }
        }

        // return urls
        return urls.toArray(new URL[urls.size()]);
    }
}
