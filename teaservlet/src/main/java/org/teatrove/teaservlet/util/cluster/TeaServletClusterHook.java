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

package org.teatrove.teaservlet.util.cluster;

import java.rmi.RemoteException;

/**
 * This class is used by the TeaServlet to share status information and reload 
 * capability across a cluster.
 * 
 * @author Jonathan Colwell
 */
public class TeaServletClusterHook extends ClusterHook 
    implements Restartable {

    Restartable mRestartable;

    public TeaServletClusterHook(Restartable restart,
                                 String clusterName, 
                                 String serverName)
        throws RemoteException {
        
        super(clusterName, serverName);
        mRestartable = restart;
    }

    public Object restart(Object paramObj) throws RemoteException {

        if (mRestartable != null) {
            return mRestartable.restart(paramObj);
        }
        else {
            return null;
        }
    }
}
