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

/**
 * @author <a href="mailto:brian.r.jackson@gmail.com">Brian Jackson</a>
 * @since Jul 25, 2008 2:38:24 PM
 */
public class ContextClassBuilderException extends Exception {
    public ContextClassBuilderException() {
    }

    public ContextClassBuilderException(String message) {
        super(message);
    }

    public ContextClassBuilderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContextClassBuilderException(Throwable cause) {
        super(cause);
    }
}
