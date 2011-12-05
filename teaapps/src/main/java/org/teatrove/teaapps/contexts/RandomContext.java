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

import java.util.Random;

/**
 * @author Scott Jappinen
 */
public class RandomContext {
    
    private Random random = new Random();
    
    /**
     * Returns the next pseudorandom, uniformly distributed boolean value from 
     * this random number generator's sequence.
     */
    public boolean nextBoolean() {
        return random.nextBoolean();
    }

    /**
     * Generates random bytes and places them into a user-supplied 
     * byte array. 
     */
    public void nextBytes(byte[] bytes) {
        random.nextBytes(bytes);
    }

    /**
     * Returns the next pseudorandom, uniformly distributed double value between
     * 0.0 and 1.0 from this random number generator's sequence. 
     */
    public double nextDouble() {
        return random.nextDouble();
    }

    /**
     * Returns the next pseudorandom, uniformly distributed float value between
     * 0.0 and 1.0 from this random number generator's sequence. 
     */
    public float nextFloat() {
        return random.nextFloat();
    }

    /**
     * Returns the next pseudorandom, Gaussian ("normally") distributed double 
     * value with mean 0.0 and standard deviation 1.0 from this random number 
     * generator's sequence. 
     */
    public double nextGaussian() {
        return random.nextGaussian();
    }

    /**
     * Returns the next pseudorandom, uniformly distributed int value from 
     * this random number generator's sequence. 
     */
    public int nextInt() {
        return random.nextInt();
    }

    /**
     * Returns a pseudorandom, uniformly distributed int value between 0 
     * (inclusive) and the specified value (exclusive), drawn from this random 
     * number generator's sequence. 
     */
    public int nextInt(int n) {
        return random.nextInt(n);
    }

    /**
     * Returns the next pseudorandom, uniformly distributed long value from 
     * this random number generator's sequence. 
     */
    public long nextLong() {
        return random.nextLong();
    }
}
