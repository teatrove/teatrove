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

import java.lang.ref.*;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;
import org.teatrove.trove.util.ReadWriteLock;

/**
 * A BTree is a balanced tree data structure whose entries are usually stored
 * in a file. BTrees can efficiently scale to contain millions of entries. For
 * this reason, BTrees are often used for indexing records in databases.
 * This class is thread-safe.
 * <p>
 * For maximum performance, BTree keys and values should be kept small. BTree
 * values are often referenced indirectly via pointers. Consider using
 * {@link ObjectRepositoryMap} with a BTree passed into it as a general
 * purpose persistent map.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 16 <!-- $-->, <!--$$JustDate:--> 02/01/03 <!-- $-->
 * @see ObjectRepositoryMap
 */
public class BTree implements PersistentSortedMapKernel {
    /**
     * Convenience method that wraps a new BTree in a
     * {@link PersistentSortedMapView}.
     */
    public static PersistentSortedMap createMap(StorageStrategy strategy)
        throws IOException
    {
        return new PersistentSortedMapView(new BTree(strategy));
    }

    /**
     * Convenience method that wraps a new BTree in a
     * {@link PersistentSortedMapView}.
     */
    public static PersistentSortedMap createMap(StorageStrategy strategy,
                                                Comparator comparator)
        throws IOException
    {
        return new PersistentSortedMapView(new BTree(strategy, comparator));
    }

    // These are modifed versions of the binary search algorithms provided in
    // java.util.Arrays. A high element must be provided, and keys are stored
    // in every other array element.

    static int binarySearch2(Object[] a, int high2, Object key) {
        int low2 = 0;
        while (low2 <= high2) {
            int mid2 = ((low2 + high2) / 2) & -2;
            Object midVal = a[mid2];
            int cmp = ((Comparable)midVal).compareTo(key);
            
            if (cmp < 0) {
                low2 = mid2 + 2;
            }
            else if (cmp > 0) {
                high2 = mid2 - 2;
            }
            else {
                return mid2; // key found
            }
        }
        return ~low2;  // key not found.
    }
    
    static int binarySearch2(Object[] a, int high2, Object key, Comparator c) {
        if (c == null) {
            return binarySearch2(a, high2, key);
        }
        
        int low2 = 0;
        while (low2 <= high2) {
            int mid2 = ((low2 + high2) / 2) & -2;
            Object midVal = a[mid2];
            int cmp = c.compare(midVal, key);
            
            if (cmp < 0) {
                low2 = mid2 + 2;
            }
            else if (cmp > 0) {
                high2 = mid2 - 2;
            }
            else {
                return mid2; // key found
            }
        }
        return ~low2;  // key not found.
    }

    final StorageStrategy mStorageStrategy;

    // Maximum number of entries per node.
    final int mMaxNodeSize;
    // Maximum number of entries per node, times 2.
    final int mMaxNodeSize2;

    // Minumum number of entries per node.
    final int mMinNodeSize;
    // Minumum number of entries per node, times 2.
    final int mMinNodeSize2;

    final Comparator mComparator;
    final ReadWriteLock mLock;

    long mRootId;
    Node mRoot;

    private int mTotalSize;

    public BTree(StorageStrategy strategy) throws IOException {
        this(strategy, null);
    }

    public BTree(StorageStrategy strategy, Comparator comparator)
        throws IOException
    {
        mStorageStrategy = strategy;
        mMaxNodeSize = strategy.getMaxNodeSize();
        mMaxNodeSize2 = mMaxNodeSize * 2;
        mMinNodeSize = mMaxNodeSize / 2;
        mMinNodeSize2 = mMinNodeSize * 2;
        mComparator = comparator;
        mLock = strategy.lock();

        mRootId = strategy.loadRootNodeId();
        if (mRootId >= 0) {
            mRoot = loadNode(mRootId);
            mTotalSize = -1;
        }
        else {
            mTotalSize = 0;
        }
    }

    public Comparator comparator() {
        return mComparator;
    }

