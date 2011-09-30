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

package org.teatrove.teaservlet.util;

import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.HashSet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * A convenience HttpServletRequest wrapper that automatically decodes request
 * parameters using the provided character encoding.
 *
 * @author Brian S O'Neill
 */
public class DecodedRequest extends HttpServletRequestWrapper {
    private static final byte[] TEST_BYTES = {65};

    private static Set cGoodEncodings = new HashSet(7);

    private static synchronized String checkEncoding(String encoding) {
        if (!cGoodEncodings.contains(encoding)) {
            // Test the encoding.
            try {
                new String(TEST_BYTES, encoding);
            }
            catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException
                    ("Unsupported character encoding: " + encoding);
            }
            cGoodEncodings.add(encoding);
        }
        return encoding;
    }

    private final String mEncoding;
    private final String mOriginalEncoding;

    /**
     * @param request wrapped request
     * @param encoding character encoding to apply to request parameters
     * @throws IllegalArgumentException when the encoding isn't supported
     */
    public DecodedRequest(HttpServletRequest request, String encoding) {
        super(request);
        mEncoding = checkEncoding(encoding);
        String original = request.getCharacterEncoding();
        if (original == null) {
            original = "iso-8859-1";
        }
        mOriginalEncoding = original;
    }

    public String getCharacterEncoding() {
        return mEncoding;
    }

    public String getParameter(String name) {
        String value;
        if ((value = super.getParameter(name)) != null) {
            try {
                return new String
                    (value.getBytes(mOriginalEncoding), mEncoding);
            }
            catch (UnsupportedEncodingException e) {
            }
        }
        return value;
    }

    public String[] getParameterValues(String name) {
        String[] values = (String[])super.getParameterValues(name).clone();
        try {
            String enc = mEncoding;
            String orig = mOriginalEncoding;
            for (int i = values.length; --i >= 0; ) {
                String value;
                if ((value = values[i]) != null) {
                    values[i] = new String(value.getBytes(orig), enc);
                }
            }
        }
        catch (UnsupportedEncodingException e) {
        }
        return values;
    }
}
