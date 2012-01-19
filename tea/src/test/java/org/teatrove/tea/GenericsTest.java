package org.teatrove.tea;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class GenericsTest extends AbstractTemplateTest {

    @Test
    public void testGenerics() throws Exception {
        // setup params
        List<List<Integer[]>> numbers = new ArrayList<List<Integer[]>>();
        numbers.add(Collections.singletonList(new Integer[] {
            Integer.valueOf(5), Integer.valueOf(3), Integer.valueOf(1)
        }));

        Map<String, List<String>> states = new HashMap<String, List<String>>();
        states.put("test", Arrays.asList("going", "going", "gone"));

        String result1 =
            executeSource(TEST_SOURCE_1, TEST_SOURCE_PARAMS, numbers, states);
        assertEquals("531", result1);

        String result2 =
            executeSource(TEST_SOURCE_2, TEST_SOURCE_PARAMS, numbers, states);
        assertEquals("goinggoinggone", result2);
    }

    protected static final String TEST_SOURCE_PARAMS =
        "List<List<Integer[]>> numbers, Map<String, List<String>> states";

    protected static final String TEST_SOURCE_1 =
        "if (numbers != null) {" +
            "foreach (a in numbers) {" +
                "foreach (b in a) {" +
                    "foreach (c in b) {" +
                        "c" +
                    "}" +
                "}" +
            "}" +
        "}";

    protected static final String TEST_SOURCE_2 =
        "state = states['test']" +
        "foreach (s in state) {" +
            "s" +
        "}";
}
