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

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *  This class implements a thread safe queue.   It differs from the LinkedList
 *  and Queue implementations in java.util.concurrent in that there is an
 *  implementation of the remove method that runs in constant time (vs. the
 *  typical linear time implementation that exists in the java.util.concurrent
 *  libraries). 
 *
 *  IMPORTANT NOTE:  Once a Node reference has been passed into the remove()
 *  call, its reference should be promptly discarded. 
 * 
 *  @author Guy A. Molinari
 *
 *  TODO: Implement the java.util.Collection and java.util.Queue interfaces.
 */
public class ConcurrentLinkedList<E> {

    protected AtomicInteger mSize = new AtomicInteger(0);
    protected Node mHead = null;
    protected Node mTail = null;

    protected final ReentrantLock mPollLock = new ReentrantLock();
    protected final ReentrantLock mPutLock = new ReentrantLock();

    private SimpleObjectPool mNodePool = new SimpleObjectPool();


    public ConcurrentLinkedList() {
        mHead = new Node(null);
        mTail = new Node(null);
        mHead.mNext = mTail;
    }


    /**
     *  Add a new item to the tail of the queue and return a Node reference.
     *  This reference can be used as a handle that can be passed to 
     *  remove(Node) where constand time removes are necessary.  Calls to
     *  this method and poll() do not mutually block, however, concurrent 
     *  calls to this method are serialized.
     */
    public Node<E> offerAndGetNode(E e) {
        mPutLock.lock();
        try {
            Node newNode = mNodePool.borrowNode(e);
            if (mSize.get() > 0)
                newNode.mPrev = mTail;
            mTail.mNext = newNode;
            if (mSize.get() <= 1) {
                mPollLock.lock();
                try {
                    if (mSize.get() == 0)
                        mHead.mNext = newNode;
                    else
                        mHead.mNext = newNode.mPrev;
                }
                finally {
                    mPollLock.unlock();
                }
            }
            mSize.incrementAndGet();
            mTail = newNode;
            return newNode;
        }
        finally {
            mPutLock.unlock();
        }
    }


    /**
     *  Retrieves the current queue length.  This is a non-blocking operation.
     */
    public int size() { return mSize.get(); }

    
    /**
     *  Clears the contents of the queue.   This is a blocking operation.
     */
    public void clear() {
        mPutLock.lock();
        mPollLock.lock();
        try {
            mSize.set(0);
            mHead = new Node(null);
            mTail = new Node(null);
            mHead.mNext = mTail;
        }
        finally {
            mPollLock.unlock();
            mPutLock.unlock();
        }
    }

    public boolean moveToTail(Node<E> e){
          mPutLock.lock();
          try {
              mPollLock.lock();
              try{
                  if (e == null)
                      return false;
          
                  if (e.mRemoved)
                      return false;
          
                  if (mSize.get() == 0)
                      return false;
    
                  if (e == mTail) {
                      return true;
                  }
                  
                  if (e.mPrev == null || e.mNext == null)
                      return false;
    
                  e.mPrev.mNext = e.mNext;
                  e.mNext.mPrev = e.mPrev;
              }
              finally{
                  mPollLock.unlock();
              }
              if (mSize.get() > 0)
                  e.mPrev = mTail;
              mTail.mNext = e;
              if (mSize.get() <= 1) {
                  mPollLock.lock();
                  try {
                      if (mSize.get() == 0)
                          mHead.mNext = e;
                      else
                          mHead.mNext = e.mPrev;
                  }
                  finally {
                      mPollLock.unlock();
                  }
              }
              mTail = e;
              return true;
          }
          finally {
              mPutLock.unlock();
          }
    }

    /**
     *  Removes a given Node handle.   This is a blocking operation that runs in constant time.
     */
    public boolean remove(Node<E> e) {

        mPutLock.lock();
        mPollLock.lock();
        try {
            if (e == null)
                return false;
    
            if (e.mRemoved)
                return false;
    
            if (mSize.get() == 0)
                return false;

            if (e == mTail) {
                removeTail();
                return true;
            }
            if (e == mHead.mNext) {
                removeHead();
                return true;
            }
            if (mSize.get() < 3)
                return false;

            if (e.mPrev == null || e.mNext == null)
                return false;

            e.mPrev.mNext = e.mNext;
            e.mNext.mPrev = e.mPrev;
            e.mRemoved = true;
            mSize.decrementAndGet();
            mNodePool.returnNode(e);
            return true;
        }
        finally {
            mPollLock.unlock();
            mPutLock.unlock();
        }
    }


