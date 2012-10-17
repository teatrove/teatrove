package org.teatrove.tea.templates;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.concurrent.Semaphore;

import org.junit.Before;
import org.junit.Test;

public class CompareTest extends AbstractTemplateTest {

    protected static final int PROCESSORS = 
        Runtime.getRuntime().availableProcessors();
    
    private Throwable error;
    
    public static class CompareContext {
        // nothing to add
    }

    @Before
    public void setup() {
        addContext("CompareApplication", new CompareContext());
    }

    @Test
    public void testDateCompare() throws Exception {
        executeTest(TEST_DATES);
    }

    @Test
    public void testStringCompare() throws Exception {
        executeTest(TEST_STRINGS);
    }

    @Test
    public void testNumbers() throws Exception {
        executeTest(TEST_NUMBERS);
    }
    
    @Test
    public void testObjectCompare() throws Exception {
        executeTest(TEST_OBJECTS);
    }
    
    @Test
    public void testNulls() throws Exception {
        String sig = "Integer a";
        Object[] params = { null };
        
        assertEquals("valid", 
                     executeSource("if (a != 2) { 'valid' }", sig, params));

        assertEquals("", 
                     executeSource("if (a == 2) { 'valid' }", sig, params));
        
        assertEquals("valid", 
                     executeSource("(a != 2 ? 'valid' : '')", sig, params));
        
        assertEquals("", 
                     executeSource("(a == 2 ? 'valid' : '')", sig, params));
        
        assertEquals("valid", 
                     executeSource("if (5.3 != a) { 'valid' }", sig, params));

        assertEquals("", 
                     executeSource("if (5.3 == a) { 'valid' }", sig, params));
        
        assertEquals("valid", 
                     executeSource("(5.3 != a ? 'valid' : '')", sig, params));
        
        assertEquals("", 
                     executeSource("(5.3 == a ? 'valid' : '')", sig, params));
        
        assertEquals("valid", 
                     executeSource("(null == a ? 'valid' : '')", sig, params));
        
        assertEquals("valid", 
                     executeSource("(a == null ? 'valid' : '')", sig, params));
        
        assertEquals("valid", 
                     executeSource("(a == a ? 'valid' : '')", sig, params));
    }
    
    @Test
    public void testIncompatibleTypes() throws Exception {

        // validate relational
        try {
            executeSource("if (left < right) { 'true' }", 
                          "Date left, String right", new Date(), "test");
            fail("expected exception to occur with relational of date/string");
        }
        catch (Exception e) { /* expected */ }
        
        // validate compare
        executeSource("left <=> right", 
                      "Date left, String right", new Date(), "test");
        
        // validate equals
        executeSource("if (left == right) { 'true' }", 
                      "Date left, String right", new Date(), "test");
        
        // validate number to string
        executeSource("left <=> right", "Integer left, String right", 
                      Integer.valueOf(5), "23");
        
        // validate date to number
        executeSource("left <=> right", "Date left, Integer right", 
                      new Date(), Integer.valueOf(1));

        // validate non-comparable that are incompatible
        executeSource("left <=> right", 
                      SimpleObject.class.getName() + " left, " +
                      		OtherObject.class.getName() + " right", 
                      new SimpleObject(), new OtherObject());
        
        // validate number to non-number
        try {
            executeSource("if (left > right) { 'true' }", 
                          "String left, Long right", 
                          "23", Long.valueOf(5));
            fail("expected exception to occur with compare of int/non-int");
        }
        catch (Exception e) { /* expected */ } 
    }
    
    @Test
    public void testNonComparable() throws Exception {
        // validate non-comparable with relational fails
        try {
            executeSource("if (left < right) { 'true' }",
                          SimpleObject.class.getName() + " left, " +
                              SimpleObject.class.getName() + " right",
                          new SimpleObject(), new SimpleObject());
            
            fail("expected exception to occur with simple objects");
        }
        catch (Exception e) { /* expected */ }

        // validate non-comparable with compare uses toString
        executeSource("left <=> right",
                      SimpleObject.class.getName() + " left, " +
                      SimpleObject.class.getName() + " right",
                      new SimpleObject("def"), new SimpleObject("abc"));
    }
    
