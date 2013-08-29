package com.bm.nio.utils.impl;

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;

public class CipherUtilsImplFast extends CipherUtilsImplStandard {

	@Override
	public byte[] decryptBlockImpl(Cipher decipher, byte[] bufEnc,
			int start, int len) throws GeneralSecurityException {
        byte [] tmp = new byte[len - start]; 
        System.arraycopy(bufEnc, start, tmp, 0, len); 
        tmp = decipher.doFinal(tmp);
        return tmp;
	}
	@Override
	public byte[] encryptBlockImpl(Cipher encipher, byte[] bufPlain,
			int start, int len) throws GeneralSecurityException {
        byte [] tmp = new byte[len - start]; 
        System.arraycopy(bufPlain, start, tmp, 0, len); 
        tmp = encipher.doFinal(tmp);
        return tmp;
	}

	@Override
	public int getEncAmtImpl(Cipher encipher, int decAmt) {
		return encipher.getOutputSize(decAmt);
	}

}
