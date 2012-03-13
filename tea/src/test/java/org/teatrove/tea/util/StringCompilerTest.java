package org.teatrove.tea.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.teatrove.trove.util.ClassInjector;

public class StringCompilerTest {

    private StringCompiler compiler;
    
    @Before
    public void init() {
        compiler = new StringCompiler(new ClassInjector());
        compiler.setTemplateSource("test", "<% template test() 'test' %>");
        compiler.setTemplateSource("abc", "<% template abc() 'abc' %>");        
    }
    
    @Test
    public void testSourceExists() {
        assertTrue(compiler.sourceExists("test"));
        assertTrue(compiler.sourceExists("abc"));
        assertFalse(compiler.sourceExists("blah"));
    }

    @Test
    public void testGetAllTemplateNames() throws IOException {
        String[] names = compiler.getAllTemplateNames();
        assertEquals(2, names.length);
    }

    @Test
    public void testCompile() throws IOException {
        String[] results = compiler.compileAll();
        assertEquals(2, results.length);
    }

}
