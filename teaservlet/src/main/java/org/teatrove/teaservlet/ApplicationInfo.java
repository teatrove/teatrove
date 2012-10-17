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

import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.teatrove.teaservlet.util.NameValuePair;
import org.teatrove.trove.util.ClassUtils;

/**
 *
 * @author Brian S O'Neill
 */
public class ApplicationInfo extends NameValuePair<Application> {
    private static final long serialVersionUID = 1L;

    private Class<?> mContext;
    private String mPrefix;

    public ApplicationInfo(String name, Application app,
                           Class<?> context, String prefix) {
        super(name, app);
        mContext = context;
        mPrefix = prefix;
    }

    public boolean isDeprecated() {
        if (ClassUtils.isDeprecated(getContextType())) {
            return true;
        }
        
        return ClassUtils.isDeprecated(this.getValue().getClass());
    }
    
    public Class<?> getContextType() {
        return mContext;
    }

    public String getContextPrefixName() {
        return mPrefix;
    }

    public FunctionInfo[] getContextFunctions() {
        Class<?> contextType = getContextType();
        if (contextType == null) {
            return new FunctionInfo[0];
        }

        try {
            MethodDescriptor[] methods =
                Introspector.getBeanInfo(contextType).getMethodDescriptors();
            
            List<FunctionInfo> list = 
                new ArrayList<FunctionInfo>(methods.length);
            
            for (int i = methods.length; --i >= 0; ) {
                MethodDescriptor m = methods[i];
                if (m.getMethod().getDeclaringClass() != Object.class) {
                    list.add(new FunctionInfo(m, this));
                }
            }

            FunctionInfo[] functions = 
                list.toArray(new FunctionInfo[list.size()]);
            
            Arrays.sort(functions);
            return functions;
        }
        catch (Exception e) {
            Thread t = Thread.currentThread();
            t.getThreadGroup().uncaughtException(t, e);
            return new FunctionInfo[0];
        }
    }

    public NameValuePair<String>[] getInitParameters() {
        // TODO: implement
        return null;
    }
}
