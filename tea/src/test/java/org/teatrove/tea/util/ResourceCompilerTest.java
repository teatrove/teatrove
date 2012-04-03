package org.teatrove.tea.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.teatrove.tea.compiler.Compiler;
import org.teatrove.trove.util.ClassInjector;

public class ResourceCompilerTest {

    private Compiler compiler;
    private ResourceCompilationProvider provider;
    
    @Before
    public void init() {
        provider = new ResourceCompilationProvider();
        compiler = new Compiler(new ClassInjector());
        compiler.addCompilationProvider(provider);
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
