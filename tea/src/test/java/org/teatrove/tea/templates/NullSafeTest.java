package org.teatrove.tea.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class NullSafeTest extends AbstractTemplateTest {

    @Before
    public void setup() {
        addContext("DateContext", new DateContext());
    }
    
    @Test
    @SuppressWarnings("deprecation")
    public void testNullSafe() throws Exception {
        assertEquals("-1", executeTest(TEST_SOURCE_1));
        assertEquals("4", executeTest(TEST_SOURCE_2));
        assertEquals("4", executeTest(TEST_SOURCE_3));
        assertEquals("4", executeTest(TEST_SOURCE_4));
        assertEquals(String.valueOf(new Date().getDate()), executeTest(TEST_SOURCE_6));
        assertEquals("invalid", executeTest(TEST_SOURCE_7));
        assertEquals(String.valueOf(new Date().getDate()), executeTest(TEST_SOURCE_8));
        assertTrue(Integer.parseInt(executeTest(TEST_SOURCE_9)) > 0);
        assertEquals("5", executeTest(TEST_SOURCE_10));
    }

    protected String executeTest(String source) throws Exception {
        // build params
        List<String> list = Arrays.asList("TEST", null);

        int[] array = { 0, 5, 2, 1 };

        Map<String, Date> map = new HashMap<String, Date>();
        map.put("test", new Date());
        map.put("test2", null);

        // execute
        return executeSource(source, TEST_SOURCE_PARAMS,
                             "TEST", list, map, array);
    }

    protected static String TEST_SOURCE_PARAMS =
        "String c, List<String> d, Map<String, Date> e, int[] f";

    protected static String TEST_SOURCE_1 =
        "a = null as String; a?.length ?: -1";

    protected static String TEST_SOURCE_2 =
        "b = 'test'; b?.length";

    protected static String TEST_SOURCE_3 =
        "c?.length";

    protected static String TEST_SOURCE_4 =
        "d?[0]?.length";

    protected static String TEST_SOURCE_6 =
        "e?['test']?.date";

    protected static String TEST_SOURCE_7 =
        "(f?[0] == 0 ? 'invalid' : 'valid')";

    protected static String TEST_SOURCE_8 =
        "g = currentDate(); g?.date";

    protected static String TEST_SOURCE_9 =
        "h = currentDate(); h?.toString()?.length";

    protected static String TEST_SOURCE_10 =
        "(c == 'blah' ? null : 'valid')?.length";
    
    public static class DateContext {
        public Date currentDate() { return new Date(); }
    }
}
