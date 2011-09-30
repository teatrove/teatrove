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

package org.teatrove.trove.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import org.teatrove.trove.log.Log;
import org.teatrove.trove.log.Syslog;

/**
 * This class provides functionality for finding the right network interface.
 * 
 * @author Jonathan Colwell, Reece Wilton
 */
public class LocalNetResolver {

    /**
     * The method finds the local InetAddress to bind to.  The netInterface 
     * parameter is the bind mask of the network interface.  It must be
     * specified in the format:  10.192.0.0/24
     * 
     * There are four possible ways this method can be called:
     * 
     * 1. host == null and netInterface == null.
     *      The local host name is looked up and returned.
     * 
     * 2. host != null and netInterface == null.
     *      The host is looked up and returned.
     * 
     * 3. host == null and netInterface != null.
     *      All the local network adapters and all their IPs are looked up.  The
     *      first IP that matches the bind mask is returned.
     *     
     * 4. host != null and netInterface != null.  
     *      The host is looked up and the first IP that matches the bind mask is 
     *      returned.
     * 
     * @param host  the name or IP of the host
     * @param netInterface  the bind mask of the network interface
     * @return
     * @throws IOException
     */
    // TODO: remove the LocalNetResolver from TeaServlet and use this instead 
    public static InetAddress resolveLocalNet(String host, 
                                              String netInterface) 
    	throws IOException {
        
        return resolveLocalNet(host, netInterface, null);
    }
    
    public static InetAddress resolveLocalNet(String host, 
                                              String netInterface,
                                              Log log) 
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
                    if (log != null) {
                        log.debug("net: "
                                  + (maskedNet[0] &0xFF)
                                  + "." + (maskedNet[1] & 0xFF)
                                  + "." + (maskedNet[2] & 0XFF)
                                  + "." + (maskedNet[3] & 0xFF)); 
                    }

                    InetAddress[] addresses;
                    if (host != null) {
                        addresses = InetAddress.getAllByName(host);
                    }
                    else {
                        addresses = getAllLocalInetAddresses(log);
                    }
                    if (log != null) {
                        log.debug("addresses on this host.");
                        for (int j = 0;j<addresses.length;j++) {
                            log.debug(addresses[j].getHostAddress());
                        }
                    }

                    //pick the address that is on the localNet
                    for (int j=0;j<addresses.length;j++) {
                        byte[] testAddress = addresses[j].getAddress();
                        if (log != null) {
                            log.debug("testing: " 
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
        if (host != null) {
            return InetAddress.getByName(host);
        }
        else {
            return InetAddress.getLocalHost();
        }
    }
    
    // TODO: the IPs returned by this method don't return host names.  
    //       calling getHostName returns the IP.  how do we get the host?
    private static InetAddress[] getAllLocalInetAddresses(final Log log) 
    throws SocketException {

        final List addresses = new ArrayList();
        
        // get the network adapters
        final Enumeration netInterfaces = NetworkInterface.getNetworkInterfaces();
        while (netInterfaces.hasMoreElements()) {
            final NetworkInterface ni = (NetworkInterface)netInterfaces.nextElement();
            if (log != null) {
                log.debug("Found interface: " + ni.getName());
            }
            
            // get the IPs for this network interface
            final Enumeration ips = ni.getInetAddresses();
            while (ips.hasMoreElements()) {
                final InetAddress ip = (InetAddress)ips.nextElement();
                if (log != null) {
                    log.debug("Found ip: " + ip.getHostName() + "/" 
                        + ip.getHostAddress() + " on interface: " + ni.getName());
                }

                // ignore the local loopback address: 127.0.0.1
                if (!ip.isLoopbackAddress()) {
                    if (log != null) {
                        log.debug("Let's add this IP: " + ip.getCanonicalHostName());
                    }
                    addresses.add(ip);
                }
            }
        }
        
        return (InetAddress[])addresses.toArray(new InetAddress[addresses.size()]);
    }
}
