package org.teatrove.tea.templates;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;



public class FunctionsTest extends AbstractTemplateTest {

    private static final String TEST_STRING_2 = "test2";
    private static final String TEST_STRING_1 = "test1";
    private static final double TEST_DOUBLE = 5.0;
    private static final String TEST_GREETING = "blah";
    private static final int TEST_WEIGHT = 10;
    private static final int TEST_HEIGHT = 5;
    
    public static final FunctionsContext INSTANCE = new FunctionsContext();

    @Before
    public void setup() {
        addContext("FunctionsApplication", new FunctionsContext());
    }

    @Test
    public void testValues() throws Exception {
        assertEquals(String.valueOf(TEST_WEIGHT), executeSource(TEST_WEIGHT_SOURCE));
        assertEquals("true", executeSource(TEST_HEIGHT_SOURCE));
        assertEquals("none", executeSource(TEST_NULL_SOURCE_1));
        assertEquals("5", executeSource(TEST_NULL_SOURCE_2));
        assertEquals(String.valueOf(TEST_DOUBLE + 1.2), executeSource(TEST_DOUBLE_SOURCE));
        assertEquals(TEST_STRING_1, executeSource(TEST_STRINGS_SOURCE_1));
        assertEquals(TEST_STRING_2, executeSource(TEST_STRINGS_SOURCE_2));
        assertEquals("blah", executeSource(TEST_MAPS_SOURCE_1));
        assertEquals("null", executeSource(TEST_MAPS_SOURCE_2));
        assertEquals("1", executeSource(TEST_LISTS_SOURCE_1));
        assertEquals(TEST_STRING_1, executeSource(TEST_LISTS_SOURCE_2));
        assertEquals(TEST_STRING_1, executeSource(TEST_LISTS_SOURCE_3));
        assertEquals(TEST_GREETING, executeSource(TEST_GREETING_SOURCE));
        assertEquals(String.valueOf(TEST_GREETING.length()), executeSource(TEST_GREETING_SOURCE_1));
        assertEquals(String.valueOf(TEST_GREETING.length()), executeSource(TEST_GREETING_SOURCE_2));
        assertEquals(String.valueOf(TEST_GREETING.length()), executeSource(TEST_GREETING_SOURCE_3));
        assertEquals(TEST_GREETING, executeSource(TEST_GREETING_SOURCE_4));
        assertEquals(String.valueOf(TEST_STRING_1.length()), executeSource(TEST_LISTS_SOURCE_4));
        assertEquals("0", executeSource(TEST_PAREN_SOURCE));
        assertEquals(String.valueOf(Math.PI), executeSource(TEST_STATIC_SOURCE_1));
        assertEquals(String.valueOf(Math.PI * Math.pow(5, 2)), executeSource(TEST_STATIC_SOURCE_2));
        assertEquals("test", executeSource(TEST_STATIC_SOURCE_3));
        assertEquals("test", executeSource(TEST_STATIC_SOURCE_4));
        assertEquals(TEST_GREETING, executeSource(TEST_STATIC_SOURCE_5));
        assertEquals(TEST_GREETING, executeSource(TEST_STATIC_SOURCE_6));
        assertEquals(TEST_GREETING, executeSource(TEST_STATIC_SOURCE_7));
    	assertEquals(String.valueOf(TEST_GREETING.length()), executeSource(TEST_STATIC_SOURCE_8));
        assertEquals(TEST_GREETING, executeSource(TEST_STATIC_SOURCE_9));
    }

    protected static final String TEST_WEIGHT_SOURCE =
        "func = getFunction(); func.getWeight()";

    protected static final String TEST_HEIGHT_SOURCE =
        "getFunction().getHeight() == 5 ? 'true' : 'false'";

    protected static final String TEST_GREETING_SOURCE =
        "getFunction().getGreeting()";

    protected static final String TEST_GREETING_SOURCE_1 =
        "getFunction().getGreeting().length()";

    protected static final String TEST_GREETING_SOURCE_2 =
        "getFunction().getGreeting().length";
    