    private void removeTail() {

        Node e = mTail;
        if (mTail.mPrev == null) {
            mTail = new Node(null);
            mHead.mNext = mTail;
            mSize.set(0);
        }
        else  {
            mTail = mTail.mPrev;
            mSize.decrementAndGet();
            mTail.mNext = null;
            e.mRemoved = true;
            mNodePool.returnNode(e);
        }
    }


    private E removeHead() {
        E value = null;
        if (mHead.mNext != null) {
            Node<E> e = mHead.mNext;
            mHead.mNext = e.mNext;
            if (mHead.mNext != null)
                mHead.mNext.mPrev = null;
            e.mRemoved = true;
            value = e.mEntry;
            mNodePool.returnNode(e);
        }
        else 
            mHead.mNext = mTail;
        mSize.decrementAndGet();
        return value;
    }


    /**
     *  Removes and returns the item at the head of the queue.  Concurrent calls to
     *  this method and offerAndGetNode() do not mutually block, however, concurrent 
     *  calls to this method are serialized.
     */
    public E poll() {
        if (mSize.get() == 0)
            return null;
        mPollLock.lock();
        try {
            return removeHead();
        }
        finally {
            mPollLock.unlock();
        }
    }


    public synchronized void integrityCheck() {

        mPutLock.lock();
        mPollLock.lock();
        try {
            if (mSize.get() == 0 && mHead.mNext != mTail)
                return;

            // forward traversal
            Node a = mHead.mNext;
            int i = 0;
            for (i = 0; i < mSize.get() - 1; i++) {
                if (a == null)
                    throw new IllegalArgumentException("Null found during forward traversal");
                a = a.mNext;
            }
            if (a != mTail)
                throw new IllegalArgumentException("Last item in the list should be the tail, scount = " + 
                        i + ", item = " + a + ", tail = " + mTail);
    
            // backwards traversal
            a = mTail;
            for (i = 0; i < mSize.get() - 1; i++) {
                if (a == null)
                    throw new IllegalArgumentException("Null found during forward traversal");
                a = a.mPrev;
            }
            if (a != mHead.mNext)
                throw new IllegalArgumentException("First item in the list should be the head, count = " + 
                        i + ", item = " + a + ", head = " + mHead.mNext);
        }
        finally {
            mPollLock.unlock();
            mPutLock.unlock();
        }
    }

    
    /**
     *  This class is a representation of Nodes in a linked list.  It is fully accessible internally, but
     *  when passed to the external world via offerAndGetNode() it is immutable and can be used as a
     *  handle to the remove(Node) method.  Once a Node is passed to the remove call its reference should
     *  be discarded.
     */
    public static class Node<E> {

        E mEntry;
        Node mNext = null;
        Node mPrev = null;
        volatile boolean mRemoved = false;

        Node(E e) {
            mEntry = e;
        }

        public E getValue() { return mEntry; }

        public String toString() {
            return "This = " + (mEntry != null ? mEntry.toString() : "null") + 
                ", next = " + (mNext != null && mNext.mEntry != null ? mNext.mEntry.toString() : "null") + 
                ", prev = " + (mPrev != null && mPrev.mEntry != null ? mPrev.mEntry.toString() : "null");
        }
    }


    /*
     *  This class provides a simple pooling mechanizm to reduce heap churn and minimize GC.
     *  The maximum size of the pool should never exceed the concurrent thread count.  If it does
     *  Then new objects will be created continuously if the pool size is exceeded.
     */
    private static class SimpleObjectPool<E> {

        private static final int POOL_MAX_SIZE = 100;
        private LinkedBlockingQueue<Node<E>> mPool = new LinkedBlockingQueue(POOL_MAX_SIZE);

        /*
         * Return a Node instance from the pool or create a new one if the pool is empty.
         */
        public Node<E> borrowNode(E e) {
            Node<E> o = mPool.poll();
            if (o == null)
                return new Node(e);
            o.mEntry = e;
            return o;
        }


        /*
         *  The call to offer does nothing if the pool is full (should not happen normally.
         */
        public void returnNode(Node<E> e) {
            e.mNext = null; 
            e.mPrev = null; 
            e.mRemoved = false;
            e.mEntry = null;
            mPool.offer(e);
        }
 
    }
}
