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

package org.teatrove.teaservlet;

import java.util.*;
import java.rmi.RemoteException;
import java.lang.ref.*;

import org.teatrove.trove.log.Log;
import org.teatrove.trove.util.*;
import org.teatrove.trove.util.tq.*;
import org.teatrove.tea.runtime.TemplateLoader;
import org.teatrove.tea.runtime.Substitution;
import org.teatrove.teaservlet.util.cluster.*;
import javax.servlet.ServletException;


/**
 * Application that defines a cache function for templates. The cache is
 * applied over a region within a template and is called like:
 *
 * <pre>
 * cache() {
 *     // Cached template code and text goes here
 * }
 * </pre>
 *
 * The contents within the cache are used up to a configurable time-to-live
 * value, specified in milliseconds. A specific TTL value can be passed as an
 * argument to the cache function, in order to override the configured default.
 * <p>
 * Regions are keyed on enclosing template, region number, and HTTP query
 * parameters. An optional secondary key may be passed in which helps to
 * further identify the region. To pass in multiple values for the secondary
 * key, pass in an array.
 * <p>
 * The cache function can be invoked multiple times within a template and the
 * cache calls can be nested within each other. When templates are reloaded,
 * the cached regions are dropped.
 * <p>
 * The RegionCachingApplication can also compress cached regions and send the
 * GZIP compressed response to the client. The intention is not to reduce
 * memory usage on the server, but to conserve bandwidth. If the client
 * supports GZIP encoding, then it is eligible to receive a fully or partially
 * GZIP compressed response.
 * <p>
 * If provided with cluster configuration information to pass along to the 
 * {@link org.teatrove.teaservlet.util.cluster.ClusterManager}, status information 
 * may be shared between machines to compare cache sizes.
 *
 * @author Brian S O'Neill
 */
public class RegionCachingApplication implements AdminApp {

    private static final String CUSTOM_KEY = "REGION_CACHE_CUSTOM_KEY";

    private Log mLog;
    private int mCacheSize;
    private long mDefaultTTL;
    private long mTimeout;
    private String[] mHeaders;
    private int mCompressLevel;

    private ClusterManager mClusterManager;
    private ClusterCacheInfo mInfo;

    // Maps Templates to TemplateKeys
    private Map mTemplateKeys = new IdentityMap();

    // List of DepotLinks for linking TemplateLoaders and Depots. A Map isn't
    // used here because the list will be small, usually just one element.
    // After a template reload, the old Depots should be discarded as soon as
    // possible in order to free up memory. Iterating over the list can pick
    // up cleared references.
    private List mDepots = new ArrayList(10);

    // Shared by all Depots.
    private TransactionQueue mTQ;
    private boolean mUseLRU;

