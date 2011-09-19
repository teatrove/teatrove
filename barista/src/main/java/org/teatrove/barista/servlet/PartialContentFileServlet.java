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

package org.teatrove.barista.servlet;

import java.io.*;
import java.util.*;
import java.net.URLEncoder;
import javax.servlet.*;
import javax.servlet.http.*;
import org.teatrove.barista.http.HttpHeaders;
import org.teatrove.trove.util.SortedArrayList;

/**
 * Non-caching file viewing servlet that provides basic web server
 * functionality.
 *
 * @author Brian S O'Neill
 */
public class PartialContentFileServlet extends HttpServlet {
    private static final String DIRECTORY_ROOT_PARAM = "directory.root";
    private static final String DIRECTORY_BROWSE_PARAM = "directory.browse";
    private static final String DOCUMENT_DEFAULT_PARAM = "document.default";
    private static final String MIME_DEFAULT_PARAM = "mime.default";

    private File mRootDir;
    private boolean mDirectoryBrowse;
    private String mDefaultDoc;
    private String mDefaultMime;

    public PartialContentFileServlet() {
    }

    /**
     * Supports the following initialization parameters:
     *
     * <ul>
     * <li>directory.root (optional root directory to serve up files from,
     *                     which overrides the setting used by
     *                     ServletContext.getRealPath)
     * <li>directory.browse (optional directory browsing switch, defaults to
     *                       false)
     * <li>document.default (optional default document to show for directories)
     * <li>mime.default (optional MIME type to apply to unknown file types)
     * </ul>
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String rootDir = config.getInitParameter(DIRECTORY_ROOT_PARAM);
        String browse = config.getInitParameter(DIRECTORY_BROWSE_PARAM);
        mDefaultDoc = config.getInitParameter(DOCUMENT_DEFAULT_PARAM);
        mDefaultMime = config.getInitParameter(MIME_DEFAULT_PARAM);

        String error = null;

        if (rootDir != null) {
            try {
                mRootDir = new File(rootDir).getCanonicalFile();
            }
            catch (IOException e) {
                throw new ServletException(e);
            }

            if (!mRootDir.exists()) {
                error = "Directory doesn't exist: " + rootDir;
            }
            else if (!mRootDir.isDirectory()) {
                error = "Not a directory: " + rootDir;
            }
            else if (!mRootDir.canRead()) {
                error = "Unable to read from: " + rootDir;
            }
            else if (mRootDir.isHidden()) {
                error = "Directory is hidden: " + rootDir;
            }
        }

        if (error != null) {
            throw new UnavailableException(this, error);
        }

        log("Root directory: " + mRootDir);

        mDirectoryBrowse = "true".equalsIgnoreCase(browse);

        log("Directory browsing: " + mDirectoryBrowse);
        log("Default document: " + mDefaultDoc);
        log("Default MIME type: " + mDefaultMime);
    }

    public void log(String msg) {
        getServletContext().log(msg);
    }

    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
        throws IOException
    {
        resp.setDateHeader(HttpHeaders.DATE, System.currentTimeMillis());
        resp.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");

        String path;
        if ((path = req.getPathInfo()) == null) {
            path = "/";
        }
        else {
            if (File.separatorChar != '/' &&
                path.indexOf(File.separatorChar) >= 0) {

                // This test prevents a case of infinite redirection. If the
                // pattern "%5c" is in the request URI, it will appear in the
                // path info as "\". When the replacement is made to the
                // request URI, nothing happens. Don't try to be clever. Just
                // return a 404.
                if (req.getRequestURI().indexOf(File.separatorChar) < 0) {
                    resp.sendError(resp.SC_NOT_FOUND);
                    return;
                }

                resp.setStatus(resp.SC_MOVED_PERMANENTLY);
                StringBuffer newPath = new StringBuffer(req.getRequestURI());
                if (req.getQueryString() != null) {
                    newPath.append('?');
                    newPath.append(req.getQueryString());
                }
                path = newPath.toString();
                resp.sendRedirect(path.replace(File.separatorChar, '/'));
                return;
            }

            if (path.length() > 0 && path.charAt(0) != '/') {
                resp.sendError(resp.SC_BAD_REQUEST);
                return;
            }
        }

        File file;
        if (mRootDir != null) {
            file = new File(mRootDir, path);
            // Security check. Make sure the canonical file is still in the
            // root directory. I do an additional check for ".." patterns in
            // the canonical file because of a defect that incorrectly converts
            // the pattern ".../..." to "..\.." on win32, which is a security
            // hole.
            file = file.getCanonicalFile();
            String filePath = file.getPath();
            if (filePath.indexOf("..") >= 0 ||
                !filePath.startsWith(mRootDir.getPath())) {

                resp.sendError(resp.SC_NOT_FOUND);
                return;
            }
        }
        else {
            String realPath = getServletContext()
                .getRealPath(req.getRequestURI());
            if (realPath == null) {
                log("Unable to get real path for " + path);
                resp.sendError(resp.SC_NOT_FOUND, req.getRequestURI());
                return;
            }
            else {
                file = new File(realPath);
            }
            // Since I don't know what the root directory is, I can't do
            // any security check. We are at the mercy of the servlet engine.
            // Barista does do the above security check.
        }

        if (!file.exists() || file.isHidden()) {
            resp.sendError(resp.SC_NOT_FOUND, req.getRequestURI());
            return;
        }

        boolean displayDirectory;

        if (!file.isDirectory()) {
            if (path.endsWith("/")) {
                resp.sendError(resp.SC_NOT_FOUND, req.getRequestURI());
                return;
            }

            displayDirectory = false;
        }
        else {
            if (!req.getRequestURI().endsWith("/")) {
                resp.setStatus(resp.SC_MOVED_PERMANENTLY);
                resp.sendRedirect(req.getRequestURI().concat("/"));
                return;
            }

            displayDirectory = true;

            String defaultDoc;
            if ((defaultDoc = mDefaultDoc) != null) {
                File defaultFile = new File(file, defaultDoc);
                if (defaultFile.exists()) {
                    file = defaultFile;
                    displayDirectory = false;
                }
            }
        }

        if (displayDirectory) {
            if (!mDirectoryBrowse) {
                resp.sendError(resp.SC_FORBIDDEN);
                return;
            }

            resp.setDateHeader(HttpHeaders.LAST_MODIFIED,
                               file.lastModified());
            displayDirectory(req, resp, file);
            return;
        }

        String mimeType = getServletContext().getMimeType(file.getPath());
        if (mimeType != null) {
            resp.setContentType(mimeType);
        }
        else {
            resp.setContentType(mDefaultMime);
        }


        // Open up file this way in order to acquire a stronger lock on it.
        RandomAccessFile raf = new RandomAccessFile(file, "r");

        try {
            long lastModified = file.lastModified();
            // Round up to nearest second.
            lastModified = lastModified / 1000 * 1000 + 1000;

            try {
                long ifModifiedSince =
                    req.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);

                if (ifModifiedSince >= 0 && ifModifiedSince >= lastModified) {
                    resp.setStatus(resp.SC_NOT_MODIFIED);
                    resp.setContentLength(0);
                    return;
                }
            }
            catch (IllegalArgumentException e) {
            }

            resp.setDateHeader(HttpHeaders.LAST_MODIFIED, lastModified);
            
            // Retrieve all byte ranges, if any.
            List ranges = getFileRanges(raf, req, resp);
            long length = getTotalContentLength(ranges);

            if (length <= 0x7fffffff) {
                resp.setContentLength((int)length);
            }

            StringBuffer etag = new StringBuffer(65);
            // Radix 35 uses numbers 0-9, letters a-y.
            etag.append(Long.toString(lastModified, 35));
            // Using z as a separator is safe since radix 35 won't use it.
            etag.append('z');
            etag.append(Long.toString(length, 35));
            resp.setHeader(HttpHeaders.ETAG, etag.toString());

            OutputStream out = resp.getOutputStream();

            // Iterate through all of the ranges and write them out.
            Iterator i = ranges.iterator();
            // log("Writing out file");
            while (i.hasNext()) {
                Range range = (Range)i.next();
                log("Writing out range: " + range.getStart() +
                    ", " + range.getEnd());
                length = range.getLength();

                long offs = range.getStart();

                int bufSize;
                if (length > 512) {
                    bufSize = 512;
                }
                else {
                    bufSize = (int)length;
                }

                byte[] inputBuffer = new byte[bufSize];

                int readAmount;
                int totalWritten = 0;

                raf.skipBytes((int)offs);
                while ((readAmount = raf.read(inputBuffer, 0, bufSize)) >
                       0 && totalWritten < length) {
                    totalWritten += readAmount;
                    // System.out.println("wrote: " + readAmount);
                    out.write(inputBuffer, 0, readAmount);
                }
                raf.seek(0);
            }
            // System.out.println("Finished writing file");
        }
        finally {
            raf.close();
        }
    }

    public String getServletInfo() {
        return "Non-caching file browsing servlet";
    }

    protected void displayDirectory(HttpServletRequest req,
                                    HttpServletResponse resp,
                                    File file)
        throws IOException
    {
        String[] list = file.list();
        if (list == null) {
            resp.sendError(resp.SC_FORBIDDEN);
            return;
        }

        resp.setContentType("text/html");

        String pathInfo = req.getPathInfo();
        String dirName;
        if (pathInfo == null) {
            dirName = req.getServletPath();
        }
        else {
            dirName = req.getServletPath() + pathInfo;
        }

        if (!dirName.endsWith("/")) {
            dirName += '/';
        }

        StringBuffer buf = new StringBuffer(4000);

        buf.append("<HTML><HEAD><TITLE>");
        buf.append(dirName);
        buf.append("</TITLE></HEAD><BODY><H1>");
        buf.append(dirName);
        buf.append("</H1><HR><PRE>");

        String path = file.getPath();
        for (int i=0; i<list.length; i++) {
            File sub = new File(file, list[i]);

            if (!sub.isHidden()) {
                String name;
                if (sub.isDirectory()) {
                    name = list[i] + '/';
                }
                else {
                    name = list[i];
                }

                buf.append("<A HREF=\"");
                buf.append(encodePath(dirName + name));
                buf.append("\">");
                buf.append(name);
                buf.append("</A><BR>");
            }
        }

        buf.append("</PRE></BODY></HTML>");

        String page = buf.toString();
        resp.setContentLength(page.length());
        resp.getWriter().write(page);
    }

    /**
     *
     */
    private Range getFileRange(String str, long length) {

        StringTokenizer rangeTokens = new StringTokenizer(str, "-");
        int numToks = rangeTokens.countTokens();
        if (numToks > 2) {
            System.out.println("Error: There are too many tokens in" +
                               " this string: " + str);
        }

        // check to make sure that a start byte has been specified
        if (str.startsWith("-") && rangeTokens.hasMoreTokens()) {
            try {
                long l = Long.parseLong(rangeTokens.nextToken());
                return new Range((length-1) - l, length-1);
            }
            catch (NumberFormatException nfe) {
                return new Range(-1, -1);
            }
        }

        long[] range = {-1, -1};
        int nextElement = 0;
        while (rangeTokens.hasMoreTokens()) {
            try {
                long l = Long.parseLong(rangeTokens.nextToken());
                range[nextElement] = l;
            }
            catch (NumberFormatException nfe) {
                range[nextElement] = -1;
            }
            finally {
                ++nextElement;
            }
        }

        long start, end;
        // no numbers defined in Range header. Invalid range, so send
        // the whole file.
        // ex: Range: bytes=
        if (range[0] < 0 && range[1] < 0) {
            start = 0;
            end = length;
        }
        // no begin range was specified
        // ex: Range: bytes=-500
        else if (range[0] < 0 && range[1] > 0) {
            start = length - range[1];
            end = length;
        }
        // no end range was specified
        // ex: Range: bytes=0-
        else if (range[1] < 0) {
            start = range[0];
            end = length;
        }
        // a standard byte range was specified
        // ex: Range: bytes=0-499
        // if the end > length, use length
        else {
            start = range[0];
            end = (range[1] < length) ? (range[1]+1) : length;
        }

        log("range = start: " + start + " end: " + end);
        return new Range(start, end);
    }

