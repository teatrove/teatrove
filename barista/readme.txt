README for Barista/3.9.x

Copyright (C) 1999-2011 TeaTrove.org

This README includes late breaking product information pertaining to defects
or issues to be aware of in Barista.  The main set of documentation can be
found on the TeaTrove site at the following link:

http://teatrove.org/

*************************

Change History

3.3.6 to 3.9.0
===============================
- Changed version to match that of the entire TeaTrove project, from here on the project
  will be versioned together.
- Changed packaging from go to org.teatrove
- Added license Apache License 2.0
- Changed copyright from Walt Disney Internet Group to TeaTrove.org


3.3.5 to 3.3.6
===============================
- Incorporate double logging fix (service manager only).
- Updated to use latest application stack components.


3.3.4 to 3.3.5
===============================
- All new versions of Trove, Tea, TeaServlet
- Unicode charset for internationalization support via file.encoding parameter


3.3.3 to 3.3.4
===============================
- Fixed yet another bug with TransientSessionStrategy.

3.3.2 to 3.3.3
===============================
- Import new Trove and Tea jars.

3.3.1 to 3.3.2
===============================
- Fixed bug in TransientSessionStrategy.   The getSession() method on HttpSession 
  was returning null when called a second time within the same request.

3.3.0 to 3.3.1
===============================
- Fixed startup validator so that TeaServlet property files referenced from
  the Barista properties files are handled correctly.
- Added support for org.teatrove.trove.util.Plugin to Barista.   Needed for SpyGlass
  project so that JMX support can be "bolted on".
- Added new "Inactive Templates" admin page that lists templates without a 
  corresponding transaction queue.

3.2.11 to 3.3.0
===============================
- Import latest versions of Trove, Tea and TeaServlet

3.2.10 to 3.2.11
===============================
- Import latest versions of Trove, Tea and TeaServlet

3.2.9 to 3.2.10
===============================
- Import latest versions of Trove 1.4.9, Tea 3.2.11, and TeaServlet 1.5.11
- Fixed SWID generation format.   Use java.rmi.server.UID to generate ID's.

3.2.8 to 3.2.9
===============================
- Import new version of TeaServlet, 1.5.10
- Fixed session management bug in TransientSessionStrategy.  Anonymous inner class
  that implements HttpSessionStrategy.Support (nested interface) holding an unecessary
  reference to HttpSession.   HttpSession.getSession(false) not returning null as 
  expected for invalid sessions.
- Added template service time warning threshold to TQTeaServlet and admin template.
  Configured by adding "template.queue.warn.threshold.level" to barista properties file.
  This should be set equal to the desired number of milliseconds.  Add this parameter 
  after the "template.path".  On the Queue Stats. admin page, Avg. Template Times will
  display with a red background when the threshold value is exceeded.  The feature is
  disabled by default.
- Add parameter (template.queue.warn.threshold.logenabled) that when set = 'true' sends 
  template service threshold warnings to the log as well.
- Added decodeParameter() function to HttpAdminContext in TeaServlet.


3.2.7 to 3.2.8
===============================
- Imported new version of TeaServlet, 1.5.9.
- If configured context listener or session listener is not of a supported
  type, an error is logged now.
- Added support for capturing statistics about template execution time in the
  TQTeaServlet.

3.2.6 to 3.2.7
===============================
- Barista now sets a single cookie for each name unless the HttpHandler
  configuration specifies cookieMode = multiple
- Supports proxy requests by saving host from full URI into host header.

3.2.5 to 3.2.6
===============================
- Updated versions of Trove, Tea and TeaServlet
- Added validation of the httpServer.servlets.Tea.init.plugins
  properties. This gets rid of the startup error message
  "org.teatrove.barista.validate.event.InvalidPropertyError: plugins is not
  allowed in section: httpServer.servlets.Tea.init.plugins".

3.2.4 to 3.2.5
===============================
- Setting cookies with a value of null actually sets "" instead of "null".
- SWIDSessionStrategy overwrites all SWID cookies if the length of the SWID is
  not 36.  This is incorrect since sometimes the size of the SWID varies 
  depending on login status (the curly brackets).
- The getRequestURL method was adding an extra port to the URL when the "Host"
  header was provided.

