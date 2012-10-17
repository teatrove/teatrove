package org.teatrove.tea.templates;

import static org.junit.Assert.assertEquals;

import org.junit.Test;



public class TypeTest extends AbstractTemplateTest {

    @Test
    public void testValues() throws Exception {
        assertEquals(String.class.getName(), executeSource(TEST_SOURCE_1));
        assertEquals(String.class.getName(), executeSource(TEST_SOURCE_2));
        assertEquals(String.class.getName(), executeSource(TEST_SOURCE_3));
        assertEquals("1", executeSource(TEST_SOURCE_4));
    }

    protected static final String TEST_SOURCE_1 =
        "test = 'test'; test.class.name";

    protected static final String TEST_SOURCE_2 =
        "String.class.name";
    
    protected static final String TEST_SOURCE_3 =
        "java.lang.String.class.name";
    
    protected static final String TEST_SOURCE_4 =
        "java.lang.String.valueOf(1)";
}
