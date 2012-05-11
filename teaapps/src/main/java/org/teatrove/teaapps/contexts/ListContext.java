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
 * Custom Tea context that provides access to {@link List}s including creation,
 * modification, and convenience methods.
 * 
 * @author Scott Jappinen
 */
public class ListContext {    
    
    /**
     * Add the given value to the given list.
     * 
     * @param <T> The component type of the list
     * 
     * @param list The list to add to
     * @param value The value to add
     * 
     * @return <code>true</code> if the value was added,
     *         <code>false</code> otherwise
     *         
     * @see List#add(Object)
     */
    public <T> boolean add(List<T> list, T value) {
        return list.add(value);
    }

    /**
     * Insert the given value to the given list at the given index. This will
     * insert the value at the index shifting the elements accordingly.
     * 
     * @param <T> The component type of the list
     * 
     * @param list The list to add to
     * @param index The index to add at
     * @param value The value to add
     *         
     * @see List#add(int, Object)
     */
    public <T> void add(List<T> list, int index, T value) {
        list.add(index, value);
    }

    /**
     * Add all items of the given collection to the given list.
     * 
     * @param <T> The component type of the list
     * 
     * @param listToAddTo The list to add to
     * @param collectionToAdd The elements to add to the list
     * 
     * @return <code>true</code> if all items were added,
     *         <code>false</code> otherwise
     *         
     * @see List#addAll(Collection)
     */
    public <T> boolean addAll(List<T> listToAddTo, 
                              Collection<? extends T> collectionToAdd) {
        return listToAddTo.addAll(collectionToAdd);
    }

    /**
     * Insert all items of the given collection at the given index to the given 
     * list.
     * 
     * @param <T> The component type of the list
     * 
     * @param listToAddTo The list to add to
     * @param index The index to insert at
     * @param collectionToAdd The elements to add to the list
     * 
     * @return <code>true</code> if all items were added,
     *         <code>false</code> otherwise
     *         
     * @see List#addAll(int, Collection)
     */
    public <T> boolean addAll(List<T> listToAddTo, int index, 
                              Collection<? extends T> collectionToAdd) {
        return listToAddTo.addAll(collectionToAdd);
    }

    /**
     * Clear all elements from the given list.
     * 
     * @param list The list to clear
     * 
     * @see List#clear()
     */
    public void clear(List<?> list) {
        list.clear();
    }
    
    /**
     * Check if the given list contains the given object.
     * 
     * @param list The list to search in
     * @param obj The object to search for
     * 
     * @return <code>true</code> if the list contains the object,
     *         <code>false</code> otherwise
     *         
     * @see List#contains(Object)
     */
    public boolean contains(List<?> list, Object obj) {
        return list.contains(obj);
    }

    /**
     * Check if the given list contains all of the given objects.
     * 
     * @param list The list to search in
     * @param collection The collection of elements to search for
     * 
     * @return <code>true</code> if the list contains all of the objects,
     *         <code>false</code> otherwise
     *         
     * @see List#containsAll(Collection)
     */
    public boolean containsAll(List<?> list, Collection<?> collection) {
        return list.containsAll(collection);
    }

    /**
     * Create a new and empty {@link ArrayList}.
     * 
     * @return the created array list
     */
    public List<?> createArrayList() {
        return new ArrayList<Object>();
    }
    
    /**
     * Get the first index within the list of a matching object or 
     * <code>-1</code> if not found.
     * 
     * @param list The list to search in
     * @param obj The object to search for
     * 
     * @return The first matching index or <code>-1</code> if not found
     * 
     * @see List#indexOf(Object)
     */
    public int indexOf(List<?> list, Object obj) {
        return list.indexOf(obj);
    }
    
    /**
     * Get the last index within the list of a matching object or 
     * <code>-1</code> if not found.
     * 
     * @param list The list to search in
     * @param obj The object to search for
     * 
     * @return The last matching index or <code>-1</code> if not found
     * 
     * @see List#lastIndexOf(Object)
     */
    public int lastIndexOf(List<?> list, Object obj) {
        return list.lastIndexOf(obj);
    }

    /**
     * Remove the given object from the given list.
     * 
     * @param list The list to remove from
     * @param obj The object to remove
     * 
     * @return <code>true</code> if the object was found and removed,
     *         <code>false</code> false otherwise
     *         
     * @see List#remove(Object)
     */
    public boolean remove(List<?> list, Object obj) {
        return list.remove(obj);
    }

    /**
     * Remove the object at the given index from the given list.
     * 
     * @param list The list to remove from
     * @param index The index of the object to remove
     * 
     * @return <code>true</code> if the object was removed,
     *         <code>false</code> false otherwise
     *         
     * @see List#remove(int)
     */
    public Object remove(List<?> list, int index) {
        return list.remove(index);
    }

