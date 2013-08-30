package com.bm.nio.utils;

public class CacheLocal <T> extends ThreadLocal<T> {

	@Override
	public T get() {
		// additionally, when local variable for thread does not exist
		// existing can be used from another thread, that already finished
		// stay with this implementation to not overcomplicate
		return super.get();
	}
	
}