    public int size() throws IOException {
        try {
            mLock.acquireReadLock();
            if (mTotalSize < 0) {
                mTotalSize = mRoot == null ? 0 : mRoot.size();
            }
            return mTotalSize;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public boolean isEmpty() throws IOException {
        try {
            mLock.acquireReadLock();
            return mRoot == null;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public boolean containsKey(Object key) throws IOException {
        try {
            mLock.acquireReadLock();
            return mRoot.containsKey(key);
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        catch (NullPointerException e) {
            if (mRoot == null) {
                return false;
            }
            throw e;
        }
        finally {
            mLock.releaseLock();
        }
    }
    
    public boolean containsValue(Object value) throws IOException {
        try {
            mLock.acquireReadLock();
            return mRoot.containsValue(value);
        }
        catch (NullPointerException e) {
            if (mRoot == null) {
                return false;
            }
            throw e;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public Object get(Object key) throws IOException {
        try {
            mLock.acquireReadLock();
            return mRoot.get(key);
        }
        catch (NullPointerException e) {
            if (mRoot == null) {
                return null;
            }
            throw e;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public PersistentMap.Entry getEntry(Object key) throws IOException {
        try {
            mLock.acquireReadLock();
            return mRoot.getEntry(key);
        }
        catch (NullPointerException e) {
            if (mRoot == null) {
                return null;
            }
            throw e;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public Object put(Object key, Object value) throws IOException {
        try {
            mLock.acquireWriteLock();
            mStorageStrategy.begin();
            
            if (mRoot == null) {
                mRoot = new Leaf(0, new Object[mMaxNodeSize2]);
                mRoot.put(mRootId = allocLeafNode(), key, value);
                mStorageStrategy.saveRootNodeId(mRootId);
                mStorageStrategy.commit();
                return null;
            }
            
            Object old = mRoot.put(mRootId, key, value);
            
            if (old instanceof Split) {
                Split split = (Split)old;
                
                // Create a new root node.
                Object[] entries = new Object[mMaxNodeSize2];
                entries[0] = split.mKey;
                entries[1] = split.mValue;
                long[] childrenIds = new long[mMaxNodeSize + 1];
                childrenIds[0] = mRootId;
                childrenIds[1] = split.mHighNodeId;
                Reference[] children = new Reference[mMaxNodeSize + 1];
                children[0] = new SoftReference(mRoot);
                children[1] = split.mHighNodeRef;
                mRoot = new NonLeaf(2, entries, childrenIds, children);
                saveNode(mRootId = allocNonLeafNode(), mRoot);
                mStorageStrategy.saveRootNodeId(mRootId);
                
                mStorageStrategy.commit();
                return null;
            }
            
            mStorageStrategy.commit();
            return old;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public Object remove(Object key) throws IOException {
        try {
            mLock.acquireUpgradableLock();
            if (mRoot == null) {
                return null;
            }
            
            // Maps node ids to node objects that require saving. If the node
            // object is null, it should be freed. The use of this map has
            // several purposes.
            // 1. Transaction and write lock is used only if entry was found.
            // 2. Modified nodes get saved once, even if they underflow.
            // 3. Hard references to Node objects prevent them from being 
            //    garbagecollected prematurely. This works with #2, the
            //    delayed save.
            Map unsavedNodes = new HashMap(7);
            Object old = mRoot.remove(unsavedNodes, mRootId, key);

            if (!unsavedNodes.isEmpty()) {
                // An element was removed, so save changes.
                try {
                    mLock.acquireWriteLock();
                    mStorageStrategy.begin();
                    
                    if (mRoot.mSize2 <= 0) {
                        // Delete the root.
                        unsavedNodes.put(new Long(mRootId), null);
                        if (mRoot instanceof Leaf) {
                            // The last entry was just removed.
                            mRoot = null;
                            mRootId = -1;
                            mTotalSize = 0;
                        }
                        else {
                            // Promote the one remaining child to the root. The last
                            // child remaining is always at index 0 because
                            // repairUnderflow always merges child nodes to the left
                            // child.
                            NonLeaf nRoot = (NonLeaf)mRoot;
                            mRoot = nRoot.getChild(0);
                            mRootId = nRoot.getChildId(0);
                        }
                        mStorageStrategy.saveRootNodeId(mRootId);
                    }
                    
                    Iterator it = unsavedNodes.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry e = (Map.Entry)it.next();
                        long id = ((Long)e.getKey()).longValue();
                        Node node = (Node)e.getValue();
                        if (node == null) {
                            freeNode(id);
                        }
                        else {
                            saveNode(id, node);
                        }
                    }
                    
                    decrementTotalSize();
                    mStorageStrategy.commit();
                }
                finally {
                    mLock.releaseLock();
                }
            }
            
            return old;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public Object firstKey() throws IOException {
        try {
            mLock.acquireReadLock();
            return mRoot.firstKey();
        }
        catch (NullPointerException e) {
            if (mRoot == null) {
                return NO_KEY;
            }
            throw e;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public Object lastKey() throws IOException {
        try {
            mLock.acquireReadLock();
            return mRoot.lastKey();
        }
        catch (NullPointerException e) {
            if (mRoot == null) {
                return NO_KEY;
            }
            throw e;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public PersistentMap.Entry firstEntry() throws IOException {
        try {
            mLock.acquireReadLock();
            return mRoot.firstEntry();
        }
        catch (NullPointerException e) {
            if (mRoot == null) {
                return null;
            }
            throw e;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public PersistentMap.Entry lastEntry() throws IOException {
        try {
            mLock.acquireReadLock();
            return mRoot.lastEntry();
        }
        catch (NullPointerException e) {
            if (mRoot == null) {
                return null;
            }
            throw e;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public Object nextKey(Object key) throws IOException {
        try {
            mLock.acquireReadLock();
            return mRoot.nextKey(key);
        }
        catch (NullPointerException e) {
            if (mRoot == null) {
                return NO_KEY;
            }
            throw e;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public Object previousKey(Object key) throws IOException {
        try {
            mLock.acquireReadLock();
            return mRoot.previousKey(key);
        }
        catch (NullPointerException e) {
            if (mRoot == null) {
                return NO_KEY;
            }
            throw e;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public PersistentMap.Entry nextEntry(Object key) throws IOException {
        try {
            mLock.acquireReadLock();
            return mRoot.nextEntry(key);
        }
        catch (NullPointerException e) {
            if (mRoot == null) {
                return null;
            }
            throw e;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public PersistentMap.Entry previousEntry(Object key) throws IOException {
        try {
            mLock.acquireReadLock();
            return mRoot.previousEntry(key);
        }
        catch (NullPointerException e) {
            if (mRoot == null) {
                return null;
            }
            throw e;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public void clear() throws IOException {
        try {
            mLock.acquireWriteLock();
            if (mRoot != null) {
                if (!mStorageStrategy.clear()) {
                    clear(mRootId);
                }
                mRoot = null;
                mRootId = -1;
                mTotalSize = 0;
                mStorageStrategy.saveRootNodeId(-1);
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public void clear(Object fromKey, Object toKey) throws IOException {
        // Ranges are cleared by iterating and removing one at a time. A more
        // efficient implementation would delete whole nodes at once.

        Comparator c = mComparator;

        try {
            mLock.acquireWriteLock();
            if (fromKey == NO_KEY) {
                if (toKey == NO_KEY) {
                    clear();
                    return;
                }
                
                // Iterate in reverse, removing entries until there's nothing left.
                while (true) {
                    if ((toKey = previousKey(toKey)) == NO_KEY) {
                        break;
                    }
                    remove(toKey);
                }
                
                return;
            }

            while (true) {
                remove(fromKey);
                if ((fromKey = nextKey(fromKey)) == NO_KEY) {
                    break;
                }
                if (toKey != NO_KEY) {
                    int result;
                    if (c == null) {
                        result = ((Comparable)fromKey).compareTo(toKey);
                    }
                    else {
                        result = c.compare(fromKey, toKey);
                    }
                    if (result >= 0) {
                        break;
                    }
                }
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public int copyKeysInto(Object[] a, int index) throws IOException {
        try {
            mLock.acquireReadLock();
            return mRoot.copyKeysInto(a, index);
        }
        catch (NullPointerException e) {
            if (mRoot == null) {
                return index;
            }
            throw e;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public int copyEntriesInto(Object[] a, int index) throws IOException {
        try {
            mLock.acquireReadLock();
            return mRoot.copyEntriesInto(a, index);
        }
        catch (NullPointerException e) {
            if (mRoot == null) {
                return index;
            }
            throw e;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        finally {
            mLock.releaseLock();
        }
    }

    public ReadWriteLock lock() {
        return mLock;
    }

    final Node loadNode(long id) throws IOException {
        NodeData data = new NodeData();
        mStorageStrategy.loadNode(id, data);
        if (data.childrenIds == null) {
            return new Leaf(data.size * 2, data.entries);
        }
        else {
            return new NonLeaf(data.size * 2, data.entries,
                               data.childrenIds,
                               new Reference[data.childrenIds.length]);
        }
    }

    final void saveNode(long id, Node node) throws IOException {
        NodeData data = new NodeData();
        data.size = node.mSize2 / 2;
        data.entries = node.mEntries;
        if (node instanceof NonLeaf) {
            data.childrenIds = ((NonLeaf)node).mChildrenIds;
        }
        mStorageStrategy.saveNode(id, data);
    }

    final long allocLeafNode() throws IOException {
        return mStorageStrategy.allocLeafNode();
    }

    final long allocNonLeafNode() throws IOException {
        return mStorageStrategy.allocNonLeafNode();
    }

    final void freeNode(long id) throws IOException {
        mStorageStrategy.freeNode(id);
    }

    final void clear(long id) throws IOException {
        NodeData data = new NodeData();
        mStorageStrategy.loadNodeExceptEntries(id, data);
        if (data.childrenIds != null) {
            int limit = data.size;
            for (int i = 0; i <= limit; i++) {
                clear(data.childrenIds[i]);
            }
        }
        mStorageStrategy.freeNode(id);
    }

    final int size(long id) throws IOException {
        NodeData data = new NodeData();
        mStorageStrategy.loadNodeExceptEntries(id, data);
        int count = data.size;
        if (data.childrenIds != null) {
            int limit = count;
            for (int i = 0; i <= limit; i++) {
                count += size(data.childrenIds[i]);
            }
        }
        return count;
    }

    final void incrementTotalSize() {
        if (mTotalSize >= 0) {
            mTotalSize++;
        }
    }

    final void decrementTotalSize() {
        if (mTotalSize > 0) {
            mTotalSize--;
        }
    }

    final int dump() throws IOException {
        return mRoot.dump("");
    }

    public interface StorageStrategy {
        int getMaxNodeSize();

        int loadTotalSize() throws IOException;

        void saveTotalSize(int size) throws IOException;

        long loadRootNodeId() throws IOException;

        void saveRootNodeId(long id) throws IOException;
        
        void loadNodeExceptEntries(long id, NodeData data) throws IOException;

        void loadNode(long id, NodeData data) throws IOException;

        void saveNode(long id, NodeData data) throws IOException;

        long allocLeafNode() throws IOException;

        long allocNonLeafNode() throws IOException;

        void freeNode(long id) throws IOException;

        /**
         * Return false if clear not supported.
         */
        boolean clear() throws IOException;

        /**
         * The BTree will access this StorageStrategy safely using this lock,
         * and it will also return this lock from its own lock method.
         */
        ReadWriteLock lock();

        /**
         * Begin a transaction.
         * @see TxFileBuffer#begin
         */
        void begin() throws IOException;

        /**
         * Commits a transaction.
         * @see TxFileBuffer#commit
         */
        boolean commit() throws IOException;

        /**
         * Forces changes to disk.
         * @see TxFileBuffer#force
         */
        boolean force() throws IOException;
    }

    public static class NodeData {
        /**
         * Number of key-value pairs actually in array.
         */
        public int size;

        /**
         * Is null if data is for leaf node, otherwise array length must be
         * one plus the max node size.
         */
        public long[] childrenIds; 

        /**
         * Array length must be twice the max node size.
         */
        public Object[] entries;
    }

    private abstract class Node {
        // Number of used elements in mEntries.
        int mSize2;
        // Even elements are keys, odd elements are values.
        Object[] mEntries;

        /**
         * Entries array alternates key and value. Entries must be sorted
         * according to key order.
         * 
         * @param size2 number of used elements in entries array
         */
        Node(int size2, Object[] entries) {
            mSize2 = size2;
            mEntries = entries;
        }

        /**
         * Returns the size of this node plus the size of all child nodes.
         */
        abstract int size() throws IOException;

        /**
         * Searches in this node for the given key, and if not found, will
         * recurse into any children.
         */
        abstract boolean containsKey(Object key) throws IOException;

        /**
         * Searches in this node for the given value, and if not found, will
         * recurse into any children. (subclass implements recursion)
         */
        boolean containsValue(Object value) throws IOException {
            if (mComparator == null) {
                for (int i = mSize2 - 1; i >= 1; i -= 2) {
                    if (((Comparable)mEntries[i]).compareTo(value) == 0) {
                        return true;
                    }
                }
            }
            else {
                for (int i = mSize2 - 1; i >= 1; i -= 2) {
                    if (mComparator.compare(mEntries[i], value) == 0) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Searches in this node for the given key, and if not found, will
         * recurse into any children.
         */
        abstract Object get(Object key) throws IOException;

        /**
         * Searches in this node for the given key, and if not found, will
         * recurse into any children.
         */
        abstract Entry getEntry(Object key) throws IOException;

        /**
         * Will put entry into either this node or a child node. As a result
         * of this operation, new nodes may be created.
         *
         * @param id id of node so that it may save itself. I see no reason to
         * needlessly store the id in the node when it can be passed in.
         * @return null, old object or Split.
         */
        abstract Object put(long id, Object key, Object value)
            throws IOException;

        /**
         * Will remove entry from this node or child node with matching key.
         * As a result of this operation, nodes may be deleted. After call,
         * node may have underflowed. The caller is responsible for repairing
         * this and saving changes.
         *
         * @param unsavedNodes map of ids to nodes that need to be saved
         * @param id id of node so that it may save itself. I see no reason to
         * needlessly store the id in the node when it can be passed in.
         * @return null or old object.
         */
        abstract Object remove(Map unsavedNodes, long id, Object key)
            throws IOException;

        abstract Object firstKey() throws IOException;

        abstract Object lastKey() throws IOException;
        
        abstract Object nextKey(Object key) throws IOException;

        abstract Object previousKey(Object key) throws IOException;

        abstract Entry firstEntry() throws IOException;

        abstract Entry lastEntry() throws IOException;

        abstract Entry nextEntry(Object key) throws IOException;

        abstract Entry previousEntry(Object key) throws IOException;

        /**
         * Removes the first entry from this node, or a child node. After call,
         * node may have underflowed. The caller is responsible for repairing
         * this.
         */
        abstract Entry removeFirst(Map unsavedNodes, long id)
            throws IOException;

        /**
         * Removes the last entry from this node, or a child node. After call,
         * node may have underflowed. The caller is responsible for repairing
         * this.
         */
        abstract Entry removeLast(Map unsavedNodes, long id)
            throws IOException;

        abstract int copyKeysInto(Object[] a, int index) throws IOException;

        abstract int copyEntriesInto(Object[] a, int index) throws IOException;

        abstract int dump(String indent) throws IOException;
    }

    /**
     * B-Trees always grow at the root, and so leaf nodes will never grow
     * branches. Leaf nodes can split into two leaf nodes.
     */
    private class Leaf extends Node {
        Leaf(int size2, Object[] entries) {
            super(size2, entries);
        }

        int size() throws IOException {
            return mSize2 / 2;
        }

        boolean containsKey(Object key) throws IOException {
            return binarySearch2(mEntries, mSize2 - 2, key, mComparator) >= 0;
        }

        Object get(Object key) throws IOException {
            int index2 = binarySearch2(mEntries, mSize2 - 2, key, mComparator);
            return (index2 >= 0) ? mEntries[index2 + 1] : null;
        }

        Entry getEntry(Object key) throws IOException {
            int index2 = binarySearch2(mEntries, mSize2 - 2, key, mComparator);
            if (index2 >= 0) {
                return new MutableEntry
                    (mEntries[index2], mEntries[index2 + 1]);
            }
            return null;
        }

        Object put(long id, Object key, Object value) throws IOException
        {
            int index2 = binarySearch2(mEntries, mSize2 - 2, key, mComparator);
            Object old;
            if (index2 >= 0) {
                // Slot already exists for the entry, just save the value.
                old = mEntries[index2 + 1];
                mEntries[index2 + 1] = value;
                saveNode(id, this);
                return old;
            }
            
            // Switch index to insertion point.
            index2 = ~index2;

            int maxSize2 = mMaxNodeSize2;

            if (mSize2 < maxSize2) {
                int amt2 = mSize2 - index2;
                if (amt2 > 0) {
                    // Shift array contents to make room for insert.
                    System.arraycopy
                        (mEntries, index2, mEntries, index2 + 2, amt2);
                }
                
                mEntries[index2] = key;
                mEntries[index2 + 1] = value;
                mSize2 += 2;

                saveNode(id, this);
                incrementTotalSize();
                return null;
            }

            // No more room in this leaf node so split it. This now becomes
            // the low node, and a new one is the high node.
            Split split = new Split();

            int lowSize2 = mMinNodeSize2;
            // High size is different from low size if max size is odd.
            int highSize2 = maxSize2 - lowSize2;

            Object[] highEntries = new Object[maxSize2];

            if (index2 == lowSize2) {
                // Insert is at median, and so it is promoted.
                split.mKey = key;
                split.mValue = value;

                // Copy high entries to high node.
                System.arraycopy(mEntries, index2,
                                 highEntries, 0, highSize2);
            }
            else if (index2 < lowSize2) {
                // Insert to low node.
                // Promote median entry.
                split.mKey = mEntries[lowSize2 - 2];
                split.mValue = mEntries[lowSize2 - 1];
                
                // Copy high entries to high node.
                System.arraycopy(mEntries, lowSize2,
                                 highEntries, 0, highSize2);

                int dst2 = index2 + 2;
                int amt2 = lowSize2 - dst2;
                if (amt2 > 0) {
                    // Shift array contents to make room for insert.
                    System.arraycopy(mEntries, index2, mEntries, dst2, amt2);
                }

                mEntries[index2] = key;
                mEntries[index2 + 1] = value;
            }
            else if (index2 > lowSize2) {
                // Insert to high node.
                // Promote median entry.
                split.mKey = mEntries[lowSize2];
                split.mValue = mEntries[lowSize2 + 1];

                // Copy high entries below insert to high node.
                int src2 = lowSize2 + 2;
                int amt2 = index2 - src2;
                if (amt2 > 0) {
                    System.arraycopy(mEntries, src2, highEntries, 0, amt2);
                }

                highEntries[amt2] = key;
                highEntries[amt2 + 1] = value;

                amt2 = mSize2 - index2;
                if (amt2 > 0) {
                    // Copy high entries above insert to high node.
                    int dst2 = index2 - lowSize2;
                    System.arraycopy(mEntries, index2,
                                     highEntries, dst2, amt2);
                }
            }

            // Finish off high node definition.
            Node highNode = new Leaf(highSize2, highEntries);
            split.mHighNodeRef = new SoftReference(highNode);
            saveNode(split.mHighNodeId = allocLeafNode(), highNode);

            // This node becomes the low node.
            mSize2 = lowSize2;

            saveNode(id, this);
            incrementTotalSize();
            return split;
        }

        Object remove(Map unsavedNodes, long id, Object key)
            throws IOException
        {
            int index2 = binarySearch2(mEntries, mSize2 - 2, key, mComparator);
            if (index2 < 0) {
                // Not found.
                return null;
            }

            Object old = mEntries[index2 + 1];

            int amt2 = mSize2 - index2 - 2;
            if (amt2 > 0) {
                // Shift array contents over deleted entry.
                System.arraycopy
                    (mEntries, index2 + 2, mEntries, index2, amt2);
            }

            // After adjustment, size may have underflowed. It is the
            // responsibility of the caller to check for underflow and fix it.
            mSize2 -= 2;

            unsavedNodes.put(new Long(id), this);
            return old;
        }

        Object firstKey() throws IOException {
            return mEntries[0];
        }

        Object lastKey() throws IOException {
            return mEntries[mSize2 - 2];
        }

        Object nextKey(Object key) throws IOException {
            int high2 = mSize2 - 2;
            int index2 = binarySearch2(mEntries, high2, key, mComparator);
            if (index2 >= 0) {
                // Key found, move to next.
                index2 += 2;
            }
            else {
                // Key not found, index is at the next.
                index2 = ~index2;
            }
            return (index2 <= high2) ? mEntries[index2] : NO_KEY;
        }

        Object previousKey(Object key) throws IOException {
            int index2 = binarySearch2(mEntries, mSize2 - 2, key, mComparator);
            if (index2 >= 0) {
                // Key found, move to previous.
                index2 -= 2;
            }
            else {
                // Key not found, index is at the next, move to previous.
                index2 = (~index2) - 2;
            }
            return (index2 >= 0) ? mEntries[index2] : NO_KEY;
        }

        Entry firstEntry() throws IOException {
            return new MutableEntry(mEntries[0], mEntries[1]);
        }

        Entry lastEntry() throws IOException {
            return new MutableEntry
                (mEntries[mSize2 - 2], mEntries[mSize2 - 1]);
        }

        Entry nextEntry(Object key) throws IOException {
            int high2 = mSize2 - 2;
            int index2 = binarySearch2(mEntries, high2, key, mComparator);
            if (index2 >= 0) {
                // Key found, move to next.
                index2 += 2;
            }
            else {
                // Key not found, index is at the next.
                index2 = ~index2;
            }
            if (index2 <= high2) {
                return new MutableEntry
                    (mEntries[index2], mEntries[index2 + 1]);
            }
            return null;
        }

        Entry previousEntry(Object key) throws IOException {
            int index2 = binarySearch2(mEntries, mSize2 - 2, key, mComparator);
            if (index2 >= 0) {
                // Key found, move to previous.
                index2 -= 2;
            }
            else {
                // Key not found, index is at the next, move to previous.
                index2 = (~index2) - 2;
            }
            if (index2 >= 0) {
                return new MutableEntry
                    (mEntries[index2], mEntries[index2 + 1]);
            }
            return null;
        }

        Entry removeFirst(Map unsavedNodes, long id) throws IOException {
            Entry entry = new Entry();
            entry.mKey = mEntries[0];
            entry.mValue = mEntries[1];
            int newSize2 = mSize2 - 2;
            System.arraycopy(mEntries, 2, mEntries, 0, newSize2);
            mSize2 = newSize2;
            unsavedNodes.put(new Long(id), this);
            return entry;
        }

        Entry removeLast(Map unsavedNodes, long id) throws IOException {
            Entry entry = new Entry();
            int i = mSize2 - 2;
            entry.mKey = mEntries[i];
            entry.mValue = mEntries[i + 1];
            mSize2 = i;
            unsavedNodes.put(new Long(id), this);
            return entry;
        }

        int copyKeysInto(Object[] a, int index) throws IOException {
            int limit = mSize2;
            for (int i=0; i<limit; i += 2) {
                a[index++] = mEntries[i];
            }
            return index;
        }

        int copyEntriesInto(Object[] a, int index) throws IOException {
            int limit = mSize2;
            for (int i=0; i<limit; i += 2) {
                a[index++] = new MutableEntry(mEntries[i], mEntries[i + 1]);
            }
            return index;
        }

        int dump(String indent) throws IOException {
            for (int i=0; i<mSize2; i += 2) {
                System.out.print(indent);
                System.out.print(mEntries[i]);
                System.out.print('=');
                System.out.println(mEntries[i + 1]);
            }
            return mSize2 / 2;
        }

        /**
         * Merge this node, the given entry, and the high node, in that order.
         * The high node should be deleted. Changes are not saved.
         */
        final void merge(Object key, Object value, Leaf high) {
            int size2 = mSize2;
            mEntries[size2++] = key;
            mEntries[size2++] = value;

            int highSize2 = high.mSize2;
            System.arraycopy(high.mEntries, 0, mEntries, size2, highSize2);
            mSize2 = size2 + highSize2;
        }
    }

    private class NonLeaf extends Node {
        // Array length must always one more than mMaxNodeSize.
        private final long[] mChildrenIds;
        // Array length must always one more than mMaxNodeSize.
        private final Reference[] mChildrenRefs;

        NonLeaf(int size2, Object[] entries, long[] childrenIds) {
            super(size2, entries);
            mChildrenIds = childrenIds;
            mChildrenRefs = new Reference[childrenIds.length];
        }

        NonLeaf(int size2, Object[] entries,
                long[] childrenIds, Reference[] children)
        {
            super(size2, entries);
            mChildrenIds = childrenIds;
            mChildrenRefs = children;
        }

        int size() throws IOException {
            int count = mSize2 / 2;
            for (int i = mSize2 / 2; i >= 0; i--) {
                count += getChildSize(i);
            }
            return count;
        }

        boolean containsKey(Object key) throws IOException {
            int index2 = binarySearch2(mEntries, mSize2 - 2, key, mComparator);
            if (index2 >= 0) {
                return true;
            }
            return getChild((~index2) / 2).containsKey(key);
        }

        boolean containsValue(Object value) throws IOException {
            if (super.containsValue(value)) {
                return true;
            }
            for (int i = mSize2 / 2; i >= 0; i--) {
                if (getChild(i).containsValue(value)) {
                    return true;
                }
            }
            return false;
        }

        Object get(Object key) throws IOException {
            int index2 = binarySearch2(mEntries, mSize2 - 2, key, mComparator);
            if (index2 >= 0) {
                return mEntries[index2 + 1];
            }
            return getChild((~index2) / 2).get(key);
        }

        Entry getEntry(Object key) throws IOException {
            int index2 = binarySearch2(mEntries, mSize2 - 2, key, mComparator);
            if (index2 >= 0) {
                return new MutableEntry
                    (mEntries[index2], mEntries[index2 + 1]);
            }
            return getChild((~index2) / 2).getEntry(key);
        }

        Object put(long id, Object key, Object value) throws IOException {
            int index2 = binarySearch2(mEntries, mSize2 - 2, key, mComparator);
            Object old;
            if (index2 >= 0) {
                // Slot already exists for the entry, just save the value.
                old = mEntries[index2 + 1];
                mEntries[index2 + 1] = value;
                saveNode(id, this);
                return old;
            }
            
            // Switch index to insertion point.
            index2 = ~index2;

            // This index is not doubled, and is used to reference the low
            // child node.
            int index = index2 / 2;

            old = getChild(index).put(mChildrenIds[index], key, value);

            if (!(old instanceof Split)) {
                return old;
            }

            // Child node split to accomodate new entry. Move promoted entry
            // into this node.
            Split split = (Split)old;

            int maxSize2 = mMaxNodeSize2;

            if (mSize2 < maxSize2) {
                // To accomodate insert operations, move child index for
                // referencing high child node.
                index++;
                
                int amt2 = mSize2 - index2;
                if (amt2 > 0) {
                    // Shift array contents to make room for insert.
                    System.arraycopy
                        (mEntries, index2, mEntries, index2 + 2, amt2);
                    System.arraycopy(mChildrenIds, index,
                                     mChildrenIds, index + 1, amt2 / 2);
                    System.arraycopy(mChildrenRefs, index,
                                     mChildrenRefs, index + 1, amt2 / 2);
                }
                
                mEntries[index2] = split.mKey;
                mEntries[index2 + 1] = split.mValue;
                mChildrenIds[index] = split.mHighNodeId;
                mChildrenRefs[index] = split.mHighNodeRef;

                mSize2 += 2;

                saveNode(id, this);
                return null;
            }

            // No more room in this node so split it too. This now becomes
            // the low node, and a new one is the high node.

            int maxSize = mMaxNodeSize;
            int lowSize2 = mMinNodeSize2;
            // High size is different from low size if max size is odd.
            int highSize2 = maxSize2 - lowSize2;

            Object[] highEntries = new Object[maxSize2];
            long[] highChildrenIds = new long[maxSize + 1];
            Reference[] highChildrenRefs = new Reference[maxSize + 1];

            if (index2 == lowSize2) {
                // Insert is at median, and so it is promoted.

                // Copy high entries to high node.
                System.arraycopy(mEntries, index2,
                                 highEntries, 0, highSize2);
                System.arraycopy(mChildrenIds, index + 1,
                                 highChildrenIds, 1, highSize2 / 2);
                System.arraycopy(mChildrenRefs, index + 1,
                                 highChildrenRefs, 1, highSize2 / 2);

                highChildrenIds[0] = split.mHighNodeId;
                highChildrenRefs[0] = split.mHighNodeRef;
            }
            else if (index2 < lowSize2) {
                // Insert to low node.
                // Promote median entry.
                key = split.mKey;
                value = split.mValue;
                split.mKey = mEntries[lowSize2 - 2];
                split.mValue = mEntries[lowSize2 - 1];

                // Copy high entries to high node.
                System.arraycopy(mEntries, lowSize2,
                                 highEntries, 0, highSize2);
                System.arraycopy(mChildrenIds, lowSize2 / 2,
                                 highChildrenIds, 0, highSize2 / 2 + 1);
                System.arraycopy(mChildrenRefs, lowSize2 / 2,
                                 highChildrenRefs, 0, highSize2 / 2 + 1);

                int dst2 = index2 + 2;
                int amt2 = lowSize2 - dst2;
                if (amt2 > 0) {
                    // Shift array contents to make room for insert.
                    System.arraycopy(mEntries, index2, mEntries, dst2, amt2);
                    System.arraycopy(mChildrenIds, index + 1,
                                     mChildrenIds, index + 2, amt2 / 2);
                    System.arraycopy(mChildrenRefs, index + 1,
                                     mChildrenRefs, index + 2, amt2 / 2);
                }

                mEntries[index2] = key;
                mEntries[index2 + 1] = value;
                mChildrenIds[index + 1] = split.mHighNodeId;
                mChildrenRefs[index + 1] = split.mHighNodeRef;
            }
            else if (index2 > lowSize2) {
                // Insert to high node.
                // Promote median entry.
                key = split.mKey;
                value = split.mValue;
                split.mKey = mEntries[lowSize2];
                split.mValue = mEntries[lowSize2 + 1];

                // Copy high entries below insert to high node.
                int src2 = lowSize2 + 2;
                int amt2 = index2 - src2;
                if (amt2 > 0) {
                    System.arraycopy(mEntries, src2, highEntries, 0, amt2);
                }
                int amt = amt2 / 2 + 1;
                if (amt > 0) {
                    System.arraycopy(mChildrenIds, src2 / 2,
                                     highChildrenIds, 0, amt);
                    System.arraycopy(mChildrenRefs, src2 / 2,
                                     highChildrenRefs, 0, amt);
                }

                highEntries[amt2] = key;
                highEntries[amt2 + 1] = value;
                highChildrenIds[amt2 / 2 + 1] = split.mHighNodeId;
                highChildrenRefs[amt2 / 2 + 1] = split.mHighNodeRef;

                amt2 = mSize2 - index2;
                if (amt2 > 0) {
                    // Copy high entries above insert to high node.
                    int dst2 = index2 - lowSize2;
                    System.arraycopy(mEntries, index2,
                                     highEntries, dst2, amt2);
                    System.arraycopy(mChildrenIds, index + 1,
                                     highChildrenIds, dst2 / 2 + 1, amt2 / 2);
                    System.arraycopy(mChildrenRefs, index + 1,
                                     highChildrenRefs, dst2 / 2 + 1, amt2 / 2);
                }
            }

            // Finish off high node definition.
            Node highNode = new NonLeaf
                (highSize2, highEntries, highChildrenIds, highChildrenRefs);
            split.mHighNodeRef = new SoftReference(highNode);
            saveNode(split.mHighNodeId = allocNonLeafNode(), highNode);

            // This node becomes the low node.
            mSize2 = lowSize2;

            saveNode(id, this);
            return split;
        }

        Object remove(Map unsavedNodes, long id, Object key)
            throws IOException
        {
            int index2 = binarySearch2(mEntries, mSize2 - 2, key, mComparator);
            Object old;
            if (index2 < 0) {
                // Not found in this node. Try closest matching child.
                index2 = ~index2;
                int index = index2 / 2;
                Node child = getChild(index);
                old = child.remove(unsavedNodes, mChildrenIds[index], key);
                if (child.mSize2 < mMinNodeSize2) {
                    if (index2 > 0) {
                        repairUnderflow(unsavedNodes, index2 - 2);
                    }
                    else {
                        repairUnderflow(unsavedNodes, index2);
                    }
                    unsavedNodes.put(new Long(id), this);
                }
                return old;
            }

            // Found match to remove in this node, now a replacement needs to
            // be found for this slot.
            old = mEntries[index2 + 1];

            // Remove from child with larger local size. This minimizes
            // risk of underflow reaching us. If underflow does occur, then
            // a merge can be performed without too much trouble.
            // The actual choice of which child to remove from isn't all that
            // critical, but choosing on this basis might favor merging over
            // redistribution, which I think is better.

            Entry entry;
            int index = index2 / 2;
            Node lowChild = getChild(index);
            Node highChild = getChild(index + 1);
            boolean underflow;
            if (lowChild.mSize2 >= highChild.mSize2) {
                entry = lowChild.removeLast
                    (unsavedNodes, mChildrenIds[index]);
                underflow = lowChild.mSize2 < mMinNodeSize2;
            }
            else {
                entry = highChild.removeFirst
                    (unsavedNodes, mChildrenIds[index + 1]);
                underflow = highChild.mSize2 < mMinNodeSize2;
            }

            // Place removed entry in the empty space in this node.
            mEntries[index2] = entry.mKey;
            mEntries[index2 + 1] = entry.mValue;

            if (underflow) {
                repairUnderflow(unsavedNodes, index2);
            }

            unsavedNodes.put(new Long(id), this);
            return old;
        }

        Object firstKey() throws IOException {
            return getChild(0).firstKey();
        }

        Object lastKey() throws IOException {
            return getChild(mSize2 / 2).lastKey();
        }

        Object nextKey(Object key) throws IOException {
            int high2 = mSize2 - 2;
            int index2 = binarySearch2(mEntries, high2, key, mComparator);
            if (index2 >= 0) {
                // Key found, get first from next child.
                return getChild(index2 / 2 + 1).firstKey();
            }
            // Key not found, get next from child.
            index2 = ~index2;
            Object next = getChild(index2 / 2).nextKey(key);
            if (next != NO_KEY) {
                return next;
            }
            // Child has no next key, so return our next key.
            return (index2 <= high2) ? mEntries[index2] : NO_KEY;
        }

        Object previousKey(Object key) throws IOException {
            int index2 = binarySearch2(mEntries, mSize2 - 2, key, mComparator);
            if (index2 >= 0) {
                // Key found, get last from previous child.
                return getChild(index2 / 2).lastKey();
            }
            // Key not found, get previous from child.
            index2 = ~index2;
            Object prev = getChild(index2 / 2).previousKey(key);
            if (prev != NO_KEY) {
                return prev;
            }
            // Child has no previous key, so return our previous key.
            index2 -= 2;
            return (index2 >= 0) ? mEntries[index2] : NO_KEY;
        }

        Entry firstEntry() throws IOException {
            return getChild(0).firstEntry();
        }

        Entry lastEntry() throws IOException {
            return getChild(mSize2 / 2).lastEntry();
        }

        Entry nextEntry(Object key) throws IOException {
            int high2 = mSize2 - 2;
            int index2 = binarySearch2(mEntries, high2, key, mComparator);
            if (index2 >= 0) {
                // Key found, get first from next child.
                return getChild(index2 / 2 + 1).firstEntry();
            }
            // Key not found, get next from child.
            index2 = ~index2;
            Entry next = getChild(index2 / 2).nextEntry(key);
            if (next != null) {
                return next;
            }
            // Child has no next entry, so return our next entry.
            if (index2 <= high2) {
                return new MutableEntry
                    (mEntries[index2], mEntries[index2 + 1]);
            }
            return null;
        }

        Entry previousEntry(Object key) throws IOException {
            int index2 = binarySearch2(mEntries, mSize2 - 2, key, mComparator);
            if (index2 >= 0) {
                // Key found, get last from previous child.
                return getChild(index2 / 2).lastEntry();
            }
            // Key not found, get previous from child.
            index2 = ~index2;
            Entry prev = getChild(index2 / 2).previousEntry(key);
            if (prev != null) {
                return prev;
            }
            // Child has no previous entry, so return our previous entry.
            index2 -= 2;
            if (index2 >= 0) {
                return new MutableEntry
                    (mEntries[index2], mEntries[index2 + 1]);
            }
            return null;
        }

        Entry removeFirst(Map unsavedNodes, long id) throws IOException {
            Node child = getChild(0);
            Entry entry = child.removeFirst(unsavedNodes, mChildrenIds[0]);

            if (child.mSize2 < mMinNodeSize2) {
                // Child has underflowed, and changes will be made to this.
                repairUnderflow(unsavedNodes, 0);
                unsavedNodes.put(new Long(id), this);
            }

            return entry;
        }

        Entry removeLast(Map unsavedNodes, long id) throws IOException {
            int last = mSize2 / 2;
            Node child = getChild(last);
            Entry entry = child.removeLast(unsavedNodes, mChildrenIds[last]);

            if (child.mSize2 < mMinNodeSize2) {
                // Child has underflowed, and changes will be made to this.
                repairUnderflow(unsavedNodes, mSize2 - 2);
                unsavedNodes.put(new Long(id), this);
            }

            return entry;
        }

        int copyKeysInto(Object[] a, int index) throws IOException {
            int limit = mSize2;
            int i = 0;
            for (; i<limit; i += 2) {
                index = getChild(i >> 1).copyKeysInto(a, index);
                a[index++] = mEntries[i];
            }
            return getChild(i >> 1).copyKeysInto(a, index);
        }

        int copyEntriesInto(Object[] a, int index) throws IOException {
            int limit = mSize2;
            int i = 0;
            for (; i<limit; i += 2) {
                index = getChild(i >> 1).copyEntriesInto(a, index);
                a[index++] = new MutableEntry(mEntries[i], mEntries[i + 1]);
            }
            return getChild(i >> 1).copyEntriesInto(a, index);
        }

        int dump(String indent) throws IOException {
            int count = mSize2 / 2;
            String subIndent = indent.concat("  ");
            int i = 0;
            for (; i<mSize2; i += 2) {
                count += getChild(i / 2).dump(subIndent);
                System.out.print(indent);
                System.out.print(mEntries[i]);
                System.out.print('=');
                System.out.println(mEntries[i + 1]);
            }
            return count + getChild(i / 2).dump(subIndent);
        }

        final Node getChild(int index) throws IOException {
            Node child;
            Reference ref = mChildrenRefs[index];
            if (ref == null || (child = (Node)ref.get()) == null) {
                child = loadNode(mChildrenIds[index]);
                mChildrenRefs[index] = new SoftReference(child);
            }
            return child;
        }

        final long getChildId(int index) {
            return mChildrenIds[index];
        }

        final int getChildSize(int index) throws IOException {
            Node child;
            Reference ref = mChildrenRefs[index];
            if (ref == null || (child = (Node)ref.get()) == null) {
                return BTree.this.size(mChildrenIds[index]);
            }
            return child.size();
        }

        /**
         * Merge this node, the given entry, and the high node, in that order.
         * The high node should be deleted. Changes are not saved.
         */
        final void merge(Object key, Object value, NonLeaf high) {
            int size2 = mSize2;
            mEntries[size2++] = key;
            mEntries[size2++] = value;
            
            int highSize2 = high.mSize2;
            System.arraycopy(high.mEntries, 0, mEntries, size2, highSize2);
            int dst = size2 / 2;
            int amt = highSize2 / 2 + 1;
            System.arraycopy(high.mChildrenIds, 0, mChildrenIds, dst, amt);
            System.arraycopy(high.mChildrenRefs, 0, mChildrenRefs, dst, amt);
            mSize2 = size2 + highSize2;
        }

        /**
         * Repairs child underflow by redistribution or merging. This method
         * should not be called if there isn't any underflow.
         * After calling this method, this node itself may have underflowed.
         */
        private final void repairUnderflow(Map unsavedNodes, int index2) 
            throws IOException
        {
            int index = index2 / 2;
            Node lowChild = getChild(index);
            Node highChild = getChild(index + 1);
            int lowChildSize2 = lowChild.mSize2;
            int highChildSize2 = highChild.mSize2;

            if (lowChildSize2 < mMinNodeSize2 &&
                highChildSize2 > mMinNodeSize2) {
                
                // Redistribute from high node to this to low node.

                // First, move our local entry to low child's last.
                lowChild.mEntries[lowChildSize2] = mEntries[index2];
                lowChild.mEntries[lowChildSize2 + 1] = mEntries[index2 + 1];
                lowChild.mSize2 = (lowChildSize2 += 2);

                // Move first entry from high child to replace our local entry.
                if (highChild instanceof Leaf) { // lowChild is also leaf
                    Entry entry = highChild.removeFirst
                        (unsavedNodes, mChildrenIds[index + 1]);
                    mEntries[index2] = entry.mKey;
                    mEntries[index2 + 1] = entry.mValue;
                }
                else {
                    // Move high child first child ref to low child last child
                    // ref.
                    NonLeaf nLowChild = (NonLeaf)lowChild;
                    NonLeaf nHighChild = (NonLeaf)highChild;
                    int i = lowChildSize2 / 2;
                    nLowChild.mChildrenIds[i] = nHighChild.mChildrenIds[0];
                    nLowChild.mChildrenRefs[i] = nHighChild.mChildrenRefs[0];

                    // Now move entry and shift high child contents.
                    mEntries[index2] = highChild.mEntries[0];
                    mEntries[index2 + 1] = highChild.mEntries[1];
                    int newHighSize2 = highChildSize2 - 2;
                    System.arraycopy(highChild.mEntries, 2,
                                     highChild.mEntries, 0, newHighSize2);
                    System.arraycopy(nHighChild.mChildrenIds, 1,
                                     nHighChild.mChildrenIds, 0,
                                     highChildSize2 / 2);
                    System.arraycopy(nHighChild.mChildrenRefs, 1,
                                     nHighChild.mChildrenRefs, 0,
                                     highChildSize2 / 2);
                    highChild.mSize2 = newHighSize2;
                }

                unsavedNodes.put(new Long(mChildrenIds[index]), lowChild);
                unsavedNodes.put(new Long(mChildrenIds[index + 1]), highChild);
                return;
            }
            else if (highChildSize2 < mMinNodeSize2 &&
                     lowChildSize2 > mMinNodeSize2) {
                
                // Redistribute from low node to this to high node.
                
                // First, move our local entry to high child's first.
                System.arraycopy(highChild.mEntries, 0,
                                 highChild.mEntries, 2, highChildSize2);
                highChild.mEntries[0] = mEntries[index2];
                highChild.mEntries[1] = mEntries[index2 + 1];
                highChild.mSize2 = (highChildSize2 += 2);

                // Move last entry from low child to replace our local entry.
                if (lowChild instanceof Leaf) { // highChild is also leaf
                    Entry entry = lowChild.removeLast
                        (unsavedNodes, mChildrenIds[index]);
                    mEntries[index2] = entry.mKey;
                    mEntries[index2 + 1] = entry.mValue;
                }
                else {
                    // Move low child last child ref to high child first child
                    // ref.
                    NonLeaf nLowChild = (NonLeaf)lowChild;
                    NonLeaf nHighChild = (NonLeaf)highChild;
                    System.arraycopy(nHighChild.mChildrenIds, 0,
                                     nHighChild.mChildrenIds, 1,
                                     highChildSize2 / 2);
                    System.arraycopy(nHighChild.mChildrenRefs, 0,
                                     nHighChild.mChildrenRefs, 1,
                                     highChildSize2 / 2);
                    int i = lowChildSize2 / 2;
                    nHighChild.mChildrenIds[0] = nLowChild.mChildrenIds[i];
                    nHighChild.mChildrenRefs[0] = nLowChild.mChildrenRefs[i];

                    i = lowChild.mSize2 - 2;
                    mEntries[index2] = lowChild.mEntries[i];
                    mEntries[index2 + 1] = lowChild.mEntries[i + 1];
                    lowChild.mSize2 = i;
                }

                unsavedNodes.put(new Long(mChildrenIds[index]), lowChild);
                unsavedNodes.put(new Long(mChildrenIds[index + 1]), highChild);
                return;
            }

            // Merge into low child, shrink this, and delete high child.
            // Note: This point should only be reached when one child has
            // the minimum number of entries, and the other has one less than
            // that. The resulting merged node will be full, or one less than
            // full if max entries per node is odd.

            if (lowChild instanceof Leaf) {
                ((Leaf)lowChild).merge
                    (mEntries[index2], mEntries[index2 + 1], (Leaf)highChild);
            }
            else {
                ((NonLeaf)lowChild).merge(mEntries[index2],
                                          mEntries[index2 + 1],
                                          (NonLeaf)highChild);
            }

            unsavedNodes.put(new Long(mChildrenIds[index]), lowChild);
            // Put null to free the node.
            unsavedNodes.put(new Long(mChildrenIds[index + 1]), null);

            int newSize2 = mSize2 - 2;
            if (index2 < newSize2) {
                // Shift left over moved entry reference.
                int amt = newSize2 - index2;
                System.arraycopy(mEntries, index2 + 2, mEntries, index2, amt);
                System.arraycopy(mChildrenIds, index + 2,
                                 mChildrenIds, index + 1, amt / 2);
                System.arraycopy(mChildrenRefs, index + 2,
                                 mChildrenRefs, index + 1, amt / 2);
            }
            mSize2 = newSize2;
        }
    }

    // The entry classes are transient - they do not directly refer to
    // persistent data. They are used for implementing algorithms and
    // the entry set.

    private static class Entry extends AbstractPersistentMapEntry {
        Object mKey;
        Object mValue;

        public Object getKey() {
            return mKey;
        }

        public Object getValue() {
            return mValue;
        }
    }

    // mKey and mValue in Split refer to the entry that overflowed and must
    // be stored in a parent node.
    private static class Split extends Entry {
        // Id of new high node.
        long mHighNodeId;

        // Reference to new high node.
        Reference mHighNodeRef;
    }

    private class MutableEntry extends Entry {
        MutableEntry(Object key, Object value) {
            mKey = key;
            mValue = value;
        }

        public Object setValue(Object value) throws IOException {
            Object old = put(mKey, value);
            mValue = value;
            return old;
        }
    }
}
