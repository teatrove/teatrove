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

import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import org.teatrove.trove.log.Log;
import org.teatrove.teaservlet.*;
import org.teatrove.teaservlet.util.cluster.ClusterManager;
import org.teatrove.teaservlet.util.cluster.Clustered;
import org.teatrove.teaservlet.util.cluster.Restartable;
import org.teatrove.barista.http.*;

/**
 * The barista admin application defines functions for administering Barista.
 *
 * @author Reece Wilton
 */
public class BaristaAdminApplication implements AdminApp {
    private ApplicationConfig mConfig;
    private Log mLog;
    private String mAdminKey;
    private String mAdminValue;
    private BaristaAdmin mAdmin;
    private ClusterManager mClusterManager;

    /**
     * Initializes the Application.
     * @param config the application's configuration object
     */
    public void init(ApplicationConfig config) {
        mConfig = config;
        mLog = config.getLog();

        //REMOTE stuff
        try {          

            String servers = config
                .getInitParameter("cluster.servers");
             String clusterName = config.getInitParameter("cluster.name");
            int rmiPort = 1099;
            int multicastPort = 1099;
            InetAddress multicastGroup = null;
            String netInterface = config
                    .getInitParameter("cluster.localNet");
            try {
                rmiPort = Integer.parseInt(config
                                 .getInitParameter("cluster.rmi.port"));
         
                multicastPort = 
                    Integer.parseInt(config
                           .getInitParameter("cluster.multicast.port"));

                multicastGroup = 
                    InetAddress.getByName(config
                               .getInitParameter("cluster.multicast.group"));
            }
            catch (NumberFormatException nfe) {}
            catch (UnknownHostException uhe) {}
            if (clusterName == null) {
                clusterName = "Unnamed_Cluster";
            }
            if (multicastGroup != null) {
                mClusterManager = new ClusterManager(getBaristaAdmin(),
                                                     "Barista_" + clusterName,
                                                     null,
                                                     multicastGroup,
                                                     multicastPort,
                                                     rmiPort,
                                                     netInterface,
                                                     servers);
                mClusterManager.joinCluster();
                mClusterManager.launchAuto();
            }
            else if (servers != null) {
                mClusterManager = new ClusterManager(getBaristaAdmin(),
                                                     "Barista_" + clusterName,
                                                     null,
                                                     rmiPort,
                                                     netInterface,
                                                     servers);
            }
            else {
                mLog.info("No cluster configured.");
            }
        }
        catch (Exception e) {
            mLog.warn(e);
        }

        // Get the admin key/value.
        mAdminKey = config.getInitParameter("admin.key");
        mAdminValue = config.getInitParameter("admin.value");
    }

    public void destroy() {
        if (mClusterManager != null) {
            mClusterManager.killAuto();
        }
    }

    public Object createContext(ApplicationRequest request,
                                ApplicationResponse response) {
        return new BaristaAdminContextImpl(request, response, this);
    }

    public Class getContextType() {
        return BaristaAdminContext.class;
    }

    public ServletConfig getServletConfig() {
        return mConfig;
    }

    public Log getLog() {
        return mLog;
    }

    public void adminCheck(ApplicationRequest request,
                           ApplicationResponse response)
        throws AbortTemplateException
    {
        if (mAdminKey == null) {
            return;
        }

        // Check for admin key.
        String adminParam = request.getParameter(mAdminKey);

        // Look in cookie for admin param.
        if (adminParam == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (int i = 0; i < cookies.length; i++) {
                    Cookie cookie = cookies[i];
                    if (cookie.getName().equals(mAdminKey)) {
                        adminParam = cookie.getValue();
                    }
                }
            }
        }