    /**
     * Accepts the following optional parameters:
     *
     * <pre>
     * cache.size   Amount of most recently used cache items to keep, which is
     *              500 by default.
     * default.ttl  Default time-to-live of cached regions, in milliseconds.
     *              If left unspecified, default.ttl is 5000 milliseconds.
     * timeout      Maximum milliseconds to wait on cache before serving an
     *              expired region. Default value is 500 milliseconds.
     * headers      List of headers to use for all keys. i.e. User-Agent or
     *              Host.
     * gzip         Accepts a value from 0 to 9 to set compression level. When
     *              non-zero, GZIP compression is enabled for cached regions.
     *              A value of 1 offers fast compression, and a value of 9
     *              offers best compression. A value of 6 is the typical
     *              default used by GZIP.
     *
     * transactionQueue  Properties for TransactionQueue that executes regions.
     *    max.threads    Maximum thread count. Default is 100.
     *    max.size       Maximum size of TransactionQueue. Default is 100.
     * </pre>
     */
    public void init(ApplicationConfig config) throws ServletException {
        mLog = config.getLog();

        PropertyMap props = config.getProperties();
        mCacheSize = props.getInt("cache.size", 500);
        mDefaultTTL = props.getNumber
            ("default.ttl", new Long(5000)).longValue();
        mTimeout = props.getNumber("timeout", new Long(500)).longValue();

        PropertyMap tqProps = props.subMap("transactionQueue");

        int maxPool = tqProps.getInt("max.threads", 100);
        ThreadPool tp = new ThreadPool(config.getName(), maxPool);
        tp.setTimeout(5000);
        tp.setIdleTimeout(60000);

        mTQ = new TransactionQueue(tp, config.getName() + " TQ", 100, 100);
        mTQ.applyProperties(tqProps);

        String headers = props.getString("headers");

        if (headers == null) {
            mHeaders = null;
        }
        else {
            StringTokenizer st = new StringTokenizer(headers, " ;,");
            int count = st.countTokens();
            if (count == 0) {
                mHeaders = null;
            }
            else {
                mHeaders = new String[count];
                for (int i=0; i<count; i++) {
                    mHeaders[i] = st.nextToken();
                }
            }
        }

        mCompressLevel = props.getInt("gzip", 0);

        if (mCompressLevel < 0) {
            mLog.warn("GZIP compression lowest level is 0. Level " +
                      mCompressLevel + " interpretted as 0");
            mCompressLevel = 0;
        }
        else if (mCompressLevel > 9) {
            mLog.warn("GZIP compression highest level is 9. Level " +
                      mCompressLevel + " interpretted as 9");
            mCompressLevel = 9;
        }

        mUseLRU = props.getBoolean("useLRUCache", false);
        
        initCluster(config);
    }
    
    public void destroy() {
        if (mClusterManager != null) {
            mClusterManager.killAuto();
        }
    }

    /**
     * Returns an instance of {@link RegionCachingContext}.
     */
    public Object createContext(ApplicationRequest request,
                                ApplicationResponse response) {
        return new RegionCachingContextImpl(this, request, response);
    }

    /**
     * Returns {@link RegionCachingContext}.class.
     */
    public Class getContextType() {
        return RegionCachingContext.class;
    }

    public AppAdminLinks getAdminLinks() {

        AppAdminLinks links = new AppAdminLinks(mLog.getName());
        links.addAdminLink("Depot","system.teaservlet.Depot");
        return links;
    }


    void cache(ApplicationRequest request, 
               ApplicationResponse response,
               Substitution s)
        throws Exception
    {
        cache(request, response, mDefaultTTL, null, s);
    }

    void cache(ApplicationRequest request, 
               ApplicationResponse response,
               Object key,
               Substitution s)
        throws Exception
    {
        cache(request, response, mDefaultTTL, key, false, s);
    }

    void cache(ApplicationRequest request, 
               ApplicationResponse response,
               long ttlMillis,
               Substitution s)
        throws Exception
    {
        cache(request, response, ttlMillis, null, s);
    }

    void cache(ApplicationRequest request, 
               ApplicationResponse response,
               long ttlMillis,
               Object key,
               Substitution s)
        throws Exception
    {
        cache(request, response, ttlMillis, key, false, s);
    }

    void cache(ApplicationRequest request, 
               ApplicationResponse response,
               long ttlMillis,
               Object key,
               boolean useCustomKeyOnly,
               Substitution s)
        throws Exception
    {
        TemplateKey templateKey = getTemplateKey(request.getTemplate());

        Object[] keyElements = null;

        if (useCustomKeyOnly) {
            keyElements = new Object[] {
                CUSTOM_KEY,
                key
            };
        }
        else {
            keyElements = new Object[] {
                templateKey,
                s.getIdentifier(),
                getHeaderValues(request),
                request.getQueryString(),
                key,
            };
        }
        
        key = new MultiKey(keyElements);
            
        ApplicationResponse.Command c =
            new CacheCommand(s, templateKey, ttlMillis, key);
        if (!response.insertCommand(c)) {
            c.execute(request, response);
        }
    }

