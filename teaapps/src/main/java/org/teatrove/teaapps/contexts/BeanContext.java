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
package org.teatrove.teaapps.contexts;

import org.teatrove.trove.classfile.TypeDesc;
import org.teatrove.trove.util.BeanPropertyAccessor;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.util.HashMap;
import java.util.Map;

/**
 * The BeanContext is a Tea context that allows dynamic lookup and setting of
 * Java beans.  Tea does not support direct dynamic lookup of beans in order to
 * be highly performant.  The BeanContext provides this behavior by utilizing a
 * specially built dynamic class that performs fast and efficient property
 * lookups to decrease the performance impact.
 * <br /><br />
 * The context works by returning a special object that wraps a given bean and
 * uses a special Tea notation to allow map-based lookups.  The lookups may be
 * either simple bean properties (ie: name, location, etc) or may be composite
 * properties in dot-notation (ie: venue.city.name).  By default, if any portion
 * of the composite lookup is <code>null</code>, then <code>null</code> is
 * returned.  You can alter this behavior by passing in <code>true</code> for
 * the <code>failOnNulls</code> parameter.  In this case, if any portion of the
 * composite graph is <code>null</code>, then a {@link BeanContextException}
 * is thrown.
 * <br /><br />
 * Example:
 * <pre>
 *     bean = getBeanMap(myObject)
 *     'Name: ' bean['name']
 * </pre>
 *  
 * @author Scott Jappinen
 */
public class BeanContext {
    
    private Map<String,MethodTypePair> mMethodTypePairMap =
        new HashMap<String,MethodTypePair>();

    /**
     * Get an object wrapping the given bean that allows dynamic property
     * lookups.
     * 
     * <code>bean = getBeanMap(bean); bean['name']</code>
     * 
     * @param bean The bean to wrap and return
     * 
     * @return The object allowing dynamic lookups on the bean
     */
    public BeanMap getBeanMap(Object bean) {
        return getBeanMap(bean, false);
    }
    
    /**
     * Get an object wrapping the given bean that allows dynamic property
     * lookups.  The <code>failOnNulls</code> parameter may be used to cause
     * an exception to be thrown when portions of the composite graph are
     * <code>null</code>.
     * 
     * <code>bean = getBeanMap(bean); bean['name']</code>
     * 
     * @param bean The bean to wrap and return
     * @param failOnNulls The state of whether to throw exceptions on null graph
     * 
     * @return The object allowing dynamic lookups on the bean
     * 
     * @throws BeanContextException If any portion of the composite graph is
     *         <code>null</code> such as city being null in 'team.city.name'. 
     *         This is only thrown if failOnNulls is <code>true</code>
     */
    public BeanMap getBeanMap(Object bean, boolean failOnNulls) 
        throws BeanContextException {
        
        BeanPropertyAccessor accessor = 
            BeanPropertyAccessor.forClass(bean.getClass());
        return new BeanMap(bean, accessor, failOnNulls);
    }

    /**
     * Method to set attributes of an object.  This method may be 
     * called to set the values of an object by passing in the object, 
     * the attribute name to set, and the attribute value.  To get a 
     * listing of attribute values that can be set for a given object, 
     * see the createObject method for the object.
     * 
     * @param object The object to set an attribute value for.
     * @param attributeName The name of the attribute to set.
     * @param attributeValue The value of the attribute to set.
     */
    public void set(Object object, String attributeName, Object attributeValue)
        throws IllegalAccessException, InvocationTargetException, 
               BeanContextException
    {
        Class<?> objectClass = object.getClass();
        String methodName = 
            "set" + attributeName.substring(0,1).toUpperCase() + 
            attributeName.substring(1);
        
        String methodKey = objectClass.getName() + "." + methodName;
        MethodTypePair pair = mMethodTypePairMap.get(methodKey);
        if (pair == null) {
            pair = getMethodTypePair(objectClass, methodName, attributeValue);
            mMethodTypePairMap.put(methodKey, pair);
        }
        
        if (pair != null) {        
            Object[] params = {convertObjectType(attributeValue, pair.type)};
            pair.method.invoke(object, params);            
        } else {
            throw new BeanContextException
            (
                methodName + "(" + 
                (attributeValue == null ? "null" : attributeValue.getClass()) + 
                ") could not be found."
            );
        }
    }

    private static MethodTypePair getMethodTypePair(Class<?> objectClass, 
        String methodName, Object attributeValue) {
        
        MethodTypePair result = null;
        Method method;
        Class<?> valueClass = null;
        if (attributeValue != null) {
            valueClass = attributeValue.getClass();
            try {
                Class<?>[] param = new Class[] {toPrimitiveType(valueClass)};
                method = objectClass.getMethod(methodName, param);
                result = new MethodTypePair(method, valueClass);
            } catch (NoSuchMethodException e) {
                Class<?>[] compatibleClasses = getCompatibleClasses(valueClass);
                if (compatibleClasses != null) {
                    for (int i=0; i < compatibleClasses.length; i++) {
                        try {
                            Class<?>[] param = new Class[] {
                                toPrimitiveType(compatibleClasses[i])
                            };
                            
                            method = objectClass.getMethod(methodName, param);
                            result = new MethodTypePair(method, compatibleClasses[i]);
                            break;
                        } catch (NoSuchMethodException e2) { /* ignore */ }
                    }
                }
            }
        } else {
            Method[] methods = objectClass.getMethods();
            for (int i=0; i < methods.length; i++) {
                String name = methods[i].getName();
                Class<?>[] clazzes = methods[i].getParameterTypes();
                if (name.equals(methodName) && 
                    clazzes.length == 1 &&
                    !clazzes[0].isPrimitive()) {
                    result = new MethodTypePair(methods[i], clazzes[0]);
                    break;
                }
            }
        }
        return result;
    }
    
