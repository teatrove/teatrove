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

package org.teatrove.tea.runtime;

import org.teatrove.tea.compiler.JavaClassGenerator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * TemplateLoader manages the loading and execution of Tea templates. To
 * reload templates, create a new TemplateLoader with a new ClassLoader.
 *
 * @author Brian S O'Neill
 * @see org.teatrove.tea.util.ClassInjector
 */
public class TemplateLoader {
    private ClassLoader mBaseLoader;
    private String mPackagePrefix;

    // Maps full template names to Templates.
    private Map<String, Template> mTemplates;

    /**
     * Creates a TemplateLoader that uses the current ClassLoader as a base.
     * It is recommended that templates be compiled to a base package and that
     * a TemplateLoader be constructed with the package prefix. This way,
     * the TemplateLoader can easily distinguish between template classes and
     * normal classes, only loading the templates.
     */
    public TemplateLoader() {
        this(null);
    }

    /**
     * Creates a TemplateLoader that uses the current ClassLoader as a base.
     *
     * @param packagePrefix Package that templates should be loaded from
     */
    public TemplateLoader(String packagePrefix) {
        init(getClass().getClassLoader(), packagePrefix);
    }

    /**
     * Creates a TemplateLoader that uses the given ClassLoader as a base. A
     * base ClassLoader is used to load both template and non-template classes.
     * The base ClassLoader can use the package prefix for determining
     * whether or not it is loading a template.
     *
     * @param baseLoader Base ClassLoader
     * @param packagePrefix Package that templates should be loaded from
     */
    public TemplateLoader(ClassLoader baseLoader, String packagePrefix) {
        init(baseLoader, packagePrefix);
    }

    private void init(ClassLoader baseLoader, String packagePrefix) {
        mBaseLoader = baseLoader;

        if (packagePrefix == null) {
            packagePrefix = "";
        }
        else if (!packagePrefix.endsWith(".")) {
            packagePrefix += '.';
        }
        mPackagePrefix = packagePrefix.trim();

        mTemplates = new HashMap<String, Template>();
    }

    /**
     * Get or load a template by its full name. The full name of a template
     * has '.' characters to separate name parts, and it does not include a
     * Java package prefix.
     *
     * @throws ClassNotFoundException when template not found
     * @throws NoSuchMethodException when the template is invalid
     */
    public final synchronized Template getTemplate(String name)
        throws ClassNotFoundException, NoSuchMethodException, LinkageError
    {
        Template template = mTemplates.get(name);
        if (template == null) {
            template = loadTemplate(name);
            mTemplates.put(name, template);
        }
        return template;
    }

    /**
     * Returns all the templates that have been loaded thus far.
     */
    public final synchronized Template[] getLoadedTemplates() {
        return mTemplates.values().toArray(new Template[mTemplates.size()]);
    }

    protected Template loadTemplate(String name)
        throws ClassNotFoundException, NoSuchMethodException, LinkageError
    {
        return new TemplateImpl
            (name, mBaseLoader.loadClass(mPackagePrefix + name));
    }

    /**
     * A ready-to-use Tea template.
     *
     * @author Brian S O'Neill
     * @version
     */
    public static interface Template {
        public TemplateLoader getTemplateLoader();

        /**
         * Returns the full name of this template.
         */
        public String getName();

        /**
         * Returns the class that defines this template.
         */
        public Class<?> getTemplateClass();

        /**
         * Returns the type of runtime context that this template accepts.
         *
         * @see org.teatrove.tea.runtime.Context
         */
        public Class<?> getContextType();

        /**
         * Returns the type that this template returns.
         */
        public Class<?> getReturnType();
        
        /**
         * Returns the generic type that this template returns.
         */
        public Type getGenericReturnType();
        
        /**
         * Returns the parameter names that this template accepts. The length
         * of the returned array is the same as returned by getParameterTypes.
         * If any template parameter names is unknown, the array entry is null.
         */
        public String[] getParameterNames();

        /**
         * Returns the parameter types that this template accepts. The length
         * of the returned array is the same as returned by getParameterNames.
         */
        public Class<?>[] getParameterTypes();
        
        /**
         * Returns the generic parameter types that this template accepts. The
         * length of the returned array is the same as returned by
         * getParameterTypes.
         */
        public Type[] getGenericParameterTypes();

        /**
         * Executes this template using the given runtime context instance and
         * parameters.
         *
         * @param context Must be assignable to the type returned by
         * {@link #getContextType()}.
         * @param parameters Must have same length and types as returned by
         * {@link #getParameterTypes()}.
         */
        public void execute(Context context, Object[] parameters)
            throws Exception;

