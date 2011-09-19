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

package org.teatrove.barista.util;

import java.util.Map;

/**
 * Provides fast matching of strings against patterns containing wildcards.
 * An ordinary map must be supplied in order to create a PatternMatcher. The
 * map keys must be strings. Asterisks (*) are treated as wildcard characters.
 *
 * @deprecated Use {@link org.teatrove.trove.util.PatternMatcher}
 * @author Brian S O'Neill
 */
public class PatternMatcher {
    public static PatternMatcher forPatterns(Map patternMap) {
        return new PatternMatcher(patternMap);
    }

    private final org.teatrove.trove.util.PatternMatcher mMatcher;

    private PatternMatcher(Map patternMap) {
        mMatcher = org.teatrove.trove.util.PatternMatcher.forPatterns(patternMap);
    }

    public Result getMatch(String lookup) {
        org.teatrove.trove.util.PatternMatcher.Result result =
            mMatcher.getMatch(lookup);
        return (result == null) ? null : new Result(result);
    }

    public Result[] getMatches(String lookup, int limit) {
        org.teatrove.trove.util.PatternMatcher.Result[] results =
            mMatcher.getMatches(lookup, limit);
        int length = results.length;
        Result[] copiedResults = new Result[length];
        for (int i=0; i<length; i++) {
            copiedResults[i] = new Result(results[i]);
        }
        return copiedResults;
    }

    public static class Result {
        private final org.teatrove.trove.util.PatternMatcher.Result mResult;

        Result(org.teatrove.trove.util.PatternMatcher.Result result) {
            mResult = result;
        }

        public String getPattern() {
            return mResult.getPattern();
        }

        /**
         * Returns the value associated with the matched pattern.
         */
        public Object getValue() {
            return mResult.getValue();
        }

        /**
         * Returns the indexes used to parse the lookup string at wildcard
         * positions in order for it to match the pattern. Array length is
         * always double the number of wildcards in the pattern. Every even
         * element is the start index (inclusive) of a wildcard match, and
         * every odd element is the end index (exclusive) of a wildcard match.
         */
        public int[] getWildcardPositions() {
            return mResult.getWildcardPositions();
        }
    }
}
