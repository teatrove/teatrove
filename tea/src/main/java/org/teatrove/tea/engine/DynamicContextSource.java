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
 * Instances of this kind of ContextSource can support reloading their context
 * class. {@link TeaExecutionEngine Engines} that support context reload will
 * discover that the context class has changed by calling getContextType. This
 * call will likely be initiated by a template reload request, since all
 * templates may need to be recompiled if the context class has changed.
 * <p>
 * Because Tea engines may execute many templates concurrently, the two
 * argument createContext method is called to inform the context source
 * what specific context class is tied to the application request. The returned
 * context instance must be of the requested type. DynamicContextSources must
 * support instantiating the old context class(es) as well as the new context
 * class.
 * <p>
 * The context source may release old context classes to free resources only
 * when it is safe to do so. One way of achieving this is by using weak
 * references.
 *
 * @author Jonathan Colwell
 */
public interface DynamicContextSource extends ContextSource {

    /**
     * Creates a context instance that must be of the requested type.
     *
     * @param contextClass Expected type of returned context instance
     * @param param {@link TeaExecutionEngine engine} specific parameter that
     * may be used to create the context instance
     */
    public Object createContext(Class<?> contextClass, Object param)
        throws Exception;
}

