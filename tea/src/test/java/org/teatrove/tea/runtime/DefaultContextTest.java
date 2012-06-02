package org.teatrove.tea.runtime;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class DefaultContextTest {

    private DefaultContext _context;
    
    @Before
    public void initialize() {
        this._context = new DefaultContext() {
            @Override
            public void print(Object obj) throws Exception {
                System.out.println(toString(obj));
            }
        };
    }
    
    @Test
    public void testDump() {
        dump("null", null);
        dump("string", "test");
        dump("date", new Date());
        dump("long", Long.valueOf(3));
        dump("integer", Integer.valueOf(5));
        dump("double", Double.valueOf(235.23));
        dump("simple", new DumperSimple());
        dump("inheritance", new DumperInheritance());
        dump("advanced", new DumperAdvanced());
        dump("composite", new DumperComposite());
    }

    protected void dump(String type, Object value) {
        dump(type, value, false, false);
        dump(type, value, false, true);
        dump(type, value, true, false);
        dump(type, value, true, true);
    }
    
    protected void dump(String type, Object value, 
                        boolean recursive, boolean format) {

        System.out.println("===== [ " + type + ", " + recursive + ", " + format + " ] =====");
        
        String result = this._context.dump(value, recursive, format);
        System.out.println(result);
        assertNotNull(result);
    }
    
    public static class DumperSimple {
        public String getName() { return "simple"; }
        public int getAge() { return 5; }
        public double getStats() { return 5.2; }
    }
    
    public static class DumperInheritance extends DumperSimple {
        public Date getTimestamp() { return new Date(); }
    }
    
    public static class DumperAdvanced {
        public String getName() { return "advanced"; }
        public List<String> getItems() { return Arrays.asList("a", "b", "c"); }
        public int[] getValues() { return new int[] { 1, 2, 3 }; }
        public Class<?> getType() { return DumperAdvanced.class; }
        public Map<String, Double> getMap() {
            Map<String, Double> map = new HashMap<String, Double>();
            map.put("key1", Double.valueOf(2.3));
            map.put("key2", Double.valueOf(3.2));
            return map;
        }
    }
    
    public static class DumperComposite {
        public String getName() { return "composite"; }
        public Composite getComposite() { return new Composite(); }
        public Composite getException() { throw new IllegalStateException("ack"); }
    }
    
    public static class Composite {
        public String getType() { return "composite"; }
        public Graph getGraph() { return new Graph(); }
        public String toString() { return "[composite]"; }
    }
    
    public static class Graph {
        public Integer getState() { return Integer.valueOf(5); }
        public Graph getGraph() { return null; }
        public Graph getRecursive() { return this; }
        public String getType() { return "none"; }
    }
}
