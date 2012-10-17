README for TeaApps/4.1.x

Copyright (C) 1997-2011 TeaTrove.org
 
TeaApps contains the default and commonly used applications and contexts 
available to the TeaServlet.

Change History

4.1.2 to 4.2.0
===============================
- Fix #185 fixing stack overflow from sortList
- Removing deprecation for now
- Add createDateWithValidation to DateContext
- Fix #169 adding mergeArrays to ArrayContext
- Fix #142 adding ability to save/load JMX settings
- Fix #134 adding createLinkedHashMap to MapContext
- Fix #110 moving UtilityContext into StringContext, NumberFormatContext, DateContext, etc
- Add join methods to ArrayContext and ListContext
- Fix #77 to add HTMLContext for HTML-based pagination methods 
- Fix #76 for SortContext to add backwards compatibility methods
- Cleanup SortContext for consistency and performance
- Fix #68 to cleanup contexts and applications and add javadocs
- CookieContext: Add ability to create cookies and update
- CryptoContext: Add general digest method with any algorithm
- FileSystemContext: Add ability to get File instance for given path
- FileSystemContext: Add ability to append contents when writing files
- MathContext: Add min(array) and max(array) methods 

4.0.0 to 4.1.2
===============================
- Fix #29 adding dot notation to bean accessor
- Fix #27 removing static keywords from various contexts

4.0.0
===============================
- First release of submodule under TeaTrove
