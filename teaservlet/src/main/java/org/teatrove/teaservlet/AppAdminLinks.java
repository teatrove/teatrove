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

import java.util.List;
import java.util.Vector;

/**
 *  
 *
 * @author Jonathan Colwell
 */
public class AppAdminLinks {
    
    String mAppName;
    List mLinks;

    public AppAdminLinks(String appName) {
        super();
        mAppName = appName;
        mLinks = new Vector();
    }

    public String getAppName() {
        return mAppName;
    }

    public AdminLink[] getLinks() {
        return (AdminLink[])mLinks.toArray(new AdminLink[mLinks.size()]);
    }

    public void addAdminLink(String name,String location) {
        mLinks.add(new AdminLink(name,location));
    }

    public void addAdminLink(AdminLink link) {
        mLinks.add(link);
    }   

    public class AdminLink {

        String mName,mLocation;
        
        public AdminLink(String name,String location) {
            mName = name;
            mLocation = location;
        }

        public String getName() {
            return mName;
        }

        public String getLocation() {
            return mLocation;
        }
    }
}
