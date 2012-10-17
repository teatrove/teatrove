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

package org.teatrove.teaservlet;

import java.beans.MethodDescriptor;
import java.util.Set;

import javax.servlet.ServletException;

import org.teatrove.tea.compiler.TemplateRepository;
import org.teatrove.tea.engine.TemplateCompilationResults;
import org.teatrove.tea.engine.TemplateCompilationStatus;
import org.teatrove.tea.engine.TemplateExecutionResult;
import org.teatrove.teaservlet.stats.AggregateInterval;
import org.teatrove.teaservlet.stats.AggregateSummary;
import org.teatrove.teaservlet.stats.Milestone;
import org.teatrove.teaservlet.stats.TeaServletRequestStats;
import org.teatrove.teaservlet.stats.TemplateStats;
import org.teatrove.trove.classfile.TypeDesc;

/**
 *
 * @author Brian S O'Neill
 */
public interface AdminContext extends TeaToolsContext {

    /**
     * Gets the admin information for the TeaServlet. The user also can
     * reload the application or reload templates.
     * <p>
     * This function processes the following HTTP request parameters:
     * <ul>
     * <li>reloadTemplates - reloads the changed templates
     * <li>reloadTemplates=all - reloads all templates
     * <li>log - the id of the log
     * <li>enabled - turns on/off the log (boolean)
     * <li>debug - turns on/off log debug messages (boolean)
     * <li>info - turns on/off log info messages (boolean)
     * <li>warn - turns on/off log warning messages (boolean)
     * <li>error - turns on/off log error messages (boolean)
     * </ul>
     * @return the admin information
     */
    public TeaServletAdmin getTeaServletAdmin() throws ServletException;

    /**
     * If the call to getTeaServletAdmin() caused a reload to occur,
     * a call to this method will return the results of that reload.
     */
    public TemplateCompilationResults getCompilationResults();

    /**
     * Get the status of the currently executing compilation.  This will return
     * the number of templates being compiled and the current progress.  If no
     * active compilation is happening, <code>null</code> is returned.
     * 
     * @return The current template compilation status or <code>null</code>
     */
    public TemplateCompilationStatus getCompilationStatus();
    
    /**
     * Returns a Class object for a given name.
     * it basically lets templates perform Class.forName(classname);
     */
    public Class<?> getClassForName(String classname);
  
    /**
     * Returns a list of class objects for each known subclass.
     */
    public String[] getSubclassesForName(String classname);

    /**
     * Streams the structural bytes of the named class via the HttpResponse.
     */
    public void streamClassBytes(String className) throws ServletException;


    /** 
     * allows a template to dynamically call another template
     */
    public void dynamicTemplateCall(String templateName) throws Exception;
     
    /** 
     * allows a template to dynamically call another template
     * this time with parameters.
     */
    public void dynamicTemplateCall(String templateName, Object[] params) 
        throws Exception;

    /**
     * returns a context for the specified application instance by name.  
     * this is useful when dynamically calling a function in that context.
     */
    public Object obtainContextByName(String appName) throws ServletException;

    /**
     * allows users to leave notes to each other from admin templates.
     * when called with a null contents parameter, this will not update the 
     * messages but will still return the contents of the message list.  
     * if the ID parameter is null, a list of all known IDs will be returned.
     */
    public Set<?> addNote(String ID, String contents, int lifespan);

    public AdminApplication.ServerStatus[] getReloadStatusOfServers();

    public TemplateRepository.TemplateInfo getTemplateInfo(String templateName);
    
    public TemplateRepository.TemplateInfo[] getTemplateInfos();

    public TemplateRepository.TemplateInfo[] getCallers(String templateName);

    public TemplateRepository.TemplateInfo[] getMethodCallers(MethodDescriptor methodDesc);

    public String formatTypeDesc(TypeDesc type);

    public boolean isTemplateRepositoryEnabled();

    public FunctionInfo getFunction(String methodName);

    public TeaServletInvocationStats.Stats getStatistics(String caller, String callee);
    
    public void resetStatistics();
    
    public void resetStatistics(String caller, String callee);
    
    public void setTemplateOrdering(String orderBy);
    
    /**
     * Check available templates and compile as needed.  To forcefully recompile
     * all templates, specify <code>true</code>.  Otherwise, if 
     * <code>false</code>, then only changed templates will be compiled as well
     * as any dependent templates.
     * 
     * @param all <code>true</code> to recompile all templates,
     *            <code>false</code> to only compile changed templates
     *            
     * @return The results of the compilation
     * 
     * @throws Exception If an unexpected error occurs other than compile errors
     */
    public TemplateCompilationResults checkTemplates(boolean all) 
        throws Exception;
    
