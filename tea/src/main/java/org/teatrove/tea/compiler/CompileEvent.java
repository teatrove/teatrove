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

package org.teatrove.tea.compiler;

/**
 * A compile event is associated with template compilation and includes
 * information on the template compilation such as whether an error or warning
 * occurred.  Compile events are associated with a {@link CompileListener}.
 * 
 * @author Brian S O'Neill
 */
public class CompileEvent extends java.util.EventObject {

    private static final long serialVersionUID = 1L;

    private CompileEvent.Type mType;
    private String mMessage;
    private Token mCulprit;
    private SourceInfo mInfo;
    private CompilationUnit mUnit;

    CompileEvent(Object source, CompileEvent.Type type,
                 String msg, Token culprit) {
        this(source, type, msg, culprit, null);
    }

    CompileEvent(Object source, CompileEvent.Type type,
                 String msg, SourceInfo info) {
        this(source, type, msg, info, null);
    }

    CompileEvent(Object source, CompileEvent.Type type,
                 String msg, Token culprit, CompilationUnit unit) {
        super(source);
        
        mType = type;
        mMessage = msg;
        mCulprit = culprit;
        if (culprit != null) {
            mInfo = culprit.getSourceInfo();
        }
        mUnit = unit;
    }

    CompileEvent(Object source, CompileEvent.Type type,
                 String  msg, SourceInfo info, CompilationUnit unit) {
        super(source);
        
        mType = type;
        mMessage = msg;
        mInfo = info;
        mUnit = unit;
    }

    /**
     * Return whether this event relates to a compile error or not.
     * 
     * @return true if this event is a compile error, false otherwise
     */
    public boolean isError() {
        return mType == CompileEvent.Type.ERROR;
    }
    
    /**
     * Return whether this event relates to a compile warning or not.
     * 
     * @return true if this event is a compile warning, false otherwise
     */
    public boolean isWarning() {
        return mType == CompileEvent.Type.WARNING;
    }
    
    /**
     * Returns the type of event, such as a compile error or warning.
     * 
     * @return The type of event
     */
    public CompileEvent.Type getType() {
        return mType;
    }
    
    /**
     * Get the associated message for this event such as the reason for the
     * compile error or warning.
     * 
     * @return The message or reason for the event
     */
    public String getMessage() {
        return mMessage;
    }

    /**
     * Returns the detailed message by prepending the standard message with 
     * source file information.
     * 
     * @return The detailed message with the source file information
     * 
     * @see #getMessage()
     */
    public String getDetailedMessage() {
        String prepend = getSourceInfoMessage();
        if (prepend == null || prepend.length() == 0) {
            return mMessage;
        }
        else {
            return prepend + ": " + mMessage;
        }
    }

    /**
     * Get the source file information associated with this event.  The source
     * file information includes the name of the associated template and
     * potential line number.
     * 
     * @return The source file information associated with the event
     */
    public String getSourceInfoMessage() {
        String msg;
        if (mUnit == null) {
            if (mInfo == null) {
                msg = "";
            }
            else {
                msg = String.valueOf(mInfo.getLine());
            }
        }
        else {
            if (mInfo == null) {
                msg = mUnit.getName();
            }
            else {
                msg =
                    mUnit.getName() + ':' + mInfo.getLine();
            }
        }

        return msg;
    }

    /**
     * This method reports on where in the source code an error was found.
     *
     * @return Source information on this error or null if not known.
     */
    public SourceInfo getSourceInfo() {
        return mInfo;
    }

    /**
     * Get the offending token within the source tree that caused the event.  If
     * no token was associated with this event, <code>null</code> is returned.
     *  
     * @return The offending token in the source tree or <code>null</code>
     */
    public Token getCulpritToken() {
        return mCulprit;
    }

    /**
     * Get the associated compilation unit associated with this event.  The
     * compilation event includes the associated template.  If no unit was
     * associated with this event, then <code>null</code> is returned.
     * 
     * @return The associated compilation unit or <code>null</code>
     */
    public CompilationUnit getCompilationUnit() {
        return mUnit;
    }

    /**
     * The various types of events supported by this event.
     */
    public static enum Type {
        ERROR,
        WARNING
    }
}
