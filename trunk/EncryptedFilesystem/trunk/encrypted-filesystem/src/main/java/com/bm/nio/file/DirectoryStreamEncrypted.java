package com.bm.nio.file;

import java.io.IOException;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Iterator;

public class DirectoryStreamEncrypted implements DirectoryStream<Path> {

	private DirectoryStream<Path> mUnderDirectoryStream;
	private PathEncrypted mPath;
	protected  DirectoryStreamEncrypted(PathEncrypted path, Filter<? super Path> filter) throws IOException {
		mUnderDirectoryStream = Files.newDirectoryStream(path.getUnderPath());
		//mUnderDirectoryStream = Files.newDirectoryStream(path.getFullUnderPath());
		mPath = path;
	}
	@Override
	public synchronized void close() throws IOException {
		mUnderDirectoryStream.close();
	}

	@Override
	public synchronized Iterator<Path> iterator() {
		return new DirectoryIteratorEncrypted(mUnderDirectoryStream.iterator());
		//return mUnderDirectoryStream.iterator();
	}


	private class DirectoryIteratorEncrypted implements Iterator<Path>{

		private Iterator<Path> mUnderIterator;
		private PathEncrypted next = null;
		private DirectoryIteratorEncrypted (Iterator<Path> underIterator) {
			mUnderIterator = underIterator;
		}
		
		@Override
		public synchronized boolean hasNext() {
			if (next != null){
				return true;
			}
			if (!mUnderIterator.hasNext()){
				return false;
			}
			
			// == main check ==
			// go next until find first encrypted path
			try {
				while(mUnderIterator.hasNext() && (next = this.nextInternal()) == null);
			} catch (Exception e) {
				return false;
			}
			
			return next==null?false:true;
		}

		private PathEncrypted nextInternal(){
			//DONE
			//consider iterating over encrypted paths only, skipping not encrypted paths
			//also, create PathEncrypted based on underlying path
			if (next != null){
				PathEncrypted p = next;
				next = null;
				return p;
			}
			
			Path underPath;
			PathEncrypted res = null;
			while(res == null)
			try {
				if ((underPath = mUnderIterator.next()) == null)
					return null;
				res = new PathEncrypted(mPath.getFileSystem(), underPath);
			} catch (InvalidPathException e) {
				res = null;
			}
			return res;
		}
		
		@Override
		public synchronized Path next() {
			return nextInternal();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

}
