package org.teatrove.tea.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.go.tea.templates.CompareTest.CompareContext;
import com.go.tea.templates.CompareTest.TestObject;


public class CompareTest extends AbstractTemplateTest {

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

    public static class CompareContext {
        public Date getDate() {
            return getDate(0);
        }

        public Date getDate(int time) {
            return new Date(System.currentTimeMillis() + time);
        }

        public TestObject getTest(String value) {
            return new TestObject(value);
        }
        
        public Integer getInteger(int value) {
            return Integer.valueOf(value);
        }
        
        public Double getDouble(double value) {
            return Double.valueOf(value);
        }
        
        public Long getLong(long value) {
            return Long.valueOf(value);
        }
        
        public Number getNumber(double value, int type) {
            if (type == 1) { return Integer.valueOf((int) value); }
            else if (type == 2) { return Double.valueOf(value); }
            else if (type == 3) { return Long.valueOf((long) value); }
            else { return null; }
        }
    }

    @Before
    public void setup() {
        addContext("CompareApplication", new CompareContext());
    }

    @Test
    public void testDateCompare() throws Exception {
        assertEquals("true", executeSource(TEST_DATES_GT));
        assertEquals("true", executeSource(TEST_DATES_LT));
        assertEquals("true", executeSource(TEST_DATES_EQ));
    }

    @Test
    public void testStringCompare() throws Exception {
        assertTrue(executeAsInt(TEST_STRINGS_1) < 0);
        assertTrue(executeAsInt(TEST_STRINGS_2) > 0);
        assertTrue(executeAsInt(TEST_STRINGS_3) == 0);
    }

    @Test
    public void testObjectCompare() throws Exception {
        assertEquals("not equal",
                     executeSource(TEST_COMPARE, TEST_COMPARE_PARAMS,
                                   new TestObject("a"), new TestObject("b")));

        assertEquals("equal",
                     executeSource(TEST_COMPARE, TEST_COMPARE_PARAMS,
                                   new TestObject("a"), new TestObject("a")));
    }

    @Test
    public void testIntCompare() throws Exception {
        assertEquals(0, executeAsInt(TEST_INTS));
    }
    
    @Test
    public void testNumberCompare() throws Exception {
        assertEquals(1, executeAsInt(TEST_NUMBERS_1));
        assertEquals(-1, executeAsInt(TEST_NUMBERS_2));
        assertEquals(1, executeAsInt(TEST_NUMBERS_3));
        assertEquals(-1, executeAsInt(TEST_NUMBERS_4));
        assertEquals(1, executeAsInt(TEST_NUMBERS_5));
        assertEquals(-1, executeAsInt(TEST_NUMBERS_6));
        assertEquals(1, executeAsInt(TEST_NUMBERS_7));
        assertEquals(0, executeAsInt(TEST_NUMBERS_8));
        assertEquals(1, executeAsInt(TEST_NUMBERS_9));
        assertEquals(-1, executeAsInt(TEST_NUMBERS_10));
        assertEquals(1, executeAsInt(TEST_NUMBERS_11));
        assertEquals(-1, executeAsInt(TEST_NUMBERS_12));
    }

    protected int executeAsInt(String source) throws Exception {
        return Integer.parseInt(executeSource(source));
    }

    protected static final String TEST_DATES_GT =
        "start = getDate(10000); " +
        "end = getDate(); " +
        "start > end ? 'true' : 'false'";

    protected static final String TEST_DATES_LT =
        "start = getDate(); " +
        "end = getDate(10000); " +
        "start < end ? 'true' : 'false'";

    protected static final String TEST_DATES_EQ =
        "start = getDate(10000); " +
        "end = getDate(10000); " +
        "start == end ? 'true' : 'false'";

    protected static final String TEST_STRINGS_1 =
        "str1 = 'abc'; str2 = 'def'; str1 <=> str2";

    protected static final String TEST_STRINGS_2 =
        "str1 = 'zyx'; str2 = 'lmn'; str1 <=> str2";

    protected static final String TEST_STRINGS_3 =
        "str1 = 'ghi'; str2 = 'ghi'; str1 <=> str2";

    protected static final String TEST_COMPARE =
        "test1 <=> test2 == 0 ? 'equal' : 'not equal'";

    protected static final String TEST_COMPARE_PARAMS =
        "Object test1, Object test2";

    protected static final String TEST_INTS =
        "num1 = 10; num2 = 10; num1 <=> num2";
    
    protected static final String TEST_NUMBERS_1 =
        "val1 = getInteger(1); val2 = getLong(2); " +
        "if (val1 < val2) { 1 } else { 0 }";
    
    protected static final String TEST_NUMBERS_2 =
        "val1 = getInteger(1); val2 = getLong(2); val1 <=> val2";
    
    protected static final String TEST_NUMBERS_3 =
        "val1 = getInteger(1); val2 = getNumber(2, 1); " +
        "if (val1 < val2) { 1 } else { 0 }";
    
    protected static final String TEST_NUMBERS_4 =
        "val1 = getInteger(1); val2 = getNumber(2, 1); val1 <=> val2";
    
    protected static final String TEST_NUMBERS_5 =
        "val1 = getNumber(1, 2); val2 = getInteger(2); " +
        "if (val1 < val2) { 1 } else { 0 }";
    
    protected static final String TEST_NUMBERS_6 =
        "val1 = getNumber(1, 1); val2 = getInteger(2); val1 <=> val2";
    
    protected static final String TEST_NUMBERS_7 =
        "val1 = getNumber(1, 3); val2 = getNumber(2, 1); " +
        "if (val1 < val2) { 1 } else { 0 }";
    
    protected static final String TEST_NUMBERS_8 =
        "val1 = getNumber(2, 3); val2 = getNumber(2, 2); val1 <=> val2";
    
    protected static final String TEST_NUMBERS_9 =
        "val1 = getNumber(1, 3); val2 = 2; " +
        "if (val1 < val2) { 1 } else { 0 }";
    
    protected static final String TEST_NUMBERS_10 =
        "val1 = getDouble(1); val2 = 2; val1 <=> val2";
    
    protected static final String TEST_NUMBERS_11 =
        "val1 = 1.0; val2 = getInteger(2); " +
        "if (val1 < val2) { 1 } else { 0 }";
    
    protected static final String TEST_NUMBERS_12 =
        "val1 = 1.0; val2 = getDouble(1.5); val1 <=> val2";
}
