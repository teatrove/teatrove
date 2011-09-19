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
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.teatrove.trove.io.*;
import org.teatrove.trove.log.*;
import org.teatrove.trove.net.HttpHeaderMap;
import org.teatrove.trove.util.PropertyMap;

/**
 * A special HTTP stage that writes {@link Impression impressions} to an
 * output writer in accordance with the ULF specification. ULF stands for
 * Unified Log Format.
 *
 * @author Brian S O'Neill
 */
public class ULFScribe implements HttpHandlerStage, LogListener {
    private static final DateFormat DATE_FORMAT =
        new SimpleDateFormat("[dd/MMM/yyyy:HH:mm:ss]");

    private Config mConfig;
    private String mServiceField;
    private OutputStream mOut;

    /**
     * Zero argument constructor allowing instantiation when the class is not
     * known ahead of time.
     * The init method must be called for this instance to be useful.
     */
    public ULFScribe() {
    }

    /**
     * @param servideField the service field as per the ULF specification. ex:
     * "go", "abcnews", "espn", etc.
     * @param out
     */
    public ULFScribe(String serviceField, OutputStream out) {
        mServiceField = serviceField;
        mOut = out;
    }

    public boolean handle(HttpServerConnection con, Chain chain)
        throws Exception
    {
        return chain.doNextStage(con);
    }

    /**
     * Understands two properties: serviceField and directory. The
     * directory is where daily log files will be written to.
     */
    public void init(Config config) {
        if (mConfig != null) {
            mConfig.getImpressionLog().removeLogListener(this);
        }

        mConfig = config;

        if (config == null) {
            return;
        }

        PropertyMap properties = config.getProperties();

        mServiceField = properties.getString("serviceField");
        IntervalLogStream out = new DailyFileLogStream
            (new File(properties.getString("directory")));
        out.startAutoRollover();
        mOut = out;

        config.getImpressionLog().addLogListener(this);
    }

    public Config getConfig() {
        return mConfig;
    }

    /**
     * The LogEvent passed in must be an instance of {@link Impression} or else
     * a cast exception is thrown.
     */
    public void logMessage(LogEvent e) {
        logMessage((Impression)e);
    }

    public void logMessage(Impression impression) {
        try {
            CharToByteBuffer buffer =
                new DefaultCharToByteBuffer(new DefaultByteBuffer());
            buffer = new InternedCharToByteBuffer(buffer);
            
            HttpServerConnection con = impression.getConnection();
            HttpHeaderMap headerMap = con.getRequestHeaders();
            
            // service_field
            buffer.append((byte)'"');
            buffer.append(mServiceField);
            buffer.append((byte)'"');
            buffer.append((byte)' ');
            
            // ip_address
            buffer.append(con.getSocket().getInetAddress().getHostAddress());
            buffer.append((byte)' ');
            
            // -
            buffer.append((byte)'-');
            buffer.append((byte)' ');
            
            // subscriber_field
            buffer.append((byte)'-');
            buffer.append((byte)' ');
            
            // date_field
            synchronized (DATE_FORMAT) {
                buffer.append(DATE_FORMAT.format(impression.getTimestamp()));
            }
            buffer.append((byte)' ');
            
            // operation url_field
            buffer.append((byte)'"');
            buffer.append(con.getRequestMethod());
            buffer.append((byte)' ');
            buffer.append(con.getRequestURI());
            buffer.append((byte)'"');
            buffer.append((byte)' ');
            
            // status_result_field
            int sc = con.getResponseStatusCode();
            if (sc == 200) {
                // Slight optimization
                buffer.append("200SS");
            }
            else {
                buffer.append(Integer.toString(sc));
                buffer.append("SS");
            }
            buffer.append((byte)' ');
            
            // bytes_field
            buffer.append(Long.toString(con.getBytesWritten()));
            buffer.append((byte)' ');
            
            // browser_field
            buffer.append((byte)'"');
            String userAgent = (String)headerMap.get("User-Agent");
            if (userAgent != null) {
                buffer.append(userAgent);
            }
            buffer.append((byte)'"');
            buffer.append((byte)' ');
            
            // banner_field, guid_field, target_field
            buffer.append("\"\" \"\" \"\" ");
            
            // referral_field
            buffer.append((byte)'"');
            String referrer = (String)headerMap.get("Referer");
            if (referrer != null) {
                buffer.append(referrer);
            }
            buffer.append((byte)'"');
            buffer.append((byte)' ');
            
            // swuid_field
            buffer.append((byte)'"');
            String cookie = (String)(con.getRequestCookies().get("SWID"));
            if (cookie != null) {
                buffer.append(cookie);
            }
            
            buffer.append((byte)'"');
            buffer.append((byte)' ');
            
            // bitstring, server_field
            buffer.append("\"\" \"\" ");
            
            buffer.append("\r\n");

            buffer.writeTo(mOut);
            mOut.flush();
        }
        catch (IOException e) {
            mConfig.getLog().error(e);
        }
    }

    public void logException(LogEvent e) {
        logMessage(e);
    }
}
