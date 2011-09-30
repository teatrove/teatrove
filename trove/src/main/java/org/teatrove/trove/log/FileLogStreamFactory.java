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

package org.teatrove.trove.log;

import java.io.*;
import java.util.*;
import java.text.*;

/**
 * Opens up files to be used by an {@link IntervalLogStream}.
 *
 * @author Brian S O'Neill
 */
public class FileLogStreamFactory implements IntervalLogStream.Factory {
    private File mDirectory;
    private DateFormat mDateFormat;
    private String mExtension;

    /**
     * Creates log files in the given directory. The names are created by
     * appending the name of the directory, a hyphen, a time stamp and 
     * an extension.
     * 
     * For example, if the directory is "/logs/MyApp", the format is 
     * "yyyyMMdd", and the extension is ".log", then a generated file might be:
     * "/logs/MyApp/MyApp-19990608.log".
     *
     * @param directory Directory to create log files in.
     * @param format DateFormat to use for creating new file names.
     * @param extension Extension to put at the end of new file name.
     */
    public FileLogStreamFactory(File directory, 
                                DateFormat format,
                                String extension) {

        if (directory == null) {
            throw new NullPointerException
                ("FileLogStreamFactory directory not specified");
        }

        try {
            mDirectory = new File(directory.getCanonicalPath());
        }
        catch (IOException e) {
            mDirectory = directory;
        }

        if (format == null) {
            throw new NullPointerException
                ("FileLogStreamFactory date format not specified");
        }

        mDateFormat = format;

        if (extension == null) {
            mExtension = "";
        }
        else {
            mExtension = extension;
        }
    }

    public OutputStream openOutputStream(Date date) throws IOException {
        if (!mDirectory.exists()) {
            if (!mDirectory.mkdirs()) {
                throw new IOException("Unable to create directory: \"" + 
                                      mDirectory + '"');
            }
        }
        
        String fileName = mDirectory.getName() + '-';
        synchronized (mDateFormat) {
            fileName += mDateFormat.format(date);
        }
        
        File file = new File(mDirectory, fileName + mExtension);
        return new FileOutputStream(file.getPath(), true);
    }
}

