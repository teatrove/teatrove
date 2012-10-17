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

import org.teatrove.tea.runtime.UtilityContext;
import org.teatrove.trove.util.ClassInjector;
import org.teatrove.trove.util.DelegateClassLoader;
import org.teatrove.trove.util.MergedClass;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

/**
 * A ContextSource implementation that merges several ContextSources into one.
 *
 * @author Jonathan Colwell
 */
public class MergedContextSource implements ContextSource {

    private ClassInjector mInjector;
    private ContextSource[] mSources;
    private Class<?>[] mContextsInOrder;
    private boolean mProfilingEnabled = false;

    private Constructor<?> mConstr;

    // TODO: Why use init? Constructers may make more sense.

    public void init(ClassLoader loader, ContextSource[] contextSources, boolean profilingEnabled)
        throws Exception {

        init(loader, contextSources, null, profilingEnabled);
    }

    /**
     * Creates a unified context source for all those passed in.  An Exception
     * may be thrown if the call to a source's getContextType() method throws
     * an exception.
     */
    public void init(ClassLoader loader,
                     ContextSource[] contextSources,
                     String[] prefixes, boolean profilingEnabled) throws Exception {


        mSources = contextSources;
        int len = contextSources.length;
        ArrayList<Class<?>> contextList = new ArrayList<Class<?>>(len);
        ArrayList<ClassLoader> delegateList = new ArrayList<ClassLoader>(len);

        for (int j = 0; j < contextSources.length; j++) {
            Class<?> type = contextSources[j].getContextType();
            if (type != null) {
                contextList.add(type);
                ClassLoader scout = type.getClassLoader();
                if (scout != null && !delegateList.contains(scout)) {
                    delegateList.add(scout);
                }
            }
        }

        mContextsInOrder = contextList.toArray(new Class[contextList.size()]);
        ClassLoader[] delegateLoaders =
            delegateList.toArray(new ClassLoader[delegateList.size()]);

        mInjector = new ClassInjector
            (new DelegateClassLoader(loader, delegateLoaders));

        mProfilingEnabled = profilingEnabled;

        int observerMode = profilingEnabled ? MergedClass.OBSERVER_ENABLED | MergedClass.OBSERVER_EXTERNAL : MergedClass.OBSERVER_ENABLED;

        // temporarily include interface for UtilityContext for backwards
        // compatibility with old code relying on it.
        Class<?>[] interfaces = { UtilityContext.class };
        mConstr = MergedClass.getConstructor2(mInjector,
                                              mContextsInOrder,
                                              prefixes,
                                              interfaces,
                                              observerMode);
    }

    protected Class<?>[] getContextsInOrder() {
        return mContextsInOrder;
    }

    /**
     * let subclasses get at the constructor
     */
    protected Constructor<?> getConstructor() {
        return mConstr;
    }

    protected boolean isProfilingEnabled() {
        return mProfilingEnabled;
    }

    /**
     * @return the Class of the object returned by createContext.
     */
    public Class<?> getContextType() {
        return mConstr.getDeclaringClass();
    }

    /**
     * a generic method to create context instances
     */
    public Object createContext(Object param) throws Exception {

        Class<?>[] params = mConstr != null ? mConstr.getParameterTypes() : new Class[0];
        if (params.length > 1 && MergedClass.InvocationEventObserver.class.equals(params[1])) {
            return mConstr.newInstance(new Object[] {
                new MergingContextFactory(param),
                new MergedClass.InvocationEventObserver() {
                    public void invokedEvent(String caller, String callee, long elapsedTime) { }
                    public long currentTime() { return 0L; }
            }});
        }
        else {
            return mConstr.newInstance(new Object[] {
                new MergingContextFactory(param)});
        }
    }

    private class MergingContextFactory
        implements MergedClass.InstanceFactory {

        private final Object mContextParameter;

        MergingContextFactory(Object contextParam) {
            mContextParameter = contextParam;
        }

        public Object getInstance(int i) {
            try {
                return mSources[i].createContext(mContextParameter);
            }
            catch (Exception e) {
                throw new ContextCreationException(e);
            }
        }
    }
}
