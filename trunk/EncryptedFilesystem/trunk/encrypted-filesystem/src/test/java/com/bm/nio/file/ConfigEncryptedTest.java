package com.bm.nio.file;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bm.nio.file.utils.TestUtils;

public class ConfigEncryptedTest {

	final Path configPath = Paths.get(TestUtils.SANDBOX_PATH, "config.xml");
	
	@Before
	public void create() throws Exception {
		Files.createFile(configPath);		
	}
	
	@Test
	public void testSaveLoadEqualsNewConfig() throws Exception {
		ConfigEncrypted ce = new ConfigEncrypted();
		ConfigEncrypted ceTemplate = ConfigEncrypted.newConfig(ce);
		ce.saveConfig(configPath);
		ce.setBlockSize(ce.getBlockSize() + 1);
		ce.setIterationCount(ce.getIterationCount() + 1);
		ce.setKeyStrength(ce.getKeyStrength() + 1);
		ce.setMacFiles(!ce.isMacFiles());
		ce.setMacNames(!ce.isMacNames());
		//ce.setProvider(ce.getProvider() + 1);//provider is not required field
		ce.setTransformation(ce.getTransformation() + 1);
		ce.loadConfig(configPath);
		Assert.assertEquals(ceTemplate, ce);
		// check save data 
		final ByteArrayOutputStream b1 = new ByteArrayOutputStream();
		final ByteArrayOutputStream b2 = new ByteArrayOutputStream();
		ceTemplate.saveConfig(b1);
		ce.saveConfig(b2);
		Assert.assertTrue(Arrays.equals(b1.toByteArray(), b2.toByteArray()));
		//
		
		ce.setProvider(ce.getProvider() + 1);
		Assert.assertNotSame(ceTemplate, ce);
		
		// check save data
		b1.reset(); b2.reset();
		ceTemplate.saveConfig(b1);
		ce.saveConfig(b2);
		Assert.assertFalse(Arrays.equals(b1.toByteArray(), b2.toByteArray()));
		//
	}
	
	@Test
	public void testEqualsHashCode(){
		ConfigEncrypted ce = new ConfigEncrypted();
		ConfigEncrypted ceTemplate = ConfigEncrypted.newConfig(ce);
		ce.setBlockSize(1);
		ce.setProvider("p");
		ceTemplate.setBlockSize(1);
		ceTemplate.setProvider("p");
		Assert.assertEquals(ce, ceTemplate);
		Assert.assertEquals(ce.hashCode(), ceTemplate.hashCode());
	}
	
	@After
	public void clean() throws Exception {
		Files.delete(configPath);
	}
	
}
