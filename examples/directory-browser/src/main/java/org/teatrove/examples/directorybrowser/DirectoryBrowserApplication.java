package org.teatrove.examples.directorybrowser;

import javax.servlet.ServletException;

import org.teatrove.teaservlet.Application;
import org.teatrove.teaservlet.ApplicationConfig;
import org.teatrove.teaservlet.ApplicationRequest;
import org.teatrove.teaservlet.ApplicationResponse;
import org.teatrove.trove.log.Log;

/**
 * This is a simple example of a working TeaServlet application that presents 
 * content based on data passed in by the request. All TeaServlet application 
 * classes must either implement Application or extend some other class that 
 * does so.
 */
public class DirectoryBrowserApplication implements Application {

    private Log mLog;
    private ApplicationConfig mConfig;

    /**
     * Default constructor required for TeaServlet applications.
     */
    public DirectoryBrowserApplication() {
    	super();
    }
    
    /**
     * Initialize this application with the provided application configuration.
     */
    @Override
    public void init(ApplicationConfig config) 
    	throws ServletException {

        // The ApplicationConfig is used by the Application to configure itself.
        mConfig = config;

        // A log for keeping track of events specific to this application
        mLog = config.getLog();

    }

    /**
     * Destroy this application and clean up any necessary data.
     */
    @Override
    public void destroy() {
    	// nothing to do
    }

    /**
     * Get the associated context that this application will create, manage, 
     * and register in the TeaServlet.
     */
    @Override
    public Class<?> getContextType() {
        return DirectoryBrowserContext.class;
    }
    
    /**
     * Create the stateful context for the given request/response.
     */
    @Override
    public Object createContext(ApplicationRequest request,
                                ApplicationResponse response) {
    	mLog.debug("Creating DirectoryBrowserContext...");
        return new DirectoryBrowserContext(request, response, this);
    }

    /**
     * Allow functions in the context get at the initialization parameters.
     */
    public String getInitParameter(String param) {
        return mConfig.getInitParameter(param);
    }

}



