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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

/**
 * Custom Tea context used to provide cryptography-based functionality including
 * MD5 and SHA digests and HMAC signatures.
 * 
 * @author Scott Jappinen
 */
public class CryptoContext {

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    
    /**
     * Generates a digest based on the given algorithm.
     * 
     * @param algorithm The algorithm to use (MD5, SHA, etc)
     * @param bytes The bytes to digest
     * 
     * @return The associated digest
     * 
     * @see MessageDigest
     */
    public byte[] digest(String algorithm, byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            return md.digest(bytes);
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "unable to access MD5 algorithm", exception
            );
        }
    }
    
    /**
     * Generates a digest based on the MD5 digest instance.
     * 
     * @param bytes The bytes to digest
     * 
     * @return The MD5 digest
     * 
     * @see MessageDigest
     */
    public byte[] md5Digest(byte[] bytes) {
        return digest("MD5", bytes);
    }
    
    /**
     * Generates a digest based on the SHA digest instance.
     * 
     * @param bytes The bytes to digest
     * 
     * @return The SHA digest
     * 
     * @see MessageDigest
     */
    public byte[] shaDigest(byte[] bytes) {
        return digest("SHA", bytes);
    }
    
    /**
     * Computes RFC 2104-compliant HMAC signature.
     * 
     * @param data The data to be signed.
     * @param key The signing key.
     * 
     * @return The Base64-encoded RFC 2104-compliant HMAC signature.
     * 
     * @throws java.security.SignatureException when signature generation fails
     */
    public String calculateRFC2104HMAC(String data, String key)
        throws java.security.SignatureException {
        
        String result;
        try {
            // get an hmac_sha1 key from the raw key bytes
            SecretKeySpec signingKey = 
                new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);

            // get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);
            
            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data.getBytes());
            
            // base64-encode the hmac
            result = new String(Base64.encodeBase64(rawHmac));
        } 
        catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC", e);
        }
        
        return result;
    }
    
    /**
     * Creates a MD5 digest of the given signature and returns the result as
     * a hex-encoded string.
     * 
     * @param signature The signature to digest
     * 
     * @return The MD5 digest of the signature in hex format
     */
    public String md5HexDigest(String signature) {
		byte[] bytes = md5Digest(signature.getBytes());
		return new String(Hex.encodeHex(bytes));
	}
    
    /**
     * Creates a MD5 digest of the given signature and returns the result as
     * a hex-encoded string. The signature is converted using the given 
     * charsetName.
     * 
     * @param signature The signature to digest
     * @param charsetName The name of the character set to transform with
     * 
     * @return The MD5 digest of the signature in hex format
     * 
     * @throws UnsupportedEncodingException If the given character set cannot
     *         be used
     */
    public String md5HexDigest(String signature, String charsetName)
        throws UnsupportedEncodingException {
        
    	byte[] bytes = md5Digest(signature.getBytes(charsetName));
    	return new String(Hex.encodeHex(bytes));
    }
    
    /**
     * Creates a MD5 digest of the given signature and returns the result as
     * a Base64-encoded string.
     * 
     * @param signature The signature to digest
     * 
     * @return The MD5 digest of the signature in Base64 format
     */
    public String md5Base64Digest(String signature) {
		byte[] bytes = md5Digest(signature.getBytes());
		return new String(Base64.encodeBase64(bytes));
	}
	
    /**
     * Creates a MD5 digest of the given signature and returns the result as
     * a Base64-encoded string. The signature is converted using the given 
     * charsetName.
     * 
     * @param signature The signature to digest
     * @param charsetName The name of the character set to transform with
     * 
     * @return The MD5 digest of the signature in Base64 format
     * 
     * @throws UnsupportedEncodingException If the given character set cannot
     *         be used
     */
	public String md5Base64Digest(String signature, String charsetName) 
		throws UnsupportedEncodingException {
	    
		byte[] bytes = md5Digest(signature.getBytes(charsetName));
		return new String(Base64.encodeBase64(bytes));
	}
}

