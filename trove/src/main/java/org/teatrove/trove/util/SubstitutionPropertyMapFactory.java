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

import java.io.IOException;

public class SubstitutionPropertyMapFactory implements PropertyMapFactory {

    private PropertyMap substitutions;
    private PropertyMapFactory factory;
    
    public SubstitutionPropertyMapFactory(PropertyMapFactory factory) {
        this.factory = factory;
    }
    
    public SubstitutionPropertyMapFactory(PropertyMapFactory factory,
                                          PropertyMap substitutions) {
        this.factory = factory;
        this.substitutions = substitutions;
    }
    
    @Override
    public PropertyMap createProperties() 
        throws IOException {
        
        return createProperties(null);
    }

    @Override
    public PropertyMap createProperties(PropertyChangeListener listener)
        throws IOException {
        
        PropertyMap props = factory.createProperties(listener);
        substitute(props);
        return props;
    }
    
    @SuppressWarnings({ "unchecked" })
    protected void substitute(PropertyMap props) {
        Object[] keys = props.keySet().toArray(new Object[props.size()]);
        for (Object key : keys) {
            Object value = props.get(key);
            if (value != null) {
                value = SubstitutionFactory.substitute
                (
                    value.toString(), this.substitutions
                );

                props.put(key, value);
            }
        }
    }
}