3.2.3 to 3.2.4
===============================
- Added attribute map to HttpServerConnection, which is shared by
  ServletRequest.
- Modified the TQTeaServlet to create transaction queues for each template
  when hit via a mapping beginning with * (e.g. *.tea = TeaServlet).

3.2.2 to 3.2.3
===============================
- Request parameter values converted to encoding specified by
  setCharacterEncoding. As per the servlet spec, setCharacterEncoding must
  be called prior to accessing any parameters in order to have any effect.

3.2.0 to 3.2.2
===============================
- Modified BaristaAdminApplication and BaristaAdmin to account for changes to
  the Restartable interface.
- Fixed bug in BaristaPropertyMapValidator where comma delimited lists in the 
  FilterMap sections were not evaluated properly resulting in erroneous errors.
- Filters are now invoked when using getRequestDispatcher.

3.1.x to 3.2.0
===============================
- Added validation for the Barista Properties File. Rules files are included in
  the deliverables jar file. Custom rules files can be created for Servlets
  and Tea Applications.
- getRequestURL() will now look for the "Host" header when recreating the URL.
- Supports the final release of the Servlet 2.3 API. Filters that were compiled
  against earlier versions of the 2.3 API will still work, but they must be
  modified if they are compiled against the servlet classes distributed with
  Barista.
- Fixed security defects in FileServlet and ServletContext.getRealPath. When
  given special escape characters, files outside the wwwroot directory could
  be downloaded.
- Compatible with jdk1.4, as well as jdk1.3.
- Removed confusing debug messages that appear when clients close the HTTP
  connection.
- Servlet and filter mapping of "/" is no longer interpreted as "/*".
- Servlets now loaded and mapped before being initialized. Servlets can now
  access other servlets from their intialize method.

3.1.3 to 3.1.4
===============================
- Made getId() in TransientSessionStrategy.Session not synchronized to fix a 
  deadlock condition.
- TransientSessionStrategy, BaristaSessionStrategy, and SWIDSessionStrategy
  would always send a new unique cookie value when creating a session. Now, if
  the user provides an acceptable cookie value, it is not replaced. Creating
  new unique values for new sessions had two problems. First, if the user came 
  in from a proxy that does routing based on cookie, it could take several hits
  before the user actually got stuck to a server. Second problem was if the
  user provided a cookie that a login system already set. The
  SWIDSessionStrategy would throw away the valid SWID and replace it with a
  synthetic one, logging the user out.
- TransientSessionStrategy, BaristaSessionStrategy, and SWIDSessionStrategy
  support a new property, "redirect.on.create". When true, the user is
  redirected back to the same URI when the session cookie is first created.
  If requests are coming from a proxy that performs sticky routing based on the
  session cookie's hash code, the redirect ensures that the session is created
  on the correct server. Since the browser is likely to ignore a redirect to
  an identical URI, an addional '?' or '&' is added to the URI to make it
  slightly different.

3.1.2 to 3.1.3
===============================
- Calling HttpServletRequest.getCookies() no longer aborts if an invalid cookie
  is present.  All valid cookies will be returned.

3.1.1 to 3.1.2
===============================
- TransientSessionStrategy now uses a SessionTimer to immediately remove 
  invalid Session Objects rather than having them hang around until expiration
- RequestDispatcher.include was allowing status code to be set by included
  servlet.
- Added BaristaRequestDispatcher which extends RequestDispatcher.

3.1.0 to 3.1.1
===============================
- Fixed bug when calling sendError or sendRedirect after calling
  getOutputStream or getWriter. A "Response has been committed" error would be
  logged even though no attempt had been made to write output.

3.0.x to 3.1.0
===============================
- Fixed bug in HttpServerConnectionImpl not flushing when flush was called.
- Servlets definitely destroyed upon exit.

2.x.x to 3.0.0
===============================
- Major refactoring of HTTP implementation.
- Supports Servlet 2.3 features, including filters.
- Configuration of some features has changed: Error forwarding, impression
  logging, and charset aliases.

2.5.1 to 2.5.2
===============================
- Imports new versions of TeaServlet and Trove to pick up bug fixes.
- DatabaseScribe uses transactions during database updates.

