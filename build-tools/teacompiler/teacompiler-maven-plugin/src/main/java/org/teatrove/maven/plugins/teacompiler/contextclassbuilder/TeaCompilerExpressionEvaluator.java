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

package org.teatrove.maven.plugins.teacompiler.contextclassbuilder;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.path.PathTranslator;

/**
 * @author <a href="mailto:brian.r.jackson@gmail.com">Brian Jackson</a>
 * @since Jul 27, 2008 12:42:33 AM
 */
public class TeaCompilerExpressionEvaluator extends PluginParameterExpressionEvaluator {

    /**
     * The Constructor.
     *
     * @param context the the context
     * @param pathTranslator the the path translator
     * @param project the the project
     */
    public TeaCompilerExpressionEvaluator( MavenSession context, PathTranslator pathTranslator,
                                        MavenProject project )
    {
        super( context, new MojoExecution( new MojoDescriptor() ), pathTranslator, null, project,
               context.getExecutionProperties() );
    }
}
