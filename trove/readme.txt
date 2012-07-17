README for Trove/4.1.x

Copyright (C) 1997-2011 TeaTrove.org
 
Trove contains a rich cache of useful utilities used by both Tea and the TeaServlet.

Change History

4.1.2 to 4.1.3
===============================
- Fix #152 fixing signature calculation for wildcard types using 'super'
- Fix #96 adding @Deprecated to merged classs methods
- Adding annotation support to classfile (thanks to the Cojen library)
- Fix #19 to support file, classpath, or any URL-supported protocol for resources
- Fix #55 and cleanup merged context to better support generics and bridge methods

4.0.0 to 4.1.2
===============================
- Bug fixes

3.9.0 to 4.0.0
===============================
- Add generics support to generate generics in class files and reference generics
- Add substitution factory to load property files with substitution parameters
- Support XMl and legacy property file formats natively

1.7.5-espn-legacy-branch to 3.9.0
===============================
- Changed version to match that of the entire TeaTrove project, from here on the project
  will be versioned together.
- Changed packaging from com.go to org.teatrove
- Changed license from The Tea Software License 1.1 to Apache License 2.0
- Changed copyright from Walt Disney Internet Group to TeaTrove.org


1.7.5 to 1.7.5-espn-legacy-branch
===============================
- changed pom to reflect branch, small improvements.
- project structure standardized.
- this version is intented to be the basis for a maintanence and forking.


1.7.4 to 1.7.5
===============================
- TRV-17.  Parameterize TTL cleanup thread interval with an option to disable.
  New overloaded constructors added so that the cleaner thread interval can
  be specified.  A non-positive value will disable this feature (defaults to
  10 seconds).
- TRV-18.  Add auto tune capability to LRUCache.  Enabled if cache size is set
  to -2 via the Depot constructors, or any non-positive value if using the 
  LRUCache class directly.   See the JIRA issue for additional info.
- TRV-19.  Depot get() methods return null on a factory timeout rather than 
  the item in the invalid cache.


1.7.3 to 1.7.4
===============================
- TRV-15.  Memory sizing estimates differ from JVM reported data.
  Sizing estimates reflect an additional 8 bytes of overhead per
  object (one level deep).
- TRV-16.  Cleanup thread should be started only when a PerishablesFactory
  is in use.  ESPN can have up to 1200 Depot instances in one app.


1.7.2 to 1.7.3
===============================
- TRV-13.  Logic was added to clear TTL expirations in the expirations map. 
  Items will now expire shortly after the TTL is reached when the next iteration 
  of the cleaner runs. 


1.7.1 to 1.7.2
===============================
- TRV-3.  Added the ability to track memory consumed by a Depot.  The
  new method calculateMemorySize() is useful for creating administrative
  tools to manage memory.  This is important for LRU tuning. 
- TRV-4.  Provide tracking of timed out retrievals on the value wrapper 
  (ValueWrapper class returned by getWrappedValue() methods).
- TRV-5.  Bug fix (introduced in Trove 1.7.1).  The entrySet() and 
  values() methods where returning a wrapper and not the underlying
  values.
- TRV-6.  Added a FilteredMap decorator class to the util package.  This
  supports adding arbitrary filter implementations to the entrySet(), 
  values(), and keySet() methods for any Map passed to its constructor.
- TRV-7.  Added a SpringFactory adapter to the JCache package.  This will
  simplify Spring support for those interested.
- TRV-8.  Added a set of adapter classes to implement the JCache API's 
  (javax.cache package).  For further info see:
  https://jsr-107-interest.dev.java.net/javadoc/javax/cache/package-summary.html
- TRV-9.  Comparator was wrong resulting in MRU rolloff instead of LRU.
- TRV-10.  Items stored in PriorityQueue must have immutable comparators.


1.7.0 to 1.7.1
===============================
- TRV-1.  Fixed.  Depot throws CastClassException sporadically.
- TRV-2.  Resolved outstanding concurrency issues in LRUCache.


1.6.2 to 1.7.0
===============================
NOTE*  As of this version, Trove is dependent upon JDK 1.6!
- DPTS-13.  Relax synchronization in Depot class.  Synchronized wrappers are optional
  through the use of overloaded constructors.
- DPTS-15.  LRUCache re-written to be thread safe.


1.6.1 to 1.6.2
===============================
- The use of soft references was restored to the Cache class.  A new LRUCache class
  was created.  With LRUCache, when items are expired from the UsageMap (LRU rolloff), 
  they are eligible for garbage collection.
- New overloaded constructors were added to the Depot class so that the LRUCache
  implementation could be chosen instead of the default Cache class.
