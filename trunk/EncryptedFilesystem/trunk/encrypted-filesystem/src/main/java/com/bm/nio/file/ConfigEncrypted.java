package com.bm.nio.file;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.Provider.Service;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.RC2ParameterSpec;
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
	//uses constant iv
	private byte [] iv;
	
	private void loadConfigInternal(InputStream is) throws Exception {
			getSerializer().read(this, is);
	}
	
	
	public static ConfigEncrypted newConfig(ConfigEncrypted config) {
		ConfigEncrypted res = new ConfigEncrypted();
		try {
			res.blockSize = config.blockSize;
			res.iterationCount = config.iterationCount;
			res.keyStrength = config.keyStrength;
			res.macFiles = config.macFiles;
			res.macNames = config.macNames;
			res.provider = config.provider;
			res.salt = config.salt;
			res.transformation = config.transformation;
			res.version = config.version;
		} catch (Exception e) {
			final RuntimeException rte = new RuntimeException(
					"Unable to copy values", e);
			throw rte;
		}
		return res;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + blockSize;
		result = prime * result + iterationCount;
		result = prime * result + keyStrength;
		result = prime * result + (macFiles ? 1231 : 1237);
		result = prime * result + (macNames ? 1231 : 1237);
		result = prime * result
				+ ((provider == null) ? 0 : provider.hashCode());
		result = prime * result + ((salt == null) ? 0 : salt.hashCode());
		result = prime * result
				+ ((transformation == null) ? 0 : transformation.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConfigEncrypted other = (ConfigEncrypted) obj;
		if (blockSize != other.blockSize)
			return false;
		if (iterationCount != other.iterationCount)
			return false;
		if (keyStrength != other.keyStrength)
			return false;
		if (macFiles != other.macFiles)
			return false;
		if (macNames != other.macNames)
			return false;
		if (provider == null) {
			if (other.provider != null)
				return false;
		} else if (!provider.equals(other.provider))
			return false;
		if (salt == null) {
			if (other.salt != null)
				return false;
		} else if (!salt.equals(other.salt))
			return false;
		if (transformation == null) {
			if (other.transformation != null)
				return false;
		} else if (!transformation.equals(other.transformation))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
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
        final SecretKeySpec key = newSecretKeySpec(password);
        return newCiphers(key);
	}
	
	public Ciphers newCiphers(SecretKeySpec key) throws GeneralSecurityException{
		Cipher encipher;
		Cipher decipher;
		if (provider == null){
			encipher = Cipher.getInstance(transformation);
			decipher = Cipher.getInstance(transformation);
		}else{
			encipher = Cipher.getInstance(transformation, provider);
			decipher = Cipher.getInstance(transformation, provider);
		}
		
		initCiphers(encipher, decipher, key);
		
//		AlgorithmParameters params = initEncipher(encipher, key);
//        initDecipher(decipher, key, params);
        Ciphers ciphers = new Ciphers();
        ciphers.decipher = decipher;
        ciphers.encipher = encipher;
        return ciphers;
	}
	
	public SecretKeySpec newSecretKeySpec(char [] password) throws GeneralSecurityException {
    	//--- transform password to a key ---
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password, salt.getBytes(), iterationCount, keyStrength);
        SecretKey tmp = factory.generateSecret(spec);
//        final SecretKeySpec key = new SecretKeySpec(tmp.getEncoded(), "AES");// TODO: get appropriate key
        final SecretKeySpec key = new SecretKeySpec(tmp.getEncoded(), transformation.substring(0, transformation.indexOf("/")));// TODO: get appropriate key
        return key;
	}
	
    private void initCiphers(Cipher encipher, Cipher decipher, SecretKey key) throws GeneralSecurityException{
   		encipher.init(Cipher.ENCRYPT_MODE, key);
        AlgorithmParameters params = encipher.getParameters();
    	if (params == null)// does not use IV
    		decipher.init(Cipher.DECRYPT_MODE, key);
    	else{
    		// (pt. 20)
    		// 1. set constant iv, as soon as it can't be loaded.
    		// 2. without specifying iv channel won't be able 
    		//    to decrypt encrypted data by the channel with the same config (algorithm/password)
    		try {
                IvParameterSpec ivGenerated = params.getParameterSpec(IvParameterSpec.class);
                iv = new byte [ivGenerated.getIV().length];
                encipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
                decipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
                return;
			} catch (InvalidParameterSpecException e) {
			}
    		decipher.init(Cipher.DECRYPT_MODE, key, params);
    	}
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
