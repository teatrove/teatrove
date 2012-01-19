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

package org.teatrove.tea.parsetree;

import org.teatrove.tea.compiler.SourceInfo;
import org.teatrove.tea.compiler.CompilationUnit;

/**
 * A CallExpression to a template is specified in a template with the "call"
 * keyword.
 * 
 * @author Brian S O'Neill
 * @see FunctionCallExpression
 */
public class TemplateCallExpression extends CallExpression {
    private static final long serialVersionUID = 1L;

    private CompilationUnit mCalledTemplate;

    public TemplateCallExpression(SourceInfo info, 
                                  Expression expression, Name target,
                                  ExpressionList params,
                                  Block subParam) {
        super(info, expression, target, params, subParam);
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    /**
     * Returns the template being called, which is set by a type checker.
     */
    public CompilationUnit getCalledTemplate() {
        return mCalledTemplate;
    }

    public void setCalledTemplate(CompilationUnit template) {
        mCalledTemplate = template;
    }
}
