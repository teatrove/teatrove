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

package org.teatrove.barista.http;

import java.io.*;
import java.net.*;
import java.security.*;
import javax.net.*;
import javax.net.ssl.*;
import javax.security.cert.*;

//Note: in JDK1.4, these are defined in javax.net.ssl package
import com.sun.net.ssl.SSLContext;
import com.sun.net.ssl.KeyManager;
import com.sun.net.ssl.KeyManagerFactory;
import com.sun.net.ssl.TrustManager;
import com.sun.net.ssl.TrustManagerFactory;

/**
 * @author Brian S O'Neill
 */
public class SSLServerSocketFactory
    extends javax.net.ssl.SSLServerSocketFactory {

    private javax.net.ssl.SSLServerSocketFactory mFactory;

    public SSLServerSocketFactory() throws Exception {
        super();

        // This may be redundant if you have added this to your 
        // JDK_HOME/jre/lib/security/java.security file
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

        // KeyManager stuff, currently the refernce implementation only
        // supports SunX509 as the key management algorithm.
        
        // Load the keystore from the home directory of the user running
        // the server.
        String keyStoreName = 
            System.getProperty("java.home") + File.separator + "lib" 
                                            + File.separator +"security" 
                                            + File.separator + "cacerts";
        
        InputStream in = new FileInputStream(keyStoreName);
        
        KeyStore ks = KeyStore.getInstance("JKS");
        
        ks.load(in, null);
        
        // Get the key manager factory as we have the preliminaries out of
        // the way.
        KeyManagerFactory km = KeyManagerFactory.getInstance("SunX509");
        
        // Change this to whatever the password is for the key.
        char[] password = {'B','a','r','i','s','t','a'};
        try {
            km.init(ks, password);
        }
        finally {
            password = null;
        }
        
        KeyManager[] keyManagers = km.getKeyManagers();
        
        TrustManagerFactory tmFactory = 
            TrustManagerFactory.getInstance("SunX509");
        tmFactory.init(ks);
        TrustManager[] trustManagers = tmFactory.getTrustManagers();
        
        SSLContext context = SSLContext.getInstance("TLS");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        
        context.init(keyManagers, trustManagers, random);
        
        // Finally we can create a socket factory.
        mFactory = context.getServerSocketFactory();
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        return mFactory.createServerSocket(port);
    }

    public ServerSocket createServerSocket
        (int port, int backlog) throws IOException
    {
        return mFactory.createServerSocket(port, backlog);
    }

    public ServerSocket createServerSocket
        (int port, int backlog, InetAddress bind) throws IOException
    {
        return mFactory.createServerSocket(port, backlog, bind);
    }

    public String[] getDefaultCipherSuites() {
        return mFactory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return mFactory.getSupportedCipherSuites();
    }
}
