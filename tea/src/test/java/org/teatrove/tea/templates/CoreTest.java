package org.teatrove.tea.templates;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.junit.Before;
import org.junit.Test;
import org.teatrove.tea.runtime.Truthful;

public class CoreTest extends AbstractTemplateTest {

    private Throwable error;
    
    @Before
    public void setup() {
        addContext("CoreApplication", new CoreContext());
    }
    
    @Test
    public void testScripts() throws Exception {
        
        // TODO: add static constant test cases
        
        int cpus = Runtime.getRuntime().availableProcessors();
        final Semaphore semaphore = new Semaphore(cpus);
        final ExecutorService executor = Executors.newFixedThreadPool(cpus);
        
        outer: for (int i = 1; i < TEST_CASES.length; i++) {
            final TestCase testCase = TEST_CASES[i];
            for (int j = 1; j < TEST_DATA.length; j++) {
                for (int k = 0; k < TEST_DATA.length; k++) {
                    final TestData a = TEST_DATA[j], b = TEST_DATA[k];
                    final TestRun testRun = new TestRun(a, b);
                    
                    final String message = 
                        "[" + i + "," + j + "," + k + "] " +
                        "Failed test condition '" + testCase + "' " + 
                        "with " + testRun;
                    
                    semaphore.acquire();
                    if (this.error != null) { break outer; }
                    
                    executor.submit(new Callable<Boolean>() {
                        public Boolean call() {
                            try { runTest(message, testCase, testRun); }
                            catch (Throwable e) { error = e; }
                            finally { semaphore.release(); }
                            
                            return Boolean.TRUE;
                        }
                    });
                }
            }
        }
        
        // throw any caught exception
        if (error != null) {
            throw new Exception("error processing test: " + error.getMessage(),
                                error);
        }
    }
    
    protected void runTest(String message, TestCase testCase, TestRun testRun)
        throws Throwable {
        
        String source = 
            "if (" + testCase.getCondition() + ") { " +
                "'" + THEN + "' " +
            "} " +
            "else { '" + ELSE + "' } " + 
            "if (" + testCase.getCondition() + ") { " +
                "'" + THEN + "' " +
            "}";

        TestData a = testRun.a, b = testRun.b;
        String signature = a.getType() + " a, " + b.getType() + " b";
        String result = executeSource(
            source, signature, a.getValue(), b.getValue()
        );

        Expectation expectation = testCase.getExpectation();
        String expected = (expectation.validate(testRun) ? THEN + THEN : ELSE);

        assertEquals(message, expected, result);
    }

    protected static final int A = 0x01;
    protected static final int B = 0x02;
    
    protected static final String THEN = "then";
    protected static final String ELSE = "else";

    protected static class TestData {
        private String type;
        private Object value;
        private boolean truth;
        private boolean nullable;
        
        public <T> TestData(Class<T> type, T value, boolean truth, 
                        boolean nullable) {
            this.type = type.getName();
            this.value = value;
            this.truth = truth;
            this.nullable = nullable;
        }

        public String getType() { return this.type; }
        public Object getValue() { return this.value; }
        public boolean isTrue() { return this.truth; }
        public boolean isNull() { return this.nullable; }
        
        public String toString() {
            return this.type + "(" + this.value + ")";
        }
    }
    
    protected static class TestRun {
        private TestData a;
        private TestData b;
        
        public TestRun(TestData a, TestData b) {
            this.a = a;
            this.b = b;
        }
        
        public boolean isValid(int type) {
            if (type == A) { return this.a.isTrue(); }
            else if (type == B) { return this.b.isTrue(); }
            else { throw new IllegalStateException("invalid type: " + type); }
        }
        
        public boolean isNull(int type) {
            if (type == A) { return this.a.isNull(); }
            else if (type == B) { return this.b.isNull(); }
            else { throw new IllegalStateException("invalid type: " + type); }
        }
        
        public String toString() { 
            return "a = " + this.a.toString() + ", b = " + this.b.toString();
        }
    }
    
    protected static class TestCase {
        
        private String condition;
        private Expectation expectation;
        
        public TestCase(String condition, Expectation expectation) {
            this.condition = condition;
            this.expectation = expectation;
        }
        
        public String getCondition() { return this.condition; }
        public Expectation getExpectation() { return this.expectation; }
        
        public String toString() {
            return this.condition;
        }
    }
    
    public static class TruthData implements Truthful {

        private boolean state;
        
        public TruthData(boolean state) {
            this.state = state;
        }
        
        @Override
        public boolean isTrue() {
            return this.state;
        }
        
        public String toString() {
            return "TruthData(" + this.state + ")";
        }
    }
    
    public static Expectation isTrue(final int type) {
        return new Expectation() {
            public boolean validate(TestRun testRun) {
                return testRun.isValid(type);
            }
        };
    }
    
    public static Expectation isNull(final int type) {
        return new Expectation() {
            public boolean validate(TestRun testRun) {
                return testRun.isNull(type);
            }
        };
    }
    
    public static Expectation not(final Expectation expectation) {
        return new Expectation() {
            public boolean validate(TestRun testRun) {
                return !expectation.validate(testRun);
            }
        };
    }
    
