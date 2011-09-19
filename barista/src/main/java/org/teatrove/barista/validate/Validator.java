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

package org.teatrove.barista.validate;

import org.teatrove.barista.validate.event.*;

/**
 * Interface defining a basic set of methods that can be used for 
 * validating anything.
 *
 * @author Sean T. Treat
 */
public interface Validator {
    public void addValidationListener(ValidationListener listener);
    public void removeValidationListener(ValidationListener listener);
    public void validate();
}
