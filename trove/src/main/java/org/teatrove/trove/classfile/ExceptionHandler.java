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

import java.io.*;

/**
 * This class corresponds to the exception_table structure as defined in
 * section 4.7.4 of <i>The Java Virtual Machine Specification</i>.
 *
 * @author Brian S O'Neill
 */
public class ExceptionHandler implements LocationRange {
    private Location mStart;
    private Location mEnd;
    private Location mCatch;
    private ConstantClassInfo mCatchType;
    
    /**
     * @param startLocation
     * @param endLocation
     * @param catchLocation
     * @param catchType if null, then catch every object.
     */
    public ExceptionHandler(Location startLocation,
                            Location endLocation,
                            Location catchLocation,
                            ConstantClassInfo catchType) {
        mStart = startLocation;
        mEnd = endLocation;
        mCatch = catchLocation;
        mCatchType = catchType;
    }
    
    public Location getStartLocation() {
        return mStart;
    }
    
    public Location getEndLocation() {
        return mEnd;
    }
    
    public Location getCatchLocation() {
        return mCatch;
    }
    
    /**
     * Returns null if every object is caught by this handler.
     */
    public ConstantClassInfo getCatchType() {
        return mCatchType;
    }

    public int compareTo(Object obj) {
        if (this == obj) {
            return 0;
        }

        LocationRange other = (LocationRange)obj;

        int result = getStartLocation().compareTo(other.getStartLocation());

        if (result == 0) {
            result = getEndLocation().compareTo(other.getEndLocation());
        }

        if (result == 0 && obj instanceof ExceptionHandler) {
            result = getCatchLocation().compareTo
                (((ExceptionHandler)other).getCatchLocation());
        }

        return result;
    }
    
    public void writeTo(DataOutput dout) throws IOException {
        int start_pc = getStartLocation().getLocation();
        int end_pc = getEndLocation().getLocation();
        int handler_pc = getCatchLocation().getLocation();
        int catch_type;
        ConstantClassInfo catchType = getCatchType();
        if (catchType == null) {
            catch_type = 0;
        }
        else {
            catch_type = catchType.getIndex();
        }

        check("exception start PC", start_pc);
        check("exception end PC", end_pc);
        check("exception handler PC", handler_pc);

        dout.writeShort(start_pc);
        dout.writeShort(end_pc);
        dout.writeShort(handler_pc);
        dout.writeShort(catch_type);
    }

    private void check(String type, int addr) throws RuntimeException {
        if (addr < 0 || addr > 65535) {
            throw new RuntimeException("Value for " + type + " out of " +
                                       "valid range: " + addr);

        }
    }

    static ExceptionHandler readFrom(ConstantPool cp,
                                     DataInput din) throws IOException {
        int start_pc = din.readUnsignedShort();
        int end_pc = din.readUnsignedShort();
        int handler_pc = din.readUnsignedShort();
        int catch_type = din.readUnsignedShort();

        ConstantClassInfo catchTypeConstant;
        if (catch_type == 0) {
            catchTypeConstant = null;
        }
        else {
            catchTypeConstant = (ConstantClassInfo)cp.getConstant(catch_type);
        }

        return new ExceptionHandler(new FixedLocation(start_pc),
                                    new FixedLocation(end_pc),
                                    new FixedLocation(handler_pc),
                                    catchTypeConstant);
    }
}
