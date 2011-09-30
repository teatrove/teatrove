/* ====================================================================
 * TeaServlet - Copyright (c) 1999-2000 GO.com
 * ====================================================================
 * The Tea Software License, Version 1.0
 *
 * Copyright (c) 2000 GO.com.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by GO.com
 *        (http://opensource.go.com/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Tea", "TeaServlet", "Kettle", "Trove" and "GO.com" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact opensource@go.com.
 *
 * 5. Products derived from this software may not be called "Tea",
 *    "TeaServlet", "Kettle" or "Trove", nor may "Tea", "TeaServlet",
 *    "Kettle" or "Trove" appear in their name, without prior written
 *    permission of GO.com.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL GO.COM OR ITS CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR 
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 * For more information about Tea, please see http://opensource.go.com/.
 */


import org.teatrove.teaservlet.*;
import java.io.*;
import java.util.Vector;

/**
 *
 * @author Jonathan Colwell
 * @version

 */
public class SampleDirectoryBrowserContext {

    ApplicationRequest mRequest;
    ApplicationResponse mResponse;
    SampleDirectoryBrowserApp mApp;

    public SampleDirectoryBrowserContext(ApplicationRequest request,
                              ApplicationResponse response,
                              SampleDirectoryBrowserApp app) {
        mRequest = request;
        mResponse = response;
        mApp = app;
    }

    // Gets an array of files in the directory specified by the "path" parameter.

    public File[] getFiles() {

        String path = mRequest.getParameter("path");
        if (path == null) {
            path = mApp.getInitParameter("defaultPath");
        }
        if (path == null) {
            path = "/";
        }

        File activefile = new File(path);
        if( activefile.isDirectory()) {
            return activefile.listFiles();
        } else {
            return null;
        }
    }
}
