package com.bm.nio.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

import javax.crypto.Cipher;
import com.bm.nio.utils.impl.CipherUtilsImplStandard;

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
		@OverrideRequired
	    public byte [] decryptBlockImpl(Cipher decipher, byte [] bufEnc, int start, int len) throws GeneralSecurityException;
		@OverrideRequired
	    public byte [] encryptBlockImpl(Cipher encipher, byte [] bufPlain, int start, int len) throws GeneralSecurityException;
		@OverrideRequired
	    public int getEncAmtImpl(Cipher encipher, int decAmt);
	    //name
	    public String encryptNameImpl(String plainName, Cipher encipher) throws GeneralSecurityException;
	    public String decryptName(String encName, Cipher decipher) throws GeneralSecurityException;
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface OverrideRequired{
	}
	
	private static boolean isImplSet = false;
	private static CipherUtilsImpl impl = new CipherUtilsImplStandard();
	private static Object lock = new Object();
	/**
	 * Sets implementation of encryption/encding functions Can only be set once.
	 * @param impl
	 */
	public static void setImpl(CipherUtilsImpl impl) throws CipherUtilsImplException {
		synchronized (lock) {
			if (isImplSet)
				throw new CipherUtilsImplException(CipherUtilsImpl.class.getSimpleName() + " was already set before");

			String requiredStr = "";
			try {
				final Method [] methodsTmp = CipherUtilsImpl.class.getMethods();
				ArrayList<Method> methods = new ArrayList<Method>();
				for(int i = 0; i < methodsTmp.length - 1; i ++){
					if (methodsTmp[i].getAnnotation(OverrideRequired.class) != null){
						methods.add(methodsTmp[i]);
						requiredStr += methodsTmp[i].getName() + " ";
					}
				}

				// check required methods
				for(int i = 0; i < methods.size() - 1; i ++){
					if (!impl.getClass()
							.getMethod(methods.get(i).getName(), methods.get(i).getParameterTypes())
							.getDeclaringClass()
							.equals(impl.getClass().getMethod(methods.get(i + 1).getName(), methods.get(i + 1).getParameterTypes())
									.getDeclaringClass()))
						throw new CipherUtilsImplException(
								impl.getClass().getSimpleName()
										+ " class should implement " + requiredStr + " methods of interface "
										+ CipherUtilsImpl.class.getSimpleName());
				}
			} catch (SecurityException e) {
			} catch (NoSuchMethodException e) {
				throw new CipherUtilsImplException(
						impl.getClass().getSimpleName()
								+ " class should implement " + requiredStr + "methods of interface "
								+ CipherUtilsImpl.class.getSimpleName());
			}
			isImplSet = true;
			CipherUtils.impl = impl;
		}
	}
	
	public static void resetImpl() {
		synchronized (lock) {
			isImplSet = false;
			impl = new CipherUtilsImplStandard();
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
