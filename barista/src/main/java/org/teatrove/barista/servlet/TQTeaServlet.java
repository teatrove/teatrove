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

import java.io.IOException;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.teatrove.teaservlet.TeaServlet;
import org.teatrove.trove.util.*;
import org.teatrove.trove.util.tq.TransactionQueue;
import org.teatrove.trove.util.tq.TransactionQueueAdapter;
import org.teatrove.trove.util.tq.TransactionQueueEvent;
import org.teatrove.barista.http.*;

/**
 * This is an extended version of the {@link TeaServlet TeaServlet} that 
 * makes use of {@link TransactionQueue TransactionQueues} for better 
 * performance across templates. The improved performance is gained by
 * creating an individual {@link TransactionQueue TransactionQueue} for each
 * unique template name that is called from a URL. Internal template calls ARE
 * NOT handled through {@link TransactionQueue TransactionQueues}
 * <p>
 * (copied from {@link TeaServlet TeaServlet})<br>
 * The TeaServlet accepts the following initialization parameters:
 * <ul>
 * <li>properties.file - optional path to file with TeaServlet properties, in format used by {@link org.teatrove.teaservlet.util.PropertyParser}
 * <li>template.path - path to the templates
 * <li>template.classes - directory to save compiled templates
 * <li>template.default - the default name for templates
 * <li>template.file.encoding - character encoding of template source files
 * <li>separator.query - override the query separator of '?'
 * <li>separator.parameter - override the parameter separator of '&'
 * <li>separator.value - override the parameter separator of '='
 * <li>log.enabled - turns on/off log (boolean)
 * <li>log.debug - turns on/off log debug messages (boolean)
 * <li>log.info - turns on/off log info messages (boolean)
 * <li>log.warn - turns on/off log warning messages (boolean)
 * <li>log.error - turns on/off log error messages (boolean)
 * <li>log.max - the max log lines to keep in memory
 * <li>applications.[name].class - the application class (required)
 * <li>applications.[name].init.* - prefix for application specific initialization parameters
 * <li>applications.[name].log.enabled - turns on/off application log (boolean)
 * <li>applications.[name].log.debug - turns on/off application log debug messages (boolean)
 * <li>applications.[name].log.info - turns on/off application log info messages (boolean)
 * <li>applications.[name].log.warn - turns on/off application log warning messages (boolean)
 * <li>applications.[name].log.error - turns on/off application log error messages (boolean)
 * </ul>
 *
 * @author Sean Treat
 */
public class TQTeaServlet extends TeaServlet implements TQServlet {
    private static final int DEFAULT_MAX_SIZE = 50;
    private static final int DEFAULT_MAX_THREADS = 10;
    private static final int DEFAULT_TIMEOUT_TRANSACTION = 15000;

    private ThreadPool mThreadPool;
    private Map mCache = Collections.synchronizedMap(new Cache(100));
    private PropertyMap mTransactionQueueProps = new PropertyMap();
    private int mQueueWarnThreshold = 0;
    private boolean mLogQueueWarnings = false;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        PropertyMap props = getProperties();
        
        // Get warning threshold for template service times
        try {
           mQueueWarnThreshold = Integer.parseInt(props.getString("template.queue.warn.threshold.level"));
        }
        catch (NumberFormatException ne) { 
           log("template.queue.warn.threshold.level parameter not an integer. Defaulting to zero (disabled).");
        }

        if (mQueueWarnThreshold != 0)
          log("TQ warning threshold = " + mQueueWarnThreshold);
        mLogQueueWarnings = "true".equals(props.getString(
            "template.queue.warn.threshold.logenabled"));

        mTransactionQueueProps = props.subMap("transactionQueue");

        try {
            HttpServer server = (HttpServer)config.getServletContext()
                .getAttribute("org.teatrove.barista.http.HttpServer");
            if (server != null) {
                mThreadPool = server.getConfig().getThreadPool();
            }
        }
        catch (ClassCastException e) {
        }

        if (mThreadPool == null) {
            throw new ServletException("TQServlet must be run in Barista");
        }
    }
    
	protected void doGet(HttpServletRequest request,
						 HttpServletResponse response)
	throws ServletException, IOException {

		// TODO: capture stats here for total time spent in the TeaServlet
		//       for each request.  save data in TQTeaServletTransactionQueueData
		super.doGet(new TQTeaServletRequest(request, getTQQueue(request)), 
		            response);
	}

    public TransactionQueue[] getTransactionQueues() {
        if (mCache.isEmpty()) {
            return null;
        }
        Object[] objs;
        synchronized(mCache) {
            objs = mCache.values().toArray();    
        }                
        int size = objs.length;
        TransactionQueue[] tmpCpy = new TQTeaServletTransactionQueue[size];
        int validCount = 0;
        for (int i=0; i<size; ++i) {
            if (objs[i] == null) {
                continue;
            }            
            tmpCpy[validCount] = (TransactionQueue)objs[i];
            ++validCount;
        }        
        TransactionQueue[] tq = new TQTeaServletTransactionQueue[validCount];
        for (int i=0; i<validCount; i++) {
            tq[i] = tmpCpy[i];
        }
        return tq;
    }

	public TransactionQueue selectQueue(ServletRequest request) {

		return getTQQueue(request);
	}

    private TQTeaServletTransactionQueue getTQQueue(ServletRequest request) {
    	
        // Get the requested pathInfo to lookup the TransactionQueue by.
        String pathInfo;
        try {
            pathInfo = ((HttpServletRequest)request).getPathInfo();
        }
        catch (ClassCastException e) {
            log("Not an HttpServletRequest", e);
            return null;
        }

        if (pathInfo == null) {
            //Template probably hit using a servlet mapping like *.tea
            pathInfo = ((HttpServletRequest)request).getServletPath();
        }

        TQTeaServletTransactionQueue tq;
        synchronized(mCache) {
            // Try to get a TQ from the cache that maps to the pathInfo.
            tq = (TQTeaServletTransactionQueue)mCache.get(pathInfo);

            // If the search fails, make a new transactionQueue.
            if (tq == null) {
                tq = new TQTeaServletTransactionQueue(mThreadPool,
                                          pathInfo,
                                          DEFAULT_MAX_SIZE,
                                          DEFAULT_MAX_THREADS);
                tq.setIdleTimeout(DEFAULT_TIMEOUT_TRANSACTION);
                tq.setQueueWarnThreshold(mQueueWarnThreshold);
                // Apply any custom settings, if any, to the newly created
                // TransactionQueue.
                tq.applyProperties(mTransactionQueueProps);
                // Add the the path/transaction queue pair to the cache
                mCache.put(pathInfo, tq);

                // Add transaction queue service threshold watcher
                if (mLogQueueWarnings) {
                  tq.addTransactionQueueListener(new TransactionQueueAdapter() {
                     public void transactionServiced(TransactionQueueEvent e) {
                        TransactionQueue q = e.getTransactionQueue();
                        TQTeaServletTransactionQueueData info = 
                           (TQTeaServletTransactionQueueData) q.getStatistics();
                        if (info.getServiceDurationThresholdExceeded()) 
                            getLog().warn("Template '" + q.getName() + 
                                "' exceeds service duration threshold of " +
                                mQueueWarnThreshold + " ms.");
                     }
                  });
                }
            }
        }

        return tq;
    }
}
