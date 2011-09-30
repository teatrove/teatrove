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

import java.lang.reflect.Method;

/**
 * This class finds methods that best fit a given description. The compiler
 * will then bind to one of those methods.
 *
 * @author Brian S O'Neill
 * @version

 */
public class MethodMatcher {
    /**
     * The best result candidates are stored in the Method array passed in.
     * The int returned indicates the number of candidates in the array. Zero
     * is returned if there is no possible match.
     */
    public static int match(Method[] methods, String name, Type[] params) {
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
                Class[] methodParams = m.getParameterTypes();
                if (methodParams.length == paramCount) {

                    int total = 0;
                    int j;
                    for (j=0; j<paramCount; j++) {
                        int cost = new Type(methodParams[j])
                            .convertableFrom(params[j]);
                        if (cost < 0) {
                            break;
                        }
                        else {
                            total += cost;
                        }
                    }

                    if (j == paramCount) {
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
        // in the hierarchy.

        for (int j=0; j<paramCount; j++) {
            Class lastMatch = null;
            Method bestFit = null;

            length = matchCount;
            matchCount = 0;
            for (int i=0; i < length; i++) {
                m = methods[i];
                if (bestFit == null) {
                    bestFit = m;
                }
                Class methodParam = m.getParameterTypes()[j];
                Class param = params[j].getNaturalClass();
                if (methodParam.isAssignableFrom(param)) {
                    if (lastMatch == null ||
                        lastMatch.isAssignableFrom(methodParam)) {
                        
                        bestFit = m;
                        lastMatch = methodParam;
                    }
                }
            }

            methods[matchCount++] = bestFit;
        }

        return matchCount;
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