    /**
     * Remove all matching objects from the given collection from the given 
     * list. If a given element in the collection is not contained in the list,
     * then that element is ignored.
     * 
     * @param list The list to remove from
     * @param collection The collection of objects to remove
     * 
     * @return <code>true</code> if any elements were removed from the list,
     *         <code>false</code> otherwise
     *         
     * @see List#removeAll(Collection)
     */
    public boolean removeAll(List<?> list, Collection<?> collection) {
        return list.removeAll(collection);
    }

    /**
     * Set the value at the given index in the given list. If the value is
     * properly set, the previous value will be returned.
     * 
     * @param list The list of set in
     * @param index The index within the list to set at
     * @param obj The object to set in the list
     * 
     * @return The previously set value
     * 
     * @see List#set(int, Object)
     */
    public <T> T set(List<T> list, int index, T obj) {
        return list.set(index, obj);
    }

    /**
     * Get the size of the given list.
     * 
     * @param list The associated list
     * 
     * @return The size of the list
     * 
     * @see List#size()
     */
    public int size(List<?> list) {
        return list.size();
    }
    
    /**
     * Get a portion of the given list as a new list.
     * 
     * @param list The list to retrieve a portion of
     * @param fromIndex The first index, inclusive, to start from
     * @param toIndex The last index, inclusive, to end at
     * 
     * @return A new list containing the given portion of the list
     * 
     * @see List#subList(int, int)
     */
    public <T> List<T> subList(List<T> list, int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    /**
     * Convert the given list to an array of the given array type.
     * 
     * @param list The list to convert
     * @param arrayType The type of array to create
     * 
     * @return The array of elements in the list
     */
    public Object[] toArray(List<?> list, Class<?> arrayType) {
        int[] dims = findArrayDimensions(list, arrayType);
        Object[] typedArray = (Object[]) Array.newInstance(arrayType, dims);
        return list.toArray(typedArray);
    }

    /**
     * Create a new {@link ArrayList} instance containing all elements of the
     * given list.  This is not a deep clone operation, so the underlying
     * elements are copied by memory and not cloned.
     * 
     * @param list The list to copy
     * 
     * @return The newly created list
     */
    public <T> List<T> cloneList(List<T> list) {
        return new ArrayList<T>(list);
    }
    
    /**
     * Create a new {@link ArrayList} instance adding all values of the given
     * lists together.  The second list will essentially be added after the
     * first list. Note that any duplicates between lists will remain as
     * duplicates.
     * 
     * @param list1 The first list to merge
     * @param list2 The second list to merge
     * 
     * @return The newly created list containing the merged lists
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<?> mergeLists(List<?> list1, List<?> list2) {
        List list = new ArrayList(list1.size() + list2.size());
        list.addAll(list1);
        list.addAll(list2);
        return list;
    }

    /**
     * Join the contents of the given list separated by the given separator.
     * For example, the list [a, b, c] with separator ' ' would result in:
     * <code>a b c</code>.
     * 
     * @param list  The list to join
     * @param separator The separator between values
     * 
     * @return The resulting string
     */
    public String join(List<?> list, String separator) {
        int size = list.size();
        StringBuilder buffer = new StringBuilder(512);
        for (int i = 0; i < size; i++) {
            Object item = list.get(i);
            if (i > 0) { buffer.append(separator); }
            buffer.append(item.toString());
        }
        
        return buffer.toString();
    }
    
    /**
     * If the elements of the list are arrays, get the max length of each needed
     * dimension, otherwise return an int[] containing one element, the length
     * of the list.
     */
    private int[] findArrayDimensions(List<?> list, Class<?> arrayType) {
        List<int[]> elementDims = new LinkedList<int[]>();
        int dimCount = 0;
        for (Object o : list) {
            if (o != null && o.getClass().isArray() && 
                isAssignableFromArray(arrayType, o.getClass().getComponentType())) {
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

    private boolean isAssignableFromArray(Class<?> arrayType, 
                                          Class<?> componentType) {
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
            dims.add(Integer.valueOf(arrLength));
            // Find the max length(s) of any additional dimensions
            for (int i = 0; i < arrLength; i++) {
                Object element = Array.get(arr, i);
                int[] elementDims = findArrayDimensions(element);
                resize(dims, elementDims.length+1);
                for (int j = 0; j < elementDims.length; j++) {
                    int elementDim = elementDims[j];
                    dims.set(j + 1, Integer.valueOf(Math.max(dims.get(j + 1).intValue(), elementDim)));
                }
            }

            // Copy the List<Integer> to an int[] since toArray(T a) does work with primitives
            int[] dimsArr = new int[dims.size()];
            for (int i = 0; i < dims.size(); i++) {
                dimsArr[i] = dims.get(i).intValue();
            }
            return dimsArr;
        }
        return new int[0];
    }

    private void resize(List<Integer> dims, int i) {
        while(dims.size() < i) {
            dims.add(Integer.valueOf(0));
        }
    }
}
