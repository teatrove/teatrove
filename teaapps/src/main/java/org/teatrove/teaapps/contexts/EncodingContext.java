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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

/**
 * Custom Tea context that provides encoding/decoding support including
 * {@link Base64} and serialization.  Base64 and serialization are helpful to
 * provide binary data in a valid HTTP request format such as query parameters
 * or POST data.
 * 
 * @author Scott Jappinen
 */
public class EncodingContext {
    
    /**
     * Base64 encode the given string.  The encoded string is also considered
     * URL-safe by properly encoding '+' and '/' characters.
     * 
     * @param str The string to encode
     * 
     * @return The Base64 encoded string
     * 
     * @see Base64#encodeBase64String(byte[])
     * @see #decodeBase64(String)
     */
    public String encodeBase64(String str) {
        return Base64.encodeBase64URLSafeString(str.getBytes());
    }
    
    /**
     * Base64 decode the given encoded string.  The value will be decoded and
     * returned.
     * 
     * @param str The Base64 encoded string
     *  
     * @return The decoded string value
     * 
     * @see Base64#decodeBase64(String)
     * @see #encodeBase64(String)
     */
    public String decodeBase64(String str) {
        return new String(Base64.decodeBase64(str));
    }
    
    /**
     * Encode the given integer array into Base64 format.
     * 
     * @param input The array of integers to encode
     * 
     * @return The Base64 encoded data
     * 
     * @throws IOException if an error occurs encoding the array stream
     * 
     * @see #decodeIntArray(String)
     */
    public String encodeIntArray(int[] input) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        int length = input.length;
        dos.writeInt(length);
        for (int i=0; i < length; i++) {
            dos.writeInt(input[i]);
        }
        
