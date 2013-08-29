package com.bm.nio.utils.impl;

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.xml.bind.DatatypeConverter;

import com.bm.nio.utils.CipherUtils;
import com.bm.nio.utils.CipherUtils.CipherUtilsImpl;

public class CipherUtilsImplStandard implements CipherUtilsImpl {

	@Override
	public byte[] decryptBlockImpl(Cipher decipher, byte[] bufEnc,
			int start, int len) throws GeneralSecurityException {
        byte [] tmp = new byte[len - start]; 
        System.arraycopy(bufEnc, start, tmp, 0, len); 
        tmp = decipher.doFinal(tmp);
        unxor(tmp);
        flip(tmp);
        tmp = decipher.doFinal(tmp);
        unxor(tmp);
        return tmp;
	}
	@Override
	public byte[] encryptBlockImpl(Cipher encipher, byte[] bufPlain,
			int start, int len) throws GeneralSecurityException {
        byte [] tmp = new byte[len - start]; 
        System.arraycopy(bufPlain, start, tmp, 0, len); 
        xor(tmp);
        tmp = encipher.doFinal(tmp);
        flip(tmp);
        xor(tmp);
        tmp = encipher.doFinal(tmp);
        return tmp;
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

	@Override
	public int getEncAmtImpl(Cipher encipher, int decAmt) {
		return encipher.getOutputSize(encipher.getOutputSize(decAmt));
	}

	@Override
	public String encryptNameImpl(String plainName, Cipher encipher)
			throws GeneralSecurityException {
		//TODO:
		// consider using MAC
		//throw new IllegalArgumentException();
		//base64 contains "/" symbol!
		//final String res = DatatypeConverter.printBase64Binary(encryptBlock(encipher, plainName.getBytes()));
		
		final String res = encodeName(CipherUtils.encryptBlock(encipher, plainName.getBytes()));
		//encode
		return res;
		//return plainName;
	}

	@Override
	public String decryptNameImpl(String encName, Cipher decipher)
			throws GeneralSecurityException {
		//TODO: consider using MAC
		//base64 contains "/" symbol!
		
		final String res = new String(CipherUtils.decryptBlock(decipher, decodeName(encName)));
		return res;
		//return encName;
	}
	@Override
	public String encodeName(byte [] name) {
		return DatatypeConverter.printHexBinary(name);
	}
	@Override
	public byte [] decodeName(String name) {
		return DatatypeConverter.parseHexBinary(name);
	}
	
}
