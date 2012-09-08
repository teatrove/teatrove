package org.teatrove.tea.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.teatrove.tea.compiler.CompilationUnit;

public class TemplateCompilationResultsTest {

    @Test
    public void testSerialization()
        throws Exception {
        
        // setup data
        Map<String, CompilationUnit> templates =
            new HashMap<String, CompilationUnit>();
        templates.put("test1", null);
        templates.put("test2", null);
        templates.put("test3", null);
        
        // setup results
        TemplateCompilationResults results =
            new TemplateCompilationResults(templates, null);
        
        // validate data
        Set<String> names = results.getReloadedTemplateNames();
        assertTrue(names.contains("test1"));
        assertTrue(names.contains("test2"));
        assertTrue(names.contains("test3"));
        assertEquals(3, results.getReloadedTemplates().size());
        
        // serialize and validate valid
        results = serialize(results); 
        names = results.getReloadedTemplateNames();
        assertTrue(names.contains("test1"));
        assertTrue(names.contains("test2"));
        assertTrue(names.contains("test3"));
        assertNull(results.getReloadedTemplates());
    }
    
    @SuppressWarnings("unchecked")
    protected <T> T serialize(T object) 
        throws Exception {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        ObjectOutputStream output = new ObjectOutputStream(baos);
        output.writeObject(object);
        output.close();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream input = new ObjectInputStream(bais);
        Object result = input.readObject();
        input.close();
        
        return (T) result;
    }

}
