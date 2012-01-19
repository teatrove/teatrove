package org.teatrove.examples.directorybrowser;

import java.io.File;

import org.teatrove.teaservlet.ApplicationRequest;
import org.teatrove.teaservlet.ApplicationResponse;

/**
 * This is a TeaServlet context that is registered via the directory browser
 * application. It provides access to functions that templates may invoke to
 * traverse a directory structure.
 */
public class DirectoryBrowserContext {

    private ApplicationRequest mRequest;
    @SuppressWarnings("unused") private ApplicationResponse mResponse;
    private DirectoryBrowserApplication mApp;

    public DirectoryBrowserContext(ApplicationRequest request,
                                   ApplicationResponse response,
                                   DirectoryBrowserApplication app) {
        mRequest = request;
        mResponse = response;
        mApp = app;
    }

    /**
     * Gets an array of files in the directory specified by the "path" 
     * query parameter.
     */
    public File[] getFiles() {

        String path = mRequest.getParameter("path");
        if (path == null) {
            path = mApp.getInitParameter("defaultPath");
        }
        if (path == null) {
            path = "/";
        }

        File activefile = new File(path);
        if( activefile.isDirectory()) {
            return activefile.listFiles();
        } else {
            return null;
        }
    }
}