    @Test
    public void testComparableHiearchy() throws Exception {
        String[][] tests = {
            //{ "if (test < subtest) { 'true' }", "true" },
            //{ "if (subtest < test) { 'true' }", "" },
            //{ "v = test <=> subtest; if (v < 0) { 'true' }", "true" },
            { "v = subtest <=> test; if (v < 0) { 'true' }", "" },
            { "if (test == subtest) { 'true' }", "" },
            { "if (subtest != test) { 'true' }", "true" },
        };

        for (String[] test : tests) {
            String source = test[0];
            String expected = test[1];

            String result = executeSource
            (
                source,
                TestObject.class.getName() + " test, " +
                    SubTestObject.class.getName() + " subtest",
                new TestObject("abc"), new SubTestObject("def")
            );
            
            assertEquals("failed test: " + source, expected, result);
        }
    }
    
    protected void executeTest(Object[][] testValues)
        throws Exception {
        
        final Semaphore semaphore = new Semaphore(PROCESSORS);
        
        outer: for (int i = 0; i < testValues.length; i++) {
            Object[] lvals = testValues[i];
            final String ltype = (String) lvals[0];
            final String lname = (String) lvals[1];
            final Object min = lvals[2];
            
            for (int j = 0; j < testValues.length; j++) {
                Object[] rvals = testValues[j];
                final String rtype = (String) rvals[0];
                final String rname = (String) rvals[1];
                final Object max = rvals[3];
            
                for (int k = 0; k < TEST_CASES.length; k++) {
                    String[] test = TEST_CASES[k];
                    final String source = test[0];
                    final String expected = test[1];
                    
                    semaphore.acquire();
                    if (this.error != null) { break outer; }
                    
                    new Thread(new Runnable() {
                        public void run() {
                            executeTestCase(source, expected, 
                                            lname, ltype, min,
                                            rname, rtype, max);
                            
                            semaphore.release();
                        }
                    }).start();
                }
            }
        }
        
        // throw any caught exception
        if (error != null) {
            throw new Exception("error processing test: " + error.getMessage(),
                                error);
        }
    }
    
    protected void executeTestCase(String source, String expected,
                                   String lname, String ltype, Object min,
                                   String rname, String rtype, Object max) {
        try {
            String name = lname + " min, " + rname + " max";
            String signature = ltype + " min, " + rtype + " max";
            //System.out.println("EXECUTE " + name + ": " + source);
            String result = executeSource(source, signature, min, max);
            assertEquals("invalid result: " + name + ": " + source,
                         expected, result);
        }
        catch (Throwable exception) {
            this.error = exception;
        }
    }

    protected static final Object[][] TEST_DATES = {
        { "Date", "Date", new Date(5), new Date(11) }
    };
    
    protected static final Object[][] TEST_STRINGS = {
        { "String", "String", "abc", "def" }
    };
    
    protected static final Object[][] TEST_OBJECTS = {
        { TestObject.class.getName(), "Custom", new TestObject("a"), new TestObject("b") }
    };

    protected static final Object[][] TEST_NUMBERS = {
        { "byte", "byte", Byte.valueOf((byte) 5), Byte.valueOf((byte) 9) },
        { "int", "int", Integer.valueOf(5), Integer.valueOf(9) },
        { "long", "long", Long.valueOf(5L), Long.valueOf(9L) },
        { "float", "float", Float.valueOf(5.0f), Float.valueOf(9.0f) },
        { "double", "double", Double.valueOf(5.0), Double.valueOf(9.0) },
        
        { "Byte", "Byte", Byte.valueOf((byte) 5), Byte.valueOf((byte) 9) },
        { "Integer", "Integer", Integer.valueOf(5), Integer.valueOf(9) },
        { "Long", "Long", Long.valueOf(5L), Long.valueOf(9L) },
        { "Float", "Float", Float.valueOf(5.0f), Float.valueOf(9.0f) },
        { "Double", "Double", Double.valueOf(5.0), Double.valueOf(9.0) },

        { "Number", "Number(Byte)", Byte.valueOf((byte) 5), Byte.valueOf((byte) 9) },
        { "Number", "Number(Integer)", Integer.valueOf(5), Integer.valueOf(9) },
        { "Number", "Number(Long)", Long.valueOf(5L), Long.valueOf(9L) },
        { "Number", "Number(Float)", Float.valueOf(5.0f), Float.valueOf(9.0f) },
        { "Number", "Number(Double)", Double.valueOf(5.0), Double.valueOf(9.0) },
        
        { "Number", "Number(Custom1)", new Custom1(5.0), new Custom1(9.0) },
        { "Number", "Number(Custom2)", new Custom2(5.0), new Custom2(9.0) }
    };
    
