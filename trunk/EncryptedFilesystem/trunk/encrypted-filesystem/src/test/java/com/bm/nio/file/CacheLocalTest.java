package com.bm.nio.file;

import junit.framework.Assert;

import org.junit.Test;

import com.bm.nio.utils.CacheLocal;

public class CacheLocalTest {

	
	@Test
	public void testGetCachedObject(){
		//TODO:
		CacheLocal<String> cl = new CacheLocal<String>(){
			@Override
			protected String initialValue() {
				return "Test";
			}
		};
		Assert.assertTrue(true);
	}
	
}
