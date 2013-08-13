package com.bm.nio.utils;

import java.lang.reflect.Method;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.xml.bind.DatatypeConverter;

import com.bm.nio.file.utils.TestUtils;

/**
 * To extend functionality use {@link #setImpl(CipherUtilsImpl)}.
 * Better than using factory template.
 * @author Mike
 *
 */
public class CipherUtils {
	
	/**
	 * Implement and set using {@link CipherUtils#setImpl(CipherUtilsImpl)} to extend functionality.
	 * All methods should be implemented in single class.
	 * @author Mike
	 */
	public interface CipherUtilsImpl{
	    public byte [] decryptBlockImpl(Cipher decipher, byte [] bufEnc, int start, int len) throws GeneralSecurityException;
	    public byte [] encryptBlockImpl(Cipher encipher, byte [] bufPlain, int start, int len) throws GeneralSecurityException;
	    public int getEncAmtImpl(Cipher encipher, int decAmt);
	    //name
	    public String encryptNameImpl(String plainName, Cipher encipher) throws GeneralSecurityException;
	    public String decryptName(String encName, Cipher decipher) throws GeneralSecurityException;
	}

	public static class CipherUtilsImplStandard implements CipherUtilsImpl {

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
			final String res = DatatypeConverter.printHexBinary(encryptBlock(encipher, plainName.getBytes()));
			//encode
			return res;
			//return plainName;
		}

		@Override
		public String decryptName(String encName, Cipher decipher)
				throws GeneralSecurityException {
			//TODO: consider using MAC
			//base64 contains "/" symbol!
			final String res = new String(decryptBlock(decipher, DatatypeConverter.parseHexBinary(encName)));
			return res;
			//return encName;
		}
		
	}
	
	private static boolean isImplSet = false;
	private static CipherUtilsImpl impl = new CipherUtilsImplStandard();
	private static Object lock = new Object();
	/**
	 * Sets implementation of encryption/encding functions Can only be set once.
	 * @param impl
	 */
	public static void setImpl(CipherUtilsImpl impl){
		synchronized (lock) {
			if (isImplSet)
				throw new RuntimeException(CipherUtilsImpl.class.getSimpleName() + " was already set before");

			try {
				final Method [] methods = CipherUtilsImpl.class.getMethods();
				for(int i = 0; i < methods.length - 1; i ++){
					if (impl.getClass()
							.getMethod(methods[i].getName())
							.getDeclaringClass()
							.equals(impl.getClass().getMethod(methods[i + 1].getName())
									.getDeclaringClass()))
						throw new RuntimeException(
								impl.getClass().getSimpleName()
										+ " class should implement all methods of interface "
										+ CipherUtilsImpl.class.getSimpleName());
				}
			} catch (NoSuchMethodException | SecurityException e) {
			}
			isImplSet = true;
			CipherUtils.impl = impl;
		}
	}
	
    /**
     * Should be overridden together with encrypt/decrypt block
     * to return correct encrypted size
     * Reverse function getEncAmt(int encSize) cannot be made because of padding in encrypted data
     * @param decAmt
     * @return
     */
    public static int getEncAmt(Cipher encipher, int decAmt){
    	return impl.getEncAmtImpl(encipher, decAmt);
    }
    
    public static byte [] decryptBlock(Cipher decipher, byte [] bufEnc) throws GeneralSecurityException {
    	return decryptBlock(decipher, bufEnc, 0, bufEnc.length);
    }

    public static byte [] decryptBlock(Cipher decipher, byte [] bufEnc, int start, int len) throws GeneralSecurityException {
    	return impl.decryptBlockImpl(decipher, bufEnc, start, len);
    }
    
    public static byte [] encryptBlock(Cipher encipher, byte [] bufPlain) throws GeneralSecurityException {
    	return encryptBlock(encipher, bufPlain, 0, bufPlain.length);
    }
    public static byte [] encryptBlock(Cipher encipher, byte [] bufPlain, int start, int len) throws GeneralSecurityException {
    	return impl.encryptBlockImpl(encipher, bufPlain, start, len);
    }

	public static String encryptName(String plainName, Cipher encipher) throws GeneralSecurityException {
		return impl.encryptNameImpl(plainName, encipher);
	}
	
	public static String decryptName(String encName, Cipher decipher) throws GeneralSecurityException {
		return impl.decryptName(encName, decipher);
	}
}
