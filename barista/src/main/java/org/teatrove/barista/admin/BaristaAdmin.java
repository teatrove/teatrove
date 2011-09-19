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

import java.util.*;
import java.net.*;
import java.text.DateFormat;
import java.rmi.RemoteException;
import javax.servlet.ServletContext;
import org.teatrove.teaservlet.util.NameValuePair;
import org.teatrove.teaservlet.util.cluster.Restartable;
import org.teatrove.trove.util.BeanComparator;
import org.teatrove.barista.http.*;

/**
 * @author Brian S O'Neill
 */
public class BaristaAdmin implements Restartable {
    private static String cServerName;
    private static DateFormat cDateFormat;
    private Map mServerStatusMap;
    private ServletContext mContext;
    private HttpServer mServer;

    public BaristaAdmin(ServletContext context, HttpServer server) {
        mServerStatusMap = Collections.synchronizedMap(new HashMap());
        mContext = context;
        mServer = server;
        if (cDateFormat == null) {
            cDateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG,
                                                          DateFormat.LONG);
        }
    }

    public Object restart(Object paramObj) throws RemoteException {

        StringBuffer status = new StringBuffer();
        Date restartTime;
        
        try {
            if (paramObj != null && paramObj instanceof Boolean 
                && ((Boolean)paramObj).booleanValue()) {
                getHttpServer().start();
            }
            if (getHttpServer() != null) {
                if (cDateFormat != null) {
                    restartTime = getHttpServer().getStartDate();
                    if (restartTime != null) {
                        status.append("Server started on " 
                                      + cDateFormat
                                      .format(restartTime));
                        restartTime = getHttpServer().getLastRestartDate();
                        if (restartTime != null) {
                            status.append("\nServer last restarted on " 
                                          + cDateFormat.format(restartTime));
                        }
                    }
                }
                else {
                    status.append("Null DateFormat");
                }
            }
            else {
                status.append("Null Server");
            }
        }
        catch (Exception e) {
            status.append(e.getMessage());
        }
        return status.toString();
    
    }

    public ServletContext getServletContext() {
        return mContext;
    }

    public HttpServer getHttpServer() {
        return mServer;
    }

    public String getServerName() {
        if (cServerName == null) {
            try {
                cServerName = InetAddress.getLocalHost().getHostName();
            }
            catch (UnknownHostException e) {
            }
        }
        return cServerName;
    }

    public Package[] getLoadedPackages() {
        Package[] packages = Package.getPackages();
        Arrays.sort(packages, new Comparator() {
            public int compare(Object obj1, Object obj2) {
                return ((Package)obj1).getName().compareToIgnoreCase
                    (((Package)obj2).getName());
            }
        });
        return packages;
    }

    public long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    public long getTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    public NameValuePair[] getSystemProperties() {
        return copyProperties(System.getProperties());
    }

    public NameValuePair[] getBaristaProperties() {
        return copyProperties(mServer.getConfig().getProperties());
    }

    public HttpHandler[] getHttpHandlers() {
        Collection c = mServer.getHttpHandlers().values();
        return (HttpHandler[])c.toArray(new HttpHandler[c.size()]);
    }

    private NameValuePair[] copyProperties(Map props) {
        NameValuePair[] array = new NameValuePair[props.entrySet().size()];
        Iterator it = props.entrySet().iterator();
        for (int i=0; i<array.length; i++) {
            array[i] = new NameValuePair((Map.Entry)it.next());
        }
        Arrays.sort(array);
        return array;
    }

    public String[] getClusteredServers() {
        return (String[])mServerStatusMap.keySet()
            .toArray(new String[mServerStatusMap.size()]);      
    }

    public ServerStatus[] getReloadStatusOfServers() {
        ServerStatus[] statusArray =
            (ServerStatus[])mServerStatusMap.values()
            .toArray(new ServerStatus[0]);

        Comparator c = BeanComparator.forClass(ServerStatus.class)
            .orderBy("statusCode").reverse()
            .orderBy("serverName");
        Arrays.sort(statusArray, c);
        return statusArray;
    }



    public void setServerReloadStatus(String name,
                                      int statusCode, String message) {
        mServerStatusMap.put(name,new ServerStatus(name, statusCode, message));
    }


    public class ServerStatus {
        private String mServerName;
        private String mMessage;
        private int mStatusCode;

        public ServerStatus(String name, int statusCode, String message) {
            mServerName = name;
            mMessage = message;
            mStatusCode = statusCode;
        }

        public String getServerName() {
            return mServerName;
        }

        public String getMessage() {
            return mMessage;
        }

        public int getStatusCode() {
            return mStatusCode;
        }
    }
}
