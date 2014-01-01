package com.bm.nio.file;

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;

import com.bm.nio.utils.CipherUtils;
import com.bm.nio.utils.CipherUtilsImplException;
import com.bm.nio.utils.impl.CipherUtilsImplStandard;

public class CipherUtilsTest {

	static class ImplIncorrect extends CipherUtilsImplStandard {

		@Override
		public byte[] decryptBlockImpl(Cipher decipher, byte[] bufEnc,
				int start, int len) throws GeneralSecurityException {
			return super.decryptBlockImpl(decipher, bufEnc, start, len);
		}

		@Override
		public byte[] encryptBlockImpl(Cipher encipher, byte[] bufPlain,
				int start, int len) throws GeneralSecurityException {
			return super.encryptBlockImpl(encipher, bufPlain, start, len);
		}

	}
	
	static class ImplIncorrect1 extends CipherUtilsImplStandard {

		@Override
		public String encodeName(byte[] name) {
			return super.encodeName(name);
		}
	}
	
	static class ImplCorrect extends ImplIncorrect {

		@Override
		public byte[] decryptBlockImpl(Cipher decipher, byte[] bufEnc,
				int start, int len) throws GeneralSecurityException {
			return super.decryptBlockImpl(decipher, bufEnc, start, len);
		}

		@Override
		public byte[] encryptBlockImpl(Cipher encipher, byte[] bufPlain,
				int start, int len) throws GeneralSecurityException {
			return super.encryptBlockImpl(encipher, bufPlain, start, len);
		}

		@Override
		public int getEncAmtImpl(Cipher encipher, int decAmt) {
			return super.getEncAmtImpl(encipher, decAmt);
		}

	}
	@Test
	public void testSetImpl() throws Exception {
		boolean exception;
		CipherUtils.resetImpl();
		
		// not all required methods are overriden
		exception = false;
		try {
			CipherUtils.setImpl(new ImplIncorrect());
		} catch (CipherUtilsImplException e) {
			exception = true;
		}
		Assert.assertTrue(exception);
		
		// not all required methods are overriden
		exception = false;
		try {
			CipherUtils.setImpl(new ImplIncorrect1());
		} catch (CipherUtilsImplException e) {
			exception = true;
		}

		
		Assert.assertTrue(exception);
		
		//correctly override all required methods
		exception = false;
		try {
			CipherUtils.setImpl(new ImplCorrect());
		} catch (CipherUtilsImplException e) {
			exception = true;
		}
		Assert.assertFalse(exception);
		
		//doesn't allow to set more than once
		exception = false;
		try {
			CipherUtils.setImpl(new ImplCorrect());
		} catch (CipherUtilsImplException e) {
			exception = true;
		}
		Assert.assertTrue(exception);
		
		//... unless reset
		exception = false;
		try {
			CipherUtils.resetImpl();
			CipherUtils.setImpl(new ImplCorrect());
		} catch (CipherUtilsImplException e) {
			exception = true;
		}
		Assert.assertFalse(exception);
	}
	
	@After
	public void reset(){
		CipherUtils.resetImpl();
	}
	
}
