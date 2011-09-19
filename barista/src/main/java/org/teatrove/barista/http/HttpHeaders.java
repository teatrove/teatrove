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
import java.util.*;
import java.text.*;

/**
 * A collection of constant strings for standard HTTP headers and values as
 * defined in <a href="http://www.cis.ohio-state.edu/htbin/rfc/rfc2068.html">
 * RFC2068</a>, "Hypertext&nbsp;Transfer&nbsp;Protocol&nbsp;--&nbsp;HTTP/1.1".
 *
 * @author Brian S O'Neill
 */
public class HttpHeaders {
    /** Accept header */
    public static final String ACCEPT = "Accept";

    /** Accept-Charset header */
    public static final String ACCEPT_CHARSET = "Accept-Charset";

    /** Accept-Encoding header */
    public static final String ACCEPT_ENCODING = "Accept-Encoding";

    /** Accept-Language header */
    public static final String ACCEPT_LANGUAGE = "Accept-Language";

    /** Accept-Ranges header */
    public static final String ACCEPT_RANGES = "Accept-Ranges";

    /** Age header */
    public static final String AGE = "Age";

    /** Allow header */
    public static final String ALLOW = "Allow";

    /** Authorization header */
    public static final String AUTHORIZATION = "Authorization";

    /** Cache-Control header */
    public static final String CACHE_CONTROL = "Cache-Control";

    /** Connection header */
    public static final String CONNECTION = "Connection";

    /** Content-Base header */
    public static final String CONTENT_BASE = "Content-Base";

    /** Content-Encoding header */
    public static final String CONTENT_ENCODING = "Content-Encoding";

    /** Content-Language header */
    public static final String CONTENT_LANGUAGE = "Content-Language";

    /** Content-Length header */
    public static final String CONTENT_LENGTH = "Content-Length";

    /** Content-Location header */
    public static final String CONTENT_LOCATION = "Content-Location";

    /** Content-MD5 header */
    public static final String CONTENT_MD5 = "Content-MD5";

    /** Content-Range header */
    public static final String CONTENT_RANGE = "Content-Range";

    /** Content-Type header */
    public static final String CONTENT_TYPE = "Content-Type";

    /** Date header */
    public static final String DATE = "Date";

    /** ETag header */
    public static final String ETAG = "ETag";

    /** Expires header */
    public static final String EXPIRES = "Expires";

    /** From header */
    public static final String FROM = "From";

    /** Host header */
    public static final String HOST = "Host";

    /** If-Modified-Since header */
    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";

    /** If-Match header */
    public static final String IF_MATCH = "If-Match";

    /** If-None-Match header */
    public static final String IF_NONE_MATCH = "If-None-Match";

    /** If-Range header */
    public static final String IF_RANGE = "If-Range";

    /** If-Unmodified-Since header */
    public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

    /** Last-Modified header */
    public static final String LAST_MODIFIED = "Last-Modified";

    /** Location header */
    public static final String LOCATION = "Location";

    /** Max-Forwards header */
    public static final String MAX_FORWARDS = "Max-Forwards";

    /** Pragma header */
    public static final String PRAGMA = "Pragma";

    /** Proxy-Authenticate header */
    public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";

    /** Proxy-Authorization header */
    public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";

    /** Public header */
    public static final String PUBLIC = "Public";

    /** Range header */
    public static final String RANGE = "Range";

    /** Referer [sic] header */
    public static final String REFERRER = "Referer";

    /** Retry-After header */
    public static final String RETRY_AFTER = "Retry-After";

    /** Server header */
    public static final String SERVER = "Server";

    /** Transfer-Encoding header */
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";

    /** Upgrade header */
    public static final String UPGRADE = "Upgrade";

    /** User-Agent header */
    public static final String USER_AGENT = "User-Agent";

    /** Vary header */
    public static final String VARY = "Vary";

    /** Via header */
    public static final String VIA = "Via";

    /** Warning header */
    public static final String WARNING = "Warning";

    /** WWW-Authenticate header */
    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

    /** Keep-Alive value, usually associated with a Connection header */
    public static final String KEEP_ALIVE = "Keep-Alive";

    /** Close value, usually associated with a Connection header */
    public static final String CLOSE = "Close";
}