    /**
     * Check the given templates and compile as needed.  This will compile the
     * specified templates as well as any dependent templates.
     * 
     * @param The names of the templates to check and compile
     *            
     * @return The results of the compilation
     * 
     * @throws Exception If an unexpected error occurs other than compile errors
     */
    public TemplateCompilationResults checkTemplates(String[] templateNames) 
        throws Exception;
       
    /**
     * Compile the given source into a temporary template and immediately
     * execute it to allow sampling of source and dynamic capabilities. Note 
     * that the source should not include the template declaration as the 
     * declaration is automatically provided.
     * 
     * @param source The source of the template to compile
     *            
     * @return The results of the compilation and execution
     * 
     * @throws Exception If an unexpected error occurs other than compile errors
     */
    public TemplateExecutionResult executeSource(String source) 
        throws Exception;
    
    /* new template stats */
    
    /**
     * Returns the template raw and aggregate statistics so as to
     * better understand the performance of this template through time.
     * 
     * @param fullTemplateName the name of the template with '.' as a seperator.
     * 
     * @return the template stats for this given template.
     */
    public TemplateStats getTemplateStats(String fullTemplateName);
    
    /**
     * Returns an array of template stats.
     * 
     * Returns the template raw and aggregate statistics so as to
     * better understand the performance of templates through time.
     * 
     * @return the template stats for this given template.
     */
    public TemplateStats[] getTemplateStats();
    
    /**
     * Returns object that manages template raw and aggregate statistics.
     */
    public TeaServletRequestStats getTeaServletRequestStats();
    
    /**
     * Sets the raw window size. The rawWindowSize defines how many
     * raw statistics are going to be kept in a circular queue.
     * 
     * Resets all statistics.
     * 
     * @param rawWindowSize
     */
    public void setRawWindowSize(int rawWindowSize);
    
    /**
     * Sets the aggregate interval size. The aggregateWindowSize defines how many
     * aggregate intervals are going to be kept in a circular queue.
     * 
     * Resets all statistics.
     * 
     * @param aggregateWindowSize
     */
    public void setAggregateWindowSize(int aggregateWindowSize);

    /**
	 * Returns the aggregate intervals for the specified startTime and stopTime.
	 * Any intervals that contain these two endpoints lie between them will be
	 * included.
	 * 
	 * @param templateStats the template stats object to query.
	 * @param startTime the start time to filter on.
	 * @param stopTime the stop time to filter on.
	 * 
	 * @return aggregate intervals for the specified interval.
	 */
    public AggregateInterval[] getAggregateIntervals(TemplateStats templateStats,
    		                                         long startTime, long stopTime);

    /**
	 * Returns an aggregate interval for the raw data filtered 
	 * by start and stop time.
	 * 
	 * @see TemplateStats.getAggregateIntervalForRawData()
	 * 
	 * @param startTime the start time to filter on.
	 * @param stopTime the stop time to filter on.
	 * @return an aggregate interval for the raw data.
	 */
    public AggregateInterval getAggregateIntervalForRawData(TemplateStats templateStats, 
    														long startTime, long stopTime);
    
    public AggregateSummary getDurationAggregateSummary(AggregateInterval[] intervals);
    
    /**
	 * This method returns the Aggregate interval containing the specified
	 * time stamp.
	 * 
	 * @param intervals the aggregate intervals to search.
	 * @param time aggregate intervals will be found for this time.
	 * @return the aggregate interval containing the specified time.
	 */
	public int search(AggregateInterval[] intervals, long time);
	
    /**
     * Searches for a AggregateInterval which contains the time passed in.
     *
     * @param intervals
     *            sorted array of AggregateIntervals
     * @param time
     *            key to search for
     * @param begin
     *            start position in the index
     * @param end
     *            one past the end position in the index
     * @return Integer index to key. -1 if not found
     */
    public int search(AggregateInterval[] intervals, long time, int begin, int end);
    
    /**
     * Adds a milestone for this template such as a compile event.
     * 
     * @param templateStats the template stats object to query.
     * @param description a description of the milestone
     * @param time the time of the milestone
     */
    public void addMilestone(TemplateStats templateStats, String description, long time);
    
    /**
     * Returns all milestones for this template.
     * @param templateStats the template stats object to query.
     * @return all milestones
     */
    public Milestone[] getMilestones(TemplateStats templateStats);
    
    /**
     * Returns all milestones for this template between the start and stopTime.
     * 
     * @param templateStats the template stats object to query.
     * @param startTime
     * @param stopTime
     * @return the milestones in the requested interval.
     */
    public Milestone[] getMilestones(TemplateStats templateStats, long startTime, long stopTime);
    
    /**
     * Resets the raw and aggregate template statistics.
     */
    public void resetTemplateStats();
    
    /**
     * Resets the raw and aggregate template statistics. If null is passed in all templates
     * statistics will be reset.
     * 
     * @param templateName the name of the template to reset statistics for.
     */
    public void resetTemplateStats(String templateName);

}
