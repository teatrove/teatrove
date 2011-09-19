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

import org.teatrove.tea.engine.DynamicContextSource;

/**
 * Allows an Application into masquerade as a ContextSource.
 * 
 * @author Jonathan Colwell
 * @version
 * <!--$$Revision:--> 6 <!-- $-->, <!--$$JustDate:-->  2/01/02 <!-- $-->
 */
public class ApplicationContextSource implements DynamicContextSource {

    private Application mApp;
    private boolean mContextTypeMayChange;

    public ApplicationContextSource(Application app) {
        mApp = app;
        // storing this saves an instanceof call for every hit.
        mContextTypeMayChange = (app instanceof DynamicContextSource);
    }

    /**
     * @return the Class of the object returned by createContext.
     */
    public Class getContextType() {
        return mApp.getContextType();
    }

    public Object createContext(Object param) throws Exception {
        RequestAndResponse rar;
        if (param != null) {
            rar = (RequestAndResponse) param;
        } else {
            rar = new RequestAndResponse();
        }
        return mApp.createContext(rar.getRequest(), rar.getResponse());
    }

    public Object createContext(Class clazz, Object param) throws Exception {
        if (mContextTypeMayChange) {
            return ((DynamicContextSource)mApp).createContext(clazz, param);
        }
        return createContext(param);
    }
}


