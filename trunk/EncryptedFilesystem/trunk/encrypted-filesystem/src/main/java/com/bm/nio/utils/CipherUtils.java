package com.bm.nio.utils;

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.xml.bind.DatatypeConverter;

public class CipherUtils {
    public static byte [] decryptBlock(Cipher decipher, byte [] bufEnc) throws GeneralSecurityException {
    	return decryptBlock(decipher, bufEnc, 0, bufEnc.length);
    }
    public static byte [] decryptBlock(Cipher decipher, byte [] bufEnc, int start, int len) throws GeneralSecurityException {
        byte [] tmp = new byte[len - start]; 
        System.arraycopy(bufEnc, start, tmp, 0, len); 
        tmp = decipher.doFinal(tmp);
        unxor(tmp);
        flip(tmp);
        tmp = decipher.doFinal(tmp);
        unxor(tmp);
        return tmp;
    }
    
    public static byte [] encryptBlock(Cipher encipher, byte [] bufPlain) throws GeneralSecurityException {
    	return encryptBlock(encipher, bufPlain, 0, bufPlain.length);
    }
    public static byte [] encryptBlock(Cipher encipher, byte [] bufPlain, int start, int len) throws GeneralSecurityException {
        byte [] tmp = new byte[len - start]; 
        System.arraycopy(bufPlain, start, tmp, 0, len); 
        xor(tmp);
        tmp = encipher.doFinal(tmp);
        flip(tmp);
        xor(tmp);
        tmp = encipher.doFinal(tmp);
        return tmp;
    }

	public static String encryptName(String plainName, Cipher encipher) throws GeneralSecurityException {
		//TODO:
		// consider using MAC
		//throw new IllegalArgumentException();
		//base64 contains "/" symbol!
		//final String res = DatatypeConverter.printBase64Binary(encryptBlock(encipher, plainName.getBytes()));
		final String res = DatatypeConverter.printHexBinary(encryptBlock(encipher, plainName.getBytes()));
		//encode
		return res;
		//return plainName;
	}
	
	public static String decryptName(String encName, Cipher decipher) throws GeneralSecurityException {
		//TODO: consider using MAC
		//base64 contains "/" symbol!
		final String res = new String(decryptBlock(decipher, DatatypeConverter.parseHexBinary(encName)));
		return res;
		//return encName;
	}


    static void flip(byte [] a){
    	for (int i = 0, j = a.length - 1; i < a.length/2; i ++, j --){
    		byte tmp = a[i];
    		a[i] = a[j];
    		a[j] = tmp;
    	}
    }

    static void xor(byte [] a){
    	for(int i = 0; i < a.length - 1; i++)
    		a[i + 1] ^= a[i];
    }

    static void unxor(byte [] a){
    	for(int i = a.length - 1; i > 0; --i)
    		a[i] ^= a[i-1];
    }
}
