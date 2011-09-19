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

package org.teatrove.barista.validate;

import java.util.*;

/**
 * @author Sean T. Treat
 */
public class StringStack extends Stack {

    private static String cDelimiter = ".";

    public static void main(String[] args) {
        StringStack ss = new StringStack();

        ss.push("one");
        ss.push("two");
        ss.push("three");
        System.out.println(ss.current());
        ss.pop();
        ss.push("five");
        System.out.println(ss.current());
    }

    public StringStack(String initStr, String delimiter) {
        super();
        if (initStr != null && initStr.length() > 0) {
            push(initStr);
        }
        cDelimiter = delimiter;
        StringTokenizer tokenizer = new StringTokenizer(initStr, cDelimiter);
    }

    public StringStack(String initStr) {
        this(initStr, cDelimiter);
    }

    public StringStack() {
        this("", cDelimiter);
    }
    
    public void setDelimiter(String delimiter) {
        cDelimiter = delimiter;
    }

    public String getDelimiter() {
        return cDelimiter;
    }

    public Object push(Object obj) {
        try {
            return super.push(obj);
        }
        catch (Exception e) {
            System.out.println("push(" + (String)obj + ")");
            e.printStackTrace();            
        }
        return obj;
    }

    public Object pop() {
        try {
            return super.pop();
        }
        catch (Exception e) {
            System.out.println("pop()");
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Returns a delimited String containing all the elements in the stack
     */
    public String current() {
        ListIterator i = listIterator();
        StringBuffer buf = new StringBuffer(1024);
        while (i.hasNext()) {
            buf.append((String)i.next());
            if (i.hasNext()) {
                buf.append(cDelimiter);
            }
        }
        return new String(buf);
    }
}