2.5.0 to 2.5.1
===============================
- Uses TransactionQueue, fast I/O streams, and CheckedSocket from Trove now.
- SSL support now included with Barista.
- Property files are now subject to validation to reduce configuration errors.
- Both Oracle and SQL Server now supported for impression logging with the 
  DatabaseScribe.
- Mapping of error forwarding URIs may now be specified.

2.4.x to 2.5.0
===============================
- Calling setQueryString() no longer clears the existing parameters, but 
  it does preempt existing parameters with those in the new query string.
- ServletRequest.getServerName() now stores the server name as a context
  Attribute "org.teatrove.barista.http.serverName.(ip address of server)" for 
  quicker lookup.
- Cancelling an HttpTransaction now closes the socket directly rather than 
  just the socket's OutputStream.
- Pluggable session support enabled allowing custom session strategies.
- Added servlet reload for clustered machines.
- Added DatabaseScribe for logging hits to a database.
- Multiple bind adresses can be specified in the properties file. The first 
  valid address will be used.
- Servlet reload now performed using the preferred technique of specifiying an
  optional servlet classpath. This is much safer, reliable, and predictible.
  The property "classpath" must be specified in a servlet dispatcher. This
  classpath will only be used to load classes not found in the system
  classpath.
- Fixed defect in reading HTTP headers, introduced in 2.4.0. Headers with date
  values were being split up because of the comma in the date.
- Failure to disable Nagle's algorithm does not cause response to be aborted.
- Added new caching servlet, CacheServlet. It wraps any other servlet and
  allows entire pages to be cached.

2.4.2 to 2.4.3
===============================
- Default socket write buffer size is 65535.

2.4.1 to 2.4.2
===============================
- TQTeaServlet uses Cache class for managing transaction queues so that they
  don't get removed all the time.
- The Dispatcher and TransactionQueue browsers now use a modifiable query
  string rather than the ObjectIdentifier system. This permits better 
  administration clustering.
- Properly handles absolute URI requests.
- Properly closes response if forwarding to error page when queue is full.

2.4.0 to 2.4.1
===============================
- Support for persistent connection broke in version 2.4.0, and are now working
  again.
- Pluggable session support now available.
- setAttribute and getAttribute now work the same as the deprecated
  putValue and getValue methods (not sure why they were deprecated though)
- Sessions are invalidated as soon as they time out not just when next
  accessed.
- BaristaSessionStrategy (the default) sets a cookie with max age -1 and a path
  of "/". Previously, the age was 365 days, and no path was explicitly set.
- Added ProxyServlet, which emulates Proxy.dll. A future version of Barista
  may have this functionality better integrated in.

2.3.x to 2.4.0
===============================
- Added an Impression Log to the HttpServer.
- Adheres to Servlet API version 2.2 with the exception that neither the 
  UserPrincpal and UserRole security model or The Web Application Archive(.WAR)
  deployment system is implemented. Previous versions of Barista only
  implemented Servlet API version 2.1 features.
- Created extended version of TeaServlet, TQTeaServlet that runs templates
  through individual transaction queues.
- Servlets, ServletDispatchers, and their properties can now be reloaded.

