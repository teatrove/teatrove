package org.teatrove.trove.classfile.generics;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.teatrove.trove.generics.GenericType;
import org.teatrove.trove.util.ClassInjector;
import org.teatrove.trove.util.MergedClass;

public class ParameterizedTypeDescTest {

    @Test
    public void testGetSignature() throws Exception {
        for (int i = 1; i <= 8; i++) {
            System.out.println("TEST" + i);
            System.out.println("    Param : " + getParamDesc("getTest" + i).getSignature());
            System.out.println("    Return: " + getReturnDesc("getTest" + i).getSignature());
            System.out.println();
            
            new GenericType(getReturnType("getTest" + i)).getTypeArguments();
            new GenericType(getParamType("getTest" + i)).getTypeArguments();
        }
        
        ClassInjector injector = ClassInjector.getInstance();
        Class<?>[] clazzes = { ParameterizedTypeDescTest.class };
        Constructor<?> ctor1 = MergedClass.getConstructor(injector, clazzes);
        Class<?> result1 = ctor1.getDeclaringClass();
        for (int i = 1; i <= 8; i++) {
            new GenericType(getReturnType(result1, "getTest" + i)).getTypeArguments();
            new GenericType(getParamType(result1, "getTest" + i)).getTypeArguments();
        }
        
        Constructor<?> ctor2 = MergedClass.getConstructor(injector, new Class<?>[] { ctor1.getDeclaringClass() });
        Class<?> result2 = ctor2.getDeclaringClass();
        for (int i = 1; i <= 8; i++) {
            new GenericType(getReturnType(result2, "getTest" + i)).getTypeArguments();
            new GenericType(getParamType(result2, "getTest" + i)).getTypeArguments();
        }
        
        Assert.assertNotNull(ctor2.newInstance(ctor1.newInstance(this)));
    }
    
    protected ParameterizedTypeDesc getReturnDesc(String name) {
        return ParameterizedTypeDesc.forType(getReturnType(name));
    }
    
    protected ParameterizedType getReturnType(String name) {
        return getReturnType(ParameterizedTypeDescTest.class, name);
    }
    
    protected ParameterizedType getReturnType(Class<?> type, String name) {
        try {
            return (ParameterizedType) 
                type.getMethod(name, List.class).getGenericReturnType();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    protected ParameterizedTypeDesc getParamDesc(String name) {
        return ParameterizedTypeDesc.forType(getParamType(name));
    }
    
    protected ParameterizedType getParamType(String name) {
        return getParamType(ParameterizedTypeDescTest.class, name);
    }
    
    protected ParameterizedType getParamType(Class<?> type, String name) {
        try {
            return (ParameterizedType) 
                type.getMethod(name, List.class).getGenericParameterTypes()[0];
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Object> getTest1(List<Object> a) { return null; }
    public <T> List<T> getTest2(List<T> a) { return null; }
    public <T extends Number> List<T> getTest3(List<T> a) { return null; }
    public <T extends Number> List<? extends T> getTest4(List<? extends T> a) { return null; }
    public List<? extends Number> getTest5(List<? extends Number> a) { return null; }
    public List<?> getTest6(List<?> a) { return null; }
    public List<? super Number> getTest7(List<? super Number> a) { return null; }
    public <T extends Comparable<? super T>> void getTest8(List<T> list) {
        
    }
}
