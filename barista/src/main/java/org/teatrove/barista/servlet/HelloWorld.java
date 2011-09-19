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

package org.teatrove.barista.servlet;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * A simple test servlet that just prints "Hello World!".
 *
 * @author Brian S O'Neill
 */
public class HelloWorld extends HttpServlet {
    protected void doGet(HttpServletRequest request, 
                         HttpServletResponse response) 
        throws ServletException, IOException {
                    
        String message = "Hello World!";
        response.setContentType("text/html");
        response.setContentLength(message.length());
        response.getWriter().write(message);
    }
}
