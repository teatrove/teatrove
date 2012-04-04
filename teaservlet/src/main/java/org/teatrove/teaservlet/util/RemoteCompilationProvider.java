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

package org.teatrove.teaservlet.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.teatrove.tea.compiler.CompilationProvider;
import org.teatrove.tea.compiler.CompilationSource;
import org.teatrove.trove.net.HttpClient;
import org.teatrove.trove.net.PlainSocketFactory;
import org.teatrove.trove.net.PooledSocketFactory;
import org.teatrove.trove.net.SocketFactory;
import org.teatrove.trove.util.ClassInjector;

/**
 * RemoteCompilerProvider provides tea source files by reading them from a 
 * remote location specified by a URL.
 *
 * <p>When given a URL, RemoteCompilerProvider provides all files with the
 * extension ".tea". If a destination directory is used, tea files that have a
 * matching class file that is more up-to-date will not be compiled, unless
 * they are forced to be re-compiled.
 *
 * @author Jonathan Colwell
 * @version
 * <!--$$Revision:--> 16 <!-- $--> 21 <!-- $$JustDate:-->  3/19/04 <!-- $-->
 * @see ClassInjector
 */
public class RemoteCompilationProvider implements CompilationProvider {
    
    public static final long DEFAULT_TIMEOUT = 30000;
    public static final String TEMPLATE_LOAD_PROTOCOL = "http://";

    private String mRemoteSourceDir;
    private Map<String, TemplateSourceInfo> mTemplateMap;
    private Map<String, SocketFactory> mSocketFactories;
    private long mTimeout;

    public RemoteCompilationProvider(String rootSourceDir) {
        this(rootSourceDir, DEFAULT_TIMEOUT);
    }
    
    public RemoteCompilationProvider(String rootSourceDir, long timeout) {
        mRemoteSourceDir = rootSourceDir;
        mTimeout = timeout;
        mSocketFactories = new HashMap<String, SocketFactory>();
        mTemplateMap = retrieveTemplateMap();
    }

    /**
     * Checks that the source code for a specified template exists.
     */
    @Override
    public boolean sourceExists(String name) {
        return mTemplateMap.containsKey(name);
    }
    
    @Override
    public String[] getKnownTemplateNames(boolean recurse) {
        return mTemplateMap.keySet().toArray(new String[0]);
    }
    
    @Override
    public CompilationSource createCompilationSource(String name) {
        TemplateSourceInfo template = mTemplateMap.get(name);
        return template == null ? null : new RemoteSource(template);
    }
      
    /**
     * returns a socket connected to a host running the TemplateServerServlet
     */
    HttpClient getTemplateServerClient(String remoteSource) throws IOException {
        SocketFactory factory = mSocketFactories.get(remoteSource);

        if (factory == null) {
            int port = 80;
            String host = remoteSource.substring(TEMPLATE_LOAD_PROTOCOL.length());

            int portIndex = host.indexOf("/");
        
            if (portIndex >= 0) {
                host = host.substring(0,portIndex);                         
            }
            portIndex = host.indexOf(":");
            if (portIndex >= 0) {
                try {
                    port = Integer.parseInt(host.substring(portIndex+1));
                }
                catch (NumberFormatException nfe) {
                    System.out.println("Invalid port number specified");
                }
                host = host.substring(0,portIndex);
            }
            factory = new PooledSocketFactory
                (new PlainSocketFactory(InetAddress.getByName(host), port, mTimeout));
            
            mSocketFactories.put(remoteSource, factory);
        }
        return new HttpClient(factory);
    }
    
    /**
     * turns a template name and a servlet path into a  
     */
    String createTemplateServerRequest(String servletPath,String templateName) {
        String pathInfo = servletPath.substring(servletPath.indexOf("/",TEMPLATE_LOAD_PROTOCOL.length()));
        if (templateName != null) {            
            pathInfo = pathInfo + templateName;
        }
        return pathInfo;
    }
    
    /**
     * creates a map relating the templates found on the template server
     * to the timestamp on the sourcecode
     */
    private Map<String, TemplateSourceInfo> retrieveTemplateMap() {
        Map<String, TemplateSourceInfo> templateMap = 
        	new TreeMap<String, TemplateSourceInfo>();
        
        String remoteSource = mRemoteSourceDir;
        if (!remoteSource.endsWith("/")) {
            remoteSource = remoteSource + "/";
        }
        
        try {
            HttpClient tsClient = getTemplateServerClient(remoteSource);

            HttpClient.Response response = tsClient.setURI(createTemplateServerRequest(remoteSource,null))
                .setPersistent(true).getResponse(); 

            if (response != null && response.getStatusCode() == 200) {

                Reader rin = new InputStreamReader
                    (new BufferedInputStream(response.getInputStream()));
            
                StreamTokenizer st = new StreamTokenizer(rin);
                st.resetSyntax();
                st.wordChars('!','{');
                st.wordChars('}','}');
                st.whitespaceChars(0,' ');
                st.parseNumbers();
                st.quoteChar('|');
                st.eolIsSignificant(true);
                String templateName = null; 
                int tokenID = 0;
                // ditching the headers by looking for "\r\n\r\n"
                /* 
                 * no longer needed now that HttpClient is being used but leave
                 * in for the moment.
                 *
                 * while (!((tokenID = st.nextToken()) == StreamTokenizer.TT_EOL 
                 *       && st.nextToken() == StreamTokenizer.TT_EOL) 
                 *       && tokenID != StreamTokenizer.TT_EOF) {
                 * }
                 */
                while ((tokenID = st.nextToken()) != StreamTokenizer.TT_EOF) {
                    if (tokenID == '|' || tokenID == StreamTokenizer.TT_WORD) {
                    
                        templateName = st.sval;
                    }
                    else if (tokenID == StreamTokenizer.TT_NUMBER 
                             && templateName != null) {
                        templateName = templateName.substring(1);
                        //System.out.println(templateName);
                        templateMap.put(templateName.replace('/','.'),
                                        new TemplateSourceInfo(
                                                               templateName,
                                                               remoteSource,
                                                               (long)st.nval));
                        templateName = null;
                    }
                }
            }
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return templateMap;
    }
    
    /**
     * inner class to store a templates' name, server location and timestamp
     */
    private class TemplateSourceInfo {
        public String name;
        public String server;
        public long timestamp;
        
        TemplateSourceInfo(String name,String server,long timestamp) {
            this.name = name;
            this.server = server;
            this.timestamp = timestamp;
        }
    }
    
    public class RemoteSource implements CompilationSource {

        TemplateSourceInfo mTemplate;
        
        public RemoteSource(TemplateSourceInfo template) {
            this.mTemplate = template;
        }

        @Override
        public String getSourcePath() {
            return createTemplateServerRequest(mTemplate.server,
                                               mTemplate.name + ".tea");
        }
        
        @Override
        public long getLastModified() {
            long remoteTimeStamp = mTemplate.timestamp;
            return remoteTimeStamp;
        }
        
        /**
         * get a input stream containing the template source data.
         */
        @Override
        public InputStream getSource() 
        	throws IOException {
        	
            HttpClient client = getTemplateServerClient(mTemplate.server);
            HttpClient.Response response = client
                .setURI(getSourcePath())
                .setPersistent(true).getResponse();            
            InputStream in = response.getInputStream();
            return in;
        }
    }
}
