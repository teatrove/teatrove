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

import java.rmi.Naming;
import java.net.DatagramPacket;
import org.teatrove.trove.log.Syslog;

/**
 * 
 * @author Jonathan Colwell
 */
public class AutomaticClusterManagementThread extends Thread {

    private final boolean DEBUG = false;

    private boolean exit;
    private boolean mActive;
    private ClusterManager mClusterManager;

    protected AutomaticClusterManagementThread(ClusterManager manager) {
        super();
        mClusterManager = manager;
        mActive = true;
    }

    protected AutomaticClusterManagementThread(ClusterManager manager, 
                                               boolean active) {
        super();
        mClusterManager = manager;
        mActive = active;
    }

    protected AutomaticClusterManagementThread(ClusterManager manager, 
                                               String name) {
        super(name);
        mClusterManager = manager;
        mActive = true;
    }

    protected AutomaticClusterManagementThread(ClusterManager manager, 
                                               String name, 
                                               boolean active) {
        super(name);
        mClusterManager = manager;
        mActive = active;
    }

    public void kill() {
        exit = true;
    }

    public void run() {
        exit = false;
        while (!exit) {
            try {

                String packetString = obtainPacketString();        

                if (packetString.indexOf("quit") >= 0) {
                    exit = true;
                }
                else {
                    updateCluster(packetString);
                }   
            }
            catch (Exception e) {
                e.printStackTrace();
                exit = true;
            }
        }
    }

    protected String obtainPacketString() throws Exception {

        DatagramPacket pack = mClusterManager.getNextPacket();
        byte[] packetData = pack.getData();
        int len = pack.getLength();
        if (DEBUG) {
            Syslog.debug(new String(packetData,0,len));
        }
        
        return new String(packetData,0,len,"ISO-8859-1");
    }
 
    protected void updateCluster(String packetString) throws Exception {

        int commandIndex = packetString.indexOf('~');
        int clusterIndex = packetString.lastIndexOf('~');

        if (commandIndex > 0 && clusterIndex > commandIndex) {

            String command = packetString
                .substring(0,commandIndex);
            int commandCode = -1;

            if ("join".equals(command)) {
                commandCode = 'j';
            }
            else if ("accept".equals(command)) {
                commandCode = 'a';
            }
            else if ("ping".equals(command)) {
                commandCode = 'p';
            }
            else if ("leave".equals(command)) {
                commandCode = 'L';
            }

            if (commandCode > 0) {
                String clusterName = packetString
                    .substring(commandIndex+1,clusterIndex);
                String hostName = packetString
                    .substring(clusterIndex+1);
                            
                String namingURL =
                    ("//" + hostName + ':' + 
                     mClusterManager.getRMIPort()
                     + '/' + clusterName);


                if (mClusterManager.getCluster().getClusterName()
                    .equals(clusterName)) {                 
                    if (DEBUG) {
                        Syslog.debug("Looking Up " + namingURL);
                    }
                    
                    Clustered bcl = null;
                    try {
                        bcl = (Clustered)Naming.lookup(namingURL);
                    }
                    catch (Exception e) {
                        if (DEBUG) {
                            Syslog.debug(e.getMessage());
                        }
                    }

                    if (bcl != null) {
                        if (DEBUG) {
                            Syslog.debug(namingURL + " Found on " 
                                         + bcl.getServerName());
                        }

                        if (commandCode >= 'a') {
                            if (commandCode < 'p' && 
                                !mClusterManager.getCluster().containsPeer(bcl)) {
                                mClusterManager.getCluster().addPeer(bcl);
                            }
                                    
                            /*
                             * now reply to the new guy if this is an 
                             * active thread, and the new guy is not yourself.
                             */
                            if (commandCode > 'a' && !bcl.getServerName()
                                .equals(mClusterManager.getCluster()
                                        .getServerName())
                                && mActive) {
                                try {
                                    mClusterManager.send
                                        (("accept~" + clusterName + '~'
                                          + mClusterManager.getCluster()
                                          .getServerName()).getBytes());
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else if (commandCode == 'L') {
                            try {
                                mClusterManager.getCluster()
                                    .removePeer(bcl);
                            }
                            catch (Exception e) {
                                if (DEBUG) {
                                    Syslog.debug("error removing " + hostName 
                                                 + " from the cluster");
                                }
                            }
                        }               
                    }
                }
            }     
        }
    }
}



























































































