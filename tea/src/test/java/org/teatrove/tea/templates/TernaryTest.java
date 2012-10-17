package org.teatrove.tea.templates;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.teatrove.tea.runtime.Truthful;


public class TernaryTest extends AbstractTemplateTest {

    @Before
    public void setup() {
        addContext("TernaryApplication", new TernaryContext());
    }
    
    @Test
    public void testTernary() throws Exception {
        for (String[] test : TEST_SOURCES) {
            String source = test[0];
            String expected = test[1];
            String result = executeSource(source);
            assertEquals("invalid result: " + source, expected, result);
        }
    }

    protected static final String[][] TEST_SOURCES = {
        // boolean tests
        { "a = false; a ? 'true' : 'false'", "false" },
        { "a = false; not a ? 'true' : 'false'", "true" },
        { "a = true; a ? 'true' : 'false'", "true" },
        { "a = true; not a ? 'true' : 'false'", "false" },
        
        { "a = true; if (a) { 'valid' }", "valid" },
        { "a = false; if (a) { 'valid' }", "" },
        { "a = true; if (not a) { 'valid' }", "" },
        { "a = false; if (not a) { 'valid' }", "valid" },
       
        { "a = true; if (a == true) { 'valid' }", "valid" },
        { "a = false; if (a == true) { 'valid' }", "" },
        { "a = true; if (a != true) { 'valid' }", "" },
        { "a = false; if (a != true) { 'valid' }", "valid" },
        
        { "a = true; if (not a == true) { 'valid' }", "" },
        { "a = false; if (not a == true) { 'valid' }", "valid" },
        { "a = true; if (not a != true) { 'valid' }", "valid" },
        { "a = false; if (not a != true) { 'valid' }", "" },
        
        { "a = true; a ?: false", "true" },
        { "a = false; a ?: true", "true" },
        
        // numbers (primitives)
        { "b = 5; (b ? 'valid' : 'invalid')", "valid" },
        { "b = 5; (not b ? 'valid' : 'invalid')", "invalid" },
        { "b = 0; (b ? 'valid' : 'invalid')", "invalid" },
        { "b = 0; (not b ? 'valid' : 'invalid')", "valid" },
        { "b = -1; (b ? 'valid' : 'invalid')", "valid" },
        { "b = 0.3; (b ? 'valid' : 'invalid')", "valid" },
        { "b = 0.0; (b ? 'valid' : 'invalid')", "invalid" },
        { "b = 0.0; (not b ? 'valid' : 'invalid')", "valid" },
        { "b = 5; (b ? 3 : 5)", "3" },
        { "b = 0; (b ? 3 : 9.3)", "9.3" },
        { "b = 5; (b ? 4 : 8.1)", "4.0" },
        
        { "b = 5; if (b) { 'valid' }", "valid" },
        { "b = 5; if (not b) { 'valid' }", "" },
        { "b = 0; if (b) { 'valid' }", "" },
        { "b = 0; if (not b) { 'valid' }", "valid" },
        { "b = -1; if (b) { 'valid' }", "valid" },
        { "b = 0.3; if (b) { 'valid' }", "valid" },
        { "b = 0.0; if (b) { 'valid' }", "" },
        
        { "b = 5; b ?: -1", "5" },
        { "b = 0; b ?: -1", "-1" },
        { "b = -1; b ?: 5L", "-1" },
        { "b = 0; b ?: 5L", "5" },
        { "b = 0; b ?: 5.3", "5.3" },
        { "b = 3; b ?: 2.1", "3.0" },
        { "b = 3.9; b ?: 2.1", "3.9" },
        { "b = 3.9; b ?: 2L", "3.9" },
        { "b = 0.0; b ?: 2.1", "2.1" },
        { "b = 0.0; b ?: 2", "2.0" },
        { "b = 5; b ?: 'invalid'", "5" },
        { "b = 0; b ?: 'invalid'", "invalid" },

        // numbers (wrappers)
        { "b = getWrapper(null); (b ? 'valid' : 'invalid')", "invalid" },
        { "b = getWrapper(5); (b ? 'valid' : 'invalid')", "valid" },
        { "b = getWrapper(5); (not b ? 'valid' : 'invalid')", "invalid" },
        { "b = getWrapper(0); (b ? 'valid' : 'invalid')", "invalid" },
        { "b = getWrapper(0); (not b ? 'valid' : 'invalid')", "valid" },
        { "b = getWrapper(-1); (b ? 'valid' : 'invalid')", "valid" },
        { "b = getWrapper(0.3); (b ? 'valid' : 'invalid')", "valid" },
        { "b = getWrapper(0.0); (b ? 'valid' : 'invalid')", "invalid" },
        { "b = getWrapper(0.0); (not b ? 'valid' : 'invalid')", "valid" },
        { "b = getWrapper(null); if (b) { 'valid' }", "" },
        { "b = getWrapper(5); if (b) { 'valid' }", "valid" },
        { "b = getWrapper(5); if (not b) { 'valid' }", "" },
        { "b = getWrapper(0); if (b) { 'valid' }", "" },
        { "b = getWrapper(0); if (not b) { 'valid' }", "valid" },
        { "b = getWrapper(-1); if (b) { 'valid' }", "valid" },
        { "b = getWrapper(0.3); if (b) { 'valid' }", "valid" },
        { "b = getWrapper(0.0); if (b) { 'valid' }", "" },
        
        { "b = getWrapper(null); b ?: -1", "-1" },
        { "b = getWrapper(5); b ?: getWrapper(-1)", "5" },
        { "b = getWrapper(0); b ?: -1", "-1" },
        { "b = getWrapper(-1); b ?: 5L", "-1" },
        { "b = getWrapper(0); b ?: 5L", "5" },
        { "b = getWrapper(0); b ?: getWrapper(5.3)", "5.3" },
        { "b = getWrapper(3); b ?: 2.1", "3.0" },
        { "b = getWrapper(3.9); b ?: 2.1", "3.9" },
        { "b = getWrapper(3.9); b ?: 2L", "3.9" },
        { "b = getWrapper(0.0); b ?: getWrapper(2.1)", "2.1" },
        { "b = getWrapper(0.0); b ?: getWrapper(2)", "2.0" },
        { "b = getWrapper(5); b ?: 'invalid'", "5" },
        { "b = getWrapper(0); b ?: 'invalid'", "invalid" },

        // numbers (general)
        { "b = getNumber(null); (b ? 'valid' : 'invalid')", "invalid" },
        { "b = getNumber(5); (b ? 'valid' : 'invalid')", "valid" },
        { "b = getNumber(5); (not b ? 'valid' : 'invalid')", "invalid" },
        { "b = getNumber(0); (b ? 'valid' : 'invalid')", "invalid" },
        { "b = getNumber(0); (not b ? 'valid' : 'invalid')", "valid" },
        { "b = getNumber(-1); (b ? 'valid' : 'invalid')", "valid" },
        { "b = getNumber(0.3); (b ? 'valid' : 'invalid')", "valid" },
        { "b = getNumber(0.0); (b ? 'valid' : 'invalid')", "invalid" },
        { "b = getNumber(0.0); (not b ? 'valid' : 'invalid')", "valid" },
     
        { "b = getNumber(null); if (b) { 'valid' }", "" },
        { "b = getNumber(5); if (b) { 'valid' }", "valid" },
        { "b = getNumber(5); if (not b) { 'valid' }", "" },
        { "b = getNumber(0); if (b) { 'valid' }", "" },
        { "b = getNumber(0); if (not b) { 'valid' }", "valid" },
        { "b = getNumber(-1); if (b) { 'valid' }", "valid" },
        { "b = getNumber(0.3); if (b) { 'valid' }", "valid" },
        { "b = getNumber(0.0); if (b) { 'valid' }", "" },
        
        { "b = getNumber(null); b ?: -1", "-1" },
        { "b = getNumber(5); b ?: getNumber(-1)", "5" },
        { "b = getNumber(5); b ?: getWrapper(-1)", "5" },
        { "b = getNumber(0); b ?: -1", "-1" },
        { "b = getNumber(-1); b ?: 5L", "-1" },
        { "b = getNumber(0); b ?: 5L", "5" },
        { "b = getNumber(0); b ?: getNumber(5.3)", "5.3" },
        { "b = getNumber(0); b ?: getWrapper(5.3)", "5.3" },

        { "b = getNumber(3); b ?: 2.1", "3.0" },
        { "b = getNumber(3.9); b ?: 2.1", "3.9" },
        { "b = getNumber(3.9); b ?: 2L", "3.9" },
        { "b = getNumber(0.0); b ?: getNumber(2.1)", "2.1" },
        { "b = getNumber(0.0); b ?: getWrapper(2.1)", "2.1" },
        { "b = getNumber(0.0); b ?: getNumber(2)", "2" },
        { "b = getNumber(0.0); b ?: getWrapper(2)", "2" },
        { "b = getNumber(5); b ?: 'invalid'", "5" },
        { "b = getNumber(0); b ?: 'invalid'", "invalid" },
        
        // arrays
        { "c = #(1, 2, 3); (c ? 'data' : 'nodata')", "data" },
        { "c = #(); (c ? 'data' : 'nodata')", "nodata" },
        { "c = #(1, 2, 3); (not c ? 'data' : 'nodata')", "nodata" },
        { "c = #(); (not c ? 'data' : 'nodata')", "data" },
        
        { "a = #(1, 3, 5); if (a) { 'valid' }", "valid" },
        { "a = #(); if (a) { 'valid' }", "" },
        { "a = #(1, 3, 5); if (not a) { 'valid' }", "" },
        { "a = #(); if (not a) { 'valid' }", "valid" },
       
        // TODO: need to add native array, map, list, set, string, number comparison
        // { "a = #(1, 2, 3); if (a == #(1, 2, 3)) { 'valid' }", "valid" },
        // { "a = #(); if (a == #(2, 3)) { 'valid' }", "" },
        
        { "a = #(1, 3, 5); foreach (b in (a ?: #(2))) { b }", "135" },
        { "a = #(); c = a ?: #(getWrapper(5), getWrapper(3)); foreach (b in c) { b }", "53" },
        
        // maps
        { "c = ##('a', 1, 'b', 3); (c ? 'data' : 'nodata')", "data" },
        { "c = ##(); (c ? 'data' : 'nodata')", "nodata" },
        { "c = ##('a', '2', 'b', '3'); (not c ? 'data' : 'nodata')", "nodata" },
        { "c = ##(); (not c ? 'data' : 'nodata')", "data" },
        
        { "a = ##('a', '2', 'b', '3'); if (a) { 'valid' }", "valid" },
        { "a = #(); if (a) { 'valid' }", "" },
        { "a = ##('a', '2', 'b', '3'); if (not a) { 'valid' }", "" },
        { "a = #(); if (not a) { 'valid' }", "valid" },
       
        // TODO: need to add native array, map, list, set, string, number comparison
        //{ "a = ##('a', '2', 'b', '3'); if (a == ##('a', '2', 'b', '3')) { 'valid' }", "valid" },
        //{ "a = ##(); if (a == ##('key', 'value')) { 'valid' }", "" },

        { "a = ##('a', '2', 'b', '3'); foreach (b in (a ?: ##('a', 2)).keySet()) { b }", "ab" },
        { "a = ##(); c = a ?: ##('key', 'value'); foreach (b in c.values()) { b }", "value" },

        // strings
        { "a = ''; a ? 'true' : 'false'", "false" },
        { "a = ''; not a ? 'true' : 'false'", "true" },
        { "a = 'valid'; a ? 'true' : 'false'", "true" },
        { "a = 'valid'; not a ? 'true' : 'false'", "false" },
        { "a = 'valid'; a == 'valid' ? 'true' : 'false'", "true" },
        { "a = 'valid'; a == 'invalid' ? 'true' : 'false'", "false" },
        
        { "a = 'valid'; if (a) { 'valid' }", "valid" },
        { "a = ''; if (a) { 'valid' }", "" },
        { "a = 'valid'; if (not a) { 'valid' }", "" },
        { "a = ''; if (not a) { 'valid' }", "valid" },
       
        { "a = 'valid'; if (a == 'valid') { 'valid' }", "valid" },
        { "a = ''; if (a == 'valid') { 'valid' }", "" },
        { "a = 'valid'; if (a != 'valid') { 'valid' }", "" },
        { "a = ''; if (a != 'valid') { 'valid' }", "valid" },
        
        { "a = 'valid'; a ?: 'default'", "valid" },
        { "a = ''; a ?: 'default'", "default" },
        { "a = ''; a ?: 6", "6" },

        // truthful
        { "a = getTruthful(false); a ? 'true' : 'false'", "false" },
        { "a = getTruthful(false); not a ? 'true' : 'false'", "true" },
        { "a = getTruthful(true); a ? 'true' : 'false'", "true" },
        { "a = getTruthful(true); not a ? 'true' : 'false'", "false" },
        
        { "a = getTruthful(true); if (a) { 'valid' }", "valid" },
        { "a = getTruthful(false); if (a) { 'valid' }", "" },
        { "a = getTruthful(true); if (not a) { 'valid' }", "" },
        { "a = getTruthful(false); if (not a) { 'valid' }", "valid" },
        
        { "a = getTruthful(true); a ?: getTruthful(false)", "TRUE" },
        { "a = getTruthful(false); a ?: getTruthful(true)", "TRUE" },

        // non-truthful
        { "a = getObject(null); a ? 'true' : 'false'", "false" },
        { "a = getObject(null); not a ? 'true' : 'false'", "true" },
        { "a = getObject('valid'); a ? 'true' : 'false'", "true" },
        { "a = getObject('valid'); not a ? 'true' : 'false'", "false" },
        
        { "a = getObject('valid'); if (a) { 'valid' }", "valid" },
        { "a = getObject(null); if (a) { 'valid' }", "" },
        { "a = getObject('valid'); if (not a) { 'valid' }", "" },
        { "a = getObject(null); if (not a) { 'valid' }", "valid" },
        
        { "a = getObject('valid'); a ?: 'FALSE'", "valid" },
        { "a = getObject(null); a ?: 'FALSE'", "FALSE" },

        // nullable
        { "k = null; k ?: 'valid'", "valid" },
        { "f = null; (f ? 'nonnull' : 'null')", "null" },
        { "f = null; g = f == null ? #('default') : #(f);" +
              "foreach (g1 in g) { g1 }", 
          "default"
        },
        
        // compatibility checks
        { "g = #('default');" +
              "h1 = null as HashSet<String>;" +
              "h2 = null as ArrayList<String>;" +
              "h = (g == null ? h1 : h2);" +
              "h", 
           "null" 
        },

        // complex checks
        { "l = (5 ? ('a' ? 'a5' : 'nona5') : 'b' ? 'b?' : '?'); l", "a5" },
        { "n = 5; a = 'a'; b = 'b'; l = (a ? (a ? 'a5' : 'nona5') : b ? 'b?' : '?'); l", "a5" },
        { "m = 5; m = 'test'; (m isa String ? m.length : 0)", "4" },
        { "f = null; f?.toString()?.length() == 0", "true" },
        { "f = ''; if (not f) { f = null; }; f?.toString()?.length() ?: 5", "5" },

        // multiple expressions
        { "a = 5; a = 10; b = null; b = 'test'; if (a and b) { 'valid' }", "valid" },
        { "a = 5; a = 0; b = null; b = ''; ; if (a and b) { 'valid' }", "" },
        { "a = 5; a = 10; b = null; b = 'test'; (a and b ? 'valid' : 'invalid')", "valid" },
        { "a = 5; a = 10; b = null; b = 'test'; if (a or b) { 'valid' }", "valid" },
        { "a = getObject('test'); b = getTruthful(false); (a and not b ? 'valid' : 'invalid')", "valid" },
        { "a = getObject('test'); b = getTruthful(true); if (a and not b) { 'valid' } else { 'invalid' }", "invalid" }
    };
    
    public static class TernaryContext {
        public Object getObject(Object value) { return value; }
        public TruthObject getTruthful(boolean state) {
            return new TruthObject(state);
        }
        
        public Integer getWrapper(int value) { return Integer.valueOf(value); }
        public Long getWrapper(long value) { return Long.valueOf(value); }
        public Double getWrapper(double value) { return Double.valueOf(value); }
        public Integer getWrapper(Object value) { return null; }
        
        public Number getNumber(int value) { return Integer.valueOf(value); }
        public Number getNumber(long value) { return Long.valueOf(value); }
        public Number getNumber(double value) { return Double.valueOf(value); }
        public Number getNumber(Object value) { return null; }
    }
    
    public static class TruthObject implements Truthful {
        private boolean state;
        
        public TruthObject(boolean state) {
            this.state = state;
        }
        
        public boolean isTrue() {
            return this.state;
        }
        
        @Override
        public String toString() {
            return this.state ? "TRUE" : "FALSE";
        }
    }
}
