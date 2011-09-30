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

package org.teatrove.trove.log;

import java.util.*;

/**
 * An implementation of the IntervalLogStream with an interval of one hour.
 * 
 * @author Brian S O'Neill
 */
public class HourlyLogStream extends IntervalLogStream {
    public HourlyLogStream(Factory factory) {
        super(factory);
    }
    
    /**
     * Moves calendar to beginning of hour.
     */
    protected void moveToIntervalStart(Calendar cal) {
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        cal.clear();
        cal.set(year, month, day, hour, 0);
    }

    /**
     * Moves calendar to beginning of next hour.
     */
    protected void moveToNextIntervalStart(Calendar cal) {
        moveToIntervalStart(cal);
        cal.add(Calendar.HOUR_OF_DAY, 1);
    }
}
