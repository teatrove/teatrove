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

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Vector;

/**
 * A fairly generic implementation of the @see Clustered interface.
 * This class could be used by any sort of server or application that wants to 
 * share information and call methods across a cluster.
 *
 * @author Jonathan Colwell
 */
public class ClusterHook extends UnicastRemoteObject 
    implements Clustered {

    private List mPeers;
    protected String mClusterName,mServerName;

    public ClusterHook(String clusterName, String serverName) 
        throws RemoteException {
        
        super();
        mPeers = new Vector();
        mClusterName = clusterName;
        if (serverName != null) {
            mServerName = serverName.toLowerCase();
        }
    }

    public String getServerName() throws RemoteException {

        if (mServerName == null) {
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                String hostname = localHost.getCanonicalHostName();
                if (hostname == null || hostname.length() == 0) {
                    hostname = localHost.getHostName();
                    if (hostname == null || hostname.length() == 0) {
                        hostname = localHost.getHostAddress();
                    }
                }
                
                mServerName = hostname.toLowerCase();
            }
            catch (UnknownHostException uhe) {
                uhe.printStackTrace();
            }
        }
        return mServerName;
    }

    public Clustered[] getKnownPeers() throws RemoteException {

        return (Clustered[])mPeers.toArray(new Clustered[mPeers.size()]);
    }

    public void addPeer(Clustered peer) throws RemoteException {

        mPeers.add(peer);
    }

    public boolean containsPeer(Clustered peer) throws RemoteException {

        return mPeers.contains(peer);
    }
    
    public void removePeer(Clustered peer) throws RemoteException {

        mPeers.remove(peer);
    }

    public String getClusterName() throws RemoteException {
        if (mClusterName == null) {
            mClusterName = "Unnamed_Cluster";
        }
        return mClusterName;
    }
}