        return new String(Base64.encodeBase64URLSafe(bos.toByteArray()));
    }
    
    /**
     * Decode the given Base64 encoded value into an integer array.
     * 
     * @param input The Base64 encoded value to decode
     * 
     * @return The array of decoded integers
     * 
     * @throws IOException if an error occurs decoding the array stream
     * 
     * @see #encodeIntArray(int[])
     */
    public int[] decodeIntArray(String input) throws IOException {
        int[] result = null;
        ByteArrayInputStream bis = 
            new ByteArrayInputStream(Base64.decodeBase64(input));
        DataInputStream dis = new DataInputStream(bis);
        int length = dis.readInt();
        result = new int[length];
        for (int i=0; i < length; i++) {
            result[i] = dis.readInt();
        }
        return result;
    }
    
    /**
     * Base64 encode the given array of bytes.
     * 
     * @param input The array of bytes to include
     * 
     * @return The encoded base64 value
     * 
     * @see Base64#encodeBase64(byte[])
     * @see #decodeByteArray(String)
     */
    public String encodeByteArray(byte[] input) {
        return Base64.encodeBase64String(input);
    }

    /**
     * Decode the Base64 encoded value into an array of bytes.
     * 
     * @param input The Base64 encoded value
     * 
     * @return The decoded byte array
     * 
     * @see Base64#decodeBase64(String)
     * @see #encodeByteArray(byte[])
     */
    public byte[] decodeByteArray(String input) {
        return Base64.decodeBase64(input);
    }

    /**
     * Serialize the given value into an array of bytes.  This uses the standard
     * Java-based serializers ({@link ObjectOutputStream}) to serialize the
     * data.
     * 
     * @param value The value to serialize
     * 
     * @return The array of bytes representing the serialized form of the value
     * 
     * @throws IOException if an error occurs during serialization
     * 
     * @see #deserialize(byte[])
     * @see #serializeBase64(Serializable)
     */
    public byte[] serialize(Serializable value) 
        throws IOException {
        
        // create buffer and underlying output stream
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);
        ObjectOutputStream output = new ObjectOutputStream(buffer);
        
        // write the serialized value
        try { output.writeObject(value); }
        finally { buffer.close(); }
        
        // return the underlying bytes
        return buffer.toByteArray();
    }
    
    /**
     * Deserialize a previously serialized array of bytes into its object form.
     * This uses the standard Java-based serializers ({@link ObjectInputStream})
     * to deserialize the data and instantiate the associatd class.
     * 
     * @param <T> The type of expected data
     * 
     * @param rawBytes The raw serialized bytes to deserialize
     * 
     * @return The deserialized object representing the serialized bytes
     *  
     * @throws IOException if an error occurs deserializing the data
     * 
     * @see #deserializeBase64(String)
     * @see #serialize(Serializable)
     */
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T deserialize(byte[] rawBytes)
        throws IOException {
        
        // create buffer and underlying input stream
        ByteArrayInputStream buffer = new ByteArrayInputStream(rawBytes);
        ObjectInputStream input = new ObjectInputStream(buffer);
        
        // read the serialized value
        try { return (T) input.readObject(); } 
        catch (ClassNotFoundException e) {
            throw new IOException("error loading deserialized class", e);
        }
        finally { buffer.close(); }
    }
    
    /**
     * Serialize the given value and return the serialized form as a Base64
     * encoded format. This first {@link #serialize(Serializable) serializes}
     * the data and then {@link #encodeByteArray(byte[]) encodes} the data into
     * string form.
     * 
     * @param value The value to serialize
     * 
     * @return The Base64 encoded value representing the serialized form
     * 
     * @throws IOException if an error occurs during serialization
     * 
     * @see #deserializeBase64(String)
     * @see #serialize(Serializable)
     * @see #encodeByteArray(byte[])
     */
    public String serializeBase64(Serializable value) 
        throws IOException {
        
        byte[] rawBytes = serialize(value);
        return encodeByteArray(rawBytes);
    }
    
    /**
     * Deserialize a previously Base64 encoded serialized value. This first
     * {@link #decodeByteArray(String) decodes} the Base64 format and then
     * {@link #deserialize(byte[]) deserializes} the resulting byte array into
     * its object instance.
     *  
     * @param <T> The type of expected data
     * 
     * @param input The Base64 encoded serialized bytes to deserialize
     * 
     * @return The deserialized object representing the serialized data
     *  
     * @throws IOException if an error occurs deserializing the data
     * 
     * @see #serializeBase64(Serializable)
     * @see #decodeByteArray(String)
     * @see #deserialize(byte[])
     */
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T deserializeBase64(String input)
        throws IOException {
        
        byte[] rawBytes = decodeByteArray(input);
        return (T) deserialize(rawBytes);
    }

    /**
     * Encode the given byte array into a string representing the hexidecimal
     * format.
     * 
     * @param bytes The array of bytes to encode
     * 
     * @return The hex-based format of the byte array
     * 
     * @see Hex#encodeHex(byte[])
     * @see #decodeHex(String)
     */
    public String encodeHex(byte[] bytes) {
        return new String(Hex.encodeHex(bytes));
    }

    /**
     * Decode the given hexidecimal formatted value into an array of bytes.
     * 
     * @param str The hexidecimal formatted value
     * 
     * @return The array of bytes representing the hex values
     * 
     * @throws DecoderException if an error occurs during decoding
     * 
     * @see Hex#decodeHex(char[])
     * @see #encodeHex(byte[])
     */
    public byte[] decodeHex(String str) throws DecoderException {
        return Hex.decodeHex(str.toCharArray());
    }
    
    /**
     * Decode the given hexidecimal formatted value into a string representing
     * the bytes of the decoded value.
     * 
     * @param str The hexidecimal formatted value
     * 
     * @return The string representing the decoded bytes
     * 
     * @throws DecoderException if an error occurs during decoding
     * 
     * @see #decodeHex(String)
     */
    public String decodeHexToString(String str) throws DecoderException {
        String result = null;
        byte[] bytes = Hex.decodeHex(str.toCharArray());
        if (bytes != null && bytes.length > 0) {
            result = new String(bytes);
        }
        return result;
    }
}

