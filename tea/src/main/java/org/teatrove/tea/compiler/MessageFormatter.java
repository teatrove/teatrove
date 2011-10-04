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

package org.teatrove.tea.compiler;

import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.text.MessageFormat;

/**
 * 
 * @author Brian S O'Neill
 */
class MessageFormatter {
    // Maps Classes to MessageFormatters.
    private static Map<Class<?>, MessageFormatter> cMessageFormatters;

    static {
        try {
            cMessageFormatters = new WeakHashMap<Class<?>, MessageFormatter>(7);
        }
        catch (LinkageError e) {
            cMessageFormatters = new HashMap<Class<?>, MessageFormatter>(7);
        }
        catch (Exception e) {
            // Microsoft VM sometimes throws an undeclared
            // ClassNotFoundException instead of doing the right thing and
            // throwing some form of a LinkageError if the class couldn't
            // be found.
            cMessageFormatters = new HashMap<Class<?>, MessageFormatter>(7);
        }

    }

    public static MessageFormatter lookup(Object user)
        throws MissingResourceException
    {
        return lookup(user.getClass());
    }

    private static MessageFormatter lookup(Class<?> clazz) {
        MessageFormatter formatter = cMessageFormatters.get(clazz);
        if (formatter == null) {
            String className = clazz.getName();
            String resourcesName;
            int index = className.lastIndexOf('.');
            if (index >= 0) {
                resourcesName = className.substring(0, index + 1) +
                    "resources." + className.substring(index + 1);
            }
            else {
                resourcesName = "resources." + className;
            }
            try {
                formatter = new MessageFormatter
                    (ResourceBundle.getBundle(resourcesName));
            }
            catch (MissingResourceException e) {
                if (clazz.getSuperclass() == null) {
                    throw e;
                }
                try {
                    formatter = lookup(clazz.getSuperclass());
                }
                catch (MissingResourceException e2) {
                    throw e;
                }
            }
            cMessageFormatters.put(clazz, formatter);
        }
        return formatter;
    }

    private ResourceBundle mResources;

    private MessageFormatter(ResourceBundle resources) {
        mResources = resources;
    }

    public String format(String key) {
        String message = null;
        try {
            message = mResources.getString(key);
        }
        catch (MissingResourceException e) {
        }

        if (message != null) {
            return message;
        }
        else {
            return key;
        }
    }

    public String format(String key, String arg) {
        String message = null;
        try {
            message = mResources.getString(key);
        }
        catch (MissingResourceException e) {
        }

        if (message != null) {
            return MessageFormat.format(message, new String[] {arg});
        }
        else {
            return key + ": " + arg;
        }
    }

    public String format(String key, String arg1, String arg2) {
        String message = null;
        try {
            message = mResources.getString(key);
        }
        catch (MissingResourceException e) {
        }

        if (message != null) {
            return MessageFormat.format(message, arg1, arg2);
        }
        else {
            return key + ": " + arg1 + ", " + arg2;
        }
    }

    public String format(String key, String arg1, String arg2, String arg3) {
        String message = null;
        try {
            message = mResources.getString(key);
        }
        catch (MissingResourceException e) {
        }

        if (message != null) {
            return MessageFormat.format(message, arg1, arg2, arg3);
        }
        else {
            return key + ": " + arg1 + ", " + arg2 + ", " + arg3;
        }
    }
}
