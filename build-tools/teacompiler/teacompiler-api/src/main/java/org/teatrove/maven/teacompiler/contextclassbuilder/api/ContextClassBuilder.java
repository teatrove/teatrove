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

package org.teatrove.maven.teacompiler.contextclassbuilder.api;

import org.teatrove.tea.runtime.Context;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Implementations are responsible for building a Class that will be used
 * as the runtime context for the Tea compiler.
 *
 * @author <a href="mailto:brian.r.jackson@gmail.com">Brian Jackson</a>
 * @since Jul 24, 2008 3:36:32 PM
 */
public interface ContextClassBuilder {

    /**
     * Finds or creates a Class that can be used as a Tea Runtime Context.
     * @param helper A helper class that provides access to necessary objects.
     * @return A Class that can be used as a Tea Runtime Context.
     * @throws MojoExecutionException If there was a problem
     */
    Class<? extends Context> getContextClass(ContextClassBuilderHelper helper) throws ContextClassBuilderException;
}
