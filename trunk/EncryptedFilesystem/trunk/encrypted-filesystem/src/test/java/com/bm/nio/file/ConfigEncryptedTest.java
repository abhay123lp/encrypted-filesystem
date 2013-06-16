package com.bm.nio.file;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
		//ce.setProvider(ce.getProvider() + 1);//provider is not required
		ce.setTransformation(ce.getTransformation() + 1);
		ce.loadConfig(configPath);
		Assert.assertEquals(ceTemplate, ce);
		ce.setProvider(ce.getProvider() + 1);
		Assert.assertNotSame(ceTemplate, ce);
	}
	
	@After
	public void clean() throws Exception {
		Files.delete(configPath);
	}
	
}
