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

package org.teatrove.barista.admin;

import org.teatrove.teaservlet.ApplicationRequest;
import org.teatrove.teaservlet.ApplicationResponse;
import org.teatrove.trove.util.BeanComparator;
import org.teatrove.trove.util.tq.TransactionQueue;
import org.teatrove.trove.util.tq.TransactionQueueData;

import org.teatrove.barista.http.HttpHandler;
import org.teatrove.barista.http.HttpHandlerStage;
import org.teatrove.barista.http.HttpServletDispatcher;
import org.teatrove.barista.http.Utils;
import org.teatrove.barista.servlet.TQTeaServletTransactionQueueData;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Iterator;

import javax.servlet.ServletException;

/**
 * Provides an implementation of the BaristaAdminContext.
 *
 * @author Jonathan Colwell
 */
class BaristaAdminContextImpl implements BaristaAdminContext {
    private ApplicationRequest mRequest;
    private ApplicationResponse mResponse;
    private BaristaAdminApplication mApp;
    private BaristaAdmin mAdmin;

    public BaristaAdminContextImpl(ApplicationRequest request,
                                   ApplicationResponse response,
                                   BaristaAdminApplication app) {
        mRequest = request;
        mResponse = response;
        mApp = app;
    }

    public String decodeStatusCode(int sc) {
        try{
            return Utils.decodeStatusCode(sc);
        }
        catch (NumberFormatException nfe) {
            return null;
        }
    }

    public BaristaAdmin getBaristaAdmin() throws ServletException {
        if (mAdmin != null) {
            return mAdmin;
        }
        return mAdmin = mApp.getBaristaAdmin(mRequest, mResponse);
    }

    public TransactionQueueData[] getTransactionQueueData(String handlerName,
                                                          String stageName,
                                                          String servletName,
                                                          String queueName)
        throws ServletException
    {
        TransactionQueue[] queues = getQueues
            (handlerName, stageName, servletName, queueName);

        int length = queues.length;
        TransactionQueueData[] data = new TransactionQueueData[length];
        for (int i=0; i<length; i++) {
            data[i] = queues[i].getStatistics();
        }
        
        String sort = mRequest.getParameter("sort");
        boolean reverse = false;

        if (sort == null) {
            sort = mRequest.getParameter("sort_r");
            if (sort != null) {
                reverse = true;
            }
        }

        if (sort == null) {
            return data;
        }

        BeanComparator c = BeanComparator.forClass(TQTeaServletTransactionQueueData.class);

        if ("averageServiceDuration".equals(sort)) {
            c = c.orderBy("totalServiced").using(new ZeroHigh());
        }
        else if ("averageQueueDuration".equals(sort)) {
            c = c.orderBy("totalEnqueued").using(new ZeroHigh());
        }

        try {
            c = c.orderBy(sort);
        }
        catch (IllegalArgumentException e) {
        }

        if (reverse) {
            c = c.reverse();
        }
        
        Arrays.sort(data, c);
        
        return data;
    }

    public void resetTransactionQueues(String handlerName,
                                       String stageName,
                                       String servletName,
                                       String queueName)
        throws ServletException
    {
        TransactionQueue[] queues = getQueues
            (handlerName, stageName, servletName, queueName);

        for (int i=0; i<queues.length; i++) {
            queues[i].resetStatistics();
        }
    }

    private TransactionQueue[] getQueues(String handlerName,
                                         String stageName,
                                         String servletName,
                                         String queueName)
        throws ServletException
    {
        TransactionQueue[] queues = {};

        Map handlers = getBaristaAdmin().getHttpServer().getHttpHandlers();

        HttpHandler handler = (HttpHandler)handlers.get(handlerName);
        if (handler == null) {
            return queues;
        }

        if (stageName == null) {
            HttpHandler.Config config = handler.getConfig();
            TransactionQueue newQueue = config.getNewSocketQueue();
            TransactionQueue perQueue = config.getPersistentSocketQueue();

            if (queueName == null) {
                queues = new TransactionQueue[] {newQueue, perQueue};
            }
            else if (queueName.equals(newQueue.getName())) {
                queues = new TransactionQueue[] {newQueue};
            }
            else if (queueName.equals(perQueue.getName())) {
                queues = new TransactionQueue[] {perQueue};
            }

            return queues;
        }

        HttpHandlerStage stage = null;
        HttpHandlerStage[] stages = handler.getStages();
        for (int i=0; i<stages.length; i++) {
            if (stageName.equals(stages[i].getConfig().getLog().getName())) {
                stage = stages[i];
                break;
            }
        }

        if (stage == null || !(stage instanceof HttpServletDispatcher)) {
            return queues;
        }

        HttpServletDispatcher dispatcher = (HttpServletDispatcher)stage;

        if (servletName == null) {
            queues = dispatcher.getTransactionQueues();
        }
        else {
            queues = dispatcher.getTransactionQueues(servletName);
        }

        if (queueName == null) {
            return queues;
        }

        for (int i=0; i<queues.length; i++) {
            if (queueName.equals(queues[i].getName())) {
                return new TransactionQueue[]{queues[i]};
            }
        }

        return queues;
    }

    public TransactionQueue getTransactionQueue(String queueName) throws ServletException {
        Map handlers = getBaristaAdmin().getHttpServer().getHttpHandlers();
        for (Iterator i = handlers.values().iterator(); i.hasNext(); ) {
            HttpHandler handler = (HttpHandler) i.next();
            HttpHandlerStage[] stages = handler.getStages();
            for (int j = 0; j < stages.length; j++) {
                HttpServletDispatcher dispatcher = (HttpServletDispatcher)stages[j];
                TransactionQueue[] queues = dispatcher.getTransactionQueues();
                for (int k = 0; k < queues.length; k++)
                    if (queues[k].getName().endsWith(queueName))
                        return queues[k];
            }
        }
        return null;
    }

    private static class ZeroHigh implements Comparator {
        public int compare(Object obj1, Object obj2) {
            int i1 = ((Integer)obj1).intValue();
            int i2 = ((Integer)obj2).intValue();
            if (i1 == 0) {
                if (i2 != 0) {
                    return 1;
                }
            }
            else if (i2 == 0) {
                if (i1 != 0) {
                    return -1;
                }
            }
            return 0;
        }
    }
}
