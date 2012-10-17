package org.teatrove.tea.templates;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class WarningTest extends AbstractTemplateTest {

    @Before
    public void setup() {
        addContext("WarningApplication", new WarningContext());
    }

    /* DISABLE as this check happens often enough and should not be a warning
     * 
    @Test
    public void testTruthfulWarnings() throws Exception {
        String source = 
            "obj = createObject(null); " +
            "if (obj) { 'valid' } " +
            "else { 'invalid' }";
        
        adddMockListener(0, 1);
        assertEquals("invalid", executeSource(source));
    }
    */
    
    @Test
    public void testCompareWarnings() throws Exception {
        String source =
            "obj1 = createObject('2'); " +
            "obj2 = createInteger(5); " +
            "result = obj1 <=> obj2; " +
            "if (result < 0) { 'valid' }";
        
        adddMockListener(0, 1);
        assertEquals("valid", executeSource(source));
    }
    
    public static class WarningContext {
        public Object createObject(Object value) {
            return value;
        }
        
        public Integer createInteger(Integer value) {
            return value;
        }
    }
}
