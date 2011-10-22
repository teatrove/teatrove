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

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Vector;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Used with the RemoteCompiler to allow templates to be loaded over http. 
 * This servlet should be running on the host specified in the template.path
 * parameter of the teaservlet.
 *
 * @author Jonathan Colwell
 * 
 */
public class TemplateServerServlet extends HttpServlet {
    File mTemplateRoot;
    String mTemplateSource;
    ServletConfig mConfig;
    private String[] mFilterExt;
    
    public void init(ServletConfig conf) {
        mConfig = conf;
        mConfig.getServletContext().log("Starting TemplateServerServlet.");
        mConfig.getServletContext().log("\ttemplate.root: " + conf.getInitParameter("template.root"));
        mConfig.getServletContext().log("\ttemplate.source: " + conf.getInitParameter("template.source"));
        mTemplateRoot = new File(conf.getInitParameter("template.root"));
        System.out.println("");
        mTemplateSource = conf.getInitParameter("template.source");
        if (mTemplateSource == null) {
                mConfig.getServletContext().log("template.source not defined.");
        }

        // always include .tea
        String filterExtStr = conf.getInitParameter("filter.allowed.ext");
        if(filterExtStr==null || filterExtStr.trim().length()==0) {
            filterExtStr = ".tea";
        }

        mFilterExt = filterExtStr.split(",");
        for (int i = 0; i < mFilterExt.length; i++) {
            mFilterExt[i] = mFilterExt[i].trim().toLowerCase();
            conf.getServletContext().log("TemplateServer "+ conf.getServletName() +" including file ext: "+mFilterExt[i]);
        }
        mConfig.getServletContext().log("TemplateServerServlet init complete.");
    }
    
    public void doPost(HttpServletRequest req,HttpServletResponse res) {
        doGet(req,res);
    }
    
    /**
     * Retrieves all the templates that are newer that the timestamp specified
     * by the client.  The pathInfo from the request specifies which templates 
     * are desired.  QueryString parameters "timeStamp" and ??? provide
     */
    public void doGet(HttpServletRequest req,HttpServletResponse res) {
        getTemplateData(req, res,req.getPathInfo());
    }
    
    public void getTemplateData(HttpServletRequest req,
                                HttpServletResponse resp,String path) {
        try {
            OutputStream out = resp.getOutputStream();
            if (req.getParameter("getSourcePath") != null) {
				if (mTemplateSource == null) {
					mConfig.getServletContext()
					                   .log("template.source not defined for " +
					                                       req.getRequestURI());
				}
				String templateSource = mTemplateSource + path;
            	out.write(templateSource.getBytes());	
            }
            else {
	            File templateFile;
	            if (path != null) {
	                templateFile = new File(mTemplateRoot,path);
	            }
	            else {
	                templateFile = mTemplateRoot;
	            }
	            if (templateFile != null) {
	                if (templateFile.isFile()) {
	                    resp.setIntHeader("Content-Length", (int)templateFile.length());
	                    InputStream fis = new BufferedInputStream(
	                          new FileInputStream(templateFile));
	                    for(int nextChar = -1;(nextChar = fis.read()) >= 0;) {
	                        out.write(nextChar);
	                    }
	                    fis.close();
	                }
	                else if (templateFile.isDirectory()) {
	                    Vector tempVec = new Vector();
	                    File[] dirlist = templateFile.listFiles();
	                    for (int j=0;j<dirlist.length;j++) {
	                        listTemplates(dirlist[j],tempVec,"/");
	                    }
	                    Iterator tempIt = tempVec.iterator();
	                    while (tempIt.hasNext()) {
	                        out.write((byte[])tempIt.next());
	                    }
	                }
	                else {
	                    mConfig.getServletContext().log(path
	                     + " doesn't map to an existing template or directory.");
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
	                }
	            }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }     
    }

    public void listTemplates(File root,Vector storage,String path) throws IOException {
        if(root.isHidden()) {
            return;
        }
        if (root.isDirectory()) {
            File[] dirlist = root.listFiles();
            for (int j=0;j<dirlist.length;j++) {
                listTemplates(dirlist[j],storage,
                                path + root.getName() + "/");
            }
        } 
        else if (root.isFile()) {
            String templateName = root.getName();
           
            filterLoop:for (int i = 0; i < mFilterExt.length; i++) {
                if (templateName.endsWith(mFilterExt[i])) {

                    if(mFilterExt[i].equalsIgnoreCase(".tea")) {
                        storage.add(("|" + path + templateName.substring(0,templateName.length()-4)+ "|"
                                                + Long.toString(root.lastModified())).getBytes());
                        break filterLoop;
                    } else {
                        storage.add(("|" + path + templateName+ "|"
                                + Long.toString(root.lastModified())).getBytes());
                        break filterLoop;
                    }
                }
            }
        }
    }
}