    private long getTotalContentLength(List ranges) {
        Iterator i = ranges.iterator();
        long length = 0;

        while (i.hasNext()) {
            length += ((Range)i.next()).getLength();
        }

        return length;
    }

    /**
     * Returns a list of file ranges that are to be written out.
     */
    private List getFileRanges(RandomAccessFile raf,
                               HttpServletRequest req,
                               HttpServletResponse resp)
        throws IOException {

        long length = raf.length();
        Enumeration rangeEnum = req.getHeaders(HttpHeaders.RANGE);
        List ranges = new SortedArrayList(new RangeComparator());

        if (rangeEnum == null) {
            return ranges;
        }

        String contentRange = new String();

        while (rangeEnum.hasMoreElements()) {
            String range = (String)rangeEnum.nextElement();
            range = range.toLowerCase();
            if (range.startsWith("bytes=")) {
                range = range.substring("bytes=".length());
            }
            Range r = getFileRange(range, length);
            log("Adding range:" + r.getStart() + ", " + r.getEnd());
            contentRange = range + "/" + length;
            ranges.add(r);
        }

        // If ranges is empty, then no range was specified.
        if (ranges.isEmpty()) {
            ranges.add(new Range(0, length));
        }
        else {
            // set the return code to indicate that partial content is
            // being returned (206)
            resp.setStatus(resp.SC_PARTIAL_CONTENT);
            resp.setHeader(HttpHeaders.CONTENT_RANGE, contentRange);
        }

        return ranges;
    }

