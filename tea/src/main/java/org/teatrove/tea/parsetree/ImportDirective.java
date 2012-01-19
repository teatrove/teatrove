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
import org.teatrove.tea.compiler.Type;

/**
 * An import directive allows for a shorthand name qualifier for type names
 *
 * @author Guy Molinari
 */
public class ImportDirective extends Directive {
    private static final long serialVersionUID = 1L;

    private String mName;

    /**
     * Used for variable declarations.
     */
    public ImportDirective(SourceInfo info, String name) {
        super(info);

        mName = name;
    }


    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }


    public int hashCode() {
        return mName.hashCode();
    }

    /**
     * ImportDirectives are tested for equality only by their name and type.
     * Field status is ignored.
     */
    public boolean equals(Object other) {
        if (other instanceof ImportDirective) {
            ImportDirective i = (ImportDirective)other;
            return mName.equals(i.mName);
        }
        else {
            return false;
        }
    }

    public Object clone() {
        return (Directive)super.clone();
    }

}
