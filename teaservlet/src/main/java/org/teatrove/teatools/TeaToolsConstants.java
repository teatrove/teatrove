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

package org.teatrove.teatools;

/**
 * Provides a set of useful Tea constants.
 *
 * @author Mark Masse
 */
public interface TeaToolsConstants {


    /** An empty array of Class objects.  Used for the reflection-based method 
        invocation */
    public final static Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

    /** An empty array of Objects.  Used for the reflection-based method 
        invocation */
    public final static Object[] EMPTY_OBJECT_ARRAY = new Object[0];


    /** The begin code tag */
    public final static String BEGIN_CODE_TAG = "<%";
    
    /** The end code tag */
    public final static String END_CODE_TAG = "%>";


    /** Implicitly imported Tea packages */
    public final static String[] IMPLICIT_TEA_IMPORTS =
        new String[] { "java.lang", "java.util" };

    /** The "default" runtime context class */
    public final static Class<?> DEFAULT_CONTEXT_CLASS = 
        org.teatrove.tea.runtime.Context.class;

    /** The file extension for Tea files */
    public final static String TEA_FILE_EXTENSION = ".tea"; 


}
