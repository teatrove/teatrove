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

package org.teatrove.teaservlet.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.teatrove.tea.compiler.CompilationUnit;
import org.teatrove.tea.compiler.Compiler;
import org.teatrove.tea.compiler.TemplateRepository;
import org.teatrove.tea.util.AbstractFileCompiler;
import org.teatrove.trove.io.DualOutput;
import org.teatrove.trove.util.ClassInjector;

public abstract class AbstractCompiler extends AbstractFileCompiler {

    protected String mRootPackage;
    protected File mRootDestDir;
    protected ClassInjector mInjector;
    protected String mEncoding;
    protected boolean mForce = false;
    protected long mPrecompiledTolerance;

    public AbstractCompiler(String rootPackage, File rootDestDir,
                            ClassInjector injector, String encoding,
                            long precompiledTolerance) {
	            	
		super();
		
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

    public abstract class AbstractUnit extends CompilationUnit {
        protected String mSourceFilePath;
        protected String mDotPath;
        protected File mDestDir;
        protected File mDestFile;
        protected String mSourceUrl;
        
        AbstractUnit(String name, Compiler compiler) {
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
                mDestFile = new File
                    (mDestDir,
                     slashPath.substring(slashPath.lastIndexOf('/') + 1) 
                     + ".class");
            /*
            try {
                if (mDestFile.createNewFile()) {
                    System.out.println(mDestFile.getPath() + " created");
                }
                else {
                    System.out.println(mDestFile.getPath() + " NOT created");
                }
            }
            catch (IOException ioe) {ioe.printStackTrace();}
            */
            }
            mSourceFilePath = slashPath;
            mSourceUrl = getSourceFileName();
        }

        public String getTargetPackage() {
            return mRootPackage;
        }

        public String getSourceFileName() {
            return mSourceFilePath + ".tea";
        }
        
        /**
         * comparable to the getSourceFile() method of FileCompiler
         */
        public String getSourceUrl() {
            return mSourceUrl;
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
        
        protected boolean shouldCompile(long timestamp) {
            final TemplateRepository.TemplateInfo templateInfo = 
            	TemplateRepository.getInstance().getTemplateInfo(mDotPath);

            if(!mForce &&
                (mDestFile == null || !mDestFile.exists()) &&
                templateInfo != null &&
                gteq(templateInfo.getLastModified(), timestamp, 
                		mPrecompiledTolerance)) {
            	
            	return false;
            }

            return true;
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
            String className = getName();
            String pack = getTargetPackage();
            if (pack != null && pack.length() > 0) {
                className = pack + '.' + className;
            }

            return className;
        }

        protected abstract InputStream 
        getTemplateSource(String templateSourceName) 
        	throws IOException;
    }
}
