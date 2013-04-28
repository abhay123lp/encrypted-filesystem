package com.bm.nio.file;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper for watch service of underlying filesystem.
 * @author Mike
 *
 */
public class WatchServiceEncrypted implements WatchService {

	private final WatchService mWatcher;
	private final FileSystemEncrypted mFs;
	WatchServiceEncrypted (WatchService watcher, FileSystemEncrypted fileSystem){
		mWatcher = watcher;
		mFs = fileSystem;
	}
	
	public WatchService getUnderWatcher(){
		return mWatcher;
	}
	
	@Override
	public void close() throws IOException {
		// TOTEST
		mWatcher.close();		
	}

	@Override
	public WatchKey poll() {
		// TOTEST
		return toWatchKeyEncrypted(mWatcher.poll());
	}

	@Override
	public WatchKey poll(long timeout, TimeUnit unit)
			throws InterruptedException {
		// TOTEST
		return toWatchKeyEncrypted(mWatcher.poll(timeout, unit));
	}

	//+ Done
	@Override
	public WatchKey take() throws InterruptedException {
		return toWatchKeyEncrypted(mWatcher.take());
	}
	
	protected WatchKeyEncrypted toWatchKeyEncrypted(WatchKey key){
		final WatchKey underKey = key;
		final Watchable underWatchable = underKey.watchable();
		Watchable watchable;
		if (underWatchable instanceof PathEncrypted)
			watchable = underWatchable;
		else if (underWatchable instanceof Path)
			watchable = mFs.toEncrypted((Path)underWatchable);
		else watchable = underWatchable;
		return new WatchKeyEncrypted(underKey, this, watchable);
	}

	//+ Done
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof WatchServiceEncrypted))
			return false;
		return this.mWatcher.equals(((WatchServiceEncrypted)obj).mWatcher);
	}
	
	//+ Done
	@Override
	public int hashCode() {
		return mWatcher.hashCode();
	}
	
	
	// ===
	private static class WatchKeyEncrypted implements WatchKey {

		private WatchKey mKey; 
		private WatchServiceEncrypted mWs;
		private Watchable mWatchable;
		private WatchKeyEncrypted(WatchKey key, WatchServiceEncrypted watcher, Watchable watchable){
			mKey = key;
			mWs = watcher;
			mWatchable = watchable;
		}
		
		@Override
		public boolean isValid() {
			//TOTEST
			return mKey.isValid();
		}

		//+ Done
		@Override
		public List<WatchEvent<?>> pollEvents() {
			ArrayList<WatchEvent<?>> res = new ArrayList<WatchEvent<?>>();
			for (WatchEvent<?> event : mKey.pollEvents()){
				if (Path.class.isAssignableFrom(event.kind().type())){
					final WatchEvent<Path> lEvent = (WatchEvent<Path>)event;
					res.add(new WatchEvent<Path>() {
						@Override
						public Kind<Path> kind() {
							return lEvent.kind();
						};

						@Override
						public int count() {
							return lEvent.count();
						}

						@Override
						public PathEncrypted context() {
							return mWs.mFs.toEncrypted(lEvent.context());
						}
					});
				}
				else
					res.add(event);//non path events - pass without changing
			}
			//WatchEvent --> kind, count, context
			return res;
		}

		@Override
		public boolean reset() {
			// TOTEST
			return mKey.reset();
		}

		@Override
		public void cancel() {
			// TOTEST
			mKey.cancel();
		}

		//+ Done
		@Override
		public Watchable watchable() {
			return mWatchable;
//			Watchable w = mKey.watchable();
//			if (w instanceof PathEncrypted)
//				return w;
//			else if (w instanceof Path)
//				return mWs.mFs.toEncrypted((Path)w);
//			else
//				return w;
		}
		
		//+ Done
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof WatchKeyEncrypted))
				return false;
			return this.mKey.equals(((WatchKeyEncrypted)obj).mKey);
		}
		
		//+ Done
		@Override
		public int hashCode() {
			return mKey.hashCode();
		}
		
	}
	// ===

}
