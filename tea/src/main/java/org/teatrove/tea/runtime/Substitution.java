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

package org.teatrove.tea.runtime;

/**
 * A block of code in a template that can be passed as a substitution to
 * another template or to a function, must implement this interface. A
 * function that defines its last parameter as a Substitution can receive
 * a block of code from a template. To execute it, call substitute.
 * <p>
 * Substitution blocks can contain internal state information which may change
 * when the called function returns. Therefore, Substitution objects should
 * never be saved unless explicitly detached.
 * <p>
 * Functions that accept a Substitution appear to extend the template language
 * itself. Condsider the following example, which implements a simple looping
 * function:
 *
 * <pre>
 * public void loop(int count, Substitution s) throws Exception {
 *     while (--count >= 0) {
 *         s.substitute();
 *     }
 * }
 * </pre>
 *
 * The template might invoke this function as:
 *
 * <pre>
 * loop (100) {
 *     "This message is printed 100 times\n"
 * }
 * </pre>
 *
 * @author Brian S O'Neill
 */
public interface Substitution {
    /**
     * Causes the code substitution block to execute against its current
     * output receiver.
     *
     * @throws UnsupportedOperationException if this Substitution was detached.
     */
    public void substitute() throws Exception;

    /**
     * Causes the code substitution block to execute against any context.
     *
     * @throws ClassCastException if context is incompatible with this
     * substitution.
     */
    public void substitute(Context context) throws Exception;

    /**
     * Returns an object that uniquely identifies this substitution block.
     */
    public Object getIdentifier();

    /**
     * Returns a detached substitution that can be saved and re-used. Detaching
     * a substitution provides greater flexibilty when implementing template
     * output caching strategies. One thread may execute the substitution while
     * another thread may, upon timing out, output the previously cached output
     * from this substitution.
     * <p>
     * When calling substitute, a context must be provided or else an
     * UnsupportedOperationException is thrown. In order for multiple threads
     * to safely execute this substitution, each must have its own detached
     * instance.
     */
    public Substitution detach();
}