- Made a minor change to the Depot.ValueWrapper class.  The wrapped value can be
  rerieved via getValue() instead of get().  This allows ValueWrapper to be rendered
  in a Tea template.


1.6.0 to 1.6.1
===============================
- The use of soft references was removed from the Cache class.  When items are 
  expired from the UsageMap (LRU rolloff), they are eligible for garbage collection.


1.5.4 to 1.6.0
===============================
- MergedClass was enhanced to support profiling of Tea templates.  Specialized constructors
  were added to implement this functionality to preserve backward compatibility.  This
  release is required for Tea 3.4.0.
- new methods added to the Depot class to add cache statistics instrumentation.  This 
  includes Depot level aggregate values as well as "per access" data items that could be
  used by a consuming application to produce fine grained stats.


1.5.3 to 1.5.4
===============================
- logger Log class was updated so that monitor locks are more "fine grained" by synchronizing
  on the listener collection rather than the log instance monitor.  This provides 
  better performance especially on JDK 1.5.


1.5.1 to 1.5.3
===============================
***** NOTE *****
Due to problems with the current release scripts and integration with Perforce. 
Release 1.5.2 was skipped (and latter versions 1.5.4, 1.5.5 were created).  
This means that all changes listed below have been rolled into 1.5.3.  This
should be used as the lastest "tip" release.  Do not use later versions.  The
next Trove release will most likely be 1.6.0.

- Added returnInvalidNoWait flag to Depot constructors so that the Depot will
  immediately return data from the invalid cache (if there) while retrieval
  happens normally.
- Resolved JDK 1.5 compatibility issue with MergedClass
- Resolved unchecked exception handling bug in ClassInjector
- Increased max HTTP header length from 4K to 10K

1.5.0 to 1.5.1
===============================
- Added PropertyMap architecture to support variable substitution in property files


1.4.9 to 1.5.0
===============================
- Made minor changes to the Log classes to make them accessible to the TeaLog.

1.4.8 to 1.4.9
===============================
- Made TransactionQueueData constructor public so it can be used by the
  TQTeaServlet.
- Added validContainsKey method to Depot to determine whether a key is
  in the valid cache. This can be used to determine whether an object is in
  cache without having the depot go back to the DepotFactory to get it.
