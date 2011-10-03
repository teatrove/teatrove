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

import org.teatrove.tea.compiler.TemplateRepository;
import org.teatrove.tea.compiler.TemplateRepository.TemplateInfo;
import org.teatrove.tea.engine.Template;
import org.teatrove.tea.engine.TemplateCompilationResults;
import org.teatrove.trove.log.Log;
import org.teatrove.trove.util.PropertyMap;
import org.teatrove.trove.util.plugin.PluginContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author eearle
 */
public class TeaServletEngineImplDev extends TeaServletEngineImpl {

    Log mLog;
    private boolean mRecurse;

    public void startEngine(PropertyMap properties,
                            ServletContext servletContext,
                            String servletName,
                            Log log,
                            List memLog,
                            PluginContext plug) throws ServletException {
        mLog = log;

        mRecurse = properties.getBoolean("autocompile.recurse", true);

        mLog.warn("DEVELOPEMENT MODE - changed templates will be autocompiled when requested");

        super.startEngine(properties, servletContext, servletName, log, memLog, plug);
    }


    public TeaServletTransaction createTransaction(HttpServletRequest request, HttpServletResponse response, boolean lookupTemplate) throws IOException {

        TeaServletTemplateSource templateSrc = getTemplateSource();

        String templateName;
        if ((templateName = request.getPathInfo()) == null) {
            if ((templateName = request.getServletPath()) == null) {
                templateName = "/";
            }
        }

        // forge template name from request info
        int index = templateName.lastIndexOf('.');
        if (index >= 0) {
            templateName = templateName.substring(0, index);
        }
        while (templateName.startsWith("/")) {
            templateName = templateName.substring(1);
        }
        while (templateName.endsWith("/")) {
            templateName = templateName.substring(0, templateName.length() - 1);
        }
        templateName = templateName.replace('/', '.');

        try {
            Map changedTemplates = templateSrc.listTouchedTemplates();

            // if this or any template called from this template are changed, mark try to compile this template
            List toCompile = getChangedDeps(changedTemplates, templateName);

            if(changedTemplates.containsKey(templateName)) {
                toCompile.add(templateName);
            }

            if (toCompile.size()>0) {
                String[] toCompArr = (String[])toCompile.toArray(new String[toCompile.size()]);
                TemplateCompilationResults r = templateSrc.compileTemplates(null, toCompArr);

                if (r.getTemplateErrors().size() > 0) {
                    Template template = templateSrc.getTemplate("system.teaservlet.InlineCompileErrors");

                    request = new HttpServletRequestWrapper(request) {

                        public String getPathInfo() {
                            return "/system/teaservlet/InlineCompileErrors";
                        }
                    };

                    request.setAttribute("inline.reload.result", r);
                }
            }

        } catch (Exception ex) {
            mLog.error(ex);
        }

        return super.createTransaction(request, response, lookupTemplate);
    }

    private List getChangedDeps(Map changedTemplates, String templateName) {
        List changed = new ArrayList();

        if(mRecurse) {
            TemplateInfo tInfo = TemplateRepository.getInstance().getTemplateInfo(templateName);
            if(tInfo!=null) {
                String[] deps = tInfo.getDependents();
                depLoop:for (int i = 0; i < deps.length; i++) {
                    String dep = deps[i].replace('/', '.');
                    dep = dep.substring("org/teatrove/teaservlet/template/".length());

                    if(changedTemplates.containsKey(dep)) {
                        changed.add(dep);
                    }

                    changed.addAll(getChangedDeps(changedTemplates, dep));
                }
            }
        }
        return changed;
    }
}
