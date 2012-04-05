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


/**
 * Tea context that provides access to useful mathematical operations including
 * the {@link Math} class.
 */
public class MathContext {

    /**
     * Get the double value that is closer than any other to E, the base of the 
     * natural logarithms.
     * 
     * @return {@link Math#E}
     */
    public double getE() {
        return Math.E;
    }
    
    /**
     * Get the double value that is closer than any other to PI, the ratio of 
     * the circumference of a circle to its diameter.
     * 
     * @return {@link Math#PI}
     */
    public double getPI() {
        return Math.PI;
    }

    /**
     * Get the absolute value of a double value.
     * 
     * @param value The associated value
     * 
     * @return the absolute value of the value
     * 
     * @see Math#abs(double)
     */
    public double abs(double value) {
        return Math.abs(value);
    }
        
    /**
     * Get the absolute value of a float value.
     * 
     * @param value The associated value
     * 
     * @return the absolute value of the value
     * 
     * @see Math#abs(float)
     */
    public float abs(float value) {
        return Math.abs(value);
    }
    
    /**
     * Get the absolute value of an int value.
     * 
     * @param value The associated value
     * 
     * @return the absolute value of the value
     * 
     * @see Math#abs(int)
     */
    public int abs(int value) {
        return Math.abs(value);
    }
    
    /**
     * Get the absolute value of a long value.
     * 
     * @param value The associated value
     * 
     * @return the absolute value of the value
     * 
     * @see Math#abs(long)
     */
    public long abs(long value) {
        return Math.abs(value);
    }
    
    /** 
     * Get the arc cosine of an angle, in the range of 0.0 through PI.
     * 
     * @param angle The associated angle
     * 
     * @return The arc cosine of the angle
     * 
     * @see Math#acos(double)
     */
    public double acos(double angle) {
        return Math.acos(angle);
    }
    
    /** 
     * Get the arc sine of an angle, in the range of -PI/2 through PI/2.
     * 
     * @param angle The associated angle
     * 
     * @return The arc sine of the angle
     * 
     * @see Math#asin(double)
     */
    public double asin(double angle) {
        return Math.asin(angle);
    }
    
    /** 
     * Get the arc tangent of an angle, in the range of -PI/2 through PI/2.
     * 
     * @param angle The associated angle
     * 
     * @return The arc tangent of the angle
     * 
     * @see Math#atan(double)
     */
    public double atan(double angle) {
        return Math.atan(angle);
    }
    
    /** 
     * Get the angle theta from the conversion of the rectangular coordinates 
     * (x, y) to polar coordinates (r, theta) using the arc tangent to compute
     * theta.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * 
     * @return The angle theta from the conversion
     * 
     * @see Math#atan2(double, double)
     */
    public double atan2(double x, double y) {
        return Math.atan2(x, y);
    }
    
    /** 
     * Get the smallest (closest to negative infinity) double value that is 
     * not less than the argument and is equal to a mathematical integer.
     * 
     * @param value the associated value
     * 
     * @return The ceiling of the associated value
     * 
     * @see Math#ceil(double)
     */
    public double ceil(double value) {
        return Math.ceil(value);
    }
    
    /**
     * Get the trigonometric cosine of an angle.
     * 
     * @param angle the associated angle
     * 
     * @return The cosine of the given angle
     * 
     * @see Math#cos(double)
     */
    public double cos(double angle) {
        return Math.cos(angle);
    }
    
    /** 
     * Returns the exponential number E (i.e., 2.718...) raised to the power 
     * of a double value. 
     * 
     * @param power the power to raise to
     * 
     * @return The exponential number E raised to the given power
     * 
     * @see Math#exp(double)
     */
    public double exp(double power) {
        return Math.exp(power);
    }
    
    /** 
     * Get the largest (closest to positive infinity) double value that is 
     * not greater than the argument and is equal to a mathematical integer.
     * 
     * @param value the associated value
     * 
     * @return The floor of the associated value
     * 
     * @see Math#floor(double)
     */
    public double floor(double value) {
        return Math.floor(value);
    }
    
