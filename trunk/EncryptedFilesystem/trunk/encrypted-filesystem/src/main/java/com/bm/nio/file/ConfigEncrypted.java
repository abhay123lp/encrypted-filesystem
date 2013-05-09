package com.bm.nio.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.ElementException;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.core.ValueRequiredException;
import org.simpleframework.xml.stream.Format;

/**
 * class holding encrypted configuration
 * @author Mike
 *
 */
@Root(strict = true, name = "encryptionConfiguration")
public class ConfigEncrypted {
	
	private Persister serializer = new Persister(new Format("<?xml version=\"1.0\" encoding= \"UTF-8\"?>"));
	private ConfigEncrypted() {
	};
	
	/**
	 * Creates default config
	 * @param password
	 * @return
	 */
	public static ConfigEncrypted newConfig() {
		return new ConfigEncrypted();
	}
	
//    public static final String PROPERTY_PLAIN_BLOCK_SIZE = "block.size";
//    public static final String PROPERTY_PASSWORD = "password";
//    public static final String PROPERTY_TRANSFORMATION = "transformation";
//    //new
//	public static final String PROPERTY_SALT = "salt";
//	public static final String PROPERTY_KEY_STRENGTH = "keystrength";
//	public static final String PROPERTY_ITERATION_COUNT = "iterationcount";
//	public static final String PROPERTY_VERSION = "version";
	
	// properties

	//DONE: consider nulling after generating cipher
	//better replace with SecretKeySpec
	//private SecretKeySpec key;

	@Attribute
	private String version = "1.0";//TODO: take from meta-inf
	@Element
	private int blockSize = 8192;
	@Element
	private String transformation = "AES/CFB/NoPadding";
	@Element(required = false)
	private String provider = null;
	@Element
	private String salt = "12345678";
	@Element
	private int keyStrength = 128;
	@Element
	private int iterationCount = 1024;
	@Element
	private boolean macNames = true;
	@Element
	private boolean macFiles = true;
	
	public static ConfigEncrypted loadConfig(Path path) {
		//TOTEST
		ConfigEncrypted res = new ConfigEncrypted();
		//Properties p = new Properties();
		try (InputStream is = Files.newInputStream(path, StandardOpenOption.READ);){
//			p.load(is);
//			return loadConfig(p);
			try {
				res.serializer.read(res, is);
			} catch (IOException e) {
				throw e;
			} catch (ValueRequiredException e) {
				final RuntimeException rte = new RuntimeException(
						"Required configuration parameter is missing. Unable to load from "
								+ path);
				rte.initCause(e);
				throw rte;
			} catch (Exception e) {
				final RuntimeException rte = new RuntimeException(
						"Unable to save encrypted configuration to " + path);
				rte.initCause(e);
				throw rte;
			}
			return res;
		} catch (IOException e) {
			return res;
		}
	}
	
//	public static ConfigEncrypted loadConfig(Properties properties){
//		//TODO:
//		return null;
//	}
	
	public synchronized void saveConfig(Path path) throws IOException {
		//TOTEST
		//Properties p = new Properties();
		try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.WRITE);){
			//saveConfig(p);
			//p.store(os, null);
			try {
				serializer.write(this, os);
			} catch (IOException e) {
				throw e;
			} catch (ElementException e) {
				final RuntimeException rte = new RuntimeException(
						"Incorrect field value of configuration class. Unable to save encrypted configuration to "
								+ path);
				rte.initCause(e);
				throw rte;
			} catch (Exception e) {
				final RuntimeException rte = new RuntimeException(
						"Unable to save encrypted configuration to " + path);
				rte.initCause(e);
				throw rte;
			}
			return;
		} catch (IOException e) {
			return;
		}
		
	}
	
	public Ciphers newCiphers(char [] password) throws GeneralSecurityException{
        final SecretKeySpec key = newKey(password);
        
		Cipher encipher;
		Cipher decipher;
		if (provider == null){
			encipher = Cipher.getInstance(transformation);
			decipher = Cipher.getInstance(transformation);
		}else{
			encipher = Cipher.getInstance(transformation, provider);
			decipher = Cipher.getInstance(transformation, provider);
		}
        byte [] iv = initEncipher(encipher, key);
        initDecipher(decipher, key, iv);
        Ciphers ciphers = new Ciphers();
        ciphers.decipher = decipher;
        ciphers.encipher = encipher;
        return ciphers;
	}
	
	private SecretKeySpec newKey(char [] password) throws GeneralSecurityException {
    	//--- transform password to a key ---
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password, salt.getBytes(), iterationCount, keyStrength);
        SecretKey tmp = factory.generateSecret(spec);
        final SecretKeySpec key = new SecretKeySpec(tmp.getEncoded(), "AES");
        return key;
	}
	
    private byte[] initEncipher(Cipher encipher, SecretKey key) throws GeneralSecurityException{
        encipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(new byte[16]));
        AlgorithmParameters params = encipher.getParameters();
        return params.getParameterSpec(IvParameterSpec.class).getIV();
    }
    
    private void initDecipher(Cipher decipher, SecretKey key, byte [] iv) throws GeneralSecurityException{
        decipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
    }
	
//	public synchronized void saveConfig(Properties properties){
//		//TODO:
//	}
	
	
//	public Cipher newEnCipher(){
//		//TODO:
//		return null;
//	}
	public Cipher newDeCipher(){
		//TODO:
		return null;
	}
//	public Properties getProperties(){
//		Properties res = new Properties();
//		res.setProperty(PROPERTY_PLAIN_BLOCK_SIZE, "");
//		//TODO:
//		return null;
//	}
	//TODO:
	private class Ciphers{
		private Cipher encipher;
		private Cipher decipher;
		public Cipher getEncipher() {
			return encipher;
		}
		public Cipher getDecipher() {
			return decipher;
		}
	}
}
