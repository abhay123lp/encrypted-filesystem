package com.bm.nio.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.Cipher;

/**
 * class holding encrypted configuration
 * @author Mike
 *
 */
public class ConfigEncrypted {
	
	@Target(ElementType.FIELD)
	private @interface PropertyEncrypted{
		String value();
	}
	
	private ConfigEncrypted(){};
	public static ConfigEncrypted newConfig(){
		return new ConfigEncrypted();
	}
	
    public static final String PROPERTY_PLAIN_BLOCK_SIZE = "block.size";
    public static final String PROPERTY_PASSWORD = "password";
    public static final String PROPERTY_TRANSFORMATION = "transformation";
    //new
	public static final String PROPERTY_SALT = "salt";
	public static final String PROPERTY_KEY_STRENGTH = "keystrength";
	public static final String PROPERTY_ITERATION_COUNT = "iterationcount";
	public static final String PROPERTY_VERSION = "version";
	
	// properties

	@PropertyEncrypted(PROPERTY_TRANSFORMATION)
	private String transformation;
	
	public static ConfigEncrypted loadConfig(Path path){
		//TOTEST
		ConfigEncrypted res = new ConfigEncrypted();
		Properties p = new Properties();
		try (InputStream is = Files.newInputStream(path, StandardOpenOption.READ);){
			p.load(is);
			return loadConfig(p);
		} catch (IOException e) {
			return res;
		}
	}
	
	public static ConfigEncrypted loadConfig(Properties properties){
		//TODO:
		
		return null;
	}
	
	public synchronized void saveConfig(Path path){
		//TOTEST
		Properties p = new Properties();
		try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.WRITE);){
			saveConfig(p);
			p.store(os, null);
			return;
		} catch (IOException e) {
			return;
		}
		
	}
	
	public synchronized void saveConfig(Properties properties){
		//TODO:
	}
	
	
	public Cipher newEnCipher(){
		//TODO:
		return null;
	}
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
}
