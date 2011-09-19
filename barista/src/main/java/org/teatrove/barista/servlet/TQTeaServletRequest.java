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

package org.teatrove.barista.servlet;

import org.teatrove.teaservlet.TeaServletStats;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * The TQTeaServletRequest class handles...
 * 
 * @author Reece Wilton
 */
public class TQTeaServletRequest extends HttpServletRequestWrapper
implements TeaServletStats {

	private TQTeaServletTransactionQueue mTransactionQueue;
	
	public TQTeaServletRequest(HttpServletRequest request,
	                           TQTeaServletTransactionQueue transactionQueue) {
		super(request);
		
		mTransactionQueue = transactionQueue;
	}

	public void setTemplateDuration(long templateDuration) {
		
		mTransactionQueue.addTemplateDuration(templateDuration);
	}
}
