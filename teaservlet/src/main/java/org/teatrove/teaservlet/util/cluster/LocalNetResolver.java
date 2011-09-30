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

import java.io.IOException;
import java.net.InetAddress;
import java.util.StringTokenizer;

import org.teatrove.trove.log.Syslog;

/**
 * 
 * @author Jonathan Colwell
 */
public class LocalNetResolver {

    public static final boolean DEBUG = false;

    public static InetAddress resolveLocalNet(String host, 
                                              String netInterface) 
        throws IOException {

        if (netInterface != null) {
            try {
                byte[] mask = {(byte)255,(byte)255,(byte)255,(byte)0};
                int slashindex = -1;
                if ((slashindex = netInterface.indexOf('/')) >= 0) {
                    int maskID = Integer
                        .parseInt(netInterface.substring(slashindex+1));
                    netInterface = netInterface.substring(0,slashindex);
                    slashindex = (0x80000000 >> maskID-1);
                    mask[3] = (byte)(slashindex & 0xFF);
                    mask[2] = (byte)((slashindex >> 8) & 0xFF);
                    mask[1] = (byte)((slashindex >> 16) & 0xFF);
                    mask[0] = (byte)((slashindex >> 24) & 0xFF);
                }

                StringTokenizer st = new StringTokenizer(netInterface," .");
                if (st.countTokens() == 4) {
                    byte[] maskedNet = new byte[4];
                    for(int k=0; k<4;k++) {
                        String token = st.nextToken();
                        maskedNet[k] = (byte)(Integer.parseInt(token) 
                                              & mask[k]);
                    }
                    if (DEBUG) {
                        Syslog.debug("net: "
                                     + (maskedNet[0] &0xFF)
                                     + "." + (maskedNet[1] & 0xFF)
                                     + "." + (maskedNet[2] & 0XFF)
                                     + "." + (maskedNet[3] & 0xFF)); 
                    }

                    InetAddress[] addresses = InetAddress.getAllByName(host);
                    
                    if (DEBUG) {
                        Syslog.debug("addresses on this host.");
                        for (int j = 0;j<addresses.length;j++) {
                            Syslog.debug(addresses[j].getHostAddress());
                        }
                    }


                    //pick the address that is on the localNet
                    for (int j=0;j<addresses.length;j++) {
                        byte[] testAddress = addresses[j].getAddress();
                        if (DEBUG) {
                            Syslog.debug("testing: " 
                                         + addresses[j].getHostAddress());
                        }
                        if (maskedNet[0] == (testAddress[0] & mask[0]) 
                            && maskedNet[1] == (testAddress[1] & mask[1]) 
                            && maskedNet[2] == (testAddress[2] & mask[2]) 
                            && maskedNet[3] == (testAddress[3] & mask[3])) {

                            Syslog.info(addresses[j].getHostAddress() 
                                        + " matched the specified localNet");
                            
                            return addresses[j];                             
                        }
                    }
                }
            }
            catch (Exception e) {
                Syslog.warn(e);
            }
            throw new IOException("Failed to resolve local network");
        }
        return InetAddress.getByName(host);
    }
}
