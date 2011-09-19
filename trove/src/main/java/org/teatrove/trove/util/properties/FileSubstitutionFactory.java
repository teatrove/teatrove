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

package org.teatrove.trove.util.properties;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Properties;

import org.teatrove.trove.log.Syslog;
import org.teatrove.trove.util.PropertyMap;

/*
 * @author: Josh Yockey -- Feb 11, 2005
 */
public class FileSubstitutionFactory implements SubstitutionFactory {

    private Properties m_subs = new Properties();
    
    public void init(PropertyMap props) {
        String sFile = props.getString("file");
        if (sFile == null) {
            Syslog.error("No file specified for FileSubstitutionFactory");
            return;
        }

        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sFile));
            m_subs.load(bis);
        } catch (Exception e) {
            Syslog.error("Unable to load property substitutions from " + sFile);
            Syslog.error(e);
        }
    }

    public String getSubstitution(String key) {
        return m_subs.getProperty(key);
    }

}
