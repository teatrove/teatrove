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

package org.teatrove.maven.plugins.packageinfo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * @goal create-package-infos
 * @description creates PackageInfo.java files
 * @phase generate-sources
 *
 * @author eearle
 */
public class PackageInfoMojo
    extends AbstractMojo
{

    public static final String TAB = "    ";

    public static final String NL = System.getProperty( "line.separator" );

    public static final String NL_TAB = NL + TAB;

    public static final String NL_2TABS = NL_TAB + TAB;

    /** @parameter expression="${project.groupId}" */
    private String groupId;

    /** @parameter expression="${project.artifactId}" */
    private String artifactId;

    /** @parameter expression="${project.version}" */
    private String version;

    /** @parameter expression="" */
    private String buildNumber;

    /** @parameter expression="${project.name}" */
//    private String projectName;

    /** @parameter expression="${basedir}" */
    private File basedir;

    /** The repository where the source code of the project lives
     * @parameter expression="teatrove" */
    private String srcRepo;

    /** The local root of where the .java files are
     * @parameter expression="${project.build.sourceDirectory}" */
    //private File srcRoot;

    /** The base package to start writing package info in
     * @parameter expression="" */
    private String packageRoot;

    /** @parameter expression="${package.info.specification.title}" */
//    private String specTitle;

    /** @parameter expression="${package.info.specification.version}" */
//    private String specVersion;

    /** @parameter expression="${package.info.specification.vendor}" */
//    private String specVendor;

    /** @parameter expression="${package.info.implementation.title}" */
//    private String implTitle;

    /** @parameter expression="${package.info.implementation.version}" */
//    private String implVersion;

    /** @parameter expression="${package.info.implementation.vendor}" */
//    private String implVendor;

    /**
     * @parameter expression="${project.build.directory}/generated-sources/packageinfo/"
     */
    private File outputDirectory;

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;


    private String mBusinessUnit = "teatrove.org";

    private String mBuildMachine;

    private File mSrcRoot;
    
    private String mSrcRootPath;

    private File mPackageRootDir;

    private long mBuildTime = System.currentTimeMillis();

    private String mUser = System.getProperty( "user.name" );

    private String mBuildPlatform = System.getProperty( "os.arch" ) + " " + System.getProperty( "os.name" ) + " "
        + System.getProperty( "os.version" );

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        // Lookup build machine info
        try
        {
            InetAddress address = InetAddress.getLocalHost();
            mBuildMachine = address.getHostName();
        }
        catch ( UnknownHostException e )
        {
            //throw new MojoExecutionException( "could not get the host name", e );
            mBuildMachine = "Unknown host";
        }

        File outputRootDir;
        List<String> srcRoots = project.getCompileSourceRoots();
        for (String srcRoot : srcRoots) 
        {
            // validate some things
            mSrcRoot = new File(srcRoot);
            if ( !mSrcRoot.exists() )
                throw new MojoExecutionException( "srcRoot does not exist: " + mSrcRoot );

            // setup paths
            if ( packageRoot != null && packageRoot.trim().length() > 0 )
            {
                final String packageRootPath = packageRoot.replace('.', File.separatorChar);
                mPackageRootDir = new File( srcRoot, packageRootPath);
                outputRootDir = new File(outputDirectory, packageRootPath);
            }
            else
            {
                mPackageRootDir = mSrcRoot;
                outputRootDir = outputDirectory;
            }
            getLog().debug( "packageRoot dir: " + mPackageRootDir );
            getLog().debug( "outputDirectory dir: " + outputRootDir );
    
            if ( !mPackageRootDir.exists() ) {
                throw new MojoExecutionException( "packageRoot does not exist: " + packageRoot );
            }
    
            try
            {
                mSrcRootPath = mSrcRoot.getCanonicalPath();
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "error calling getCanonicalPath() on File object: " + srcRoot, e );
            }
    
            getLog().debug( "srcRoot path: " + mSrcRootPath );
            writePackageInfos( mPackageRootDir, outputRootDir );
        }
        
        // Add the generated files as a source directory to compile against
        project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
    }

    /** recurse into rootDir writing PackageInfos on the way in.
     * @param rootDir The root directory for the source
     * @param outputDir The root directory to output to
     * @throws org.apache.maven.plugin.MojoExecutionException
     */
    private void writePackageInfos( File rootDir, File outputDir )
        throws MojoExecutionException
    {
        getLog().debug( "in writePackageInfos(" + rootDir + ", " + outputDir + ")" );

        try
        {
            if ( shouldWritePackageInfo( rootDir ) )
            {
                if( !outputDir.exists() && !outputDir.mkdirs() ) {
                    throw new MojoExecutionException( "outputDirectory was unable to be created: " + outputDir );
                }

                writePackageInfo( outputDir );
            }
            else
            {
                getLog().debug( "no files in:" + rootDir );
            }

        }
        catch ( Throwable e )
        {
            throw new MojoExecutionException( "could not write PackageInfo.java in: " + outputDir, e );
        }

        // get any sub-directories
        File[] subdirs = rootDir.listFiles( new FileFilter()
        {
            public boolean accept( File pathname )
            {
                return pathname.isDirectory();
            }
        } );

        // recurse
        if ( subdirs != null ) {
            for (File subdir : subdirs) {
                writePackageInfos(subdir, new File(outputDir, subdir.getName()));
            }
        }
    }

    private void writePackageInfo( File dir )
        throws IOException
    {
        getLog().debug( "in writePackageInfo(" + dir + ")" );
        File outFile = new File( dir, "PackageInfo.java" );
        PrintWriter out = new PrintWriter( new BufferedWriter( new FileWriter( outFile, false ) ) );

        try
        {
            // write the package statement
            writePackageDeclaration( out, dir );

            // write the class declaration
            writeClassDeclaration( out );

            // write the main method;
            writeMainMethod( out );

            // write the getters;
            writeGetters( out );

            // write the end of the class;
            out.write( "}" + NL );

            out.flush();
        }
        finally
        {
            try
            {
                out.close();
            }
            catch ( Throwable ignore )
            {
            }
        }
    }

    /** Tests if a PackageInfo.java file should be written in the directory passed in.
     *
     *  If the File passed in is not a directory or is not writable, false is returned.
     *  If an explicit starting package is specified to the mojo (the legacy behavior), true is returned if 
     *     the directory passed in is a sub-package of that root package.
     *  If an explicit starting package is NOT specified, true will only be returned if the directory passed in 
     *     contains at least one file.
     *
     *  @param directory The directory to write package info to.
     * @return returns true if a PackageInfo.java should be written.
     */
    private boolean shouldWritePackageInfo( File directory )
    {
        if ( !directory.isDirectory() )
            return false;

        if (!directory.exists())
            return false;

        // legacy behavior: if mPackageRootDir is a sub-dir of mSrcRootPath, an explicit package was specified
        if(isChild(new File(mSrcRootPath), mPackageRootDir)) {
            
            // return true if 'directory' passed in equals() or is a child of mPackageRootDir
            return mPackageRootDir.equals(directory) || isChild(mPackageRootDir, directory);
            
        } else {  // only return true if 'directory' passed in contains at least one file

            File[] files = directory.listFiles( new FileFilter()
            {
                public boolean accept( File pathname )
                {
                    return pathname.isFile();
                }
            } );

            return files != null && files.length > 0;
        }
    }

    /** @param parent The parent file.
     * @param child The child file.
     * @return returns true if child is anywhere in the sub-tree of directories of parent.
     *     returns false if parent.equals(child), isChild(child, parent), or parent and child are un-related
     */
    private boolean isChild(File parent, File child) {
        
        if(parent==null || child==null) return false;
        
        File childsParent = child.getParentFile();
        return parent.equals(childsParent) || isChild(parent, childsParent);
    }
    
    private void writePackageDeclaration( PrintWriter out, File packageDir )
        throws IOException
    {
        if ( !mSrcRoot.equals( packageDir ) )
        {

            String pkgPath = packageDir.getCanonicalPath();
            String packageName = pkgPath.substring(outputDirectory.getCanonicalPath().length());
            if ( packageName.startsWith( File.separator ) )
                packageName = packageName.substring( File.separator.length() );
            packageName = packageName.replace( File.separatorChar, '.' );

            out.write( "package " + packageName + ";" + NL + NL );
        }
    }

    private void writeClassDeclaration( PrintWriter out )
    {
        // comment
        out.write( "/**" + NL
            + " * This class was generated by " + NL + " * " + this.getClass().getName() + NL + " * on "
            + new java.util.Date().toString() + NL + " */" + NL );

        out.write( "public final class PackageInfo {" );
        out.write( NL + NL );
    }

    private void writeMainMethod( PrintWriter out )
    {
        out.write( TAB + "/**" + NL_TAB + " * Prints all the PackageInfo properties to standard out." + NL_TAB + " */"
            + NL );

        out.write( TAB + "public static void main(String[] args) {" + NL_2TABS
            + "System.out.println(\"Base Directory: \" + getBaseDirectory());" + NL_2TABS
            + "System.out.println(\"Repository: \" + getRepository());" + NL_2TABS
            + "System.out.println(\"Username: \" + getUsername());" + NL_2TABS
            + "System.out.println(\"Build Machine: \" + getBuildMachine());" + NL_2TABS
            + "System.out.println(\"Group: \" + getGroup());" + NL_2TABS
            + "System.out.println(\"Project: \" + getProject());" + NL_2TABS
            + "System.out.println(\"Build Location: \" + getBuildLocation());" + NL_2TABS
            + "System.out.println(\"Product: \" + getProduct());" + NL_2TABS
            + "System.out.println(\"Product Version: \" + getProductVersion());" + NL_2TABS
            + "System.out.println(\"Build Number: \" + getBuildNumber());" + NL_2TABS
            + "System.out.println(\"Build Date: \" + getBuildDate());" + NL_2TABS + "System.out.println();" + NL_2TABS
            + "System.out.println(\"Specification Title: \" + getSpecificationTitle());" + NL_2TABS
            + "System.out.println(\"Specification Version: \" + getSpecificationVersion());" + NL_2TABS
            + "System.out.println(\"Specification Vendor: \" + getSpecificationVendor());" + NL_2TABS
            + "System.out.println(\"Implementation Title: \" + getImplementationTitle());" + NL_2TABS
            + "System.out.println(\"Implementation Version: \" + getImplementationVersion());" + NL_2TABS
            + "System.out.println(\"Implementation Vendor: \" + getImplementationVendor());" + NL_2TABS
            + "System.out.println();" + NL_2TABS + "System.out.println(\"Build Platform: \" + getBuildPlatform());"
            + NL_2TABS + "}" + NL + NL );
    }

    private void writeGetters( PrintWriter out )
        throws IOException
    {
        // TODO - fix java src and trgt versions:
        writeVarAndGetter( out, "JAVA_SRC_VERSION", quote( "0.0" ), "getJavaSourceVersion" );
        writeVarAndGetter( out, "JAVA_TRGT_VERSION", quote( "0.0" ), "getJavaTargetVersion" );
        writeVarAndGetter( out, "BUILD_PLATFORM", quote( mBuildPlatform ), "getBuildPlatform" );

        writeVarAndGetter( out, "BASE_DIRECTORY", quote( basedir.getCanonicalPath() ), "getBaseDirectory" );
        writeVarAndGetter( out, "USERNAME", quote( mUser ), "getUsername" );

        writeVarAndGetter( out, "BUILD_MACHINE", quote( mBuildMachine ), "getBuildMachine" );
        writeVarAndGetter( out, "BUILD_NUMBER", quote( buildNumber ), "getBuildNumber" );
        writeVarAndGetter( out, "BUILD_LOCATION", "BASE_DIRECTORY", "getBuildLocation" );
        writeVarAndGetter( out, java.util.Date.class, "BUILD_DATE", "new java.util.Date(" + mBuildTime + "L)",
                           "getBuildDate" );

        writeVarAndGetter( out, "GROUP", quote( mBusinessUnit ), "getGroup" );
        writeVarAndGetter( out, "PRODUCT", quote( artifactId ), "getProduct" );
        writeVarAndGetter( out, "PRODUCT_VERSION", quote( version ), "getProductVersion" );
        writeVarAndGetter( out, "PROJECT", quote( groupId ), "getProject" );
        writeVarAndGetter( out, "REPOSITORY", quote( srcRepo ), "getRepository" );

        writeVarAndGetter( out, "IMPL_TITLE", "PRODUCT", "getImplementationTitle" );
        writeVarAndGetter( out, "IMPL_VENDOR", "GROUP", "getImplementationVendor" );
        writeVarAndGetter( out, "IMPL_VERSION", "PRODUCT_VERSION", "getImplementationVersion" );

        writeVarAndGetter( out, "SPEC_TITLE", "PRODUCT", "getSpecificationTitle" );
        writeVarAndGetter( out, "SPEC_VENDOR", quote( "Tea Users Anonymous" ), "getSpecificationVendor" );
        writeVarAndGetter( out, "SPEC_VERSION", "PRODUCT_VERSION", "getSpecificationVersion" );
    }

    private void writeVarAndGetter( PrintWriter out, String varName, Object varValue, String getterName )
    {
        writeVarAndGetter( out, "".getClass(), varName, varValue, getterName, null );
    }

    private void writeVarAndGetter( PrintWriter out, Class<?> type, String varName, Object varValue, String getterName )
    {
        writeVarAndGetter( out, type, varName, varValue, getterName, null );
    }

    private void writeVarAndGetter( PrintWriter out, Class<?> type, String varName, Object varValue, String getterName,
                                   String comment )
    {

        // write the comment
        if ( comment != null && comment.trim().length() > 0 )
            out.write( "/**" + NL_TAB + " * " + comment + NL_TAB + " */" + NL );

        // declare the variable and assign the value
        out.write( TAB + "private static final " + type.getName() + " " + varName + " = "
            + ( varValue == null ? "null" : varValue.toString() ) + ";" + NL_TAB );

        // write the getter
        out.write( "public static " + type.getName() + " " + getterName + "() {" + NL_2TABS );
        out.write( "return " + varName + ";" + NL_TAB );
        out.write( "}" + NL + NL );
    }

    private String quote( String s )
    {
        // loop through the string, handling the \ characters and escaping them properly
        if ( s == null )
        {
            return "\"\"";
        }
        StringBuffer updatedString = new StringBuffer( "\"" );
        int i;
        for ( i = 0; i < s.length(); i++ )
        {
            if ( s.charAt( i ) == '\\' )
            {
                updatedString.append( "\\\\" );
            }
            else
            {
                updatedString.append( s.charAt( i ) );
            }
        }
        updatedString.append( "\"" );
        return updatedString.toString();
    }
}
