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

package org.teatrove.trove.classfile;

/**
 * 
 *
 * @author Nick Hagan
 */
public class GenericDescriptor {
    private String prefix;
    private String signature;
    private StringBuilder types = new StringBuilder(128);

    public GenericDescriptor() {
        super();
    }

    public GenericDescriptor(String signature) {
        this("", signature);
    }

    public GenericDescriptor(String prefix, String signature) {
        this.prefix = prefix;
        this.signature = signature;
    }

    public String getPrefix() {
        if (this.prefix == null) {
            this.prefix = '<' + this.types.toString() + '>';
        }

        return this.prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSignature() {
        return this.signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getTypes() {
        return this.types.toString();
    }

    public void addType(String name, String signature) {
        this.types.append(name).append(':').append(signature);
    }

    public int hashCode() {
        return (17 * this.signature.hashCode()) ^ (11 * this.types.hashCode());
    }

    public boolean equals(Object other) {
        if (other == this) { return true; }
        else if (other instanceof GenericDescriptor) {
            GenericDescriptor desc = (GenericDescriptor) other;
            return this.getSignature().equals(desc.getSignature()) &&
                   this.getTypes().equals(desc.getTypes());
        }
        else { return false; }
    }
}
