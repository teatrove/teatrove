package org.teatrove.trove.util.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.net.URL;

import org.junit.Test;
import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.SubstitutionFactory;

public class DefaultResourceFactoryTest {
    
    @Test
    public void testGetInstance() {
        ResourceFactory factory1 = DefaultResourceFactory.getInstance();
        ResourceFactory factory2 = DefaultResourceFactory.getInstance();
        assertNotNull("expected valid factory", factory1);
        assertSame("expected singleton instance", factory1, factory2);
    }
    
    @Test
    public void testGetResourceFile() throws Exception {
        // create temp file
        File file = File.createTempFile("DefaultResourceFactoryTest", ".xml");
        file.deleteOnExit();
        
        // retrieve the file w/ file prefix
        ResourceFactory factory = DefaultResourceFactory.getInstance();
        URL url = factory.getResource("file:".concat(file.getAbsolutePath()));
        assertNotNull("expected valid resource", url);
        
        // retrieve the file w/o file prefix
        url = factory.getResource(file.getAbsolutePath());
        assertNotNull("expected valid resource", url);
    }

    @Test
    public void testGetResourceClasspath() throws Exception {
        // retrieve the classpath data w/ classpath prefix
        ResourceFactory factory = DefaultResourceFactory.getInstance();
        URL url = factory.getResource(
            "classpath:org/teatrove/trove/util/resources/".concat(
                "DefaultResourceFactoryTest.properties"
            )
        );
        
        assertNotNull("expected valid resource", url);
        
        // retrieve the classpath data w/ classpath prefix and leading /
        url = factory.getResource(
            "classpath:/org/teatrove/trove/util/resources/".concat(
                "DefaultResourceFactoryTest.properties"
            )
        );
        
        assertNotNull("expected valid resource", url);
        
        // retrieve the classpath data w/o classpath prefix
        url = factory.getResource(
            "org/teatrove/trove/util/resources/".concat(
                 "DefaultResourceFactoryTest.properties"
            )
        );
                                  
        assertNotNull("expected valid resource", url);
        
        // retrieve the classpath data w/o classpath prefix and leading /
        url = factory.getResource(
            "/org/teatrove/trove/util/resources/".concat(
                 "DefaultResourceFactoryTest.properties"
            )
        );
                                  
        assertNotNull("expected valid resource", url);
    }

    @Test
    public void testGetPropertiesClasspath() throws Exception {
        System.setProperty("substitution", "TEST");
        ResourceFactory factory = 
            new DefaultResourceFactory(SubstitutionFactory.getDefaults());
        PropertyMap properties = factory.getResourceAsProperties(
            "classpath:/org/teatrove/trove/util/resources/".concat(
                "DefaultResourceFactoryTest.properties"
            )
        );
        
        // verify properties
        assertEquals("expected classpath type property", 
                     "classpath", properties.getString("type"));
        
        assertEquals("expected substitution properties",
                     "TEST", properties.getString("substitution"));
        
        assertEquals("expected default properties",
                     "VALID", properties.getString("invalid"));
    }
    
    @Test
    public void testGetResourceUrl() throws Exception {
        
        // retrieve the classpath data
        ResourceFactory factory = DefaultResourceFactory.getInstance();
        URL url = factory.getResource("http://localhost:12345/test.properties");
        assertNotNull("expected valid resource", url);
    }
}
