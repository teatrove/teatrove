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

import java.lang.reflect.Type;

import org.teatrove.tea.runtime.Context;
import org.teatrove.tea.runtime.TemplateLoader;
import org.teatrove.tea.engine.TemplateSource;

/**
 * 
 * @author Jonathan Colwell
 */
public class TeaServletTemplate implements TemplateLoader.Template {

    private TemplateLoader.Template mTemplate;
    private TemplateSource mSource;

    public TeaServletTemplate(TemplateLoader.Template template,
                              TemplateSource source) {
        mTemplate = template;
        mSource = source;
    }

    public TemplateSource getTemplateSource() {
        return mSource;
    }

    public TemplateLoader getTemplateLoader() {
        return mTemplate.getTemplateLoader();
    }

    public String getName() {
        return mTemplate.getName();
    }

    public Class<?> getTemplateClass() {
        return mTemplate.getTemplateClass();
    }

    public Class<?> getContextType() {
        return mTemplate.getContextType();
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
        mTemplate.execute(context, parameters);
    }
}


