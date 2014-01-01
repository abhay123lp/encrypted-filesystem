package com.bm.nio.file;

import junit.framework.Assert;

import org.junit.Test;

import com.bm.nio.utils.CacheLocal;

public class CacheLocalTest {

	
	class CacheUser implements Runnable {
		CacheLocal<String> cl;
		public CacheUser(){
		}
		@Override
		public void run() {
			cl = new CacheLocal<String>(){
				@Override
				protected String initialValue() {
					return "Test";
				}
			};	
		}
	}
	
	@Test
	public void testGetCachedObject(){
		//TODO:
		CacheUser cu1 = new CacheUser();
		CacheUser cu2 = new CacheUser();
		
		
		Assert.assertTrue(true);
	}
	
}
