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

import java.beans.IntrospectionException;
import java.lang.reflect.Method;

/**
 * This class finds methods that best fit a given description. The compiler
 * will then bind to one of those methods.
 *
 * @author Brian S O'Neill
 */
public class MethodMatcher {
    /**
     * The best result candidates are stored in the Method array passed in.
     * The int returned indicates the number of candidates in the array. Zero
     * is returned if there is no possible match.
     */
    public static int match(Method[] methods, String name, Type... params) {
        int paramCount = params.length;
        int matchCount = methods.length;
        Method m;

        int[] costs = new int[matchCount];

        // Filter the available methods down to a smaller set, tossing
        // out candidates that could not possibly match because the name
        // differs, and the number of parameters differ. Also eliminate
        // ones in which the parameter types are not compatible at all
        // because no known conversion could be applied.

        int lowestTotalCost = Integer.MAX_VALUE;
        int length = matchCount;
        matchCount = 0;
        for (int i=0; i < length; i++) {
            m = methods[i];
            if (name == null || m.getName().equals(name)) {
                Class<?>[] methodParams = m.getParameterTypes();
                
                int cnt = methodParams.length;
                if ((cnt == paramCount) || 
                    (m.isVarArgs() && paramCount >= cnt - 1)) {
                    
                    int j = 0;
                    int total = 0;
                    for (; j < paramCount; j++) {
                        Type methodParam = getMethodParam(m, j, params[j]);
                        int cost = methodParam.convertableFrom(params[j]);
                        
                        if (cost < 0) { break; }
                        else { total += cost; }
                    }

                    if (j == paramCount) {
                        // discount non-use of var args
                        if (m.isVarArgs() && j < methodParams.length) {
                            total++;
                        }
                        
                        costs[matchCount] = total;
                        methods[matchCount++] = m;
                        if (total < lowestTotalCost) {
                            lowestTotalCost = total;
                        }
                    }
                }
            }
        }

        if (matchCount <= 1) {
            return matchCount;
        }

        // Filter out those that have a cost higher than lowestTotalCost.
        length = matchCount;
        matchCount = 0;
        for (int i=0; i < length; i++) {
            if (costs[i] <= lowestTotalCost) {
                costs[matchCount] = costs[i];
                methods[matchCount++] = methods[i];
            }
        }

        if (matchCount <= 1) {
            return matchCount;
        }

        // Filter further by matching parameters with the shortest distance
        // in the hierarchy. This matches each parameter to each matching method
        // to either find the best possible match where the method parameter is
        // the shortest. If multiple matches exist, the parameter is considered
        // ambiguous. If all parameters are ambiguous, then this will return -1
        // as a failiure. If multiple methods match for different parmaters,
        // then the result is also considered ambiguous and -1 returned. For
        // example, the methods:
        //     void doX(String a, Object b)
        //     void doX(Object a, String b)
        // with the method: 
        //     doX("string1", "string2")
        // will best match the first method as String is closer than object but
        // will best match the second method as again String is closer. Thus,
        // the result is ambiguous.

        // maintain the best matching method per parameter
        Method[] paramResults = new Method[paramCount];
        
        // search each parameter separately to find best matching method
        for (int j=0; j<paramCount; j++) {
            Class<?> lastMatch = null;
            Method bestFit = null;
            
            length = matchCount;
            int bestFits = 0;
            
            // search each matching method for the best match. If we have not
            // yet marked a best fit, then we mark the first method. Otherwise,
            // if the next method is closer (ie: the last match is assignable)
            // then we update the best fit. If the last match ever matches
            // the current match, then it is considered ambiguous and the best
            // fit is reset.
            
            for (int i=0; i < length; i++) {
                m = methods[i];
                
                Type methodType = getMethodParam(m, j, params[j]);
                Class<?> methodParam = methodType.getNaturalClass();
                
                // TODO: match against generics?
                //java.lang.reflect.Type methodType = methodType.getGeneric();

                Class<?> param = params[j].getNaturalClass();
                if (methodParam.isAssignableFrom(param)) {
                	
                	// check for ambiguity by determining if multiple methods
                	// have same type
                	
                    if (lastMatch != null) {
                        if (lastMatch.equals(methodParam)) {
                            bestFits++;
                        }
                        else { bestFits = 0; }
                    }

                    // if first time through or last match is assignable (ie:
                    // farther away), then update best fit
                    
                    if (lastMatch == null ||
                        lastMatch.isAssignableFrom(methodParam)) {
                        
                        bestFit = m;
                        lastMatch = methodParam;
                    }
                }
            }

            // set best fitting method if single match...otherwise, leave null
            // to denote ambiguous result
            
            if (bestFits == 0) {
            	paramResults[j] = bestFit;
            }
        }
        
        // check if multiple matching methods (ambiguous) or if all params are
        // ambiguous
        
        Method last = null;
        for (int i = 0; i < paramCount; i++) {
        	// update last match if not yet determined
        	if (last == null) {
        		last = paramResults[i];
        	}
        	
    		// check if non-match (ie: multiple matching methods), and return
        	// ambiguous (-1)
        	else if (!last.equals(paramResults[i])) {
        		return -1;
        	}
        }
        
        // check if all ambiguous..no matches
        if (last == null) {
        	return -1;
        }
        
        // ensure first result is set to match
        methods[0] = last;
        
        // return single result
        return 1;
    }