    void nocache(ApplicationRequest request, 
                 ApplicationResponse response,
                 Substitution s)
        throws Exception
    {
        ApplicationResponse.Command c = new NoCacheCommand(s);
        if (!response.insertCommand(c)) {
            c.execute(request, response);
        }
    }

    private TemplateKey getTemplateKey(TemplateLoader.Template template) {
        synchronized (mTemplateKeys) {
            TemplateKey key = (TemplateKey)mTemplateKeys.get(template);
            if (key == null) {
                key = new TemplateKey(template);
                mTemplateKeys.put(template, key);
            }
            return key;
        }
    }

    private String[] getHeaderValues(ApplicationRequest request) {
        if (mHeaders == null) {
            return null;
        }

        String[] headers = (String[])mHeaders.clone();
        for (int i = headers.length; --i >= 0; ) {
            headers[i] = request.getHeader(headers[i]);
        }

        return headers;
    }

    Depot getDepot(TemplateLoader.Template template) {
        return getDepot(getTemplateKey(template));
    }

    Depot getDepot(TemplateKey key) {
        TemplateLoader depotKey = key.getTemplateLoader();
        if (depotKey == null) {
            return null;
        }

        DepotLink link;
        Object linkKey;
        Depot depot;

        synchronized (mDepots) {
            int size = mDepots.size();
            if (size == 1) {
                link = (DepotLink)mDepots.get(0);
                linkKey = link.get();
                if (linkKey == null) {
                    // Remove cleared reference.
                    mDepots.clear();
                }
                else if (linkKey == depotKey) {
                    return link.mDepot;
                }
            }
            else if (size > 1) {
                depot = null;
                Iterator it = mDepots.iterator();
                while (it.hasNext()) {
                    link = (DepotLink)it.next();
                    linkKey = link.get();
                    if (linkKey == null) {
                        // Remove cleared reference.
                        it.remove();
                    }
                    else if (linkKey == depotKey) {
                        depot = link.mDepot;
                        // Don't break loop: keep searching for cleared refs.
                    }
                }
                if (depot != null) {
                    return depot;
                }
            }

            // If here, no Depot found. Make one.
            depot = new Depot(null, mCacheSize, mCacheSize, mTQ, mTimeout, mUseLRU);
            mDepots.add(new DepotLink(depotKey, depot));
        }

        return depot;
    }


    private void initCluster(ApplicationConfig config) {
        
        try {
            String clusterName = config.getInitParameter("cluster.name");
            if (clusterName != null) {
                mInfo = new ClusterCacheInfoImpl(clusterName, null);
                mClusterManager = ClusterManager
                    .createClusterManager(config.getProperties(), mInfo);
            }
        }
        catch(Exception e) {
            mLog.warn("Failed to create ClusterManager.");
            mLog.warn(e);
        }
    }

    // Allows old templates to be garbage collected when reloaded.
    private static class TemplateKey extends WeakReference {
        TemplateKey(TemplateLoader.Template template) {
            super(template);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof TemplateKey) {
                TemplateKey other = (TemplateKey)obj;
                Object t = this.get();
                if (t != null) {
                    return t == other.get();
                }
            }
            return false;
        }

        public int hashCode() {
            Object t = get();
            return (t == null) ? 0 : t.hashCode();
        }

        public String toString() {
            TemplateLoader.Template t = (TemplateLoader.Template)get();
            if (t != null) {
                return t.getName();
            }
            else {
                return super.toString();
            }
        }

