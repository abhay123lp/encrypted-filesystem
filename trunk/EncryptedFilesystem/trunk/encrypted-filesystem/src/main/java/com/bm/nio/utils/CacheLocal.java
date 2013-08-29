package com.bm.nio.utils;

public class CacheLocal <T> {

	public interface CachingObjectCreator <T> {
		public T create();
	}
	
	private CachingObjectCreator<T> cachingObjectCreator;
	
	public void init(CachingObjectCreator<T> cachingObjectCreator){
		this.cachingObjectCreator = cachingObjectCreator;
	}
	
	public T getCachedObject(){
		//TODO:
		return cachingObjectCreator.create();
	}
	
}
