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

import java.util.*;
import java.io.*;

/**
 * This class corresponds to the LineNumberTable_attribute structure as
 * defined  in section 4.7.6 of <i>The Java Virtual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 */
class LineNumberTableAttr extends Attribute {
    private List mEntries = new ArrayList();
    private boolean mClean = false;
    
    public LineNumberTableAttr(ConstantPool cp) {
        super(cp, LINE_NUMBER_TABLE);
    }
    
    public int getLineNumber(Location start) {
        clean();
        int index = Collections.binarySearch(mEntries, new Entry(start, 0));
        if (index < 0) {
            if ((index = -index - 2) < 0) {
                return -1;
            }
        }
        return ((Entry)mEntries.get(index)).mLineNumber;
    }

    public void addEntry(Location start, int line_number) {
        check("line number", line_number);
        mEntries.add(new Entry(start, line_number));
        mClean = false;
    }
    
    public int getLength() {
        clean();
        return 2 + 4 * mEntries.size();
    }
    
    public void writeDataTo(DataOutput dout) throws IOException {
        int size = mEntries.size();
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            Entry entry = (Entry)mEntries.get(i);
            
            int start_pc = entry.mStart.getLocation();

            check("line number table entry start PC", start_pc);

            dout.writeShort(start_pc);
            dout.writeShort(entry.mLineNumber);
        }
    }

    private void check(String type, int addr) throws RuntimeException {
        if (addr < 0 || addr > 65535) {
            throw new RuntimeException("Value for " + type + " out of " +
                                       "valid range: " + addr);

        }
    }

    private void clean() {
        if (!mClean) {
            mClean = true;

            // Clean things up by removing multiple mappings of the same
            // start_pc to line numbers. Only keep the last one.
            // This has to be performed now because the Labels should have
            // a pc location, but before they did not. Since entries must be
            // sorted ascending by start_pc, use a sorted set.

            Set reduced = new TreeSet();
            for (int i = mEntries.size(); --i >= 0; ) {
                reduced.add(mEntries.get(i));
            }

            mEntries = new ArrayList(reduced);
        }
    }

    static Attribute define(ConstantPool cp,
                            String name,
                            int length,
                            DataInput din) throws IOException {

        LineNumberTableAttr lineNumbers = new LineNumberTableAttr(cp);

        int size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            int start_pc = din.readUnsignedShort();
            int line_number = din.readUnsignedShort();

            lineNumbers.addEntry(new FixedLocation(start_pc), line_number);
        }

        return lineNumbers;
    }

    private static class Entry implements Comparable {
        public final Location mStart;
        public final int mLineNumber;
        
        public Entry(Location start, int line_number) {
            mStart = start;
            mLineNumber = line_number;
        }

        public int compareTo(Object other) {
            int thisLoc = mStart.getLocation();
            int thatLoc = ((Entry)other).mStart.getLocation();
            
            if (thisLoc < thatLoc) {
                return -1;
            }
            else if (thisLoc > thatLoc) {
                return 1;
            }
            else {
                return 0;
            }
        }

        public boolean equals(Object other) {
            if (other instanceof Entry) {
                return mStart.getLocation() == 
                    ((Entry)other).mStart.getLocation();
            }
            return false;
        }

        public String toString() {
            return "start_pc=" + mStart.getLocation() + " => " +
                "line_number=" + mLineNumber;
        }
    }
}