        TemplateLoader getTemplateLoader() {
            TemplateLoader.Template t = (TemplateLoader.Template)get();
            return (t != null) ? t.getTemplateLoader() : null;
        }
    }

    private static class DepotLink extends WeakReference {
        final Depot mDepot;

        DepotLink(TemplateLoader loader, Depot depot) {
            super(loader);
            mDepot = depot;
        }
    }

    private class CacheCommand implements ApplicationResponse.Command {
        final Substitution mSub;
        final TemplateKey mTemplateKey;
        final long mTTLMillis;
        final Object mKey;

        CacheCommand(Substitution s, TemplateKey templateKey,
                     long ttlMillis, Object key) {
            mSub = s.detach();
            mTemplateKey = templateKey;
            mTTLMillis = ttlMillis;
            mKey = key;
        }

        public void execute(ApplicationRequest request,
                            ApplicationResponse response)
            throws Exception
        {
            DetachedDataFactory factory =
                new DetachedDataFactory(this, response);

            ApplicationResponse.DetachedData data;
            Depot depot = getDepot(mTemplateKey);

            if (depot != null) {
                data = (ApplicationResponse.DetachedData)
                    depot.get(factory, mKey);
                if (mCompressLevel > 0 && !factory.mCalled) {
                    // If the factory wasn't called, then this was a cache hit.
                    // Its likely it will be seen again, so take the time to
                    // compress.
                    if (data != null && request.isCompressionAccepted()) {
                        try {
                            data.compress(mCompressLevel);
                        }
                        catch (UnsatisfiedLinkError e) {
                            // Native library to support compression not found.
                            mCompressLevel = 0;
                            mLog.error(e);
                        }
                    }
                }
            }
            else {
                // This should never happen, but just in case, generate data
                // without caching.
                data = (ApplicationResponse.DetachedData)factory.create(mKey);
            }
            
            if (data != null) {
                data.playback(request, response);
            }
        }

        Log getLog() {
            return mLog;
        }
    }

    private static class DetachedDataFactory
        implements Depot.PerishablesFactory
    {
        boolean mCalled;

        private CacheCommand mCommand;
        private ApplicationResponse mResponse;

        DetachedDataFactory(CacheCommand c, ApplicationResponse response) {
            mCommand = c;
            mResponse = response;
        }

        public Object create(Object xxx) throws InterruptedException {
            mCalled = true;
            try {
                return mResponse.execDetached(mCommand.mSub);
            }
            catch (InterruptedException e) {
                mCommand.getLog().error(e);
                throw e;
            }
            catch (Exception e) {
                mCommand.getLog().error(e);
                throw new InterruptedException(e.getMessage());
            }
        }
        
        public long getValidDuration() {
            return mCommand.mTTLMillis;
        }
    };

    private class NoCacheCommand implements ApplicationResponse.Command {
        private Substitution mSub;

        NoCacheCommand(Substitution s) {
            mSub = s.detach();
        }

        public void execute(ApplicationRequest request,
                            ApplicationResponse response) {
            try {
                mSub.detach().substitute(response.getHttpContext());
            }
            catch (Exception e) {
                mLog.error(e);
            }
        }
    }

    private class RegionCachingContextImpl 
        implements RegionCachingContext {

        private final RegionCachingApplication mApp;
        private final ApplicationRequest mRequest;
        private final ApplicationResponse mResponse;

        public RegionCachingContextImpl(RegionCachingApplication app,
                                        ApplicationRequest request,
                                        ApplicationResponse response) {
            mApp = app;
            mRequest = request;
            mResponse = response;
        }

        /**
         * Caches and reuses a region of a page. The cached region expies after
         * a default time-to-live period has elapsed.
         *
         * @param s substitution block whose contents will be cached
         */
        public void cache(Substitution s) throws Exception {
            mApp.cache(mRequest, mResponse, s);
        }

        /**
         * Caches and reuses a region of a page. The cached region expies after
         * a default time-to-live period has elapsed. An additional parameter
         * is specified which helps to identify the uniqueness of the region.
         *
         * @param key key to further identify cache region uniqueness
         * @param s substitution block whose contents will be cached
         */
        public void cache(Object key, Substitution s)
            throws Exception {
            mApp.cache(mRequest, mResponse, key, s);
        }

        /**
         * Caches and reuses a region of a page. The cached region expies after
         * the specified time-to-live period has elapsed.
         *
         * @param ttlMillis maximum time to live of cached region, in milliseconds
         * @param s substitution block whose contents will be cached
         */
        public void cache(long ttlMillis, Substitution s) throws Exception {
            mApp.cache(mRequest, mResponse, ttlMillis, s);
        }

        /**
         * Caches and reuses a region of a page. The cached region expies after
         * the specified time-to-live period has elapsed. An additional parameter
         * is specified which helps to identify the uniqueness of the region.
         *
         * @param ttlMillis maximum time to live of cached region, in milliseconds
         * @param key key to further identify cache region uniqueness
         * @param s substitution block whose contents will be cached
         */
        public void cache(long ttlMillis, Object key, Substitution s)
            throws Exception {
            mApp.cache(mRequest, mResponse, ttlMillis, key, s);
        }

        /**
         * Caches and reuses a region of a page. The cached region expies after
         * the specified time-to-live period has elapsed. An additional parameter
         * is specified which helps to identify the uniqueness of the region.
         * A boolean paramaeter indicates if the key parameter is used as the
         * only unique identifier for the cache region.
         *
         * @param ttlMillis maximum time to live of cached region, in milliseconds
         * @param key key to further identify cache region uniqueness
         * @param useCustomKeyOnly true to indicate that the key parameter is used
         * as the only unique identifier for the cache region
         * @param s substitution block whose contents will be cached
         */
        public void cache(long ttlMillis, Object key, boolean useCustomKeyOnly, Substitution s)
            throws Exception {
            mApp.cache(mRequest, mResponse, ttlMillis, key, useCustomKeyOnly, s);
        }

        public void nocache(Substitution s) throws Exception {
            mApp.nocache(mRequest, mResponse, s);
        }

        public RegionCacheInfo getRegionCacheInfo() {
            return new RegionCacheInfo(mApp.getDepot(mRequest.getTemplate()));
        }

        public void resetDepotStats() {
            mApp.getDepot(mRequest.getTemplate()).reset();
        }

        public RegionCachingApplication.ClusterCacheInfo getClusterCacheInfo() {
            try {
                if (mClusterManager != null) {
                    mClusterManager.resolveServerNames();
                }
                return mInfo;
            }
            catch (Exception e) {
                return null;
            }
        }

        public int getCacheSize() {
            return mApp.getDepot(mRequest.getTemplate()).size();
        }

        public int getValidEntryCount() {
            return mApp.getDepot(mRequest.getTemplate()).validSize();
        }

        public int getInvalidEntryCount() {
            return mApp.getDepot(mRequest.getTemplate()).invalidSize();
        }

        public long getCacheGets() {
            return mApp.getDepot(mRequest.getTemplate()).getCacheGets();
        }

        public long getCacheHits() {
            return mApp.getDepot(mRequest.getTemplate()).getCacheHits();
        }

        public long getCacheMisses() {
            return mApp.getDepot(mRequest.getTemplate()).getCacheMisses();
        }

    }

    public interface ClusterCacheInfo extends Clustered {

        public RegionCacheInfo getRegionCacheInfo() 
            throws RemoteException;
    }

    public class ClusterCacheInfoImpl extends ClusterHook 
        implements ClusterCacheInfo {
        
        ClusterCacheInfoImpl(String cluster, String server) 
            throws RemoteException {
            super(cluster, server);
        }

        public RegionCacheInfo getRegionCacheInfo() 
            throws RemoteException {
            List depotList = RegionCachingApplication.this.mDepots;
            if (depotList != null && depotList.size() > 0) {
                DepotLink link = (DepotLink)depotList.get(0);
                if (link != null) {
                    return new RegionCacheInfo(link.mDepot);
                }
            }
            return null;
        }
    }
}
