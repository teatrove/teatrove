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

package org.teatrove.teaservlet;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.teatrove.teaservlet.util.FilteredServletContext;
import org.teatrove.trove.log.*;
import org.teatrove.trove.util.Utils;
import org.teatrove.trove.util.ClassInjector;
import org.teatrove.trove.util.Config;
import org.teatrove.trove.util.ConfigSupport;
import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.PropertyParser;
import org.teatrove.trove.util.plugin.Plugin;
import org.teatrove.trove.util.plugin.PluginContext;
import org.teatrove.trove.util.plugin.PluginFactory;
import org.teatrove.trove.util.plugin.PluginFactoryConfig;
import org.teatrove.trove.util.plugin.PluginFactoryConfigSupport;
import org.teatrove.trove.util.plugin.PluginFactoryException;
import org.teatrove.trove.io.ByteBuffer;
import org.teatrove.tea.runtime.TemplateLoader;

import org.teatrove.tea.engine.*;

/**
 * The ApplicationDepot stores the Applications that were specified in the
 * props file. The ApplicationDepot is also responsible for creating logs for
 * each Application.
 * <p>
 * depot -- a place for storing goods
 *
 * @author Reece Wilton, Jonathan Colwell
 * @version
 * <!--$$Revision:--> 17 <!-- $-->, <!--$$JustDate:-->  8/11/04 <!-- $-->
 */
public class ApplicationDepot {

    private TeaServletEngineImpl mEngine;

    private ContextSource mContextSource;
    private ContextSource mGenericContextSource;

    // a map to retrieve applications by name.
    private Map mAppMap;
    private Map mAppContextMap;

    // an array containing the applications in the order of initialization.
    private Application[] mApplications;
    // a array of the application names in the same order
    private String[] mApplicationNames;
    // same as above, but cleaned to contain only valid java identifiers
    private String[] mContextPrefixNames;


    /**
     * Creates the ApplicationDepot.
     * @param engine the teaservlet engine in which this depot lives.
     */
    ApplicationDepot(TeaServletEngineImpl engine) throws ServletException {

        mEngine = engine;

        // Initialize Applications
        loadApplications();
    }

    public ContextSource getContextSource() {
        if (mContextSource == null) {
            return reloadContextSource();
        }
        return mContextSource;
    }

    public ContextSource getGenericContextSource() {
        if (mGenericContextSource == null) {
            return reloadGenericContextSource();
        }
        return mGenericContextSource;
    }


    public ContextSource reloadContextSource() {
        try {
            mContextSource = createContextSource(true);
        }
        catch (Exception e) {
            mEngine.getLog().error(e);
        }
        return mContextSource;
    }


    public ContextSource reloadGenericContextSource() {
        try {
            mGenericContextSource = createContextSource(false);
        }
        catch (Exception e) {
            mEngine.getLog().error(e);
        }
        return mGenericContextSource;
    }

    public void setContextSource(ContextSource contextSource) {
        mContextSource = contextSource;
    }

    public final Class getContextType() throws Exception {
            return getContextSource().getContextType();
    }

    public Application[] getApplications() {
        return mApplications;
    }

    public Application getApplicationForClassName(String className) {

        for (int i = 0; i < mApplications.length; i++) {
            if (mApplications[i].getClass().getName().indexOf(className) != -1)
                return mApplications[i];
        }

        return null;
    }

    public String[] getApplicationNames() {
        return mApplicationNames;
    }

    public String[] getContextPrefixNames() {
        return mContextPrefixNames;
    }

    /**
     * This method destroys the ApplicationDepot.
     */
    public void destroy() {
        for (int j = 0; j < mApplications.length; j++) {
            if (mApplications[j] != null) {
                mApplications[j].destroy();
            }
        }
    }

    /**
     * creates a single context source from the applications in the depot.
     */
    private TeaServletContextSource createContextSource(boolean http)
        throws Exception {

        return new TeaServletContextSource(getClass().getClassLoader(), this, mEngine.getServletContext(), mEngine.getLog(), http, mEngine.getProperties().getBoolean("profiling.enabled", true));
    }

    private void loadApplications() throws ServletException {

        PropertyMap props = mEngine.getProperties().subMap("applications");
        Set appSet = props.subMapKeySet();

        mAppMap = new TreeMap();

        List lApplications = new ArrayList();
        List lAppNames = new ArrayList();


        Log log = mEngine.getLog();

        log.info("Loading Applications");
        for (Iterator appIt = appSet.iterator(); appIt.hasNext(); ) {
            String appName = (String)appIt.next();

            log.info("Loading: " + appName);

			PropertyMap appProps = props.subMap(appName);
            String appClassName = appProps.getString("class");

            log.debug("Application class: (" + appClassName + ")");

            if (appClassName == null) {
				log.warn("No class specified.  Skipping application.");
				continue;
			}


            ApplicationConfig ac =
                new InternalApplicationConfig(mEngine, appProps,
                                              mEngine.getPlugins(),
                                              appName);


			try {
				Application app = (Application)getClass()
					.getClassLoader().loadClass(appClassName)
					.newInstance();

				app.init(ac);
				lApplications.add(app);
				lAppNames.add(appName);
				mAppMap.put(appName, app);
			}
			catch (ClassNotFoundException cnfe) {
				log.error("Could not find class: " + appClassName);
			}
			catch (InstantiationException ie) {
				log.error("Could not create an instance of: "
						 + appClassName);
			}
			catch (IllegalAccessException iae) {
				log.error("Could not create an instance of: "
						 + appClassName);
			}
			catch (ClassCastException cce) {
				log.error(appClassName
						 + " does not implement Application.");
				log.error(cce);
			}
        }

        // generate arrays
        int numApps = lApplications.size();
        mApplications = new Application[numApps];
		mApplicationNames = new String[numApps];
        mContextPrefixNames = new String[numApps];

        for (int x = 0; x < numApps; x++) {
			mApplications[x] = (Application)lApplications.get(x);
			mApplicationNames[x] = (String)lAppNames.get(x);
			mContextPrefixNames[x] = cleanName(mApplicationNames[x]);
		}
    }

    /**
     * Ensures that name only contains valid Java identifiers by converting
     * non identifier characters to '$' characters. If name begins with a
     * numeral, name is prefixed with an underscore. The returned name is also
     * trimmed at the first hyphen character, if there is one. This allows
     * multiple applications to appear to provide a unified set of functions.
     */
    public static String cleanName(String name) {
        int index = name.indexOf('-');
        if (index > 0) {
            name = name.substring(0, index);
        }

        int length = name.length();
        StringBuffer buf = new StringBuffer(length + 1);

        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            buf.append('_');
        }
        else if (Character.isJavaIdentifierPart(name.charAt(0))) {
            buf.append(name.charAt(0));
        }

        for (int i=1; i<length; i++) {
            char c = name.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                buf.append(c);
            }
            else {
                buf.append('$');
            }
        }

        return buf.toString();
    }
}