    private String encodePath(String path) {
        int length = path.length();
        StringBuffer buf = new StringBuffer(length);

        int cursor = 0;
        int index;
        while ((index = path.indexOf('/', cursor)) >= 0) {
            buf.append(URLEncoder.encode(path.substring(cursor, index)));
            buf.append('/');
            cursor = index + 1;
        }

        if (cursor < length) {
            buf.append(URLEncoder.encode(path.substring(cursor, length)));
        }

        return buf.toString();
    }

    /**
     *
     * @author Sean T. Treat
     * @version
     * <!--$$Revision:--> 3 <!-- $-->, <!--$$JustDate:--> 10/18/02 <!-- $-->
     */
    private class Range {
        private long mStart;
        private long mEnd;

        public Range(long start, long end) {
            mStart = start;
            mEnd = end;
        }

        public long getStart() {
            return mStart;
        }

        public long getEnd() {
            return mEnd;
        }

        public long getLength() {
            return (mEnd - mStart);
        }

        public String toString() {
            return mStart + "-" + mEnd;
        }
    }

    private class RangeComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            long delta = ((Range)o2).getStart() - ((Range)o1).getStart();
            if(delta == 0) {
                delta = ((Range)o2).getEnd() - ((Range)o1).getEnd();
            }

            return (int)delta;
        }

        public boolean equals(Object obj) {
            return this.equals(obj);
        }
    }
}
