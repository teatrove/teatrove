package org.teatrove.examples.helloworld;

import javax.servlet.ServletException;

import org.teatrove.teaservlet.Application;
import org.teatrove.teaservlet.ApplicationConfig;
import org.teatrove.teaservlet.ApplicationRequest;
import org.teatrove.teaservlet.ApplicationResponse;

/**
 * This is a simple Hello World TeaServlet application that provides access to 
 * the {@link HelloWorldContext}. This application may be registered within Tea
 * to provide templates access to the public methods within the associated
 * context.
 * <br /><br />
 * The application registers its context type via {@link #getContextType()}.
 * This example always returns the HelloWorldContext type. TeaServlet will
 * invoke {@link #createContext(ApplicationRequest, ApplicationResponse)} per
 * request to actually provide access to the associated context. This particular
 * request is a singleton and stateless and therefore always returns the same
 * context as created during {@link #init(ApplicationConfig) initialization}.
 */
public class HelloWorldApplication implements Application {

	// Define the particular context for this application
	private HelloWorldContext context;
	
	/**
	 * Default constructor. Tea expects a default constructor to create an
	 * instance of the application.
	 */
	public HelloWorldApplication() {
		super();
	}
	
	/**
	 * Initialization routine that is invoked by TeaServlet after invoking the
	 * default constructor to allow the application to configure itself. The
	 * supplied configuration may be used to get access to the TeaServlet
	 * environment as well as to get access to the configuraton properties
	 * provided in the application configuration defined in the TeaServlet
	 * configuration file (ie: the &lt;init&gt; block of the application).
	 * 
	 * @param config The application configuration and TeaServlet environment
	 * 
	 * @throws ServletException if a critical error occurs during initialization
	 *                          that should stop TeaServlet from continuing
	 */
	@Override
	public void init(ApplicationConfig config) throws ServletException {
		// get the configuration properties and get the "greeting" configuration
		// if not provided, it will default to "Hello"
		String greeting = config.getProperties().getString("greeting", "Hello");
		
		// create the singleton/stateless instance
		this.context = new HelloWorldContext(greeting);
	}

	/**
	 * Destroy routine that is invoked by TeaServlet when the servlet is being
	 * shutdown. This allows the application to clean up after itself such as
	 * closing any active connections.
	 */
	@Override
	public void destroy() {
		// nothing to do
	}

	/**
	 * Get the type of context that this application will create. The TeaServlet
	 * will register any public/non-static methods of the specified context
	 * type in the list of available functions that may be invoked from within
	 * templates. Note that the 
	 * {@link #createContext(ApplicationRequest, ApplicationResponse)} method
	 * must return an instance that is compatible with this type. If not, a
	 * runtime exception will occur.
	 * 
	 * @return The context that this application maintains that should be
	 *         registered with the TeaServlet
	 */
	@Override
	public Class<?> getContextType() {
		// return the type of the context
		return this.context.getClass();
	}
	
	/**
	 * Create an instance of the associated {@link #getContextType() context}
	 * type per the given request/response. This is invoked per request by the
	 * TeaServlet to get the associated context instance. This may return a
	 * stateless context or create a new context with the given request/response
	 * each time. The returned type, however, MUST be compatible with the
	 * asociated context type. Otherwise, a runtime exception will occur.
	 * 
	 * @param request The active TeaServlet request
	 * @param response The active TeaServlet response
	 * 
	 * @return The associated context instance
	 */
	@Override
	public Object createContext(ApplicationRequest request,
                                ApplicationResponse response) {
		return this.context;
	}
}
