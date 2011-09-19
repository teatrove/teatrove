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

import java.lang.reflect.Method;
import java.beans.*;
import java.util.*;
import org.teatrove.teaservlet.util.NameValuePair;
import org.teatrove.trove.util.BeanComparator;

/**
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 2 <!-- $-->, <!--$$JustDate:-->  1/30/02 <!-- $-->
 */
public class ApplicationInfo extends NameValuePair {
    private Class mContext;
    private String mPrefix;

    public ApplicationInfo(String name, Application app,
                           Class context, String prefix) {
        super(name, app);
        mContext = context;
        mPrefix = prefix;
    }

    public Class getContextType() {
        return mContext;
    }

    public String getContextPrefixName() {
        return mPrefix;
    }

    public MethodDescriptor[] getContextFunctions() {
        Class contextType = getContextType();
        if (contextType == null) {
            return new MethodDescriptor[0];
        }

        try {
            MethodDescriptor[] methods =
                Introspector.getBeanInfo(contextType).getMethodDescriptors();
            
            List list = new ArrayList(methods.length);
            for (int i=methods.length; --i >= 0; ) {
                MethodDescriptor m = methods[i];
                if (m.getMethod().getDeclaringClass() != Object.class) {
                    list.add(m);
                }
            }

            MethodDescriptor[] functions = 
                (MethodDescriptor[])list.toArray(new MethodDescriptor[list.size()]);
            Comparator c = BeanComparator.forClass(MethodDescriptor.class).orderBy("method.name");
            Arrays.sort(functions, c);
            return functions;
        }
        catch (Exception e) {
            Thread t = Thread.currentThread();
            t.getThreadGroup().uncaughtException(t, e);
            return new MethodDescriptor[0];
        }
    }

    public NameValuePair[] getInitParameters() {
        // TODO: implement
        return null;
    }
}
