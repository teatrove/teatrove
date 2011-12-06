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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Scott Jappinen
 */
public class ListContext {    
    
    public boolean add(List<Object> list, Object value) {
        return list.add(value);
    }

    public void add(List<Object> list, int index, Object value) {
        list.add(index, value);
    }

    public boolean addAll(List<Object> listToAddTo, Collection<Object> collectionToAdd) {
        return listToAddTo.addAll(collectionToAdd);
    }

    public boolean addAll(List<Object> listToAddTo, int index, Collection<Object> collectionToAdd) {
        return listToAddTo.addAll(collectionToAdd);
    }

    public void clear(List list) {
        list.clear();
    }
    
    public boolean contains(List list, Object obj) {
        return list.contains(obj);
    }

    public boolean containsAll(List<Object> list, Collection<Object> collection) {
        return list.containsAll(collection);
    }

    public List<?> createArrayList() {
        return new ArrayList();
    }
    
    public int indexOf(List list, Object obj) {
        return list.indexOf(obj);
    }
    
    public int lastIndexOf(List list, Object obj) {
        return list.lastIndexOf(obj);
    }

    public boolean remove(List list, Object obj) {
        return list.remove(obj);
    }

    public Object remove(List list, int index) {
        return list.remove(index);
    }

    public boolean removeAll(List<Object> list, Collection<Object> collection) {
        return list.removeAll(collection);
    }

    public Object set(List<Object> list, int index, Object obj) {
        return list.set(index, obj);
    }

    public int size(List list) {
        return list.size();
    }
    
    public List subList(List list, int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    //public Object[] toArray(List<Object> list, Class arrayType) {
    //    Object[] typedArray = (Object[]) Array.newInstance(arrayType, list.size());
    //    return list.toArray(typedArray);
    //}
    
    public Object[] toArray(List list, Class arrayType) {
        int[] dims = findArrayDimensions(list, arrayType);
        Object[] typedArray = (Object[]) Array.newInstance(arrayType, dims);
        return list.toArray(typedArray);
    }

    /**
     * If the elements of the list are arrays, get the max length of each needed
     * dimension, otherwise return an int[] containing one element, the length of the list.
     * @param list
     * @param arrayType
     * @return
     */
    private int[] findArrayDimensions(List list, Class arrayType) {
        List<int[]> elementDims = new LinkedList<int[]>();
        int dimCount = 0;
        for (Object o : list) {
            if(o != null && o.getClass().isArray() && isAssignableFromArray(arrayType, o.getClass().getComponentType())) {
                final int[] dimensions = findArrayDimensions(o);
                elementDims.add(dimensions);
                dimCount = Math.max(dimCount, dimensions.length);
            }
        }

        int[] dimsArr = new int[dimCount + 1];
        // First dimension length is always size of given list
        dimsArr[0] = list.size();
        // Find the max length of each additional dimension
        for (int[] childDim : elementDims) {
            for (int i = 0; i < childDim.length; i++) {
                dimsArr[i + 1] = Math.max(dimsArr[i + 1], childDim[i]);
            }
        }
        return dimsArr;
    }

    private boolean isAssignableFromArray(Class arrayType, Class<?> componentType) {
        if(componentType.isArray()) {
            return isAssignableFromArray(arrayType, componentType.getComponentType());
        } else {
            return arrayType.isAssignableFrom(componentType);
        }
    }

    private int[] findArrayDimensions(Object arr) {
        if(arr != null && arr.getClass().isArray()) {
            // For each element, find its array dim
            int arrLength = Array.getLength(arr);
            List<Integer> dims = new ArrayList<Integer>();
            dims.add(arrLength);
            // Find the max length(s) of any additional dimensions
            for (int i = 0; i < arrLength; i++) {
                Object element = Array.get(arr, i);
                int[] elementDims = findArrayDimensions(element);
                resize(dims, elementDims.length+1);
                for (int j = 0; j < elementDims.length; j++) {
                    int elementDim = elementDims[j];
                    dims.set(j + 1, Math.max(dims.get(j + 1), elementDim));
                }
            }

            // Copy the List<Integer> to an int[] since toArray(T a) does work with primitives
            int[] dimsArr = new int[dims.size()];
            for (int i = 0; i < dims.size(); i++) {
                dimsArr[i] = dims.get(i);
            }
            return dimsArr;
        }
        return new int[0];
    }

    private void resize(List<Integer> dims, int i) {
        while(dims.size() < i) {
            dims.add(0);
        }
    }
}
