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

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Complete hourly file logging stream. Outputs to files created by
 * {@link FileLogStreamFactory}.
 *
 * @author Brian S O'Neill
 */
public class HourlyFileLogStream extends HourlyLogStream {
    /** Default format is "yyyyMMdd-HH" */
    public static final DateFormat DEFAULT_FORMAT =
        new SimpleDateFormat("yyyyMMdd-HH");

    /** Default extension is ".log" */
    public static final String DEFAULT_EXTENSION = ".log";

    public HourlyFileLogStream(File directory) {
        this(directory, DEFAULT_FORMAT, DEFAULT_EXTENSION);
    }

    public HourlyFileLogStream(File directory, DateFormat format) {
        this(directory, format, DEFAULT_EXTENSION);
    }

    public HourlyFileLogStream(File directory, 
                               DateFormat format, 
                               String extension) {

        super(new FileLogStreamFactory(directory, format, extension));
    }
}
