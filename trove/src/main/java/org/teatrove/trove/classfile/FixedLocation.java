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

package org.teatrove.trove.classfile;

/**
 * 
 * @author Brian S O'Neill
 */
class FixedLocation implements Location {
    private int mLocation;
    
    public FixedLocation(int location) {
        mLocation = location;
    }
    
    public int getLocation() {
        return mLocation;
    }

    public int compareTo(Object obj) {
        if (this == obj) {
            return 0;
        }
        Location other = (Location)obj;
        
        int loca = getLocation();
        int locb = other.getLocation();
        
        if (loca < locb) {
            return -1;
        }
        else if (loca > locb) {
            return 1;
        }
        else {
            return 0;
        }
    }
}
