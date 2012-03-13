package org.teatrove.trove.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class BeanPropertyAccessorTest {

    private static final int TEST_PINT = 5;
    private static final double TEST_PDOUBLE = 25.23;
    private static final Integer TEST_WINT = Integer.valueOf(21);
    private static final Double TEST_WDOUBLE = Double.valueOf(92.1);
    private static final String TEST_STRING = "testing";
    
    @Test
    public void testForClass() {
        BeanPropertyAccessor accessor =
            BeanPropertyAccessor.forClass(MyObject.class);
        assertNotNull("accessor invalid", accessor);
        
        BeanPropertyAccessor cached =
            BeanPropertyAccessor.forClass(MyObject.class);
        assertSame("accessor not cached", accessor, cached);
    }

    @Test
    public void testGetPropertyValue() {
        MyObject bean = new MyObject
        (
            TEST_PINT, TEST_PDOUBLE, TEST_WINT, TEST_WDOUBLE, TEST_STRING
        );
        
        BeanPropertyAccessor accessor =
            BeanPropertyAccessor.forClass(MyObject.class);
        
        assertEquals("invalid primitive int", Integer.valueOf(TEST_PINT),
                     accessor.getPropertyValue(bean, "primitiveInt"));
        assertEquals("invalid primitive double", Double.valueOf(TEST_PDOUBLE),
                     accessor.getPropertyValue(bean, "primitiveDouble"));
        assertEquals("invalid wrapper int", TEST_WINT,
                     accessor.getPropertyValue(bean, "wrapperInteger"));
        assertEquals("invalid wrapper double", TEST_WDOUBLE,
                     accessor.getPropertyValue(bean, "wrapperDouble"));
        assertEquals("invalid string", TEST_STRING,
                     accessor.getPropertyValue(bean, "string"));
    }

    @Test
    public void testSetPropertyValue() {
        MyObject bean = new MyObject();
        BeanPropertyAccessor accessor =
            BeanPropertyAccessor.forClass(MyObject.class);
        
        accessor.setPropertyValue(bean, "primitiveInt", 
                                  Integer.valueOf(TEST_PINT));
        assertEquals("invalid primitive int", TEST_PINT, 
                     bean.getPrimitiveInt());
        
        accessor.setPropertyValue(bean, "primitiveDouble", 
                                  Double.valueOf(TEST_PDOUBLE));
        assertEquals("invalid primitive double", TEST_PDOUBLE, 
                     bean.getPrimitiveDouble(), 0.0);
        
        accessor.setPropertyValue(bean, "wrapperInteger", TEST_WINT);
        assertEquals("invalid wrapper int", TEST_WINT, 
                     bean.getWrapperInteger());
        
        accessor.setPropertyValue(bean, "wrapperDouble", TEST_WDOUBLE);
        assertEquals("invalid wrapper double", TEST_WDOUBLE, 
                     bean.getWrapperDouble());
        
        accessor.setPropertyValue(bean, "string", TEST_STRING);
        assertEquals("invalid string", TEST_STRING, bean.getString());
    }

    public static class MyObject {
        private int primitiveInt;
        private double primitiveDouble;
        private Integer wrapperInteger;
        private Double wrapperDouble;
        private String string;
        
        public MyObject() {
            super();
        }
        
        public MyObject(int pint, double pdouble, Integer wint, Double wdouble,
                        String string) {
            this.primitiveInt = pint;
            this.primitiveDouble = pdouble;
            this.wrapperInteger = wint;
            this.wrapperDouble = wdouble;
            this.string = string;
        }
        
        public int getPrimitiveInt() { return this.primitiveInt; }
        public void setPrimitiveInt(int pint) { 
            this.primitiveInt = pint;
        }
        
        public double getPrimitiveDouble() { return this.primitiveDouble; }
        public void setPrimitiveDouble(double pdouble) { 
            this.primitiveDouble = pdouble; 
        }
        
        public Integer getWrapperInteger() { return this.wrapperInteger; }
        public void setWrapperInteger(Integer wint) { 
            this.wrapperInteger = wint; 
        }
        
        public Double getWrapperDouble() { return this.wrapperDouble; }
        public void setWrapperDouble(Double wdouble) { 
            this.wrapperDouble = wdouble; 
        }
        
        public String getString() { return this.string; }
        public void setString(String string) { this.string = string; }
    }
}
