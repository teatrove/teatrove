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

package org.teatrove.teatools;

/**
 * Simple structure for holding an integer range.  This class can be used to 
 * represent a region of text within a document.
 *
 * @author Mark Masse
 */
public class Range {

    public int start;
    public int end;

    public Range() {
    }

    public Range(int start, int end) {
        setStart(start);
        setEnd(end);
    }

    /**
     * Sets the Range's start postion.
     */
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * Gets the Range's start postion.
     */
    public int getStart() {
        return start;
    }

    /**
     * Sets the Range's end postion.
     */
    public void setEnd(int end) {
        this.end = end;
    }

    /**
     * Gets the Range's end postion.
     */
    public int getEnd() {
        return end;
    }   
    
    /**
     * Compares to Ranges for equality
     */
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        
        if (other instanceof Range) {
            Range otherRange = (Range) other;
            return (getStart() == otherRange.getStart() &&
                    getEnd() == otherRange.getEnd());
        }
        else {
            return false;
        }
    }

}
