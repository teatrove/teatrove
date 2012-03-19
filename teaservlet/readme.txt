README for TeaServlet/4.1.x

Copyright (C) 1999-2011 TeaTrove.org

The TeaServlet provides a way to separate the presentation from the processing
of data through the use of tea templates and simple tea applications. The three
required components for setting up and running the teaservlet are: The java
classes including both the TeaServlet itself and any user defined apps,
the templates for providing the HTML like front end for the apps and the 
properties file with configuration information and initialization parameters.

* NOTE - As of Tea 3.5.0  and TeaServlet 1.8.0, the minimum requried JVM version is 1.6.

Change History

4.1.0 to 4.1.2
===============================
- Fix #38 adding Asset Management feature
- Fix #26 addressing issue with servlet context compiler and sub-directories
- Fix #14 adding ability to auto load/inject config files per module


4.0.0 to 4.1.0
===============================
- Added substitution files and variable expressions in configuration files
- Enabled resolving configuration files against WEB-INF (via new ResourceFactory)
- Added support for both XML and enhanced property files natively
- Added initial support for serving static assets for admin applications


3.9.0 to 4.0.0
===============================
- Added automatic imports configuration to template block


1.8.2 to 3.9.0
===============================
- Changed version to match that of the entire TeaTrove project, from here on the project
  will be versioned together.
- Changed packaging from com.go to org.teatrove
- Changed license from The Tea Software License 1.1 to Apache License 2.0
- Changed copyright from Walt Disney Internet Group to TeaTrove.org
- Added raw and aggregate template statistics.
- Redesigned admin and monitoring pages.


1.8.1 to 1.8.2
===============================
- [DPTS-33] - TeaServlet now ignores template source files marked as hidden by the OS


1.8.0 to 1.8.1
===============================
- [DPTS-29] - add the ability to call out of Tea to other view technologies like JSP with a context function
- [DPTS-32] - update the changes made in [DPTS-17].  The 'info' log level messages made for compile progress in [DPTS-17] 
     were too verbose.  These have been changed to debug level
- [DPTS-31] - Set the JDK 1.5 cause on the ServletException thrown in TeaServletEngineImpl line 80


1.7.4 to 1.8.0
===============================
- *** Building for target platfrom java 1.6. ***

- [DPTS-24] - Depot.tea throws null pointer when TeaServlet is not clustered
- [DPTS-27] - Teaservlet Admin page Tea Template Stats table sorting does not work as expected
- [DPTS-17] - Template compile progress indicator log messages
- [DPTS-19] - Remove synchronization code introduced in the Tea profiler stats gathering 
- [DPTS-23] - Provide a configuration item so that RegionCaching can take advantage of LRUCache
- [DPTS-25] - Display source file path when showing template compile errors
- [DPTS-28] - performance bottleneck in hashCode() of TeaServletInvocationStats&Stats
- [DPTS-22] - Add a counter for RegionCache timeouts


1.7.3 to 1.7.4
===============================
- Resolve issue #DPTS-4. Fix broken anchor tags in BeanDoc.
- Resolve issue #DPTS-5. if template.path is specified, TeaServlet is 
  prepending the /WEB-INF/tea path. 
- Resolve issue #DPTS-6. Change profiler weight factors to percentages 
  on main templates page.
- Resolve issue #DPTS-7. Add inline profiler docs to template info page.
- Resolve issue #DPTS-9. Ability to check templates for errors without recompile.
- Resolve issue #DPTS-11. Fix Incorrect rounding of per page invokes impacting 
  net template times.
- Resolve issue #DPTS-14. Space after delimiter in template.path causes directory 
  not found error.
- Resolve issue #DPTS-18. Add logging API methods to TeaServlet's default context.


1.7.2 to 1.7.3
===============================
- Resolve issue #DPTS-3.  Template info page navigation bug.
- Resolve issues #TEA-32.  Added weight and invocation columns to main templates
  page with sorting capability.  Added raw invocation counters to template info
  page.
- Resolve issue #TEA-34.  Added new counters to Template Info page regarding
  substitution blocks.  Fixed "bookeeping" issues pertaining to double counting
  of substitution block times.


1.7.1 to 1.7.2
===============================
- Resolve issue #DPTS-2.  Added template execution times/sort capability to 
  main templates admin page.


1.7.0 to 1.7.1
===============================
- TeaServlet now compiles cleanly on JDK 1.6 with the exception of 1 deprecation
  warning pertaining to setStatus().  This will not be resolved any time soon.
- Resolved Jira issue #TEA-16.  encode/decodeParameter() methods on HttpContext
  now overloaded to accept character encoding parameter.
