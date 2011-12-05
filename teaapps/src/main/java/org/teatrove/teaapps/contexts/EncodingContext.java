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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.DecoderException;

/**
 * @author Scott Jappinen
 */
public class EncodingContext {
    
    public static String encodeBase64(String str) {
        return Base64.encodeBase64URLSafeString(str.getBytes());
    }
    
    public static String decodeBase64(String str) {
        return new String(Base64.decodeBase64(str));
    }
    
    public static String encodeIntArray(int[] input) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        int length = input.length;
        dos.writeInt(length);
        for (int i=0; i < length; i++) {
            dos.writeInt(input[i]);
        }
        return new String(Base64.encodeBase64URLSafe(bos.toByteArray()));
    }
    
    public static int[] decodeIntArray(String input) throws IOException {
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
    
    public String encodeByteArray(byte[] input)
        throws IOException {
        
        return new String(JBase64.encodeBytes(input));
    }

    public byte[] decodeByteArray(String input)
        throws IOException {
        
        return JBase64.decodeBytes(input.getBytes());
    }

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
    
    public String serializeBase64(Serializable value) 
        throws IOException {
        
        byte[] rawBytes = serialize(value);
        return encodeByteArray(rawBytes);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T deserializeBase64(String input)
        throws IOException {
        
        byte[] rawBytes = decodeByteArray(input);
        return (T) deserialize(rawBytes);
    }

    
    public static String encodeHex(byte[] bytes) {
        return new String(Hex.encodeHex(bytes));
    }

    public static byte[] decodeHex(String str) throws DecoderException {
        return Hex.decodeHex(str.toCharArray());
    }
    
    public static String decodeHexToString(String str) throws DecoderException {
        String result = null;
        byte[] bytes = Hex.decodeHex(str.toCharArray());
        if (bytes != null && bytes.length > 0) {
            result = new String(bytes);
        }
        return result;
    }
}