- Fixed bug (bugzilla: 1660) where servletMap not ending in an asterisk (/*.xml) returns http 301

1.4.7 to 1.4.8
===============================
- HttpClient does not retry requests if the first timed out while reading the
  response.

1.4.6 to 1.4.7
===============================
- Added SoftHashSet and WeakHashSet classes.
- Added methods in MergedClass for obtaining ClassFile directly.
- Removed deprecated API.
- Added PluginBasicConfig base class.

1.4.5 to 1.4.6
===============================
- Added getTQStatistics() to the Depot.

1.4.4 to 1.4.5
===============================
- Added getDefaultFactory() to the Depot.
- Builds using JDK1.4.
- DataIO primitive read methods call readFully internally now.
- IntegerFactory performance enhancements.
- PersistentCollectionKit falls back to RandomAccessFile if the native
  libraries for SystemFileBuffer weren't found.
- HttpClient supports 100-continue responses.

1.4.3 to 1.4.4
===============================
- UNC paths supported by SystemFileBuffer.
- Added decimal formatting classes to the util package: DecimalFormat and
  DecimalConvertor.
- Added StringReplacer the util package.
- Fixed bug in BeanPropertyAccessor where boolean return types would not 
  be properly converted to Boolean types, resulting in VerifyErrors.
- Added IntegerFactory class.
- Performance enhancements to Depot.
- Performance enhancements to InternedCharToByteBuffer.

1.4.2 to 1.4.3
===============================
- Changed HttpClient to retry once after catching an IOException when writing
  the request as well as reading the response line.
- Changed HttpClient so it doesn't throw a SocketException if available() is
  called after all data has been read from the InputStream.

1.4.1 to 1.4.2
===============================
- Fixed Depot defect: calling remove wouldn't immediately remove the value.
- Fixed BasicObjectRepository defect: calling replaceObject would sometimes
  not release a lock.

1.4.0 to 1.4.1
===============================
- Added a DistributedFactory to util so Depots can check with their neighbors 
  for valid objects before attempting to create their own copy.

1.3.x to 1.4.0
===============================
- Moved PropertyMapFactory into util.
- Net classes modified to be more compatible with jdk1.4.
- DistributedSocketFactory now supports random socket factory selection.
- Added BeanPropertyAccessor.
- Renamed TroveZip.dll to com_go_trove_util_Deflater.dll.
- Fixed CodeBuilder stack adjust for double[] and long[] return types.
- Fixed InstructionList for multiple double[] and long[] parameter types.
- Major changes made to classfile package. TypeDescriptor and MethodDescriptor
  classes replaced with TypeDesc and MethodDesc. The new classes are much
  better and are no longer dependent on class literals for primitives. Multi-
  dimensional array creation is simplified with the addition of an other
  newObject method to CodeAssembler. CodeAssembler now requires TypeDesc
  instances in place of Class instances.
- Bugs fixed in Depot relating to bad inter-thread communication. The Depot
  would sometimes return null values when it shouldn't.
- MergedClass generates different hashcode in name now. Although the value is
  different from previous releases, it is no longer dependent on the
  implementation of HashSet. When switching JVMs, the value is now consistent.

1.3.0 to 1.3.1
===============================
- Fixed a bug where PropertyMap.subMapKeySet() would only return submap names
  that contained more submap items.
- Fixed a couple minor bugs in classfile package. The stack adjust was
  incorrect in CodeBuilder for long shifts, and native methods couldn't be
  built.
- Depot cache size may now be specified as zero. This causes it to use just a
  SoftHashMap for caching.
- ThreadPool defect fix that caused the available count to slowly grow.
- Added accessors to Pair class.

1.2.4 to 1.3.0
===============================
- Added findResource methods to the DelegateClassLoader and ClassInjector.
- Added FastCharToByteBuffer. Converts to iso-8859-1 faster.
- Improved performance of HttpHeaderMap date formatting.
- Added FastDateFormat class, which supports the same patterns as
  SimpleDateFormat.
- Added Perishable interface to Depot.
- Added Deflater classes, similar to the ones in java.util.zip, except more
  support is provided for flushing data. Native C++ code is required for this,
  and the library is named TroveZip.
- Added WrappedCache class.

1.2.3 to 1.2.4
===============================
- Fixed thread safety defect in Cache class when MRU is shared.
- Fixed parsing defect in HttpHeaderMap of Set-Cookie header

1.2.2 to 1.2.3
===============================
- Added drain method CharToByteBuffer.
- Depot service method restores thread name when done. If the TQ doesn't
  recycle the thread, the thread name would keep on growing.
- Added SortedArrayList class.

1.2.1 to 1.2.2
===============================
- Depot supports automatic time based invalidation.
- Added removeAll method to Depot with a filter.
- HttpClient now closes PostData InputStream after doing POST.
- Introduced new set of classes for loading and configuring plugins.

1.2.0 to 1.2.1
===============================
- Fixed MergedClass defect that prevented merged classes and their class
  loaders from being unloaded.
- Fixed minor defect in TransactionQueue when the max thread limit is reached.
  Queued transactions wouldn't get serviced until either new ones are enqueued
  or until all serving transactions finished.
- Added filtered invalidateAll method in Depot.

1.1.x to 1.2.0
===============================
- Added net package, providing socket pooling and a HTTP client that works
  with it. Some classes in this package require Java 2, version 1.3.
- Added io package, moving those classes over from TeaServlet and Tea.
- MergedClass has an additional constructor for supporting lazy instantiation
  of merged classes.

1.1.0 to 1.1.1
===============================
- Fixed defect in BeanComparator that could cause stack overflow error because
  some internal Comparators were being shared when they shouldn't be.
- Finished off ClassFile reading functionality. Inner classes are now read, and
  custom attributes can be defined.

1.0.x to 1.1.0
===============================
- Introduced Depot class, which adds another layer of support for caching.
- Introduced MultiKey class, which makes it easier to create compound keys.
- Added tq package which contains support for the TransactionQueue scheduler.
- Added workaround in MergedClass to prevent interface static initializers
  from being wrapped. This bug is fixed in JDK1.3.

1.0.2 to 1.0.3
===============================
- Log event dispatch routine no longer locks out other threads from dispatching
  events at the same time. This can improve performance in database logging.
- UsageMap and UsageSet have newer, faster implementations.
- Cache can now piggyback onto another Cache so that the MRU may be shared.
- SoftHashMap and Cache support null values.

1.0.1 to 1.0.2
===============================
- New simplified implementation of PropertyMap fixes a few defects. Ordering of
  keys is now more consistent and views operate correctly.
- CodeBuilder in the ClassFile API performs correct flow analysis of exception
  handlers, variables, and jsr instructions.
- Fixed minor thread safety issue in ClassInjector.

1.0.0 to 1.0.1
===============================
- Fixed bug in Cache class. It wasn't ensuring that the newest values were
  those being guarded by the MRU.

1.0.0
===============================
- The first released version of Trove, consisting of classes moved from other
  packages.
