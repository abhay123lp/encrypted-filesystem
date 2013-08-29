package com.bm.nio.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;

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
		@OverrideRequired(group="common")
	    public byte [] decryptBlockImpl(Cipher decipher, byte [] bufEnc, int start, int len) throws GeneralSecurityException;
		@OverrideRequired(group="common")
	    public byte [] encryptBlockImpl(Cipher encipher, byte [] bufPlain, int start, int len) throws GeneralSecurityException;
		@OverrideRequired(group="common")
	    public int getEncAmtImpl(Cipher encipher, int decAmt);
	    //name
		@OverrideRequired(group="nameEncrypt")
	    public String encryptNameImpl(String plainName, Cipher encipher) throws GeneralSecurityException;
		@OverrideRequired(group="nameEncrypt")
	    public String decryptNameImpl(String encName, Cipher decipher) throws GeneralSecurityException;
	    /**
	     * Encode from binary to string, so can be used in file names for example.
	     * Should be implemented together with {@link CipherUtilsImpl#decodeName(String)}
	     * @param name
	     * @return
	     */
		@OverrideRequired(group="nameEncode")
	    public String encodeName(byte [] name);
	    /**
	     * decode from encoded string to binary. Should be implemented together with {@link CipherUtilsImpl#encodeName(byte[])}
	     * @param name
	     * @return
	     */
		@OverrideRequired(group="nameEncode")
	    public byte [] decodeName(String name);
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface OverrideRequired{
		String group() default "default";
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

			HashMap<String, ArrayList<Method>> groups = getGroups(CipherUtilsImpl.class);
				// check required methods
				for (ArrayList<Method> methods : groups.values()){
					if (!isAllOrNoneOverriden(methods, impl))
						throw new CipherUtilsImplException(
								impl.getClass().getSimpleName()
										+ " class should implement " + getRequiredStr(methods) + " methods of interface "
										+ CipherUtilsImpl.class.getSimpleName());
			}
			isImplSet = true;
			CipherUtils.impl = impl;
		}
	}
	
	private static String getRequiredStr(ArrayList<Method> methods){
		String res = "";
		for(int i = 0; i < methods.size() - 1; i ++){
			res += methods.get(i).getName() + " ";
		}
		return res;
	}
	
	private static HashMap<String, ArrayList<Method>> getGroups(Class cl){
		HashMap<String, ArrayList<Method>> groups = new HashMap<String, ArrayList<Method>>();
		final Method [] methodsTmp = cl.getMethods();
		for(int i = 0; i < methodsTmp.length; i ++){
			final OverrideRequired required = methodsTmp[i].getAnnotation(OverrideRequired.class);
			if (required != null){
				ArrayList<Method> group = groups.get(required.group());
				if (group == null){
					group = new ArrayList<Method>();
					groups.put(required.group(), group);
				}
				group.add(methodsTmp[i]);
			}
		}
		return groups;
	}
	
	private static boolean isAllOrNoneOverriden(ArrayList<Method> methods, Object obj){
		try {
			for(int i = 0; i < methods.size() - 1; i ++){
				if (!obj.getClass()
						.getMethod(methods.get(i).getName(), methods.get(i).getParameterTypes())
						.getDeclaringClass()
						.equals(obj.getClass().getMethod(methods.get(i + 1).getName(), methods.get(i + 1).getParameterTypes())
								.getDeclaringClass()))
					return false;
			}
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
			return false;
		}
		return true;
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
		return impl.decryptNameImpl(encName, decipher);
	}
}
