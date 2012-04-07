package org.teatrove.trove.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.junit.Test;

public class MergedClassTest {
    
    // Test classes to test use generics within merged classes to make sure top
    // level generics flow all the way through
    
    public class Game { }
    public class Team { }
    public class GamePlayLog { }
    public class HockeyGamePlayLog extends GamePlayLog { }
    
    public static interface DAO<K, E> {
        public E findById(K key);
    }
    
    public static interface PlayDAO<T extends GamePlayLog> 
        extends DAO<Integer, T> {
        
        public T getPlay(int i);
        public List<T> getPlays();
    }
    
    public static interface HockeyPlayDAO<T extends HockeyGamePlayLog> 
        extends PlayDAO<T> { }
    
    public static interface GameDAO<T extends Game>
        extends DAO<Integer, T> { }
    
    public static interface TeamDAO<K, T extends Team>
        extends DAO<K, T> { }
    
    public static interface StringDAO extends DAO<String, String> { }
    
    public static class BaseContext<K, E> implements DAO<K, E> {
        public E findById(K id) { return null; }
    }
    
    // Test class that ensures the HockeyGamePlayLog flows all the way through
    // the merged class such that getPlay is implemented properly including any
    // needed bridge methods
    public static class PlayContext<T extends HockeyGamePlayLog>
        extends BaseContext<Integer, T>
        implements HockeyPlayDAO<T> {
        
        public T getPlay(int i) { return null; }
        public List<T> getPlays() { return null; }
    }
    
    public static class GameContext<T extends Game>
        extends BaseContext<Integer, T>
        implements GameDAO<T> { }
    
    public static class TeamContext<K, T extends Team>
        extends BaseContext<K, T>
        implements TeamDAO<K, T> { }
    
    public static class StringContext 
        extends BaseContext<String, String>
        implements StringDAO { }
    
    /**
     * Test case for TEATROVE-55.
     * 
     * https://github.com/teatrove/teatrove/issues/55
     */
    @Test
    public void testGetConstructorWithGenerics() throws Exception {
        
        ClassInjector injector = ClassInjector.getInstance();
        
        // build first constructor that purely merges general play context
        Constructor<?> ctor1 = MergedClass.getConstructor2
        (
            injector, 
            new Class[] { PlayContext.class, GameContext.class, StringContext.class }, 
            new String[] { "PlayContext$", "GameContext$", "StringContext$" }
        );
        
        // ensure two get play methods and both return hockey game
        int found1 = 0, bridge1 = 0;
        Method[] methods1 = ctor1.getDeclaringClass().getMethods();
        for (Method method1 : methods1) {
            if (method1.getName().equals("getPlay")) {
                found1++;
                if (method1.isBridge()) {
                    bridge1++;
                }
            }
        }
        
        assertEquals("expected two getPlay methods", 2, found1);
        assertEquals("expected one getPlay bridge method", 1, bridge1);
        
        // now build another constructor that merges that merge plus another
        // to ensure generics flow through from one merge to another
        Constructor<?> ctor2 = MergedClass.getConstructor2
        (
            ClassInjector.getInstance(), 
            new Class[] { ctor1.getDeclaringClass(), Dog.class }
        );
        
        // ensure two get play methods and both return hockey game
        int found2 = 0, bridge2 = 0;
        Method[] methods2 = ctor2.getDeclaringClass().getMethods();
        for (Method method2 : methods2) {
            if (method2.getName().equals("getPlay")) {
                found2++;
                if (method2.isBridge()) {
                    bridge2++;
                }
            }
        }
        
        assertEquals("expected two getPlay methods", 2, found1);
        assertEquals("expected one getPlay bridge method", 1, bridge1);
        
        // ensure get plays method has generic signature for list
        Method playsMethod = ctor2.getDeclaringClass().getMethod("getPlays");
        Type returnType = playsMethod.getGenericReturnType();
        assertTrue("expected param type", 
                   returnType instanceof ParameterizedType);

        assertEquals("expected plays type for type param",
                     HockeyGamePlayLog.class,
                     ((ParameterizedType) returnType).getActualTypeArguments()[0]);
    }
    
    public static class Pet<T extends Number> {
        public T getLegs() { return null; } 
    }
    
    // Test class that extends from Pet to ensure co-variant return types are
    // handled as bridge methods
    public static class Dog extends Pet<Integer> {
        public Integer getLegs() { return Integer.valueOf(1); }
    }
    
    // Test class that purposely does not extend from Pet in order to ensure
    // co-variant types are handled as non-bridged methods
    public static class Cat {
        public Double getLegs() { return Double.valueOf(2); }
    }
    
    @Test
    public void testGetConstructor() throws Exception {

        // generate ctor
        Constructor<?> ctor = MergedClass.getConstructor
        (
            ClassInjector.getInstance(), 
            new Class[] { Dog.class, Cat.class },
            new String[] { "Dog$", "Cat$" }
        );

        // validate ctor
        assertNotNull("invalid ctor", ctor);
        
        // validate get legs returns valid value
        Object instance = ctor.newInstance(new Dog(), new Cat());
        try {
            instance.getClass().getMethod("getLegs");
            fail("expected getLegs method to be invalid as it conflicts");
        }
        catch (NoSuchMethodException nsme) { /* valid */ }
        
        // validate Dog.getLegs
        Method dmethod = instance.getClass().getMethod("Dog$getLegs");
        Object dresult = dmethod.invoke(instance);
        assertEquals("invalid Dog.getLegs value", Integer.valueOf(1), dresult);

        // validate Cat.getLegs
        Method cmethod = instance.getClass().getMethod("Cat$getLegs");
        Object cresult = cmethod.invoke(instance);
        assertEquals("invalid Cat.getLegs value", Double.valueOf(2.0), cresult);
        
        // validate equals methods
        assertFalse("invalid equals(null)", instance.equals(null));
        assertTrue("invalid equality to itself", instance.equals(instance));
        assertFalse("expected non-empty string", instance.toString().isEmpty());
    }
}