        /**
         * Returns the template signature.
         */
        public String toString();
    }

    private class TemplateImpl implements Template {
        private String mName;
        private Class<?> mClass;

        private transient Method mExecuteMethod;
        private transient Class<?> mReturnType;
        private transient Type mGenericReturnType;
        private transient String[] mParameterNames;
        private transient Class<?>[] mParameterTypes;
        private transient Type[] mGenericParameterTypes;

        private TemplateImpl(String name, Class<?> clazz)
            throws NoSuchMethodException
        {
            mName = name;
            mClass = clazz;
            doReflection();
        }

        public TemplateLoader getTemplateLoader() {
            return TemplateLoader.this;
        }

        public String getName() {
            return mName;
        }

        public Class<?> getTemplateClass() {
            return mClass;
        }

        public Class<?> getContextType() {
            return mExecuteMethod.getParameterTypes()[0];
        }

        public Class<?> getReturnType() {
            return mReturnType;
        }
        
        public Type getGenericReturnType() {
            return mGenericReturnType;
        }
        
        public String[] getParameterNames() {
            return mParameterNames.clone();
        }

        public Class<?>[] getParameterTypes() {
            return mParameterTypes.clone();
        }
        
        public Type[] getGenericParameterTypes() {
            return mGenericParameterTypes.clone();
        }

        public void execute(Context context, Object[] parameters)
            throws Exception
        {
            int length = parameters.length;
            Object[] args = new Object[1 + length];
            args[0] = context;
            for (int i=0; i<length; i++) {
                args[i + 1] = parameters[i];
            }

            try {
                Object ret = mExecuteMethod.invoke(null, args);
                if (mReturnType != void.class) {
                    context.print(ret);
                }
            }
            catch (InvocationTargetException e) {
                Throwable t = e.getTargetException();
                if (t instanceof Exception) {
                    throw (Exception)t;
                }
                else if (t instanceof Error) {
                    throw (Error)t;
                }
                else {
                    throw e;
                }
            }
        }

        public String toString() {
            StringBuffer buf = new StringBuffer(80);

            buf.append("template ");
            buf.append(getName());
            buf.append('(');

            buf.append(getContextType().getName());

            String[] paramNames = getParameterNames();
            Class<?>[] paramTypes = getParameterTypes();
            int length = paramTypes.length;
            for (int i=0; i<length; i++) {
                buf.append(", ");
                buf.append(paramTypes[i].getName());
                if (paramNames[i] != null) {
                    buf.append(' ');
                    buf.append(paramNames[i]);
                }
            }

            buf.append(')');

            return buf.toString();
        }

        private void doReflection() throws NoSuchMethodException {
            // Bind to first execute method found; there should be one.
            Method[] methods = getTemplateClass().getMethods();

            for (int i=0; i<methods.length; i++) {
                Method m = methods[i];
                if (m.getName().equals
                    (JavaClassGenerator.EXECUTE_METHOD_NAME) &&
                    Modifier.isStatic(m.getModifiers())) {

                    mExecuteMethod = m;
                    break;
                }
            }

            if (mExecuteMethod == null) {
                throw new NoSuchMethodException
                    ("No execute method found in class " +
                     "for template \"" + getName() + "\"");
            }

            mReturnType = mExecuteMethod.getReturnType();
            mGenericReturnType = mExecuteMethod.getGenericReturnType();

            Class<?>[] methodParams = mExecuteMethod.getParameterTypes();
            Type[] genericMethodParams = mExecuteMethod.getGenericParameterTypes();
            if (methodParams.length == 0 ||
                !Context.class.isAssignableFrom(methodParams[0])) {

                throw new NoSuchMethodException
                    ("Execute method does not accept a context " +
                     "for template \"" + getName() + "\"");
            }

            int length = methodParams.length - 1;
            mParameterNames = new String[length];
            mParameterTypes = new Class[length];
            mGenericParameterTypes = new Type[length];

            for (int i=0; i<length; i++) {
                mParameterTypes[i] = methodParams[i + 1];
                mGenericParameterTypes[i] = genericMethodParams[i + 1];
            }

            try {
                Method namesMethod = getTemplateClass().getMethod
                    (JavaClassGenerator.PARAMETER_METHOD_NAME);

                String[] names = (String[])namesMethod.invoke(null);
                if (names != null) {
                    // Copy, just in case the length differs.
                    for (int i=0; i<length; i++) {
                        mParameterNames[i] = names[i];
                    }
                }
            }
            catch (Exception e) {
                // No big deal, we just don't set paramater names.
            }
        }
    }
}
