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

import java.io.File;

import org.teatrove.tea.runtime.TemplateLoader;

/**
 * While Templates already have access to the loader that loaded them, this
 * interface enables them to gain access to their TemplateSource. Also provide
 * information about the source file of the template and the time it was last
 * modified.
 *
 * @author Jonathan Colwell
 */
public interface Template extends TemplateLoader.Template {

    /**
     * Provides a reference to this Template's TemplateSource.
     */
    public TemplateSource getTemplateSource();

    /**
     * Returns the path to the tea source file of this template.
     */
    public String getSourcePath();

    /**
     * Returns the last (known) modified time of the template's source file
     */
    public long getLastModifiedTime();
}