2.3.1 to 2.3.2
===============================
- Revised handling of servlet mappings to better handle Barista specific styles
  of mappings. The Servlet spec describes how the servletPath and pathInfo
  should be set against the following mapping styles: x, x*, and *x.
  Since barista accepts multiple wildcards and in more places then just the
  ends, special rules needed to be devised for setting the servletPath and
  pathInfo. In previous versions, the URI would be split at a peculiar
  position. The split now occurs at the first wildcard not at the start of the
  pattern. As a result, the following pattern: /*.thtml supplies an empty
  servletPath and a full pathInfo. Even though the '/' appears before the
  wildcard character, the pathInfo absorbs it to ensure it leads with a slash.
  The standard *.thtml pattern supplies a full servletPath and a null pathInfo,
  in accordance to the specification.
- Fixed NullPointerException bug in calling getRealPath.

2.3.0 to 2.3.1
===============================
- Sample properties files are now correct.
- Unauthorized access warning message no longer produced when hitting Barista
  admin pages. This was caused when the template tries to link to the template
  admin page.

2.2.x to 2.3.0
===============================
- Uses opensource versions of Tea, TeaServlet and Trove. PubSysCommon product
  is no longer referenced since those components are in Trove now.
- Barista admin pages are not rolled into TeaServlet admin application. To
  enable Barista admin pages, configure the following application into the
  TeaServlet: org.teatrove.barista.admin.BaristaAdminApplication
- Generic I/O exceptions thrown from sockets closed by clients are reported
  as debug messages instead of errors.
- TransactionQueue supports suspend and resume operations.

2.2.0 to 2.2.1
===============================
- HttpServletRequest.getSession(boolean create) now checks for an exisitng 
  session before creating one if create is true. getSession() continues to
  have the same behavior of getSession(true).
- HttpHandler supports custom socket write and read buffer sizes, configured
  using socket.buffer.write and socket.buffer.read.

2.1.x to 2.2.0
===============================
- HttpServletRequests now support getPathTranslated, as long as servlet sets
  "directory.root" property.
- Supports a master properties file which can be loaded via a file name or URL.
  It is specified with a top level property named "properties.master".
- Order of properties is now preserved with respect to its order in the
  properties file.
- The logging classes and some utility classes have moved to PubSysCommon and
  the go.pubsys package.
- TeaServlet uses Tea 2.3.x.
- TeaServlet supports "atomic" application reload, that is, applications may be
  safely reloaded while the TeaServlet is processing requests.
- TeaServlet properties are now broken down into different groupings so that
  an application class may be specified multiple times. Applications now
  require an assigned name. Application properties are also isolated from each
  other.
- TeaServlet Application name is accessible from the ApplicationConfig.
- TeaServlet Application defined functions can now be accessed in templates
  using the "fully qualified" name, which is <appname>.<functionname>(<params>)
- TeaServlet Application interface changed. The createContext method accepts
  two parameters now instead of three. They are ApplicationRequest and
  ApplicationResponse, which extend the standard HttpServlet request/response
  classes. The internal response buffer is available from ApplicationResponse.
- TeaServlet supports "spiderable" URLs. This feature allows a TeaServlet to
  be configured to accept different separator strings for '?', '&' and '='.
  By setting separator.query, separator.parameter and separator.value to
  something like ".go/", "/", ":", a search engine should index the URL.
- TeaServlet HttpContext supports two new functions, URLExists and insertURL.
  They operate much like fileExists and insertFile, except (not surprisingly)
  use URLs to specify resources.
- TeaServlet allows Applications to load context classes using their own
  ClassLoaders. Before, templates wouldn't have access to the context class or
  any other classes produced from the custom ClassLoader.
- Use of getRequest().parameters["name"] from HttpContext is easier. Returns
  null if property doesn't exist, or it can be compared directly against
  string values without having to be explicitly converted to a string. Since
  multiple parameter values can exist, they can be looped over using foreach.
  The number of parameter values can be accessed via the length property, and
  individual values can still be accessed by array index.

2.1.3 to 2.1.4
===============================
- Tuned socket writing performance by disabling Nagle's algorithm and
  increasing the send buffer size.

2.1.2 to 2.1.3
===============================
- Uses Tea 2.3.5.
- Improved concurrency performance in InternedCharToByteBuffer.

2.1.1 to 2.1.2
===============================
- Fixed RequestDispatcher.include and forward to allow servlets to respond
  with either a stream or writer, regardless of what the caller used.
- Fixed HttpServletResponse.getWriter to guard against being closed if being
  included.
- TransactionQueues support self-tuning of queue size and thread count. They
  are controlled with boolean properties, tune.size and tune.threads.
- Implemented ServletContext.getResource and
  ServletContext.getResourceAsStream.
- Fixed RequestDispatcher.forward to not pass into another TransactionQueue and
  thus won't deadlock.

2.1.0 to 2.1.1
===============================
- Minor changes to SoftHashMap to support proper cleanup and
  HttpServletDispatcher for better impression log integration.
- Uses StarwaveCommon version 1.1.7 to pick up improvements in the fast
  buffered I/O streams.
- Fixed defect in FileServlet that would sometimes cause Netscape to
  incorrectly render a page when that page referenced a stylesheet file. By
  reducing the buffer size, the first response packet includes the headers and
  the start of the file. Before, the first packet just contained the headers.
  This should also reduce network traffic slightly. CQ #874.
- Fixed defect in the TeaServlet FilteredClassLoader that would sometimes
  not completely read in a class file, causing a "truncated class file" error.
  CQ #1268.
- Fixed bugs with mapping of '/' path. Error would manifest itself as a 400
  error or the browser believing that the host doesn't exist. Changes were
  made to FileServlet and TeaServlet. CQ #1240.
- TeaServlet emits warnings if any template directory doesn't exist or doesn't
  have the correct permissions. Before, the the TeaServlet would be stuck in
  an invalid state. CQ #910.
- Fixed a deadlock issue in HttpServletDispatcher if calling sendError would
  forward to another servlet.
- Fixed a bug in the TeaServlet's TemplateDepot in that the template compiler
  was not informed of the correct ClassLoader to use. As a result, some objects
  were not allowed to be passed to other templates, even though they appeared
  to be of the same type.
- Added a HttpServletRequest wrapper, DecodedRequest to the http package that
  automatically decodes request parameters to a specified character encoding.
- TeaServlet default functions now includes an overloaded getRequest which
  accepts a character encoding to apply to all request parameters.
- HttpHandler supports optional charset.aliases property group which provides
  a mapping from charset codes to Java character encodings. Java can already
  understand and convert many charset codes to character encodings, but this
  allows additional ones to be provided or existing ones to be overridden.
- Several classes, constructors, methods and fields in the TeaServlet's package
  should have been hidden and now are.
- TeaServlet applications that fail to load no longer prevent other
  applications from loading. As many applications are loaded as possible,
  logging errors for ones that failed to load.

2.0.x to 2.1.0
===============================
- TeaServlet supports multiple applications (comma-delimited) using the 
  propery "teaServlet.applications". The old property, "teaServlet.application"
  is no longer supported.
- In order to support multiple applications, some of the TeaServlet API hooks
  have changed. Applications and Actions created against version 2.0.x will
  need minor modifications.
- The TeaServlet's template context, org.teatrove.barista.servlet.tea.HttpContext is now
  filled with more utility functions for processing an HTTP request and
  managing an HTTP response.  The HttpContext is now an interface to support
  precompiled templates.
- An Application's template functions no longer need to be part of an
  HttpContext.  They can be contained in any object.
- The sendError and sendRedirect template functions abort template execution.
- Support for internationalization. Barista now understands the "charset"
  property of content type. With the new functions in HttpContext, templates
  can set the content type and charset.
  i.e. setContentType("text/html; charset=iso-8859-1")
- FileServlet redirects with an relative URL, that is, the protocol and
  host are not provided. This ensures correct redirect operation when requests
  have been proxied.
- TeaServlet redirects now when a default template is served to ensure that the
  path ends in a slash. This is required for correct operation of relative
  links in the default page.
- The TickTalk utility class has been added as an example of how to talk to
  Tick or any other UberCache.

2.0.1 to 2.0.2
===============================
- TeaServlet supports optional directory for saving compiled templates via
  the new "teaServlet.template.classes" property. If not specified, compiled
  templates are just stored in memory.
- TeaServlet supports multiple template directories in the
  "teaServlet.template.path" property. Separate each with the file separator,
  which is ';' on Windows.
- Various other TeaServlet changes for better thread safety and use of new
  Tea utility components.
- Various changes made to better support JTP compatibility product.
- Some improvements made to Barista Administration pages and more information
  is shown. More work still needs to be done.
- TransactionQueue supports optional transaction timeout for cancelling
  transactions that were queued too long and have expired.
- Fixed defect that prevented SocketOutputStream print method from working if
  no content length was set.

2.0.0 to 2.0.1
===============================
- TeaServlet Admin page shows the line in the Tea file that caused the
  compilation error.
- Template errors are passed as objects instead of strings to the Admin
  template.
- Log events are passed as objects instead of strings to the logging template.
- The TeaServlet Admin page has a link to the LogViewer page.
- ServletContext.getRealPath method is implemented, but will work only when
  the new servlet property, "directory.root", is set.
- HTTP accept loop more robust with respect to certain kinds of errors. Those
  errors were causing the loop to exit.
- Persistent HTTP connections work again.
- Fixed major bug in ThreadPool that would cause a TransactionQueue to stop
  processing transactions. The previous workaround was to set the idle timeout
  to infinite (-1). The idle timeout now works properly and should be used.
