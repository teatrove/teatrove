package org.teatrove.tea.templates;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class MathTest extends AbstractTemplateTest {

    @Before
    public void setup() {
        addContext("MathApplication", new MathContext());
    }

    @Test
    public void testMath() throws Exception {
        for (int i = 0; i < TEST_SOURCES.length; i++) {
            assertEquals("test " + i + " failed: " + TEST_SOURCES[i][0],
                         TEST_SOURCES[i][1], executeSource(TEST_SOURCES[i][0]));
        }
    }

    protected static final String[][] TEST_SOURCES = {
        // int -> int
        { "a = 5; b = 3; result = a + b; result & ' - ' & result.class.name", "8 - java.lang.Integer" },
    
        // int -> long
        { "a = 5; b = 7L; result = a + b; result & ' - ' & result.class.name", "12 - java.lang.Long" },

        // int -> double
        { "a = 8.5; b = 3; result = a + b; result & ' - ' & result.class.name", "11.5 - java.lang.Double" },
    
    
        // long -> long
        { "a = 9L; b = 2L; result = a + b; result & ' - ' & result.class.name", "11 - java.lang.Long" },
    
        // long -> double
        { "a = 3.2; b = 5L; result = a + b; result & ' - ' & result.class.name", "8.2 - java.lang.Double" },
    
    
        // double -> double
        { "a = 5.9; b = 8.2; result = a + b; result & ' - ' & result.class.name", "14.1 - java.lang.Double" },
    
    
        // int -> Integer
        { "result = 5 + getInteger(1); result & ' - ' & result.class.name", "6 - java.lang.Integer" },
    
        // int -> Long
        { "result = 7 + getLong(9); result & ' - ' & result.class.name", "16 - java.lang.Long" },
    
        // int -> Double
        { "result = 11 + getDouble(2.3); result & ' - ' & result.class.name", "13.3 - java.lang.Double" },

        // int -> Number(Integer)
        { "result = 8 + getNumber('int', 3); result & ' - ' & result.class.name", "11 - java.lang.Integer" },
    
        // int -> Number(Long)
        { "result = getNumber('long', 5L) + 13; result & ' - ' & result.class.name", "18 - java.lang.Long" },
    
        // int -> Number(Double)
        { "result = 5 + getNumber('double', 9.2); result & ' - ' & result.class.name", "14.2 - java.lang.Double" },
    
        // int -> Number(Custom)
        { "result = 11 + getNumber('custom', 27.3); result & ' - ' & result.class.name", "38.3 - java.lang.Double" },
    
    
        // long -> Integer
        { "result = 5L + getInteger(9); result & ' - ' & result.class.name", "14 - java.lang.Long" },
    
        // long -> Long
        { "result = 15L + getLong(11L); result & ' - ' & result.class.name", "26 - java.lang.Long" },
    
        // long -> Double
        { "result = getDouble(3.2) + 19L; result & ' - ' & result.class.name", "22.2 - java.lang.Double" },

    
        // long -> Number(Integer)
        { "result = 18L + getNumber('int', 3); result & ' - ' & result.class.name", "21 - java.lang.Long" },
    
        // long -> Number(Long)
        { "result = getNumber('long', 5L) + 22L; result & ' - ' & result.class.name", "27 - java.lang.Long" },
    
        // long -> Number(Double)
        { "result = 5L + getNumber('double', 9.2); result & ' - ' & result.class.name", "14.2 - java.lang.Double" },
    
        // long -> Number(Custom)
        { "result = 29L + getNumber('custom', 11.4); result & ' - ' & result.class.name", "40.4 - java.lang.Double" },
    
    
        // double -> Integer
        { "result = 9.3 + getInteger(9); result & ' - ' & result.class.name", "18.3 - java.lang.Double" },
    
        // double -> Long
        { "result = 8.1 + getLong(11L); result & ' - ' & result.class.name", "19.1 - java.lang.Double" },
    
        // double -> Double
        { "result = getDouble(3.2) + 19.3; result & ' - ' & result.class.name", "22.5 - java.lang.Double" },

    
        // double -> Number(Integer)
        { "result = 7.4 + getNumber('int', 3); result & ' - ' & result.class.name", "10.4 - java.lang.Double" },
    
        // double -> Number(Long)
        { "result = getNumber('long', 5L) + 16.3; result & ' - ' & result.class.name", "21.3 - java.lang.Double" },
    
        // double -> Number(Double)
        { "result = 7.8 + getNumber('double', 9.3); result & ' - ' & result.class.name", "17.1 - java.lang.Double" },
    
        // double -> Number(Custom)
        { "result = 11.6 + getNumber('custom', 21.9); result & ' - ' & result.class.name", "33.5 - java.lang.Double" },
    
    
        // Integer -> Integer
        { "result = getInteger(3) + getInteger(9); result & ' - ' & result.class.name", "12 - java.lang.Integer" },
    
        // Integer -> Long
        { "result = getInteger(23) + getLong(19L); result & ' - ' & result.class.name", "42 - java.lang.Long" },
    
        // Integer -> Double
        { "result = getDouble(18.9) + getInteger(14); result & ' - ' & result.class.name", "32.9 - java.lang.Double" },
    
    
        // Integer -> Number(Integer)
        { "result = getInteger(11) + getNumber('int', 9); result & ' - ' & result.class.name", "20 - java.lang.Integer" },

        // Integer -> Number(Long)
        { "result = getNumber('long', 25L) + getInteger(14); result & ' - ' & result.class.name", "39 - java.lang.Long" },

        // Integer -> Number(Double)
        { "result = getInteger(21) + getNumber('double', 9.9); result & ' - ' & result.class.name", "30.9 - java.lang.Double" },
    
        // Integer -> Number(Custom)
        { "result = getInteger(26) + getNumber('custom', 17.5); result & ' - ' & result.class.name", "43.5 - java.lang.Double" },

    
    
        // Long -> Integer
        { "result = getLong(33L) + getInteger(9); result & ' - ' & result.class.name", "42 - java.lang.Long" },
    
        // Long -> Long
        { "result = getLong(45L) + getLong(19L); result & ' - ' & result.class.name", "64 - java.lang.Long" },
    
        // Long -> Double
        { "result = getDouble(18.9) + getLong(85); result & ' - ' & result.class.name", "103.9 - java.lang.Double" },
    
    
        // Long -> Number(Integer)
        { "result = getLong(11L) + getNumber('int', 9); result & ' - ' & result.class.name", "20 - java.lang.Long" },
    
        // Long -> Number(Long)
        { "result = getNumber('long', 25L) + getLong(49); result & ' - ' & result.class.name", "74 - java.lang.Long" },

        // Long -> Number(Double)
        { "result = getLong(47L) + getNumber('double', 9.9); result & ' - ' & result.class.name", "56.9 - java.lang.Double" },
    
        // Long -> Number(Custom)
        { "result = getLong(26L) + getNumber('custom', 7.3); result & ' - ' & result.class.name", "33.3 - java.lang.Double" },
    
    
        // Double -> Integer
        { "result = getDouble(29.3) + getInteger(9); result & ' - ' & result.class.name", "38.3 - java.lang.Double" },
    
        // Double -> Long
        { "result = getDouble(45.2) + getLong(19L); result & ' - ' & result.class.name", "64.2 - java.lang.Double" },
    
        // Double -> Double
        { "result = getDouble(11.4) + getDouble(13.1); result & ' - ' & result.class.name", "24.5 - java.lang.Double" },
    
    
        // Double -> Number(Integer)
        { "result = getDouble(41.8) + getNumber('int', 9); result & ' - ' & result.class.name", "50.8 - java.lang.Double" },
    
        // Double -> Number(Long)
        { "result = getNumber('long', 25L) + getDouble(59.2); result & ' - ' & result.class.name", "84.2 - java.lang.Double" },

        // Double -> Number(Double)
        { "result = getDouble(43.9) + getNumber('double', 9.9); result & ' - ' & result.class.name", "53.8 - java.lang.Double" },
    
        // Double -> Number(Custom)
        { "result = getDouble(6.8) + getNumber('custom', 1.1); result & ' - ' & result.class.name", "7.9 - java.lang.Double" },    
    
    
        // Number(Integer) -> Number(Integer)
        { "result = getNumber('int', 6) + getNumber('int', 9); result & ' - ' & result.class.name", "15 - java.lang.Integer" },
    
        // Number(Integer) -> Number(Long)
        { "result = getNumber('int', 42) + getNumber('long', 29L); result & ' - ' & result.class.name", "71 - java.lang.Long" },
    
        // Number(Integer) -> Number(Double)
        { "result = getNumber('double', 15.8) + getNumber('int', 11); result & ' - ' & result.class.name", "26.8 - java.lang.Double" },

        // Number(Integer) -> Number(Custom)
        { "result = getNumber('custom', 88.2) + getNumber('int', 82); result & ' - ' & result.class.name", "170.2 - java.lang.Double" },
    
    
    
        // Number(Long) -> Number(Integer)
        { "result = getNumber('long', 61L) + getNumber('int', 9); result & ' - ' & result.class.name", "70 - java.lang.Long" },
    
        // Number(Long) -> Number(Long)
        { "result = getNumber('long', 62L) + getNumber('long', 29L); result & ' - ' & result.class.name", "91 - java.lang.Long" },
    
        // Number(Long) -> Number(Double)
        { "result = getNumber('double', 15.8) + getNumber('long', 31L); result & ' - ' & result.class.name", "46.8 - java.lang.Double" },

        // Number(Long) -> Number(Custom)
        { "result = getNumber('custom', 88.2) + getNumber('long', 76L); result & ' - ' & result.class.name", "164.2 - java.lang.Double" },
    
    
    
        // Number(Double) -> Number(Integer)
        { "result = getNumber('double', 52.1) + getNumber('int', 9); result & ' - ' & result.class.name", "61.1 - java.lang.Double" },
    
        // Number(Double) -> Number(Long)
        { "result = getNumber('double', 61.3) + getNumber('long', 29L); result & ' - ' & result.class.name", "90.3 - java.lang.Double" },
    
        // Number(Double) -> Number(Double)
        { "result = getNumber('double', 15.8) + getNumber('double', 52.3); result & ' - ' & result.class.name", "68.1 - java.lang.Double" },

        // Number(Double) -> Number(Custom)
        { "result = getNumber('custom', 88.2) + getNumber('double', 83.1); result & ' - ' & result.class.name", "171.3 - java.lang.Double" },
    
    
    
        // Number(Custom) -> Number(Custom)
        { "result = getNumber('custom', 7.6) + getNumber('custom', 2.2); result & ' - ' & result.class.name", "9.8 - java.lang.Double" },
        
        // Number(Double) + Number(Integer) + Number(Long)
        { "result = getNumber('double', 14.9) + getNumber('int', 13) + getNumber('long', 19L); result & ' - ' & result.class.name", "46.9 - java.lang.Double" },
        
        // Validate Operations
        { "result = getInteger(5) - getNumber('int', 3); result", "2" },
        { "result = getDouble(2.5) * getNumber('long', 3L); result", "7.5" },
        { "result = getNumber('custom', 15) / getNumber('int', 3); result", "5.0" },
        { "result = getNumber('int', 7) % getNumber('int', 2); result", "1" },
    };
    
    public static class MathContext {
        public Integer getInteger(int value) { return Integer.valueOf(value); }
        public Long getLong(long value) { return Long.valueOf(value); }
        public Double getDouble(double value) { return Double.valueOf(value); }
        public Number getNumber(String type, double value) {
            if ("int".equals(type)) { return Integer.valueOf((int) value); }
            else if ("long".equals(type)) { return Long.valueOf((long) value); }
            else if ("double".equals(type)) { return Double.valueOf(value); }
            else if ("custom".equals(type)) { return new Custom(value); }
            else { return null; }
        }
    }
    
    public static class Custom extends Number {
        private static final long serialVersionUID = 1L;
        
        private double value;
        
        public Custom(double value) {
            this.value = value;
        }
        
        @Override public int intValue() { return (int) value; }
        @Override public long longValue() { return (long) value; }
        @Override public float floatValue() { return (float) value; }
        @Override public double doubleValue() { return value; }
    }
}
