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

package org.teatrove.barista.util;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.ThreadPool;
import org.teatrove.trove.log.*;

/**
 * RootConfig starts with a just a PropertyMap and creates a Log and ThreadPool
 * from those settings. RootConfig also installs {@link Syslog}, with all
 * events being routed to this config's Log. RootConfig only needs to be
 * created once, and all other configs should be {@link DerivedConfig derived}.
 *
 * @author Brian S O'Neill
 */
public class RootConfig extends DefaultConfig {
    /**
     * Reads the following optional property groups:
     *
     * <ul>
     * <li>log
     * <li>threadPool
     * </ul>
     *
     * <p>Here is a sample from a properties file:
     *
     * <pre>
     * # Log properties.
     * #
     * # If any log properties are disabled, they will be automatically re-enabled
     * # if a child log exists that has any log properties enabled. If a child log
     * # doesn't explicitly specify any log properties, then the parent's log
     * # properties are inherited.
     * #
     * # 01/18/2007 If the log parameter is omitted then the log will not be created
     * #
     * log {
     *     enabled = true
     *
     *     # If any of these log properties are true, then the log is
     *     # automaticaly enabled.
     *     debug = false
     *     info = true
     *     warn = true
     *     error = true
     *
     *     # Optionally change the date format in the log. Set to nothing to hide
     *     # the date altogether.
     *     #date.format = yyyy/MM/dd HH:mm:ss.SSS z
     *
     *     # Optionally set the file directory to receive dated log files. If not
     *     # set, all log information goes to standard out.
     *     #directory = .
     *
     *     # Optionally set the log file rollover interval to be daily or hourly
     *     # if dated log files are enabled. By default, the rollover is daily.
     *     #rollover = daily
     * }
     *
     * # ThreadPool properties.
     * threadPool {
     *     # Maximum allowed number of threads in the pool
     *     max = 200
     *
     *     # Timeout (milliseconds) before idle threads exit
     *     timeout.idle = 60000
     *
     *     # Timeout (milliseconds) for getting pooled threads or closing pool
     *     timeout = 5000
     * }
     * </pre>
     */
    public static RootConfig forProperties(PropertyMap props) {
        PropertyMap subMap = props.subMap("log");
        if (!subMap.isEmpty() || props.containsKey("log")) {
             createLog(subMap);
        }
        Log log = Syslog.log();
        ThreadPool pool = createThreadPool(props.subMap("threadPool"), log);
        return new RootConfig(props, log, pool);
    }
     
    private RootConfig(PropertyMap properties,
                       Log log,
                       ThreadPool threadPool) {
        super(properties, log, threadPool);
    }

    /**
     * Passes log properties to {@link Log#applyProperties(Map)} and also
     * reads additional optional properties for file logging:
     *
     * <ul>
     * <li>directory (directory to write dated log files)
     * <li>rollover (set to daily or hourly, but defaults to daily)
     * <li>date.format (simple date format for prepended dates)
     * </ul>
     */
    private static void createLog(PropertyMap properties) {
        Log log = Syslog.log();
        log.applyProperties(properties);

        PrintWriter writer;
        String logDir = properties.getString("directory");
        if (logDir != null) {
            File dir = new File(logDir);
            IntervalLogStream out;
            if ("hourly".equals(properties.getString("rollover"))) {
                out = new HourlyFileLogStream(dir);
            }
            else {
                out = new DailyFileLogStream(dir);
            }
            out.startAutoRollover();
            writer = new PrintWriter(out);
        }
        else {
            writer = new PrintWriter(System.out);
        }

        LogScribe scribe;
        String formatStr = properties.getString("date.format");
        if (formatStr != null) {
            DateFormat format = new SimpleDateFormat(formatStr);
            scribe = new LogScribe(writer, format);
        }
        else {
            scribe = new LogScribe(writer);
        }

        Syslog.log().addLogListener(scribe);
        Syslog.log().removeLogListener(Syslog.getSystemLogEventPrinter());
        Syslog.install();        
    }

    /**
     * Reads the following optional properties:
     *
     * <ul>
     * <li>max (maximum ThreadPool threads, defaults to 1000)
     * <li>timeout (defaults to 5000 milliseconds)
     * <li>timeout.idle (defaults to 60000 milliseconds)
     * </ul>
     */
    private static ThreadPool createThreadPool(PropertyMap properties,
                                               final Log log) {
        int max = properties.getInt("max", 1000);

        ThreadPool threadPool = new ThreadPool("Barista", max) {
            public void uncaughtException(Thread t, Throwable e) {
                if (e instanceof ThreadDeath) {
                    return;
                }
                log.logException(new LogEvent(log, LogEvent.ERROR_TYPE, e, t));
            }
        };

        threadPool.setTimeout(properties.getInt("timeout", 5000));
        threadPool.setIdleTimeout(properties.getInt("timeout.idle", 60000));

        return threadPool;
    }
}
