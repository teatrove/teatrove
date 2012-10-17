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
    public static final int AMBIGUOUS = -1;
    
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
            if (m.isBridge()) { continue; }
            
            if (name == null || m.getName().equals(name)) {
                Class<?>[] methodParams = m.getParameterTypes();
                
                int cnt = methodParams.length;
                if ((cnt == paramCount) || 
                    (m.isVarArgs() && paramCount >= cnt - 1)) {
                    
                    int j = 0;
                    int total = 0;
                    for (; j < paramCount; j++) {
                        MethodParam result = getMethodParameter(m, j, params[j]);
                        Type methodParam = result.type;
                        int cost = methodParam.convertableFrom(params[j], result.vararg);
                        if (cost < 0) { break; }
                        
                        // update total cost
                        total += cost;
                    }

                    if (j == paramCount) {
                        // increase cost for non-use of var args
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

        // If one match is non-varargs and remaining are varargs, always choose
        // the non-varargs.
        Method last = null;
        for (int i = 0; i < matchCount; i++) {
            m = methods[i];
            if (!m.isVarArgs()) {
                if (last == null) {
                    last = m;
                }
                else {
                    // multiple non-vararg methods found, so still ambiguous
                    last = null;
                    break;
                }
            }
        }
        
        if (last != null) {
            // ensure first result is set to match
            methods[0] = last;
            
            // return single result
            return 1;
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
            
            for (int i = 0; i < length; i++) {
                m = methods[i];
                if (m.isBridge()) { continue; }
                
                MethodParam result = getMethodParameter(m, j, params[j]);
                Type methodType = result.type;
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
        
        for (int i = 0; i < paramCount; i++) {
        	// update last match if not yet determined
        	if (last == null) {
        		last = paramResults[i];
        	}
        	
    		// check if non-match (ie: multiple matching methods), and return
        	// ambiguous (-1)
        	else if (!last.equals(paramResults[i])) {
        		return AMBIGUOUS;
        	}
        }
        
        // check if all ambiguous..no matches
        if (last == null) {
        	return AMBIGUOUS;
        }
        
        // ensure first result is set to match
        methods[0] = last;
        
        // return single result
        return 1;
    }

    public static Type getMethodParam(Method method, int index, Type type) {
        MethodParam param = getMethodParameter(method, index, type);
        return (param == null ? null : param.type);
    }
    
    private static MethodParam getMethodParameter(Method method, int index, Type type) {
        Type result = null;
        boolean vararg = false;
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
                try { 
                    result = varArg.getArrayElementType(); 
                    vararg = true;
                }
                catch (IntrospectionException ie) {
                    throw new RuntimeException(ie);
                }
            }
        }
        else if (index < methodParams.length) {
            result = new Type(methodParams[index], methodTypes[index]);
        }
        
        return new MethodParam(result, vararg);
    }
    
    private static class MethodParam {
        private Type type;
        private boolean vararg;
        
        private MethodParam(Type type, boolean vararg) {
            this.type = type;
            this.vararg = vararg;
        }
    }
}