- Resolved Jira issue #TEA-19.  Fix malformed "bean" links in admin pages. 


1.6.8 to 1.7.0
===============================
- Modifications to support Jira issue #TEA-6, inline, production profiling of
  Tea templates.  Profiling is enabled by default it can be disabled via the
  property setting 'profiling.enabled=false'.
- Resolve issue Jira #TEA-10.  Can now set 'template.classes' independent of
  the setting for 'template.path' when deploying on Tomcat.
- Backed out an old feature that pre-registered plugins.
- The RegionCaching application makes use of the Depot statistics API's in
  Trove 1.6.0.  The Depot admin page (under Region Caching) displays this info.



1.6.7 to 1.6.8
===============================
- Resove Jira issue #DPTS-1.  Added ability to load teaservlet.properties from
  the classpath.  This provides greater configuration flexibility especially
  when Tea is deployed on Tomcat.


1.6.6 to 1.6.7
===============================
- Minor enhancement to support Tea Model 2.  Added a call to HttpResponse.setAttribute() 
  to place a reference to the TeaServlet instance as a response attribute.  This is 
  needed to support the jasper runtime.

1.6.4 to 1.6.5
===============================
- Minor enhancement for Tomcat deployments.   The path to WEB-INF/tea is prepended
  to the template.path parameter to deploy templates from a WAR directory structure.


1.6.0 to 1.6.1
===============================
- Imported new Tea and Trove jars.

1.6.0 to 1.6.1
===============================
- Removed alpha sort ordering of applications.  This has been reverted to previous
  behavior.
- Admin template changes to support method/function call hierarchies.

1.5.11 to 1.6.0
===============================
- Using the TeaLog which simplifies stack traces from Tea templates so they are
  easier for template authors to understand.  It strips off irrelevant stack 
  trace elements in the stack trace leaving only the relevant elements
  remaining.
- The Template hierarchy info pages will not be accessible if TemplateRepository
  is not enabled (initialized).

1.5.10 to 1.5.11
===============================
- Modifed RemoteCompiler class to support the template dependency repository.
- Added new tea template AdminTemplateInfo to display information contained
  within the template repository (Caller hierarchy, etc.).
- The TeaServletEngine destroy() method now properly calls the destroy() 
  methods on all plugins registered with PluginContext.
- Added validSize and invalidSize counters to the RegionCaching admin tool
  template.   Currently this displays only the local cache info until the
  RegionCacheInfo object can be updated on all clustered servers.
- Added conversion for Boolean wrapper types passed into invoked templates.
  Unsupplied values or empty strings will be null, the value "true" will be 
  interpreted as such, any other value evaluates to false.


1.5.9 to 1.5.10
===============================
- Import Tea 3.2.10.
- Added decodeParameter() function to HttpAdminContext in TeaServlet.
- Added session scope accessor functionality so that attributes can be
  retrieved from within templates (i.e. getRequest().session.attributes["attrname"])  
- ApplicationInfo objects returned by ApplicationDept.getApplications() is now 
  sorted alphabetically.  This is reflected in the admin tool.


1.5.8 to 1.5.9
===============================
- Fixed bug in DecodedRequest where it wouldn't cope with an original encoding
  that was specified as null. It now defaults to iso-8859-1 in that case.
- Added support for capturing statistics about template execution time.
- Added support for configuring the timeout to the Template Server Service 
  through the template.server.timeout property.
  
1.5.7 to 1.5.8
===============================
- Overloaded cache() function in region caching such that a boolean can be
  passed in to indicate if the key parameter is used to uniquely identify the
  cached region.
- Fixed a bug where all applications within a TeaServlet are recreated are
  reinitialized whenever templates are reloaded when there are context changes.
- URLExists now handles 500 and 504 errors correctly.
- Removed some deprecated API and calls.
- Fixed bug in RemoteCompiler that synchronized all compiled class file
  timestamps even if there are compilation errors. Now the timestamps are
  sync'ed only if there are no compilation errors. This fixes a bug where bad
  templates would not show compilation errors.
- Fixed bug in RemoteCompiler where persistent connections to the template
  server servlet were not working.
- Updated administration pages: removed error conditions, added better links 
  between pages and simplified Kettle integration.

1.5.6 to 1.5.7
===============================
- Fixed a bug in which 404s were served during context reload because
  the mTemplateSource member of TeaServletEngine was pointing to a
  TeaServletTemplateSource that had not been fully initialized yet
  (the templates had not been compiled yet).