    public static Expectation or(final Expectation left,
                                 final Expectation right) {
        return new Expectation() {
            public boolean validate(TestRun testRun) {
                return left.validate(testRun) || right.validate(testRun);
            }
        };
    }
    
    public static Expectation and(final Expectation left,
                                  final Expectation right) {
        return new Expectation() {
            public boolean validate(TestRun testRun) {
                return left.validate(testRun) && right.validate(testRun);
            }
        };
    }
    
    protected static interface Expectation {
        boolean validate(TestRun testRun);
    }
    
    protected static final TestCase[] TEST_CASES = {
        new TestCase("a", isTrue(A)),
        new TestCase("not a", not(isTrue(A))),
        
        new TestCase("a or b", or(isTrue(A), isTrue(B))),
        new TestCase("not a or b", or(not(isTrue(A)), isTrue(B))),
        new TestCase("a or not b", or(isTrue(A), not(isTrue(B)))),
        new TestCase("not a or not b", or(not(isTrue(A)), not(isTrue(B)))),
        
        new TestCase("a and b", and(isTrue(A), isTrue(B))),
        new TestCase("not a and b", and(not(isTrue(A)), isTrue(B))),
        new TestCase("a and not b", and(isTrue(A), not(isTrue(B)))),
        new TestCase("not a and not b", and(not(isTrue(A)), not(isTrue(B)))),
        
        new TestCase("not (a or b)", not(or(isTrue(A), isTrue(B)))),
        new TestCase("not (a and b)", not(and(isTrue(A), isTrue(B)))),
        
        new TestCase("a == getNull()", isNull(A)),
        new TestCase("a != getNull()", not(isNull(A))),
        
        new TestCase("a == getNull() or b == getNull()", or(isNull(A), isNull(B))),
        new TestCase("a != getNull() or b == getNull()", or(not(isNull(A)), isNull(B))),
        new TestCase("a == getNull() or b != getNull()", or(isNull(A), not(isNull(B)))),
        new TestCase("a != getNull() or b != getNull()", or(not(isNull(A)), not(isNull(B)))),
        
        new TestCase("a == getNull() and b == getNull()", and(isNull(A), isNull(B))),
        new TestCase("a != getNull() and b == getNull()", and(not(isNull(A)), isNull(B))),
        new TestCase("a == getNull() and b != getNull()", and(isNull(A), not(isNull(B)))),
        new TestCase("a != getNull() and b != getNull()", and(not(isNull(A)), not(isNull(B)))),
        
        new TestCase("not (a == getNull() or b == getNull())", not(or(isNull(A), isNull(B)))),
        new TestCase("not (a == getNull() and b == getNull())", not(and(isNull(A), isNull(B)))),
        
        new TestCase("not (a != getNull() or b != getNull())", not(or(not(isNull(A)), not(isNull(B))))),
        new TestCase("not (a != getNull() and b != getNull())", not(and(not(isNull(A)), not(isNull(B))))),
        
        new TestCase("a == getNull() and b", and(isNull(A), isTrue(B))),
        new TestCase("a != getNull() or b", or(not(isNull(A)), isTrue(B))),
        new TestCase("a != getNull() and not b", and(not(isNull(A)), not(isTrue(B)))),
        new TestCase("a == getNull() or not b", or(isNull(A), not(isTrue(B))))
    };
    
    protected static final TestData[] TEST_DATA = {
        
        new TestData(Object.class, null, false, true),
        new TestData(Object.class, new Object(), true, false),
        
        new TestData(String.class, null, false, true),
        new TestData(String.class, "", false, false),
        new TestData(String.class, "test", true, false),
        
        new TestData(Integer.class, null, false, true),
        new TestData(Integer.class, Integer.valueOf(0), false, false),
        new TestData(Integer.class, Integer.valueOf(9), true, false),
        new TestData(Integer.class, Integer.valueOf(-1), true, false),
        
        new TestData(Number.class, null, false, true),
        new TestData(Number.class, Double.valueOf(0.0), false, false),
        new TestData(Number.class, Integer.valueOf(0), false, false),
        new TestData(Number.class, Double.valueOf(0.2), true, false),
        new TestData(Number.class, Integer.valueOf(2), true, false),
        
        new TestData(Double.class, null, false, true),
        new TestData(Double.class, Double.valueOf(0.0), false, false),
        new TestData(Double.class, Double.valueOf(0.3), true, false),
        new TestData(Double.class, Double.valueOf(-32.3), true, false),
        
        new TestData(Date.class, null, false, true),
        new TestData(Date.class, new Date(0), true, false),
        new TestData(Date.class, new Date(), true, false),

        new TestData(TruthData.class, null, false, true),
        new TestData(TruthData.class, new TruthData(false), false, false),
        new TestData(TruthData.class, new TruthData(true), true, false),
        
        new TestData(int.class, Integer.valueOf(0), false, false),
        new TestData(int.class, Integer.valueOf(-5), true, false),
        new TestData(int.class, Integer.valueOf(3), true, false),
        
        new TestData(double.class, Double.valueOf(0.0), false, false),
        new TestData(double.class, Double.valueOf(-2.3), true, false),
        new TestData(double.class, Double.valueOf(0.2), true, false),
    };
    
    public static class CoreContext {
        public Object getNull() { return null; }
    }
}
