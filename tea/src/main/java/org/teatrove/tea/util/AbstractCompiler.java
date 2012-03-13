/*
 *  Copyright 1997-2011 teatrove.org

 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teatrove.tea.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.teatrove.tea.compiler.CompilationUnit;
import org.teatrove.tea.compiler.Compiler;
import org.teatrove.tea.compiler.TemplateRepository;
import org.teatrove.tea.parsetree.Template;
import org.teatrove.trove.io.DualOutput;
import org.teatrove.trove.util.ClassInjector;

public abstract class AbstractCompiler extends Compiler {

    protected String mRootPackage;
    protected File mRootDestDir;
    protected ClassInjector mInjector;
    protected String mEncoding;
    protected boolean mForce;
    protected long mPrecompiledTolerance;

    public AbstractCompiler(ClassInjector injector) {
        this(injector, null);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AbstractCompiler(ClassInjector injector, String rootPackage) {
        this(injector, rootPackage, (Map) null);
    }
    
    public AbstractCompiler(ClassInjector injector, String rootPackage,
                            Map<String, Template> parseTreeMap) {
        this(injector, rootPackage, null, null, parseTreeMap);
    }

    public AbstractCompiler(ClassInjector injector, String rootPackage, 
                            File rootDestDir) {
        this(injector, rootPackage, rootDestDir, null);
    }
    
    public AbstractCompiler(ClassInjector injector, String rootPackage, 
                            File rootDestDir, String encoding) {
        this(injector, rootPackage, rootDestDir, encoding, null);
    }

    public AbstractCompiler(ClassInjector injector, String rootPackage, 
                            File rootDestDir, String encoding,
                            Map<String, Template> parseTreeMap) {
        this(injector, rootPackage, rootDestDir, encoding, 0L, parseTreeMap);
    }
    
    public AbstractCompiler(ClassInjector injector, String rootPackage, 
                            File rootDestDir, String encoding,
                            long precompiledTolerance,
                            Map<String, Template> parseTreeMap) {
		super((parseTreeMap == null) ?
            Collections.synchronizedMap(new HashMap<String, Template>()) : parseTreeMap);

        mInjector = injector;
        mRootPackage = rootPackage;
		mRootDestDir = rootDestDir;
		mInjector = injector;
		mEncoding = encoding;
		mPrecompiledTolerance = precompiledTolerance;
		
		if (mRootDestDir != null && !mRootDestDir.isDirectory()) {
			throw new IllegalArgumentException(
					"Destination is not a directory: " + rootDestDir);
		}
		
		if (!TemplateRepository.isInitialized()) {
			TemplateRepository.init(rootDestDir, rootPackage);
		}
	}

    /**
     * @param force When true, compile all source, even if up-to-date
     */
    public void setForceCompile(boolean force) {
        mForce = force;
    }       

    /**
     * Get the root target package for all templates. All templates will be 
     * based on this root package and extended from it.
     * 
     * @return The root or base package to use for templates
     */
    public String getRootPackage() {
        return mRootPackage;
    }
    
    /**
     * Overrides Compiler class implementation (TemplateRepository integration).
     */
    public String[] compile(String[] names) 
        throws IOException {

        if (!TemplateRepository.isInitialized()) {
            return super.compile(names);
        }
        
        String[] compNames = super.compile(names);
        ArrayList<String> compList =
            new ArrayList<String>(Arrays.asList(compNames));

        TemplateRepository rep = TemplateRepository.getInstance();
        String[] callers = rep.getCallersNeedingRecompile(compNames, this);
        if (callers.length > 0)
            compList.addAll(Arrays.asList(super.compile(callers)));
        String[] compiled = compList.toArray(new String[compList.size()]);

        // JoshY - There's a VM bug in JVM 1.4.2 that can cause the repository 
        // update to throw a NullPointerException when it shouldn't.  There's a 
        // workaround in place and we also put a catch here, to allow the 
        // TeaServlet init to finish just in case
        try { rep.update(compiled); } catch (Exception e) {
            System.err.println("Unable to update repository");
            e.printStackTrace(System.err);
        }
        
        return compiled;
    }
    
    /**
     * Recursively compiles all files in the source directory.
     *
     * @return The names of all the compiled sources
     */
    public String[] compileAll() throws IOException {
        return compile(getAllTemplateNames());
    }

    /**
     * Returns all sources (template names) available from the source
     * directory and in all sub-directories.
     */
    public abstract String[] getAllTemplateNames() throws IOException;
    
    public abstract class AbstractUnit extends CompilationUnit {
        protected String mSourceFilePath;
        protected String mSourceFileName;
        protected String mDotPath;
        protected File mDestDir;
        protected File mDestFile;
        
        protected AbstractUnit(String name, Compiler compiler) {
            super(name, compiler);
            mDotPath = name;
            String slashPath = name.replace('.', '/');
            if (slashPath.endsWith("/")) {
                slashPath = slashPath.substring(0, slashPath.length() - 1);
            }
            
            if (mRootDestDir != null) {
                if (slashPath.lastIndexOf('/') >= 0) {
                    mDestDir = new File
                        (mRootDestDir,
                         slashPath.substring(0,slashPath.lastIndexOf('/')));
                }
                else {
                    mDestDir = mRootDestDir;
                }
                
                mDestDir.mkdirs();          
                mDestFile = new File(
                    mDestDir,
                    slashPath.substring(slashPath.lastIndexOf('/') + 1) + 
                        ".class"
                );
            }
            
            mSourceFilePath = slashPath;
            mSourceFileName = slashPath.concat(".tea");
        }

        public String getTargetPackage() {
            return mRootPackage;
        }

        public String getSourceFileName() {
            return mSourceFileName;
        }
        
        public Reader getReader() throws IOException {
            Reader reader = null;
            InputStream in = getTemplateSource(mDotPath);
            if (mEncoding == null) {
                reader = new InputStreamReader(in);
            }
            else {
                reader = new InputStreamReader(in, mEncoding);
            }
            
            return reader;
        }
        
        public boolean shouldCompile() {
            return mForce || shouldCompile(getDestinationLastModified());
        }

        protected boolean shouldCompile(long timestamp) {
            if (mForce || (mDestFile != null && !mDestFile.exists())) {
                return true;
            }

            long lastModified = getLastModified();
            if (timestamp > 0 && lastModified > 0 &&
                gteq(lastModified, timestamp, mPrecompiledTolerance)) {

            	return false;
            }

            return true;
        }

        protected abstract long getLastModified();
        
        protected long getDestinationLastModified() {
            /*
            if (mDestFile == null) {
                TemplateRepository.TemplateInfo templateInfo = 
                    TemplateRepository.getInstance().getTemplateInfo(mDotPath);
                
                if (templateInfo != null) {
                    return templateInfo.getLastModified();
                }
            }
            */
            
            if (mDestFile != null && mDestFile.exists()) {
                return mDestFile.lastModified();
            }
            
            return 0L;
        }
        
        /**
         * Returns the truth that a is greater than or equal to b assuming the given tolerance.
         * <p>
         * ex. tolerance = 10, a = 1000 and b = 2000, returns false<br/>
         * ex. tolerance = 10, a = 1989 and b = 2000, returns false<br/>
         * ex. tolerance = 10, a = 1990 and b = 2000, returns true<br/>
         * ex. tolerance = 10, a = 2000 and b = 2000, returns true<br/>
         * ex. tolerance = 10, a = 3000 and b = 2000, returns true<br/>
         * </p>
         *
         * @param a
         * @param b
         * @param tolerance
         * @return
         */
        private boolean gteq(long a, long b, long tolerance) {
            return a >= b - tolerance;
        }

        /**
         * @return the file that gets written by the compiler.
         */
        public File getDestinationFile() {
            return mDestFile;
        }

        public OutputStream getOutputStream() throws IOException {
            OutputStream out1 = null;
            OutputStream out2 = null;

            if (mDestDir != null) {
                if (!mDestDir.exists()) {
                    mDestDir.mkdirs();
                }

                out1 = new FileOutputStream(mDestFile);
            }

            if (mInjector != null) {
                String className = getName();
                String pack = getTargetPackage();
                if (pack != null && pack.length() > 0) {
                    className = pack + '.' + className;
                }
                out2 = mInjector.getStream(className);
            }

            OutputStream out;

            if (out1 != null) {
                if (out2 != null) {
                    out = new DualOutput(out1, out2);
                }
                else {
                    out = out1;
                }
            }
            else if (out2 != null) {
                out = out2;
            }
            else {
                out = new OutputStream() {
                    public void write(int b) {}
                    public void write(byte[] b, int off, int len) {}
                };
            }

            return new BufferedOutputStream(out);
        }
        
        public void resetOutputStream() {
            if (mDestFile != null) {
                mDestFile.delete();
            }

            if (mInjector != null) {
                mInjector.resetStream(getClassName());
            }
        }
        
        protected String getClassName() {
            return getClassName(null);
        }

        protected String getClassName(String innerClass) {
            String className = getName();
            String pack = getTargetPackage();
            if (pack != null && pack.length() > 0) {
                className = pack + '.' + className;
            }

            if (innerClass != null) {
                className = className + '$' + innerClass;
            }

            return className;
        }

        protected abstract InputStream 
        getTemplateSource(String templateSourceName) 
        	throws IOException;
    }
}
