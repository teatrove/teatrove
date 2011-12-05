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
 * @author Scott Jappinen
 */
public class BeanContext {
    
    private Map<String,MethodTypePair> mMethodTypePairMap = new HashMap<String,MethodTypePair>();

    public BeanMap getBeanMap(Object bean) {
        BeanPropertyAccessor accessor = BeanPropertyAccessor.forClass(bean.getClass());
        return new BeanMap(bean, accessor);
    }

    /**
     * Method to set attributes of an object.  This method may be 
     * called to set the values of an object by passing in the object, 
     * the attribute name to set, and the attribute value.  To get a 
     * listing of attribute values that can be set for a given object, 
     * see the createObject method for the object.
     * <P>
     * @param object The object to set an attribute value for.
     * @param attributeName The name of the attribute to set.
     * @param attributeValue The value of the attribute to set.
     */
    public void set(Object object, String attributeName, Object attributeValue)
        throws IllegalAccessException, InvocationTargetException, BeanContextException
    {
        Class<?> objectClass = object.getClass();
        String methodName = "set" + attributeName.substring(0,1).toUpperCase() + attributeName.substring(1);        
        String methodKey = objectClass.getName() + "." + methodName;
        MethodTypePair pair = (MethodTypePair) mMethodTypePairMap.get(methodKey);
        if (pair == null) {
            pair = getMethodTypePair(objectClass, methodName, attributeValue);
            mMethodTypePairMap.put(methodKey, pair);
        }
        if (pair != null) {        
            Object[] params = {convertObjectType(attributeValue, pair.type)};
            pair.method.invoke(object, params);            
        } else {
            if (objectClass != null) {
                throw new BeanContextException("A method " + objectClass.getName() + ".");
            }
            if (methodName != null && attributeValue != null) {
                throw new BeanContextException(methodName + "(" + attributeValue.getClass() + ") could not be found.");
            }
        }
    }

    private static MethodTypePair getMethodTypePair(Class<?> objectClass, String methodName, Object attributeValue) {
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
                            Class<?>[] param = new Class[] {toPrimitiveType(compatibleClasses[i])};
                            method = objectClass.getMethod(methodName, param);
                            result = new MethodTypePair(method, compatibleClasses[i]);
                            break;
                        } catch (NoSuchMethodException e2) {
                        }
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

    private static Object convertObjectType(Object value, Class<?> typeToCastInto) {
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

    public static class MethodTypePair {
        Method method;
        Class<?> type;

        public MethodTypePair(Method method, Class<?> type) {
            this.method = method;
            this.type = type;
        }
    }
    
    public class BeanMap {
        private Object mBean;
        private BeanPropertyAccessor mAccessor;
        
        public BeanMap(Object bean, BeanPropertyAccessor accessor) {
            mBean = bean;
            mAccessor = accessor;
        }
        
        public Object get(String property) {
            return mAccessor.getPropertyValue(mBean, property);
        }
    }
    
    public class BeanContextException extends Exception {
        
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
