package org.teatrove.tea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;


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
    }

    @Before
    public void setup() {
        addContext(new CompareContext());
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
        "start = getDate(); " +
        "end = getDate(); " +
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
}
