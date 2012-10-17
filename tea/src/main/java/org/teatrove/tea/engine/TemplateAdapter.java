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

package org.teatrove.tea.engine;

import java.lang.reflect.*;
import org.teatrove.trove.util.MergedClass;
import org.teatrove.trove.util.ClassInjector;
import org.teatrove.tea.runtime.Context;
import org.teatrove.tea.runtime.TemplateLoader;

/**
 * Loads templates but can also create context adapters for pre-compiled
 * templates.
 *
 * @author Brian S O'Neill
 */
class TemplateAdapter extends TemplateLoader {
    private final Class<?> mContextClass;
    private final ClassInjector mInjector;

    public TemplateAdapter(Class<?> contextClass,
                           ClassInjector injector, String packagePrefix) {
        super(injector, packagePrefix);
        mContextClass = contextClass;
        mInjector = injector;
    }

    protected TemplateLoader.Template loadTemplate(String name)
        throws ClassNotFoundException, NoSuchMethodException, LinkageError
    {
        TemplateLoader.Template t = super.loadTemplate(name);
        if (t != null) {
            Class<?> templateContext = t.getContextType();
            if (!templateContext.isAssignableFrom(mContextClass)) {
                if (!templateContext.isInterface()) {
                    throw new NoSuchMethodException
                        ("Cannot adapt to context " + templateContext +
                         " because it is not an interface.");
                }

                // Create an adapter context.
                Constructor<?> ctor = MergedClass.getConstructor
                    (mInjector, new Class[] {mContextClass, templateContext});

                return new AdaptedTemplate(t, ctor);
            }
        }
        return t;
    }

    private class AdaptedTemplate implements TemplateLoader.Template {
        private final Template mTemplate;
        private final Constructor<?> mContextConstructor;

        public AdaptedTemplate(Template t, Constructor<?> ctor) {
            mTemplate = t;
            mContextConstructor = ctor;
        }

        public TemplateLoader getTemplateLoader() {
            return TemplateAdapter.this;
        }

        public String getName() {
            return mTemplate.getName();
        }

        public Class<?> getTemplateClass() {
            return mTemplate.getTemplateClass();
        }

        public Class<?> getContextType() {
            return mContextConstructor.getDeclaringClass();
        }

        public Class<?> getReturnType() {
            return mTemplate.getReturnType();
        }
        
        public Type getGenericReturnType() {
            return mTemplate.getGenericReturnType();
        }
        
        public String[] getParameterNames() {
            return mTemplate.getParameterNames();
        }

        public Class<?>[] getParameterTypes() {
            return mTemplate.getParameterTypes();
        }
        
        public Type[] getGenericParameterTypes() {
            return mTemplate.getGenericParameterTypes();
        }

        public void execute(Context context, Object[] parameters)
            throws Exception
        {
            // Instantiate adapter.
            context = (Context)mContextConstructor.newInstance
                (new Object[] {context, null});
            mTemplate.execute(context, parameters);
        }

        public String toString() {
            return mTemplate.toString();
        }
    }
}
