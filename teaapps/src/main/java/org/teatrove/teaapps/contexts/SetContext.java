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
package org.teatrove.teaapps.contexts;

import java.util.Set;

public class SetContext {

	public boolean add(Set set, Object object) {
		if (set == null || object == null) {
			return false;
		}
		
		return set.add(object);
	}

	public void clear(Set set) {
		if (set == null) {
			return;
		}
		
		set.clear();
	}

	public boolean contains(Set set, Object object) {
		if (set == null || object == null) {
			return false;
		}
		
		return set.contains(object);
	}
	
	public boolean equals(Set set1, Set set2) {
		if (set1 == null || set2 == null) {
			return set1 == null && set2 == null;
		}
		
		return set1.equals(set2);
	}

	public boolean isEmpty(Set set) {
		if (set == null) {
			return true;
		}
		
		return set.isEmpty();
	}

	public boolean remove(Set set, Object object) {
		if (set == null || object == null) {
			return false;
		}
		
		return set.remove(object);
	}

	public int size(Set set) {
		if (set == null) {
			return -1;
		}
		
		return set.size();
	}

	public Object[] toArray(Set set) {
		return set.toArray();
	}

}
