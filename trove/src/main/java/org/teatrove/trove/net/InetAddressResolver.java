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

import java.net.*;
import java.util.*;
import java.lang.ref.*;
import org.teatrove.trove.util.SoftHashMap;

/**
 * Allows network host names to resolve to inet addresses via event
 * notification.
 * <p>
 * Note: This class makes use of the java.util.Timer class, available only in
 * Java 2, version 1.3. 
 *
 * @author Brian S O'Neill
 */
public class InetAddressResolver {
    // Wait for 10 minutes before trying to resolve again.
    private static final long RESOLVE_PERIOD = 10 * 60 * 1000;
    private static Timer cResolveTimer;

    private static Map cResolvers = new SoftHashMap();

    private static Timer getResolveTimer() {
        Timer timer = cResolveTimer;
        if (timer == null) {
            timer = new Timer(true);
            timer.schedule(new TimerTask() {
                public void run() {
                    Thread.currentThread().setName("InetAddressResolver");
                }
            }, 0);
            cResolveTimer = timer;
        }
        return timer;
    }

    /**
     * Resolve the host name into InetAddresses and listen for changes.
     * The caller must save a reference to the resolver to prevent it from
     * being reclaimed by the garbage collector.
     *
     * @param host host to resolve into InetAddresses
     * @param listener listens for InetAddresses
     */    
    public static InetAddressResolver listenFor(String host,
                                                InetAddressListener listener) {
        synchronized (cResolvers) {
            InetAddressResolver resolver = 
                (InetAddressResolver)cResolvers.get(host);
            if (resolver == null) {
                resolver = new InetAddressResolver(host);
                cResolvers.put(host, resolver);
            }
            resolver.addListener(listener);
            return resolver;
        }
    }

    private final String mHost;

    private List mListeners = new ArrayList();
    
    // Is either an instance of UnknownHostException or InetAddress[].
    private Object mResolutionResults;

    private InetAddressResolver(String host) {
        mHost = host;

        // Initial delay is random so that requests against other hosts are
        // staggered.
        long delay = (long)(Math.random() * RESOLVE_PERIOD);
        getResolveTimer().schedule(new Resolver(this), delay, RESOLVE_PERIOD);
    }

    private synchronized void addListener(InetAddressListener listener) {
        mListeners.add(listener);
        if (!resolveAddresses()) {
            // Ensure that new listener is called the first time.
            notifyListener(listener);
        }
    }

    private synchronized void notifyListener(InetAddressListener listener) {
        if (mResolutionResults instanceof UnknownHostException) {
            listener.unknown((UnknownHostException)mResolutionResults);
        }
        else {
            InetAddress[] addresses = (InetAddress[])mResolutionResults;
            addresses = (InetAddress[])addresses.clone();
            listener.resolved(addresses);
        }
    }

    // Returns true if anything changed from the last time this was invoked.
    private synchronized boolean resolveAddresses() {
        boolean changed;
        try {
            InetAddress[] addresses = InetAddress.getAllByName(mHost);
            if (mResolutionResults instanceof UnknownHostException) {
                changed = true;
            }
            else {
                // Results may be ordered differently, so keep them sorted.
                Arrays.sort(addresses, new Comparator() {
                    public int compare(Object a, Object b) {
                        return ((InetAddress)a).getHostAddress()
                            .compareTo(((InetAddress)b).getHostAddress());
                    }
                });

                changed = !Arrays.equals
                    (addresses, (InetAddress[])mResolutionResults);
            }
            mResolutionResults = addresses;
        }
        catch (UnknownHostException e) {
            changed = !(mResolutionResults instanceof UnknownHostException);
            mResolutionResults = e;
        }

        if (changed) {
            int size = mListeners.size();
            for (int i=0; i<size; i++) {
                notifyListener((InetAddressListener)mListeners.get(i));
            }
        }

        return changed;
    }

    private class Resolver extends TimerTask {
        // Weakly references owner so that the timer won't prevent it from
        // being garbage collected.
        private final Reference mOwner;

        public Resolver(InetAddressResolver owner) {
            mOwner = new WeakReference(owner);
        }

        public void run() {
            InetAddressResolver owner = (InetAddressResolver)mOwner.get();
            if (owner == null) {
                cancel();
            }
            Thread t = Thread.currentThread();
            String originalName = t.getName();
            t.setName("InetAddressResolver:" + owner.mHost);
            try {
                owner.resolveAddresses();
            }
            finally {
                t.setName(originalName);
            }
        }
    }
}
