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

package org.teatrove.trove.util;

import java.io.Reader;

public class PropertyMapFactoryProvider {

    private PropertyMapFactoryProvider() {
        super();
    }
    
    public static 
    PropertyMapFactory createPropertyMapFactory(String path, Reader reader) {
        return createPropertyMapFactory(path, reader,
                                        SubstitutionFactory.getDefaults());
    }

    public static 
    PropertyMapFactory createPropertyMapFactory(String path, Reader reader,
                                                PropertyMap substitutions) {
        // get associated factory
        PropertyMapFactory factory = null;
        if (path.endsWith(".properties")) {
            factory = new SimplePropertyMapFactory(reader);
        }
        else if (path.endsWith(".xml")) {
            factory = new XMLPropertyMapFactory(reader, true);
        }
        else {
            factory = new SimplePropertyMapFactory(reader);
        }
        
        // load substitutions
        if (substitutions != null && substitutions.size() > 0) {
            factory = 
                new SubstitutionPropertyMapFactory(factory, substitutions);
        }

        // return factory
        return factory;
    }
}