    protected static final String[][] TEST_CASES = {
        { "if (min < max) { 'true' }", "true" },
        { "if (max < min) { 'false' }", "" },
        { "if (min < min) { 'false' }", "" },
        { "if (max < max) { 'false' }", "" },
        { "if (min <= max) { 'true' }", "true" },
        { "if (max <= min) { 'false' }", "" },
        { "if (min <= min) { 'true' }", "true" },
        { "if (max <= max) { 'true' }", "true" },
        
        { "if (max > min) { 'true' }", "true" },
        { "if (min > max) { 'false' }", "" },
        { "if (min > min) { 'false' }", "" },
        { "if (max > max) { 'false' }", "" },
        { "if (max >= min) { 'true' }", "true" },
        { "if (min >= max) { 'false' }", "" },
        { "if (max >= max) { 'true' }", "true" },
        { "if (min >= min) { 'true' }", "true" },
        
        { "if (min == min) { 'true' }", "true" },
        { "if (max == max) { 'true' }", "true" },
        { "if (max == min) { 'false' }", "" },
        { "if (min == max) { 'false' }", "" },
        
        { "if (min != max) { 'true' }", "true" },
        { "if (max != min) { 'true' }", "true" },
        { "if (max != max) { 'false' }", "" },
        { "if (min != min) { 'false' }", "" },
        
        { "(min < max ? 'true' : 'false')", "true" },
        { "(min == max ? 'true' : 'false')", "false" },
        { "(max < min ? 'true' : 'false')", "false" },
        { "(min != max ? 'true' : 'false')", "true" },
        
        { "a = (min <=> max); if (a < 0) { 'true' }", "true" },
        { "a = (max <=> min); if (a > 0) { 'true' }", "true" },
        { "a = (min <=> min); if (a == 0) { 'true' }", "true" },
        { "a = (max <=> max); if (a == 0) { 'true' }", "true" }
    };

    public static class TestObject implements Comparable<TestObject> {
        private String value;
        public TestObject(String value) { this.value = value; }
        public String toString() {
            return value;
        }

        @Override
        public int compareTo(TestObject o) {
            return this.value.compareTo(o.value);
        }
    }
    
    public static class SubTestObject extends TestObject {

        public SubTestObject(String value) {
            super(value);
        }

        @Override
        public int compareTo(TestObject o) {
            fail("unexpected comparison (SubTestObject to TestObject)");
            return -1;
        }
    }
    
    public static class SimpleObject {
        
        private String val;
        
        public SimpleObject() { }
        
        public SimpleObject(String val) {
            this.val = val;
        }
        
        public String toString() {
            return this.val == null ? "" : this.val;
        }
    }
    
    public static class OtherObject { }
    
    public static class Custom1 extends Number {
        private static final long serialVersionUID = 1L;
        
        private double value;
        
        public Custom1(double value) {
            this.value = value;
        }
        
        @Override public int intValue() { return (int) value; }
        @Override public long longValue() { return (long) value; }
        @Override public float floatValue() { return (float) value; }
        @Override public double doubleValue() { return value; }
        
        @Override
        public boolean equals(Object object) {
            if (object instanceof Custom1) {
                return this.value == ((Custom1) object).value;
            }
            
            return false;
        }
        
        @Override
        public int hashCode() {
            return 11 * (int) value;
        }
    }
    
    public static class Custom2 extends Number implements Comparable<Custom2> {
        private static final long serialVersionUID = 1L;
        
        private double value;
        
        public Custom2(double value) {
            this.value = value;
        }
        
        @Override public int intValue() { return (int) value; }
        @Override public long longValue() { return (long) value; }
        @Override public float floatValue() { return (float) value; }
        @Override public double doubleValue() { return value; }

        @Override
        public int compareTo(Custom2 other) {
            return Double.compare(this.value, other.value);
        }
        
        @Override
        public boolean equals(Object object) {
            if (object instanceof Custom2) {
                return this.value == ((Custom2) object).value;
            }
            
            return false;
        }
        
        @Override
        public int hashCode() {
            return 11 * (int) value;
        }
    }
}
