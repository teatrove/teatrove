package org.teatrove.tea;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class TernaryTest extends AbstractTemplateTest {

    @Test
    public void testSpread() throws Exception {
        assertEquals("false", executeSource(TEST_SOURCE_1));
        assertEquals("valid", executeSource(TEST_SOURCE_2));
        assertEquals("data", executeSource(TEST_SOURCE_3_1));
        assertEquals("nodata", executeSource(TEST_SOURCE_3_2));
        assertEquals("without", executeSource(TEST_SOURCE_4_1));
        assertEquals("with", executeSource(TEST_SOURCE_4_2));
        assertEquals("text", executeSource(TEST_SOURCE_5));
        assertEquals("null", executeSource(TEST_SOURCE_6));
        assertEquals("default", executeSource(TEST_SOURCE_7));
        assertEquals("null", executeSource(TEST_SOURCE_8));
        assertEquals("blah", executeSource(TEST_SOURCE_9));
        assertEquals("5", executeSource(TEST_SOURCE_10_1));
        assertEquals("10", executeSource(TEST_SOURCE_10_2));
        assertEquals("valid", executeSource(TEST_SOURCE_11_1));
        assertEquals("test", executeSource(TEST_SOURCE_11_2));
        assertEquals("a5", executeSource(TEST_SOURCE_12));
        assertEquals("4", executeSource(TEST_SOURCE_13));
        assertEquals("true", executeSource(TEST_SOURCE_14));
    }

    protected static final String TEST_SOURCE_1 =
        "a = false; a ? 'true' : 'false'"; // false

    protected static final String TEST_SOURCE_2 =
        "b = 5; (b ? 'valid' : 'invalid')"; // valid

    protected static final String TEST_SOURCE_3_1 =
        "c = #(1, 2, 3); (c ? 'data' : 'nodata')"; // data

    protected static final String TEST_SOURCE_3_2 =
        "c = #(); (c ? 'data' : 'nodata')"; // nodata

    protected static final String TEST_SOURCE_4_1 =
        "d = ##(); (d ? 'with' : 'without')"; // without

    protected static final String TEST_SOURCE_4_2 =
        "d = ##('a', 'b', 'c', 'd'); (d ? 'with' : 'without')"; // with

    protected static final String TEST_SOURCE_5 =
        "e = 'user'; (e ? 'text' : 'notext')"; // text

    protected static final String TEST_SOURCE_6 =
        "f = null; (f ? 'nonnull' : 'null')"; // null

    protected static final String TEST_SOURCE_7 =
        "f = null; g = f == null ? #('default') : #(f);" +
        "foreach (g1 in g) { g1 }"; // default

    protected static final String TEST_SOURCE_8 =
        "g = #('default');" +
        "h1 = null as HashSet<String>;" +
        "h2 = null as ArrayList<String>;" +
        "h = (g == null ? h1 : h2);" +
        "h"; // null

    protected static final String TEST_SOURCE_9 =
        "i = 'blah'; (i == 'blah' ? 'blah' : 'nonblah')"; // blah

    protected static final String TEST_SOURCE_10_1 =
        "j = 5; (j ?: 10)"; // 5

    protected static final String TEST_SOURCE_10_2 =
        "j = 0; (j ?: 10)"; // 10

    protected static final String TEST_SOURCE_11_1 =
        "k = null; k ?: 'valid'"; // valid

    protected static final String TEST_SOURCE_11_2 =
        "k = 'test'; k ?: 'valid'"; // test

    protected static final String TEST_SOURCE_12 =
        "l = (5 ? ('a' ? 'a5' : 'nona5') : 'b' ? 'b?' : '?'); l"; // a5

    protected static final String TEST_SOURCE_13 =
        "m = 5; m = 'test'; (m isa String ? m.length : 0)"; // 4

    protected static final String TEST_SOURCE_14 =
        "f = null; f?.toString()?.length() == 0"; // true

}
