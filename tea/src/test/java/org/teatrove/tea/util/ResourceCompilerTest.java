package org.teatrove.tea.util;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.teatrove.trove.util.ClassInjector;

public class ResourceCompilerTest {

    private ResourceCompiler compiler;
    
    @Before
    public void init() {
        compiler = new ResourceCompiler(new ClassInjector());
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
        assertEquals(0, names.length);
    }

    @Test
    public void testCompile() throws IOException {
        String[] results = compiler.compile(new String[] { "test", "abc" });
        assertEquals(2, results.length);
    }

}
