package org.teatrove.tea.util;

import static org.junit.Assert.assertEquals;

import java.beans.PropertyDescriptor;
import java.util.Map;

import org.junit.Test;
import org.teatrove.trove.generics.GenericType;

public class BeanAnalyzerTest {

    @Test
    public void testCovariants() throws Exception {
        Map<String, PropertyDescriptor> properties =
            BeanAnalyzer.getAllProperties(new GenericType(Int.class));
        PropertyDescriptor value = properties.get("value");
        assertEquals(Integer.class, value.getPropertyType());
        assertEquals(Integer.class, value.getReadMethod().getReturnType());
    }
    
    @Test
    public void testGenerics() throws Exception {
        Map<String, PropertyDescriptor> properties =
            BeanAnalyzer.getAllProperties(new GenericType(IntGenerics.class));
        PropertyDescriptor value = properties.get("value");
        assertEquals(Integer.class, value.getPropertyType());
        assertEquals(Integer.class, value.getReadMethod().getReturnType());
        assertEquals(Integer.class, value.getWriteMethod().getParameterTypes()[0]);
    }

    public static class Base {
        public Number getValue() { return null; }
    }
    
    public static class Int extends Base {
        public Integer getValue() { return null; }
    }
    
    public static class BaseGenerics<T extends Number> {
        public T getValue() { return null; }
        public void setValue(T value) { }
    }
    
    public static class IntGenerics extends BaseGenerics<Integer> {
        public Integer getValue() { return null; }
        public void setValue(Integer value) { }
    }
}
