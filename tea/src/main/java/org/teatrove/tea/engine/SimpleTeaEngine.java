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

import org.teatrove.tea.runtime.Context;

/**
 * 
 * @author Jonathan Colwell
 */
public class SimpleTeaEngine implements TeaExecutionEngine {

    TemplateSource mTemplateSource;

    public void init(TeaEngineConfig config) {
        mTemplateSource = config.getTemplateSource();
    }

    public void executeTemplate(String templateName, 
                                Object contextParameter, 
                                Object[] templateParameters)
        throws Exception {
        
        mTemplateSource.getTemplate(templateName)
            .execute((Context)mTemplateSource.getContextSource()
                     .createContext(contextParameter), 
                     templateParameters);
    }

    public TemplateSource getTemplateSource() {
        return mTemplateSource;
    }
}