- Restructured the TeaServletEngineImpl class to ensure that access to its
  mApplicationDepot and mTemplateSource members is synchronized to
  protect against exposing an incorrect state (e.g., the 404 bug above).
- Changed the createHttpContext method in TeaServletEngineImpl to use
  the content source from the request rather than the
  TeaServletEngineImpl object.  This was done to fix a bug in region
  caching in which the state exposed by the request and the state
  exposed by the TeaServletEngine were temporarily out of sync upon
  context reload causing a ClassCastException.

1.5.5 to 1.5.6
===============================
- Included the new 3.2.6 release of Tea.

1.5.4 to 1.5.5
===============================
- Fixed a bug in template source interpretation when the
  TemplateServerService is used.  The bug was that precedence for
  multiple template sources was right to left instead of left to
  right.
- Fixed a bug in ContextDetail template where a <BR> tag was missing between 
  the parameters in the method description.


1.5.3 to 1.5.4
===============================
- Updated to add a trailing $ to context prefixes rather than
  expecting $ to already be present.  When it is already present,
  Kettle versions before 4.0.4 are confused by the trailing $,
  resulting in compile errors when functions are called using
  prefixName.functionName notation.
- Fixed a bug in Kettle/TeaServlet integration.  The TeaServlet was returning
  context prefix names with trailing $ characters.  Kettle versions before 
  4.0.4 are confused by the trailing $, resulting in compile errors when 
  functions are called using prefixName.functionName notation. 


1.5.2 to 1.5.3
===============================
- Modified TeaServletTemplateSource and AdminApplication due to the changes
  in Tea engine.
- Fixed a NullPointerException that appeared when cluster peers were already 
  reloading and made a few changes to the display behavior after a reload.
- Fixed the bug causing peers to disappear without be restored to the list
  of Unresolved Server Names.
- Added numerous info level log messages relating to template reloads and a
  few debug messages to diagnose the disappearing cluster peers issue.
- Modified HttpContextImpl to override DefaultContext provided Writer methods.


1.5.1 to 1.5.2
===============================
- added an "else" to the cluster reload so it didn't always show errors 
  from the other machines even when things worked fine.
- fixed some synchronization issues when reloading templates.  If another
  reload is already in progress, a message will appear indicating that the
  second reload was aborted.
- fixed an ArrayIndexOutOfBoundsException that would occur when 
  getTemplateErrors() was called on the TeaservletTemplateSource and there
  more errors than known templates.
- createTransaction in TeaServletEngineImpl now only looks up a template when 
  asked, and if no Template has been looked up, a TemplateLoader is provided 
  to the ApplicationRequestImpl.
- DetachedResponseImpl now has its setRequestAndHttpContext method called 
  after being created. This allows multiple layers of region caching to work 
  properly.
- Modified the RemoteCompiler to show full template paths in the compilation
  error message.

1.5.0 to 1.5.1
===============================
- Added the TemplateAdapter to Tea so precompiled templates would continue 
  to work.

1.4.x to 1.5.0
===============================
- Modified the clustering code to ensure that all RMI communication occurs 
  on the backchannel.
- Reimplemented much of the internals of the TeaServlet in order to support
  reloadable contexts and organize key funtionality into well defined modules.
  Much of the functionality not related to servlets has been move to the 
  org.teatrove.tea.engine package, upon which the TeaServlet depends heavily.

1.4.4 to 1.4.5
===============================
- Modified the clustering code to ensure that all RMI communication occurs 
  on the backchannel.
- The RemoteCompiler now uses HttpClient from trove rather than handling
  sockets directly.
- Templates will now be shown in the Admin page even if not yet loaded. 
- Last template reload time and number of known templates is now displayed.
- Fixed bug in stealOutput that prevented it from ever working.

1.4.3 to 1.4.4
===============================
- MSIE 4.x doesn't seem to support the way the TeaServlet compresses output,
  even though it is a legal GZIP stream. Change made to isCompressionAccepted
  method of ApplicationRequestImpl.

1.4.2 to 1.4.3
===============================
- The getClassForName method in AdminApplication now uses the ClassLoader from
  the MergedContext.
- Automatic cluster discovery and RMI based cluster administration added. 
- TeaServlet also accepts "*.tea" style servlet pattern mappings. Any extension
  may be used.
- RegionCachingApplication supports gzip compression of cached regions.
  Browsers that accept GZIP content encoding may receive a compressed response.
  To enable, the gzip initialization parameter must be set 1 to 9.
- Created TeaServletEngine and TeaServletTransaction interfaces. Now any
  servlet can access and use TeaServlet services.

