package com.bm.nio.utils;

public class CacheLocal <T> extends ThreadLocal<T> {

//	public interface CachingObjectCreator <T> {
//		public T create();
//	}
//	
//	private CachingObjectCreator<T> cachingObjectCreator;
//	
//	public CacheLocal(CachingObjectCreator<T> cachingObjectCreator){
//		this.cachingObjectCreator = cachingObjectCreator;
//	}
//	
//	private static class ThreadLocalCache<T> extends ThreadLocal<T>{
//		
//	}
//	
//	private ThreadLocalCache<T> cache = new ThreadLocalCache<T>();
//	
//	public T getCachedObject(){
//		return cachingObjectCreator.create();
//	}
	
	@Override
	public T get() {
		//TODO:
		return initialValue();
	}
	
}
