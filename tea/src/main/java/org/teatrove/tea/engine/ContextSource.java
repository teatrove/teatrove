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

/**
 * Implementations of this class are responsible for providing context 
 * instances as well as the context type.
 *
 * @author Jonathan Colwell
 * @see org.teatrove.tea.runtime.Context
 */
public interface ContextSource {

    /**
     * @return the Class of the object returned by createContext.
     */
    public Class<?> getContextType() throws Exception;

    /**
     * A generic method to create context instances.
     *
     * @param param {@link TeaExecutionEngine engine} specific parameter that
     * may be used to create the context instance
     */
    public Object createContext(Object param) throws Exception;
}
