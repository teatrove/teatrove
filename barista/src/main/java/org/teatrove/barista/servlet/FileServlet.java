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
import java.net.URLEncoder;
import javax.servlet.*;
import javax.servlet.http.*;
import org.teatrove.barista.http.HttpHeaders;

/**
 * Non-caching file viewing servlet that provides basic web server
 * functionality.
 *
 * @author Brian S O'Neill
 */
public class FileServlet extends HttpServlet {
    private static final String DIRECTORY_ROOT_PARAM = "directory.root";
    private static final String DIRECTORY_BROWSE_PARAM = "directory.browse";
    private static final String DOCUMENT_DEFAULT_PARAM = "document.default";
    private static final String MIME_DEFAULT_PARAM = "mime.default";

    private File mRootDir;
    private boolean mDirectoryBrowse;
    private String mDefaultDoc;
    private String mDefaultMime;

    public FileServlet() {
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
            
            long length = raf.length();
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
            
            int bufSize;
            if (length > 512) {
                bufSize = 512;
            }
            else {
                bufSize = (int)length;
            }
            
            byte[] inputBuffer = new byte[bufSize];
            
            int readAmount;
            while ((readAmount = raf.read(inputBuffer, 0, bufSize)) > 0) {
                out.write(inputBuffer, 0, readAmount);
            }
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
}
