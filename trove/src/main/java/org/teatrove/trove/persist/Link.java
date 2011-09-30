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

package org.teatrove.trove.persist;

import java.io.*;

/**
 * When a Link is saved into an ObjectRepository, the linked value is saved
 * in a separate ObjectRepository file. When a Link is read, the linked value
 * is not retrieved until the first time "get" is called.
 * <p>
 * If a Link is written to an ObjectOutputStream, but not by an
 * ObjectRepository, the linked value is dropped. Likewise, if a Link is read
 * from an ObjectInputStream, but not by an ObjectRepository, the linked value
 * is null.
 *
 * @author Brian S O'Neill
 * @see ObjectRepository#local
 */
public class Link implements Externalizable {
    static final long serialVersionUID = 1;

    private transient volatile Object mValue;

    // Fields used for lazily retrieving value.
    private transient long mId;
    private transient volatile ObjectRepository mRepository;

    public Link() {
    }

    public Link(Object value) {
        mValue = value;
    }

    public Object get() throws IOException, ClassNotFoundException {
        if (mValue == null) {
            ObjectRepository repository = mRepository;
            if (repository != null) {
                mValue = repository.retrieveObject(mId);
                mRepository = null;
            }
        }
        return mValue;
    }

    public void set(Object value) {
        mRepository = null;
        mValue = value;
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        Object value;
        try {
            value = get();
        }
        catch (IOException e) {
            value = null;
        }
        catch (ClassNotFoundException e) {
            value = null;
        }
        ObjectRepository rep = ObjectRepository.local.get();
        out.writeLong(rep == null ? 0 : rep.saveObject(value));
    }
    
    public void readExternal(ObjectInput in)
        throws IOException, ClassNotFoundException
    {
        mId = in.readLong();
        mRepository = ObjectRepository.local.get();
    }
}