    protected static final String TEST_GREETING_SOURCE_3 =
        "func = getFunction(); func.greeting.length()";

    protected static final String TEST_GREETING_SOURCE_4 =
        "func = FunctionsApplication.getFunction(); func.greeting";
    
    protected static final String TEST_NULL_SOURCE_1 =
        "getFunction().getNull()?.toString() ?: 'none'";

    protected static final String TEST_NULL_SOURCE_2 =
        "getFunction().getNull()?.toString()?.length + 5";

    protected static final String TEST_DOUBLE_SOURCE =
        "getFunction().getDouble() + 1.2";

    protected static final String TEST_STRINGS_SOURCE_1 =
        "getFunction().getStrings()?[0]";

    protected static final String TEST_STRINGS_SOURCE_2 =
        "getFunction().getStrings()[1]";

    protected static final String TEST_MAPS_SOURCE_1 =
        "getFunction().getMaps()['test'] ?: 'blah'";

    protected static final String TEST_MAPS_SOURCE_2 =
        "getFunction().getMaps()?['next']";

    protected static final String TEST_LISTS_SOURCE_1 =
        "getFunction().getLists().length";

    protected static final String TEST_LISTS_SOURCE_2 =
        "getFunction().getLists().get(0)";

    protected static final String TEST_LISTS_SOURCE_3 =
        "getFunction().getLists()[0]";

    protected static final String TEST_LISTS_SOURCE_4 =
        "getFunction().getLists()[0].length()";

    protected static final String TEST_PAREN_SOURCE =
        "func = getFunction(); (func.getGreeting() == 'test' ? 'valid' : null)?.toString()?.length()";

    protected static final String TEST_STATIC_SOURCE_1 =
    	"Math.PI";
    
    protected static final String TEST_STATIC_SOURCE_2 =
    	"radius = 5; area = java.lang.Math.PI *  java.lang.Math.pow(radius, 2); area";
    
    protected static final String TEST_STATIC_SOURCE_3 =
    	"list = java.util.Collections.singletonList('test'); list[0];";
    
    protected static final String TEST_STATIC_SOURCE_4 =
    	"Collections.singletonList('test')[0]";
    
    protected static final String TEST_STATIC_SOURCE_5 =
    	"org.teatrove.tea.templates.FunctionsTest.INSTANCE.getFunction().getGreeting()";
    
    protected static final String TEST_STATIC_SOURCE_6 =
    	"org.teatrove.tea.templates.FunctionsTest.INSTANCE.function.getGreeting()";
    
    protected static final String TEST_STATIC_SOURCE_7 =
    	"org.teatrove.tea.templates.FunctionsTest.INSTANCE.function.greeting";
    
    protected static final String TEST_STATIC_SOURCE_8 =
    	"org.teatrove.tea.templates.FunctionsTest.INSTANCE.function.greeting.length";
    
    protected static final String TEST_STATIC_SOURCE_9 =
    	"org.teatrove.tea.templates.FunctionsTest$Interface.GREETING";
    
    public static interface Interface {
    	public static final String GREETING = TEST_GREETING;
    	
        public String getGreeting();
    }

    public static abstract class Abstract {
        public int getHeight() { return TEST_HEIGHT; }
        public abstract int getWeight();
    }

    public static class Function extends Abstract implements Interface {
        public int getWeight() { return TEST_WEIGHT; }
        public String getGreeting() { return TEST_GREETING; }
        public Object getNull() { return null; }
        public double getDouble() { return TEST_DOUBLE; }
        public String[] getStrings() {
            return new String[] { TEST_STRING_1, TEST_STRING_2 };
        }
        public Map<String, String> getMaps() {
            return new HashMap<String, String>();
        }
        public List<String> getLists() {
            return Collections.singletonList(TEST_STRING_1);
        }
    }

    public static class FunctionsContext {
       public Function getFunction() {
           return new Function();
       }

       public List<Integer> getList() {
           return new ArrayList<Integer>();
       }

       public void add(List<Integer> list, Integer num) {
           list.add(num);
       }
    }
}
