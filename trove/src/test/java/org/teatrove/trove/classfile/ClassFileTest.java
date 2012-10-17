package org.teatrove.trove.classfile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.Test;
import org.teatrove.trove.util.ClassInjector;


public class ClassFileTest {

    @Test
    public void testSimple() throws Exception {
        String testValue = "TEST";
        String methodName = "getName";
        String className = "org.teatrove.trove.test.Simple";
        
        ClassFile cf = new ClassFile(className);
        
        MethodInfo ctor = cf.addDefaultConstructor();
        CodeBuilder builder = new CodeBuilder(ctor);
        builder.loadThis();
        builder.invokeSuperConstructor();
        builder.returnVoid();
        
        Modifiers mods = new Modifiers(Modifier.PUBLIC);
        MethodInfo getName = cf.addMethod(mods, methodName, TypeDesc.STRING);
        builder = new CodeBuilder(getName);
        builder.loadConstant(testValue);
        builder.returnValue(TypeDesc.STRING);
        
        ClassInjector injector = ClassInjector.getInstance();
        OutputStream os = injector.getStream(className); 
        cf.writeTo(os);
        os.close();
        
        Class<?> clazz = injector.loadClass(className);
        assertEquals("expected class name", className, clazz.getName());
        
        Object instance = clazz.newInstance();
        Method method = clazz.getMethod(methodName);
        String result = (String) method.invoke(instance);
        assertEquals("expected test value", testValue, result);
    }
    
    @Test
    public void testAnnotations() throws Exception {
        String testValue = "TEST";
        String methodName = "getTest";
        String className = "org.teatrove.trove.test.Annotated";
        
        ClassFile cf = new ClassFile(className);
        Annotation ann = 
            cf.addRuntimeVisibleAnnotation(TypeDesc.forClass(TestAnnotation.class));
        ann.putMemberValue("value", testValue);

        MethodInfo ctor = cf.addDefaultConstructor();
        CodeBuilder builder = new CodeBuilder(ctor);
        builder.loadThis();
        builder.invokeSuperConstructor();
        builder.returnVoid();
        
        Modifiers mods = new Modifiers(Modifier.PUBLIC);
        MethodInfo getName = cf.addMethod(mods, methodName, TypeDesc.STRING);
        builder = new CodeBuilder(getName);
        builder.loadConstant(null);
        builder.returnValue(TypeDesc.STRING);
        
        getName.addRuntimeVisibleAnnotation(TypeDesc.forClass(Deprecated.class));
        
        ClassInjector injector = ClassInjector.getInstance();
        OutputStream os = injector.getStream(className); 
        cf.writeTo(os);
        os.close();
        
        Class<?> clazz = injector.loadClass(className);
        assertEquals("expected class name", className, clazz.getName());
        
        TestAnnotation annotation = clazz.getAnnotation(TestAnnotation.class);
        assertNotNull("expected annotation", annotation);
        assertEquals("expected test value", testValue, annotation.value());
        
        Method method = clazz.getMethod(methodName);
        assertNotNull("expected deprecated", method.getAnnotation(Deprecated.class));
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface TestAnnotation {
        String value();
    }
}