    /** 
     * Computes the remainder operation on two arguments as prescribed by 
     * the IEEE 754 standard. 
     * 
     * @param dividend The associated dividend
     * @param divisor The associated divisor
     * 
     * @return The IEE remainder
     * 
     * @see Math#IEEEremainder(double, double)
     */
    public double IEEEremainder(double dividend, double divisor) {
        return Math.IEEEremainder(dividend, divisor);
    }
    
    /**
     * Get the natural logarithm (base E) of a double value.
     * 
     * @param value The associated value
     * 
     * @return The natural logarithm
     * 
     * @see Math#log(double)
     */
    public double log(double value) {
        return Math.log(value);
    }
    
    /**
     * Get the greater of two double values.
     * 
     * @param a the first value
     * @param b the second value
     * 
     * @return The greater of the two values
     * 
     * @see Math#max(double, double)
     */
    public double max(double a, double b) {
        return Math.max(a, b);
    }
    
    /**
     * Get the greatest of the set of double values.
     * 
     * @param values the set of values
     * 
     * @return The greatest of the values
     */
    public double max(double... values) {
        double max = Double.MIN_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (i == 0 || values[i] > max) {
                max = values[i];
            }
        }
        
        return max;
    }
    
    /**
     * Get the greater of two float values.
     * 
     * @param a the first value
     * @param b the second value
     * 
     * @return The greater of the two values
     * 
     * @see Math#max(float, float)
     */
    public float max(float a, float b) {
        return Math.max(a, b);
    }
    
    /**
     * Get the greatest of the set of float values.
     * 
     * @param values the set of values
     * 
     * @return The greatest of the values
     */
    public float max(float... values) {
        float max = Float.MIN_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (i == 0 || values[i] > max) {
                max = values[i];
            }
        }
        
        return max;
    }
    
    /**
     * Get the greater of two int values.
     * 
     * @param a the first value
     * @param b the second value
     * 
     * @return The greater of the two values
     * 
     * @see Math#max(int, int)
     */
    public int max(int a, int b) {
        return Math.max(a, b);
    }
    
    /**
     * Get the greatest of the set of int values.
     * 
     * @param values the set of values
     * 
     * @return The greatest of the values
     */
    public int max(int... values) {
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (i == 0 || values[i] > max) {
                max = values[i];
            }
        }
        
        return max;
    }
    
    /**
     * Get the greater of two long values.
     * 
     * @param a the first value
     * @param b the second value
     * 
     * @return The greater of the two values
     * 
     * @see Math#max(long, long)
     */
    public long max(long a, long b) {
        return Math.max(a, b);
    }
    
    /**
     * Get the greatest of the set of long values.
     * 
     * @param values the set of values
     * 
     * @return The greatest of the values
     */
    public long max(long... values) {
        long max = Long.MIN_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (i == 0 || values[i] > max) {
                max = values[i];
            }
        }
        
        return max;
    }
    
    /**
     * Get the smaller of two double values.
     * 
     * @param a the first value
     * @param b the second value
     * 
     * @return The greater of the two values
     * 
     * @see Math#min(double, double)
     */
    public double min(double a, double b) {
        return Math.min(a, b);
    }
    
    /**
     * Get the smallest of the set of double values.
     * 
     * @param values the set of values
     * 
     * @return The greatest of the values
     */
    public double min(double... values) {
        double min = Double.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (i == 0 || values[i] < min) {
                min = values[i];
            }
        }
        
        return min;
    }
    
    /**
     * Get the smaller of two float values.
     * 
     * @param a the first value
     * @param b the second value
     * 
     * @return The greater of the two values
     * 
     * @see Math#min(float, float)
     */
    public float min(float a, float b) {
        return Math.min(a, b);
    }
    
    /**
     * Get the smallest of the set of float values.
     * 
     * @param values the set of values
     * 
     * @return The greatest of the values
     */
    public float min(float... values) {
        float min = Float.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (i == 0 || values[i] < min) {
                min = values[i];
            }
        }
        
        return min;
    }
    
    /**
     * Get the smaller of two int values.
     * 
     * @param a the first value
     * @param b the second value
     * 
     * @return The greater of the two values
     * 
     * @see Math#min(int, int)
     */
    public int min(int a, int b) {
        return Math.min(a, b);
    }
    
    /**
     * Get the smallest of the set of int values.
     * 
     * @param values the set of values
     * 
     * @return The greatest of the values
     */
    public int min(int... values) {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (i == 0 || values[i] < min) {
                min = values[i];
            }
        }
        
        return min;
    }
    
    /**
     * Get the smaller of two long values.
     * 
     * @param a the first value
     * @param b the second value
     * 
     * @return The greater of the two values
     * 
     * @see Math#min(long, long)
     */
    public long min(long a, long b) {
        return Math.min(a, b);
    }
    
    /**
     * Get the smallest of the set of long values.
     * 
     * @param values the set of values
     * 
     * @return The greatest of the values
     */
    public long min(long... values) {
        long min = Long.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (i == 0 || values[i] < min) {
                min = values[i];
            }
        }
        
        return min;
    }
    
    /** 
     * Get of result of the given value raised to the given power. 
     * 
     * @param value The associated value
     * @param power The power to raise the value to
     * 
     * @return The value raised to the given power
     * 
     * @see Math#pow(double, double)
     */
    public double pow(double a, double b) {
        return Math.pow(a, b);
    }
    
    /** 
     * Get a random double value with a positive sign, greater than or equal to 
     * 0.0 and less than 1.0.
     * 
     * @return A random value between 0.0 (inclusive) and 1.0 (exclusive)
     * 
     * @see Math#random()
     */
    public double random() {
        return Math.random();
    }
    
    /** 
     * Returns the double value that is closest in value to the given value and 
     * is equal to a mathematical integer.
     * 
     * @param value The associated value
     * 
     * @return The resulting value
     * 
     * @see Math#rint(double)
     */
    public double rint(double value) {
        return Math.rint(value);
    }
    
    /**
     * Get the closest long value to the given value.
     * 
     * @param value The value to round
     * 
     * @return The rounded value
     * 
     * @see Math#round(double)
     */
    public long round(double value) {
        return Math.round(value);
    }
    
    /**
     * Get the closest int value to the given value.
     * 
     * @param value The value to round
     * 
     * @return The rounded value
     * 
     * @see Math#round(float)
     */
    public int round(float value) {
        return Math.round(value);
    }
    
    /** 
     * Get the trigonometric sine of an angle. 
     *
     * @param angle The associated angle
     * 
     * @return The trigonometric sine of an angle
     * 
     * @see Math#sin(double)
     */
    public double sin(double angle) {
        return Math.sin(angle);
    }
    
    /** 
     * Get the correctly rounded positive square root of a double value.
     * 
     * @param value The associated value
     * 
     * @return The square root of the value
     * 
     * @see Math#sqrt(double)
     */
    public double sqrt(double value) {
        return Math.sqrt(value);
    }
    
    /** 
     * Get the trigonometric tangent of an angle. 
     *
     * @param angle The associated angle
     * 
     * @return The trigonometric tangent of an angle
     * 
     * @see Math#tan(double)
     */
    public double tan(double value) {
        return Math.tan(value);
    }
    
    /** 
     * Converts an angle measured in radians to the equivalent angle 
     * measured in degrees. 
     * 
     * @param angrad The angle in radians
     * 
     * @return The angle in degrees
     * 
     * @see Math#toDegrees(double)
     */
    public double toDegrees(double angrad) {
        return Math.toDegrees(angrad);
    }
    
    /** 
     * Converts an angle measured in degrees to the equivalent angle 
     * measured in radians. 
     * 
     * @param angdeg The angle in degrees
     * 
     * @return The angle in radians
     * 
     * @see Math#toRadians(double)
     */
    public double toRadians(double angdeg) {
        return Math.toRadians(angdeg);
    }
}
