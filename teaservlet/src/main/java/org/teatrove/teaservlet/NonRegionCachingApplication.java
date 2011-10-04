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

/*
 * Created on Aug 18, 2004
 *
 */
package org.teatrove.teaservlet;

import javax.servlet.ServletException;

import org.teatrove.tea.runtime.Substitution;
import org.teatrove.teaservlet.RegionCachingApplication.ClusterCacheInfo;

/**
 * A swap-in replacement for the RegionCaching app that doesn't actually 
 * do any caching, but will allow RegionCaching-dependent templates to compile 
 * and execute successfully
 * 
 */
public class NonRegionCachingApplication implements AdminApp {

	/* (non-Javadoc)
	 * @see org.teatrove.teaservlet.AdminApp#getAdminLinks()
	 */
	public AppAdminLinks getAdminLinks() {
		// TODO Auto-generated method stub
		return null;
	}

	public void init(ApplicationConfig config) throws ServletException {

	}

	public void destroy() {

	}


	public Object createContext(ApplicationRequest request, ApplicationResponse response) {
		return new NonRegionCachingContext(request, response);
	}


	public Class getContextType() {
		return NonRegionCachingContext.class;
	}

		
	public static class NonRegionCachingContext implements RegionCachingContext {
			
		private ApplicationRequest m_request;
		private ApplicationResponse m_response;
			
		public NonRegionCachingContext(ApplicationRequest request, ApplicationResponse response) {
			m_request = request;
			m_response = response;
		}
		
		public void cache(Substitution s) throws Exception {
			s.substitute(m_response.getHttpContext());
		}
		
		public void cache(Object key, Substitution s) throws Exception {
			cache(s);
		}
		
		
		public void cache(long ttlMillis, Substitution s) throws Exception {
			cache(s);
		}
		
		public void cache(long ttlMillis, Object key, Substitution s) throws Exception {
			cache(s);
		}
		
		public void cache(long ttlMillis, Object key, boolean useCustomKeyOnly, Substitution s) throws Exception {
			cache(s);
		}
		
		public void nocache(Substitution s) throws Exception {
			s.substitute(m_response.getHttpContext());
		}
		
		public RegionCacheInfo getRegionCacheInfo() {
			return null;
		}
		
		public ClusterCacheInfo getClusterCacheInfo() {
			return null;
		}
		
		public int getCacheSize() {
			return 0;
		}
		
		public int getValidEntryCount() {
			return 0;
		}
		
		public int getInvalidEntryCount() {
			return 0;
		}

        public long getCacheGets() { return 0L; }

        public long getCacheHits() { return 0L; }

        public long getCacheMisses() { return 0L; }

        public int getAvgEntrySizeInBytes() { return 0; }
            
        public void resetDepotStats() { }
	}
}