    public static Type getMethodParam(Method method, int index, Type type) {
        Type result = null;
        Class<?>[] methodParams = method.getParameterTypes();
        java.lang.reflect.Type[] methodTypes = method.getGenericParameterTypes();
        
        if (method.isVarArgs() && index >= methodParams.length - 1) {
            int idx = methodParams.length - 1;
            Type varArg = new Type(methodParams[idx], methodTypes[idx]);
            if (index == methodParams.length - 1) {
                if (varArg.convertableFrom(type) >= 0) {
                    result = varArg;
                }
            }
            
            if (result == null) {
                try { result = varArg.getArrayElementType(); }
                catch (IntrospectionException ie) {
                    throw new RuntimeException(ie);
                }
            }
            else {
                
            }
        }
        else if (index < methodParams.length) {
            result = new Type(methodParams[index], methodTypes[index]);
        }
        
        return result;
    }
    
    /**
     * Test program.
     */
    /*
    public static void main(String[] arg) throws Exception {
        new Tester().test();
    }

    private static class Tester {
        public Tester() {
        }

        public void test() {
            Type t1 = new Type(boolean.class);
            Type t2 = new Type(int.class);
            Type t3 = new Type(float.class);
            Type t4 = new Type(double.class);

            Type t5 = new Type(Boolean.class);
            Type t6 = new Type(Integer.class);
            Type t7 = new Type(Float.class);
            Type t8 = new Type(Double.class);

            test("test", new Type[] {});

            test("test", new Type[] {t1});
            test("test", new Type[] {t2});
            test("test", new Type[] {t3});
            test("test", new Type[] {t4});
            test("test", new Type[] {t5});
            test("test", new Type[] {t6});
            test("test", new Type[] {t7});
            test("test", new Type[] {t8});

            test("test2", new Type[] {t1});
            test("test2", new Type[] {t2});
            test("test2", new Type[] {t3});
            test("test2", new Type[] {t4});
            test("test2", new Type[] {t5});
            test("test2", new Type[] {t6});
            test("test2", new Type[] {t7});
            test("test2", new Type[] {t8});

            test("test3", new Type[] {t1});
            test("test3", new Type[] {t2});
            test("test3", new Type[] {t3});
            test("test3", new Type[] {t4});
            test("test3", new Type[] {t5});
            test("test3", new Type[] {t6});
            test("test3", new Type[] {t7});
            test("test3", new Type[] {t8});

            test("test4", new Type[] {t1});
            test("test4", new Type[] {t2});
            test("test4", new Type[] {t3});
            test("test4", new Type[] {t4});
            test("test4", new Type[] {t5});
            test("test4", new Type[] {t6});
            test("test4", new Type[] {t7});
            test("test4", new Type[] {t8});

            test("test5", new Type[] {t1});
            test("test5", new Type[] {t2});
            test("test5", new Type[] {t3});
            test("test5", new Type[] {t4});
            test("test5", new Type[] {t5});
            test("test5", new Type[] {t6});
            test("test5", new Type[] {t7});
            test("test5", new Type[] {t8});

            test("test6", new Type[] {t1});
            test("test6", new Type[] {t2});
            test("test6", new Type[] {t3});
            test("test6", new Type[] {t4});
            test("test6", new Type[] {t5});
            test("test6", new Type[] {t6});
            test("test6", new Type[] {t7});
            test("test6", new Type[] {t8});

            test("test7", new Type[] {t2, t6});
            test("test7", new Type[] {t6, t2});
            test("test7", new Type[] {t2, t2});
            test("test7", new Type[] {t6, t6});

            // Should only produce the method that accepts B
            test("test8", new Type[] {new Type(C.class)});
        }

        private void test(String name, Type[] params) {
            Method[] methods = this.getClass().getMethods();
            int count = MethodMatcher.match(methods, name, params);
            dump(methods, count);
        }

        private void dump(Method[] methods, int count) {
            for (int i=0; i<count; i++) {
                System.out.println(methods[i]);
            }
            System.out.println();
        }

        public void test(boolean i) {}
        public void test(char i) {}
        public void test(byte i) {}
        public void test(short i) {}
        public void test(int i) {}
        public void test(float i) {}
        public void test(long i) {}
        public void test(double i) {}
        public void test(Boolean i) {}
        public void test(Character i) {}
        public void test(Byte i) {}
        public void test(Short i) {}
        public void test(Integer i) {}
        public void test(Float i) {}
        public void test(Long i) {}
        public void test(Double i) {}
        public void test(Number i) {}
        public void test(Object i) {}
        public void test(String i) {}

        public void test2(boolean i) {}
        public void test2(char i) {}
        public void test2(byte i) {}
        public void test2(short i) {}
        public void test2(int i) {}
        public void test2(float i) {}
        public void test2(long i) {}
        public void test2(double i) {}

        public void test3(Boolean i) {}
        public void test3(Character i) {}
        public void test3(Byte i) {}
        public void test3(Short i) {}
        public void test3(Integer i) {}
        public void test3(Float i) {}
        public void test3(Long i) {}
        public void test3(Double i) {}
        public void test3(Number i) {}

        public void test4(Object i) {}
        public void test4(String i) {}

        public void test5(int i) {}
        public void test5(String i) {}

        public void test6(Number i) {}
        public void test6(Integer i) {}
        public void test6(String i) {}

        public void test7(int i, Integer I) {}
        public void test7(Integer I, int i) {}

        private class A {}
        private class B extends A {}
        private class C extends B {}

        public void test8(A a) {}
        public void test8(B b) {}
    }
    */
}
