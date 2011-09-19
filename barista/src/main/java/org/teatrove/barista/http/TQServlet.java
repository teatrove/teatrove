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

package org.teatrove.barista.http;

import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import org.teatrove.trove.util.tq.TransactionQueue;

/**
 * Each Servlet loaded by {@link HttpServletDispatcher} is given one
 * TransactionQueue for servicing requests. Servlets that implement this
 * interface will be able to enqueue requests into their own TransactionQueues.
 *
 * @author Brian S O'Neill
 */
public interface TQServlet extends Servlet {
    /**
     * Returns all the TransactionQueues used by this TQServlet.
     */
    public TransactionQueue[] getTransactionQueues();

    /**
     * Return a TransactionQueue to process the request or null if a default
     * queue should be used instead.
     */
    public TransactionQueue selectQueue(ServletRequest request);
}
