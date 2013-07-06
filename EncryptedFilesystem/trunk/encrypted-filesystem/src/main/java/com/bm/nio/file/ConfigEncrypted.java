package com.bm.nio.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;

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
//ConfigEncrypted: mutable, thread safe
@Root(strict = true, name = "encryptionConfiguration")
public class ConfigEncrypted {
	
	private static Persister getSerializer(){
		//alternatively can use threadlocal variables
		return new Persister(new Format("<?xml version=\"1.0\" encoding= \"UTF-8\"?>"));
	}
	
	public ConfigEncrypted() {
	};
	
	@Attribute
	private volatile String version = "1.0";//TODO: take from meta-inf
	@Element
	private volatile int blockSize = 8192;
	@Element
	private volatile String transformation = "AES/CFB/NoPadding";//"AES/CBC/PKCS5Padding";
	@Element(required = false)
	private volatile String provider = null;
	@Element
	private volatile String salt = "12345678";
	@Element
	private volatile int keyStrength = 128;
	@Element
	private volatile int iterationCount = 1024;
	@Element
	private volatile boolean macNames = true;
	@Element
	private volatile boolean macFiles = true;
	
	private void loadConfigInternal(InputStream is) throws Exception {
			getSerializer().read(this, is);
	}
	
	public static ConfigEncrypted newConfig(ConfigEncrypted config) {
		ConfigEncrypted res = new ConfigEncrypted();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Persister serializer = getSerializer();
		try {
			serializer.write(config, out);
			serializer.read(res,
					new ByteArrayInputStream(out.toByteArray()));
		} catch (Exception e) {
			final RuntimeException rte = new RuntimeException(
					"Unable to copy values");
			rte.initCause(e);
			throw rte;
		}
		return res;
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj))
			return true;
		final Persister serializer = getSerializer();
		final ByteArrayOutputStream b1 = new ByteArrayOutputStream();
		final ByteArrayOutputStream b2 = new ByteArrayOutputStream();
		try {
			serializer.write(this, b1);
			serializer.write(obj, b2);
		} catch (Exception e) {
			return false;
		}
		final String s1 = b1.toString();
		final String s2 = b2.toString();
		return s1.equals(s2);
	}
	
	@Override
	public int hashCode() {
		final Persister serializer = getSerializer();
		final ByteArrayOutputStream b1 = new ByteArrayOutputStream();
		try {
			serializer.write(this, b1);
		} catch (Exception e) {
			return 0;
		}
		final String s1 = b1.toString();
		return s1.hashCode();
	}
	
	public void loadConfig(Path path) throws IOException {
		try (InputStream is = Files.newInputStream(path, StandardOpenOption.READ);){
			try {
				loadConfig(is);
			} catch (IOException e) {
				throw e;
			} catch (Exception e) {
				final RuntimeException rte = new RuntimeException(
						"Unable to load encrypted configuration from " + path);
				rte.initCause(e);
				throw rte;
			}
		}
	}

	public void loadConfig(InputStream is) throws IOException {
		try {
			loadConfigInternal(is);
		} catch (IOException e) {
			throw e;
		} catch (ValueRequiredException e) {
			final RuntimeException rte = new RuntimeException(
					"Required configuration parameter is missing. Unable to load from input stream");
			rte.initCause(e);
			throw rte;
		} catch (Exception e) {
			final RuntimeException rte = new RuntimeException(
					"Unable to load encrypted configuration from input stream");
			rte.initCause(e);
			throw rte;
		}
	}
	
	
	private void saveConfigInternal(OutputStream os) throws Exception {
			getSerializer().write(this, os);
	}
	
	public void saveConfig(OutputStream os) throws IOException {
		try {
			saveConfigInternal(os);
		} catch (IOException e) {
			throw e;
		} catch (ElementException e) {
			final RuntimeException rte = new RuntimeException(
					"Incorrect field value of configuration class. Unable to save encrypted configuration to output stream");
			rte.initCause(e);
			throw rte;
		} catch (Exception e) {
			final RuntimeException rte = new RuntimeException(
					"Unable to save encrypted configuration to output stream");
			rte.initCause(e);
			throw rte;
		}
	}
	public void saveConfig(Path path) throws IOException {
		try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.WRITE);){
			try {
				saveConfig(os);
			} catch (IOException e) {
				throw e;
			} catch (Exception e) {
				final RuntimeException rte = new RuntimeException(
						"Unable to save encrypted configuration to " + path);
				rte.initCause(e);
				throw rte;
			}
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
	
	public static class Ciphers{
		private Cipher encipher;
		private Cipher decipher;
		public Cipher getEncipher() {
			return encipher;
		}
		public Cipher getDecipher() {
			return decipher;
		}
	}

	// === staff ===	

	public String getVersion() {
		return version;
	}

	public int getBlockSize() {
		return blockSize;
	}

	public void setBlockSize(int blockSize) {
		this.blockSize = blockSize;
	}

	public String getTransformation() {
		return transformation;
	}

	public void setTransformation(String transformation) {
		this.transformation = transformation;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	public int getKeyStrength() {
		return keyStrength;
	}

	public void setKeyStrength(int keyStrength) {
		this.keyStrength = keyStrength;
	}

	public int getIterationCount() {
		return iterationCount;
	}

	public void setIterationCount(int iterationCount) {
		this.iterationCount = iterationCount;
	}

	public boolean isMacNames() {
		return macNames;
	}

	public void setMacNames(boolean macNames) {
		this.macNames = macNames;
	}

	public boolean isMacFiles() {
		return macFiles;
	}

	public void setMacFiles(boolean macFiles) {
		this.macFiles = macFiles;
	}
}