1.4.1 to 1.4.2
===============================
- Fixed dynamicTemplateCall to work with servlet containers that don't support
  servlet API 2.3

1.3.x to 1.4.1
===============================
- Added a new template.preload parameter that when set to false causes
  templates to load only when called, thus reducing memory usage.
- Applications can now plug into the admin pages by implementing AdminApp.
- Admin pages use a common admin page with dynamic calls to specific detail 
  pages.
- Enhancements to RegionCachingApplication. Regions are now cached using the
  Depot class, and a new nocache function is provided.

1.3.1 to 1.3.2
===============================
- Changes for Servlet API version 2.3.
- Added support for Plugins, which provide an easier way to configure in
  resources that need to be accessed from multiple Applications.

1.3.0 to 1.3.1
===============================
- Reduced excessive synchronization during template reload that prevented
  other requests from being serviced until the reload completed.
- Region caching application now weakly references templates in its keys,
  allowing old templates to be unloaded much sooner.
- Added three new admin templates: GetClass, ContextClassNames, and 
  ContextPrefixNames.  Together these templates provided support for the
  Kettle 4 project integration features.  The GetClass template uses a new
  AdminContext function called streamClassBytes which enables HTTP-based 
  class loading directly from the TeaServlet.  The ContextPrefixNames template
  utilizes a new property (contextPrefixName) of the ApplicationInfo class.

1.2.x to 1.3.0
===============================
- Context classes are no longer created immediately on every request. Instead,
  the Application.createContext method is called only when a defined function
  is first needed.
- The insert URL family of functions no longer use java.net.URLConnection.
  Instead, they use HttpClient in Trove. A new function, setURLTimeout,
  overrides the default timeout for accessing URLs.
- Bug fixed in admin pages when displaying applications that have no context.
- The io package has been deprecated, and all classes have been moved into
  Trove.

1.2.2 to 1.2.3
===============================
- FileByteData class is now thread safe. When added as a surrogate and then
  cached, one thread would open the file while another would close it.
- Added readFile and readURL functions. They return the file contents as a
  String, and they support different character encodings.
- Application names may be specified with a hyphen. The part before the hyphen
  serves as the optional function prefix. This allows multiple applications
  to appear to provide a unified set of functions.
- Templates may now be loaded over http.  Any template path beginning with 
  "http" will use the remote loading mechanism other templates will be loaded
  as before.
- Compiled templates will be synchronized with their source timestamp to ensure
  that templates are recompiled in a consistent manner despite clock offsets.

1.2.1 to 1.2.2
===============================
- Slight change in MergedApplication with respect to class loading. Sometimes
  it would load classes from the system class loader, bypassing any class
  loader imposed by the Servlet Engine.
- Deprecated ObjectIdentifier.
- TTL in RegionCachingApplication is specified in milliseconds now instead of
  seconds for consistency and greater precision.
- Improved concurrency in RegionCachingApplication by using more specific
  synchronization monitors. Also correctly supports arrays as secondary keys.
- ServletExceptions caught by TeaServlet now pass message on to sendError call.

1.2.0 to 1.2.1
===============================
- Calling setContentType on ApplicationResponse sets character encoding of
  character buffer. Before, only the template could change character encoding.
- Supports TemplateLoader.Template.getTemplateLoader of Tea 3.1.1.
- Performance optimizing InternedCharToByteBuffer was not properly being
  invoked. Now that it is, performace of TeaServlet is back to what it was
  before Barista 2.2.
- Calling insertFile on a non-existent or inaccessible file now logs an error
  at the point the insert is called. Before, the error would be logged when
  the template output was written, making it difficult to track down the
  culprit.
- Incorporated BeanDoc into the build process so that the context BeanInfos
  can be autogenerated.

1.1.1 to 1.2.0
===============================
- Added template reload for clustered servers.
- Changed many of the fields and the ContextImpl inner class from private to
  protected within AdminApplication simplifying extension of the TeaServlet.
- Added a getTeaServletClass() method to TeaServletAdmin to help determine the
  name of the class currently serving as the TeaServlet.
- Query string shown in error processing template message.
- Added exception guardian mode, enabled with template.exception.guardian
  property. When enabled, each statement in the template has an exception
  handler so that RuntimeExceptions don't cause the template to abort.
- Fixed bug that wouldn't report internal compiler errors in reload page.

1.1.0
===============================
- The first open source version of the TeaServlet, which was pulled out of the
  Barista product and cleaned up.
- TeaServlet runs in Servlet Engines that support version 2.1 or 2.2 of the
  Servlet API.