        if (adminParam != null && adminParam.equals(mAdminValue)) {
            // Set the admin param in the cookie.
            Cookie c = new Cookie(mAdminKey, adminParam);
            // Save cookie for 7 days.
            c.setMaxAge(24 * 60 * 60 * 7);
            c.setPath("/");
            response.addCookie(c);
        }
        else {
            // User is unauthorized.

            mLog.warn("Unauthorized Admin access to " +
                      request.getRequestURI() +
                      " from " + request.getRemoteAddr() +
                      " - " + request.getRemoteHost() +
                      " - " + request.getRemoteUser());

            try {
                response.sendError
                    (response.SC_NOT_FOUND, request.getRequestURI());
            }
            catch (IOException e) {
            }

            throw new AbortTemplateException();
        }
    }

    public BaristaAdmin getBaristaAdmin() {

        if (mAdmin == null) {
            ServletContext context = getServletConfig().getServletContext();

            HttpServer server =
                (HttpServer)context.getAttribute("org.teatrove.barista.http.HttpServer");
    
            mAdmin = new BaristaAdmin(context, server);
        }
        return mAdmin;
    }

    /**
     * The cluster flag tells the application to get cluster information
     * along with the admin.
     */
    public BaristaAdmin getBaristaAdmin(ApplicationRequest request,
                                        ApplicationResponse response)
        throws ServletException
    {
        adminCheck(request, response);

        BaristaAdmin bAdmin = getBaristaAdmin();
        
        if (mClusterManager != null) {
            mClusterManager.resolveServerNames();
        }
           
     /* Is the user changing the log settings?
        String param = request.getParameter("log");
        if (param != null) {
            Log log;
            try {
                log = (Log)ObjectIdentifier.retrieve(param);
            }
            catch (ClassCastException e) {
                log = null;
            }

            if (log != null) {
                String setting = request.getParameter("enabled");
                if (setting != null) {
                    log.setEnabled(setting.equals("true"));
                }
                setting = request.getParameter("debug");
                if (setting != null) {
                    log.setDebugEnabled(setting.equals("true"));
                }
                setting = request.getParameter("info");
                if (setting != null) {
                    log.setInfoEnabled(setting.equals("true"));
                }
                setting = request.getParameter("warn");
                if (setting != null) {
                    log.setWarnEnabled(setting.equals("true"));
                }
                setting = request.getParameter("error");
                if (setting != null) {
                    log.setErrorEnabled(setting.equals("true"));
                }
            }
        }
     */
       
        // Is the user restarting server?
        try {
            String param = request.getParameter("restart");
            if (param != null) {
                if ("cluster".equals(param)) {
                    restart(true,bAdmin);
                }
                else {
                    bAdmin.restart(new Boolean(true));
                }
            }
            else {
                restart(false,bAdmin);
            }
        }
        catch (Exception e) {
            mLog.error(e);
        }

        return bAdmin;
    }

    
    public void restart(boolean really, BaristaAdmin bAdmin) 
        throws IOException {

        if (mClusterManager != null) {
            Clustered[] peers = 
                mClusterManager.getCluster().getKnownPeers();
            final ClusterThread[] ct = new ClusterThread[peers.length];
            for (int j=0;j<peers.length;j++) {
                try {
                    bAdmin.setServerReloadStatus(peers[j].getServerName(), 
                                                 408, "Timed out");
                   
                    ct[j] = new ClusterThread(bAdmin,peers[j],really);
                    ct[j].start();
                }
                catch (IOException e) {
                    mLog.warn(e);
                }
            }
        
            Thread monitor = new Thread("Cluster restart monitor") {
                    public void run() {
                        for (int i=0; i<ct.length; i++) {
                            if (ct[i] != null) {
                                try {
                                    ct[i].join();
                                }
                                catch (InterruptedException e) {
                                    mLog.warn(e);
                                    break;
                                }
                            }
                        }
                    }
                };
        
            monitor.start();
            try {
                // Wait at most 30 seconds for all servers to respond.
                monitor.join(30000);
            }
            catch (InterruptedException e) {
                mLog.warn(e);
            }
        
            monitor.interrupt();
        
            for (int i=0; i<ct.length; i++) {
                if (ct[i] != null) {
                    ct[i].interrupt();
                }
            }
        }
    }

        


    

    /**
     * This implementation uses hard coded link information, but other
     * applications can dynamically determine their admin links.
     */
    public AppAdminLinks getAdminLinks() {

        AppAdminLinks links = new AppAdminLinks(mConfig.getName());

        links.addAdminLink("System&nbsp;Information",
                           "system.barista.AdminInfo");
        links.addAdminLink("System&nbsp;Activity",
                           "system.barista.AdminActivity");
        // Full logs are now provided byt the teaservlet's log page.
        //links.addAdminLink("Logs","system.barista.LogViewer");
        links.addAdminLink("HTTP&nbsp;Handlers",
                           "system.barista.AdminHandlers");
        links.addAdminLink("Echo&nbsp;Request",
                           "system.barista.AdminEcho");
        links.addAdminLink("Restart","system.barista.AdminRestart");
        links.addAdminLink("Inactive&nbsp;Templates",
                           "system.barista.AdminInactiveTemplates");

        return links;
    }


    private class ClusterThread extends Thread {
        private BaristaAdmin mBAdmin;
        private Clustered mClusterPeer;
        private Boolean mReally;
     
        public ClusterThread(BaristaAdmin admin,
                             Clustered peer,
                             boolean really) {
            mBAdmin = admin;
            mClusterPeer = peer;
            mReally = new Boolean(really);

        }

        public void run() {
            try {
                try {
                    mBAdmin.setServerReloadStatus(mClusterPeer
                                                  .getServerName(),
                                                  200, 
                                                  (String)
                                                  ((Restartable)mClusterPeer)
                                                  .restart(mReally));
                }
                catch (IOException e) {
                    e.printStackTrace();
                    mBAdmin.setServerReloadStatus
                        (mClusterPeer.getServerName(),500, e.toString());
                }
            }
            catch (RemoteException re) {
                try {
                    mClusterManager.getCluster().removePeer(mClusterPeer);
                }
                catch (RemoteException re2) {
                    re2.printStackTrace();
                }
                mBAdmin.setServerReloadStatus
                    ("N/A",500, "Unknown host");
            }
        }
    }
}
