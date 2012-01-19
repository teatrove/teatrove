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

public class MathContext {

    /**
     * The double value that is closer than any other to e, the base of the 
     * natural logarithms.
     */
    public double getE() {
        return Math.E;
    }
    
    /**
     * The double value that is closer than any other to pi, the ratio of the 
     * circumference of a circle to its diameter.
     */
    public double getPI() {
        return Math.PI;
    }

    /**
     * Returns the absolute value of a double value.
     */
    public double abs(double a) {
        return Math.abs(a);
    }
        
    /** Returns the absolute value of a float value. */
    public float abs(float a) {
        return Math.abs(a);
    }
    
    /** Returns the absolute value of an int value. */
    public int abs(int a) {
        return Math.abs(a);
    }
    
    /** Returns the absolute value of a long value. */
    public long abs(long a) {
        return Math.abs(a);
    }
    
    /** Returns the arc cosine of an angle, in the range of 0.0 through pi. */
    public double acos(double a) {
        return Math.acos(a);
    }
    
    /** Returns the arc sine of an angle, in the range of -pi/2 through pi/2. */
    public double asin(double a) {
        return Math.asin(a);
    }
    
    /** 
     * Returns the arc tangent of an angle, in the range 
     * of -pi/2 through pi/2.
     */
    public double atan(double a) {
        return Math.atan(a);
    }
    
    /** Converts rectangular coordinates (b, a) {}to polar (r, theta). */
    public double atan2(double a, double b) {
        return Math.atan2(a, b);
    }
    
    /** 
     * Returns the smallest (closest to negative infinity)double value that is 
     * not less than the argument and is equal to a mathematical integer. 
     */
    public double ceil(double a) {
        return Math.ceil(a);
    }
    
    /** Returns the trigonometric cosine of an angle. */
    public double cos(double a) {
        return Math.cos(a);
    }
    
    /** 
     * Returns the exponential number e (i.e., 2.718...) raised to the power 
     * of a double value. 
     */
    public double exp(double a) {
        return Math.exp(a);
    }
    
    /** 
     * Returns the largest (closest to positive infinity) double value that is 
     * not greater than the argument and is equal to a mathematical integer.
     */
    public double floor(double a) {
        return Math.floor(a);
    }
    
    /** 
     * Computes the remainder operation on two arguments as prescribed by 
     * the IEEE 754 standard. 
     */
    public double IEEEremainder(double f1, double f2) {
        return Math.IEEEremainder(f1, f2);
    }
    
    /** Returns the natural logarithm (base e) of a double value. */
    public double log(double a) {
        return Math.log(a);
    }
    
    /** Returns the greater of two double values. */
    public double max(double a, double b) {
        return Math.max(a, b);
    }
    
    /** Returns the greater of two float values. */
    public float max(float a, float b) {
        return Math.max(a, b);
    }
    
    /** Returns the greater of two int values. */
    public int max(int a, int b) {
        return Math.max(a, b);
    }
    
    /** Returns the greater of two long values. */
    public long max(long a, long b) {
        return Math.max(a, b);
    }
    
    /** Returns the smaller of two double values. */
    public double min(double a, double b) {
        return Math.min(a, b);
    }
    
    /** Returns the smaller of two float values. */
    public float min(float a, float b) {
        return Math.min(a, b);
    }
    
    /** Returns the smaller of two int values. */
    public int min(int a, int b) {
        return Math.min(a, b);
    }
    
    /** Returns the smaller of two long values. */
    public long min(long a, long b) {
        return Math.min(a, b);
    }
    
    /** 
     * Returns of value of the first argument raised to the power of the 
     * second argument. 
     */
    public double pow(double a, double b) {
        return Math.pow(a, b);
    }
    
    /** 
     * Returns a double value with a positive sign, greater than or equal to 
     * 0.0 and less than 1.0.
     */
    public double random() {
        return Math.random();
    }
    
    /** 
     * Returns the double value that is closest in value to a and is equal 
     * to a mathematical integer.
     */
    public double rint(double a) {
        return Math.rint(a);
    }
    
    /** Returns the closest long to the argument. */
    public long round(double a) {
        return Math.round(a);
    }
    
    /** Returns the closest int to the argument. */
    public int round(float a) {
        return Math.round(a);
    }
    
    /** Returns the trigonometric sine of an angle. */
    public double sin(double a) {
        return Math.sin(a);
    }
    
    /** Returns the correctly rounded positive square root of a double value. */
    public double sqrt(double a) {
        return Math.sqrt(a);
    }
    
    /** Returns the trigonometric tangent of an angle. */
    public double tan(double a) {
        return Math.tan(a);
    }
    
    /** 
     * Converts an angle measured in radians to the equivalent angle 
     * measured in degrees. 
     */
    public double toDegrees(double angrad) {
        return Math.toDegrees(angrad);
    }
    
    /** 
     * Converts an angle measured in degrees to the equivalent angle 
     * measured in radians.
     */
    public double toRadians(double angdeg) {
        return Math.toRadians(angdeg);
    }
}
