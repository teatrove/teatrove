package org.teatrove.tea;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.teatrove.tea.compiler.ErrorEvent;
import org.teatrove.tea.compiler.ErrorListener;
import org.teatrove.tea.engine.ContextSource;
import org.teatrove.tea.engine.MergedContextSource;
import org.teatrove.tea.runtime.Context;
import org.teatrove.tea.util.FileCompiler;
import org.teatrove.tea.util.StringCompiler;
import org.teatrove.tea.util.TestCompiler;
import org.teatrove.trove.util.ClassInjector;

public abstract class AbstractTemplateTest {

    protected static final String DEST = "target/templates";
    protected static final String PKG = "org.teatrove.tea.templates";

    protected ClassInjector injector;
    protected Map<String, ContextSource> contexts;
    protected Object context;
    protected ByteArrayOutputStream output;
    protected int counter;

    public AbstractTemplateTest() {
        output = new ByteArrayOutputStream(1024);
        contexts = new HashMap<String, ContextSource>();
        addContext("DefaultApplication",
        		   new TestCompiler.Context(new PrintStream(output)));
    }

    public void addContext(final String name, final Object context) {
        contexts.put(name.concat("$"), new ContextSource() {

            @Override
            public Class<?> getContextType() throws Exception {
                return context.getClass();
            }

            @Override
            public Object createContext(Object param) throws Exception {
                return context;
            }
        });
    }

    public Object getContext() {
        if (context == null) {
            try { context = createContext(); }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        return context;
    }

    protected Object createContext() throws Exception {
        // setup merged context
        MergedContextSource source = new MergedContextSource();
        source.init
        (
            Thread.currentThread().getContextClassLoader(),
            contexts.values().toArray(new ContextSource[contexts.size()]),
            contexts.keySet().toArray(new String[contexts.size()]),
            false
        );

        return source.createContext(null);
    }

    protected ClassInjector getInjector() {
        if (injector == null) {
            try { injector = createInjector(); }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        return injector;
    }

    protected ClassInjector createInjector() throws Exception {
        return ClassInjector.getInstance(getContext().getClass().getClassLoader());
        /*
        (
            new URLClassLoader
            (
                new URL[] { new File(DEST).toURI().toURL() },
                getContext().getClass().getClassLoader()
            )
        );
        */
    }

    public void compileFiles(String... templates)
        throws Exception {

        for (String template : templates) {
            compileFile(template);
        }
    }

    public void compile(String template, String source)
        throws Exception {

        // create target directory
        File target = new File(DEST + '/' + PKG.replace('.', '/'));
        target.mkdirs();

        // clean template class
        new File(target, template.replace('.', '/').concat(".class")).delete();

        // create compiler
        StringCompiler compiler = new StringCompiler(getInjector(), PKG);

        // setup context
        compiler.setRuntimeContext(getContext().getClass());

        // setup error handler
        compiler.addErrorListener(new ErrorListener() {
            @Override
            public void compileError(ErrorEvent e) {
                System.err.println(e.getDetailedErrorMessage());
            }
        });

        // add sources for templates
        compiler.setTemplateSource(template, source);

        // compile templates
        String[] results = compiler.compile(template);
        if (results == null || results.length < 1) {
            throw new IllegalStateException("unable to compile");
        }
    }

    public void compileFile(String template) throws Exception {
        // create target directory
        File target = new File(DEST + '/' + PKG.replace('.', '/'));
        target.mkdirs();

        // clean template class
        new File(target, template.replace('.', '/').concat(".class")).delete();

        // create compiler
        FileCompiler compiler = new FileCompiler
        (
            new File("src/tests/templates"), PKG, target, getInjector()
        );

        // setup context
        compiler.setRuntimeContext(getContext().getClass());

        // setup error handler
        compiler.addErrorListener(new ErrorListener() {
            @Override
            public void compileError(ErrorEvent e) {
                System.err.println(e.getDetailedErrorMessage());
            }
        });

        // compile templates
        String[] results = compiler.compile(template);
        if (results == null || results.length < 1) {
            throw new IllegalStateException("unable to compile");
        }
    }

    public String executeFile(String template, Object... params)
        throws Exception {

        // compile and execute
        compileFile(template);
        return _execute(template, params);
    }

    public String getTemplateName() {
        return this.getClass().getSimpleName().toLowerCase() + counter;
    }

    public String getTemplateSource(String source) {
        return getTemplateSource(source, "");
    }

    public String getTemplateSource(String source, String signature) {
        return "<% template " + getTemplateName() +
            "(" + signature + ") " + source;
    }

    public String executeSource(String source, Object... params)
        throws Exception {

        counter++;
        return execute(getTemplateName(), getTemplateSource(source), params);
    }

    public String executeSource(String source, String signature,
                                Object... params)
        throws Exception {

        counter++;
        return execute(getTemplateName(), getTemplateSource(source, signature),
                       params);
    }

    public String execute(String template, String source, Object... params)
        throws Exception {

        // compile and execute
        compile(template, source);
        return _execute(template, params);
    }

    protected String _execute(String template, Object... params)
        throws Exception {

        // get class loader
        // ClassLoader loader = getInjector();
        ClassLoader loader = new URLClassLoader
        (
            new URL[] { new File(DEST).toURI().toURL() },
            //getContext().getClass().getClassLoader()
            getInjector()
        );

        // load template class
        Class<?> clazz = loader.loadClass(PKG + '.' + template);
        if (clazz == null) {
            throw new IllegalStateException("unable to load class");
        }

        // lookup execute methd
        Method execute = null;
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals("execute")) {
                execute = method;
                break;
            }
        }

        // verify execute method
        if (execute == null) {
            throw new IllegalStateException("unable to find execute method");
        }

        // setup params
        Object[] args = new Object[params.length + 1];
        args[0] = getContext();
        for (int i = 0; i < params.length; i++) {
            args[i + 1] = params[i];
        }

        // execute template
        output.reset();
        Object result = execute.invoke(null, args);
        if (!void.class.equals(execute.getReturnType())) {
            ((Context) getContext()).print(result);
        }

        // print code
        String outcome = output.toString();
        System.out.println("template " + template + ": " + outcome);

        // return code
        return outcome;
    }
}