    private static Class<?> toPrimitiveType(Class<?> clazz) {
        Class<?> result = clazz;
        if (clazz != null) {
            TypeDesc typeDesc = TypeDesc.forClass(clazz);
            typeDesc = typeDesc.toPrimitiveType();
            if (typeDesc != null) {
                result = typeDesc.toClass();
            }
        }
        return result;
    }

    private static Class<?>[] getCompatibleClasses(Class<?> type) {
        Class<?>[] result = null;
        if (type == Integer.class) {
            result = new Class[] {Long.class, Short.class, Byte.class};
        } else if (type == Long.class) {
            result = new Class[] {Integer.class, Short.class, Byte.class};
        } else if (type == Short.class) {
            result = new Class[] {Long.class, Integer.class, Byte.class};
        } else if (type == Byte.class) {
            result = new Class[] {Long.class, Integer.class, Short.class};
        } else if (type == Double.class) {
            result = new Class[] {Float.class};
        } else if (type == Float.class) {
            result = new Class[] {Double.class};
        }
        return result;
    }

    private static Object convertObjectType(Object value, 
        Class<?> typeToCastInto) {
        
        Object result = value;
        if (value != null) {
            Class<?> valueClass = value.getClass();
            if (!(valueClass == typeToCastInto)) {
                Number numberValue = (Number) value;
                if (valueClass == Integer.class) {
                    if (typeToCastInto == Long.class) {
                        result = new Long(numberValue.longValue());
                    } else if (typeToCastInto == Short.class) {
                        result = new Short(numberValue.shortValue());
                    } else if (typeToCastInto == Byte.class) {
                        result = new Byte(numberValue.byteValue());
                    }
                } else if (valueClass == Double.class) {
                    if (typeToCastInto == Float.class) {
                        result = new Float(numberValue.floatValue());
                    }
                }
            }
        }
        return result;
    }

    /**
     * Helper class that maps a method to its type.
     */
    public static class MethodTypePair {
        Method method;
        Class<?> type;

        public MethodTypePair(Method method, Class<?> type) {
            this.method = method;
            this.type = type;
        }
    }

    /**
     * The custom bean map that provides Tea getter support via the
     * {@link BeanMap#get(String)} method. This allows for dynamic lookups of
     * properties from the associated object. The dynamic lookups also support
     * dot-notation to create a path to a given property such as
     * <code>beanMap['address.city.name']</code>.
     */
    public static class BeanMap {
        private Object mBean;
        private boolean mFailOnNulls;
        private BeanPropertyAccessor mAccessor;
        
        public BeanMap(Object bean, BeanPropertyAccessor accessor,
                       boolean failOnNulls) {
            mBean = bean;
            mAccessor = accessor;
            mFailOnNulls = failOnNulls;
        }
        
        /**
         * Get the value of the given property within the associated object.
         * If the property is a dot-notation path, then the object will be
         * traversed for each property. Note that if any portion of the path
         * results in a <code>null</code> value, then a 
         * {@link NullPointerException} will be thrown.
         * 
         * @param property The name of the property or dot-path to the property
         * 
         * @return The value of the associated property or path
         */
        public Object get(String property) {
            // verify
            if (property == null || property.length() == 0) {
                throw new IllegalArgumentException(
                    "BeanAccessor.get: invalid property on bean " +
                    mBean.getClass().getName()
                );
            }

            // TODO: support ?. syntax for null-safe operation
            
            // check if single property and short circuit
            int idx = property.indexOf('.');
            if (idx < 0) {
                return mAccessor.getPropertyValue(mBean, property);
            }

            // otherwise, walk composite property
            int last = 0;
            Object bean = mBean;
            BeanPropertyAccessor accessor = mAccessor;
            do {
                // get next property
                String prop = property.substring(last, idx);
                bean = accessor.getPropertyValue(bean, prop);
                if (bean == null) {
                    if (mFailOnNulls) {
                        throw new IllegalStateException(
                            "BeanAccessor.get: null value for property " + 
                            prop + " on bean " + mBean.getClass().getName()
                        );
                    }

                    return null;
                }

                // update to next accessor
                accessor = BeanPropertyAccessor.forClass(bean.getClass());

                // update to next index
                last = idx + 1;
                idx = property.indexOf('.', last);
            } while (idx >= 0);

            // return last result
            return accessor.getPropertyValue(bean, property.substring(last));
        }
    }
    
    public static class BeanContextException extends RuntimeException {
        
        public static final long serialVersionUID = 1234567890L;
        
        public BeanContextException() {
            super();
        }
        public BeanContextException(String message) {
            super(message);
        }
        public BeanContextException(String message, Throwable cause) {
            super(message, cause);
        }
        public BeanContextException(Throwable cause) {
            super(cause);
        }
    }
}
