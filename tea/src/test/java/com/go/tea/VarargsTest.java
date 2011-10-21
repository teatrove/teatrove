package org.teatrove.tea;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;



public class VarargsTest extends AbstractTemplateTest {

    @Before
    public void setup() {
        addContext(new VarargsContext());
    }

    @Test
    public void testSpread() throws Exception {
        assertEquals("55", executeTest(TEST_SOURCE_1));
        assertEquals("55", executeTest(TEST_SOURCE_2));
        assertEquals("11", executeTest(TEST_SOURCE_3));
        assertEquals("77", executeTest(TEST_SOURCE_4));
        assertEquals("77", executeTest(TEST_SOURCE_5));
        assertEquals("77", executeTest(TEST_SOURCE_6));
        assertEquals("66", executeTest(TEST_SOURCE_7));
        assertEquals("11", executeTest(TEST_SOURCE_8));
        assertEquals("22", executeTest(TEST_SOURCE_9));
        assertEquals("33", executeTest(TEST_SOURCE_10));
        assertEquals("11", executeTest(TEST_SOURCE_11));
        assertEquals("44", executeTest(TEST_SOURCE_12));
        assertEquals("11", executeTest(TEST_SOURCE_13));
    }

    protected String executeTest(String source) throws Exception {
        return executeSource(source, TEST_SOURCE_PARAMS,
                             "Test", Boolean.FALSE);
    }

    public static class VarargsContext {
        public String doSomething(String test, Object... blah) {
            return "1";
        }

        public String doSomething(String test, String... blah) {
            return "2";
        }

        public String doSomething(String test, Integer... blah) {
            return "3";
        }

        public String doSomething(String test, double... blah) {
            return "4";
        }

        public String doSomething(Object test) {
            return "5";
        }

        public String doSomethingElse(Object value) {
            return "6";
        }

        public String doSomethingElse(String... values) {
            return "7";
        }

        public VarargsContext getContext() {
            return this;
        }
    }

    protected static final String TEST_SOURCE_PARAMS =
        "Object test1, Object test2";

    protected static final String TEST_SOURCE_1 =
        "doSomething('test');" +
        "getContext().doSomething('test');"; // 5

    protected static final String TEST_SOURCE_2 =
        "doSomething(test1);" +
        "getContext().doSomething(test1);"; // 5

    protected static final String TEST_SOURCE_3 =
        "doSomething(test1, test2);" +
        "getContext().doSomething(test1, test2);"; // 1

    protected static final String TEST_SOURCE_4 =
        "doSomethingElse('test1', 'test2', 'test3');" +
        "getContext().doSomethingElse('test1', 'test2', 'test3');"; // 7

    protected static final String TEST_SOURCE_5 =
        "doSomethingElse(#('test3', 'test4'));" +
        "getContext().doSomethingElse(#('test3', 'test4'));"; // 7

    protected static final String TEST_SOURCE_6 =
        "doSomethingElse('test');" +
        "getContext().doSomethingElse('test');"; // 7

    protected static final String TEST_SOURCE_7 =
        "doSomethingElse(test1);" +
        "getContext().doSomethingElse(test1);"; // 6

    protected static final String TEST_SOURCE_8 =
        "doSomething('test5', test1, test2);" +
        "getContext().doSomething('test5', test1, test2);"; // 1

    protected static final String TEST_SOURCE_9 =
        "doSomething('test6', 'test9');" +
        "getContext().doSomething('test6', 'test9');"; // 2

    protected static final String TEST_SOURCE_10 =
        "doSomething('test9', 5, 10, 11);" +
        "getContext().doSomething('test9', 5, 10, 11);"; // 3

    protected static final String TEST_SOURCE_11 =
        "doSomething('test10', 'test4', 'test', test2, 'test', test1);" +
        "getContext().doSomething('test10', 'test4', 'test', test2, 'test', test1);"; // 1

    protected static final String TEST_SOURCE_12 =
        "doSomething('test', 39.2, 5, 10, 10.1);" +
        "getContext().doSomething('test', 39.2, 5, 10, 10.1);"; // 4

    protected static final String TEST_SOURCE_13 =
        "doSomething('test', 5.2, 2.3, 2.9, 'test');" +
        "getContext()?.doSomething('test', 5.2, 2.3, 2.9, 'test');"; // 1
}
