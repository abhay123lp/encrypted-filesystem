package com.bm.nio.file.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;

import com.bm.nio.file.FileSystemProviderEncrypted;
import com.sun.nio.zipfs.ZipFileSystem;

public class SeekableByteChannelTestListUnsupported extends SeekableByteChannelTestList {
		
	
		public static final int WRITE = 		1 << 0;
		public static final int TRUNCATE = 	1 << 1;
		public static final int SIZE = 		1 << 2;
		public static final int READ = 		1 << 3;
		public static final int POSITIONSET = 	1 << 4;
		public static final int POSITIONGET = 	1 << 5;
		public static final int MASK = WRITE | TRUNCATE | SIZE | READ | POSITIONSET | POSITIONGET;
		
		volatile int mMode = 0;//all unsupported by default
		volatile int mFlags = 0;
		volatile int mFlagsMask = 0;
		/**
		 * 0 - unsupported, 1 - supported
		 * @param mode
		 */
		public void setMode(int mode){
			mMode = mode;
		}
		
		public void first(){
			mMode = 0;
		}
		
		public boolean next(){
			mMode ++;
			if (mMode > MASK)
				return false;
			else
				return true;
		}
		
		/**
		 * @param flags - set supported operations
		 */
		public void setSupported(int flags){
			mFlagsMask |= flags;
			mFlags |= flags;//set supported
		}
		
		public void setSupported(){
			setSupported(MASK);
		}
		
		public void setUnsupported(int flags){
			mFlagsMask |= flags;
			mFlags &= ~flags;//set supported
		}
		
		public void reset(int flags){
			mFlagsMask &= ~flags;
		}
		
		public void reset(){
			reset(MASK);
		}
		
		public boolean isSupported(int flags){
			return (((mMode & (~mFlagsMask)) | (mFlags & mFlagsMask)) & flags) != 0;
			
			//return (((mMode & mFlagsMask) | mFlags) & flags) == 0;
		}
		
		@Override
		public int write(ByteBuffer src) throws IOException {
			if (isSupported(WRITE))
				return super.write(src);
			throw new UnsupportedOperationException();
		}
		
		@Override
		public SeekableByteChannel truncate(long size) throws IOException {
			if (isSupported(TRUNCATE))
				return super.truncate(size);
			throw new UnsupportedOperationException();
		}
		
		@Override
		public long size() throws IOException {
			if (isSupported(SIZE))
				return super.size();
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int read(ByteBuffer dst) throws IOException {
			if (isSupported(READ))
				return super.read(dst);
			throw new UnsupportedOperationException();
		}
		
		public int readNoChange(ByteBuffer dst) throws IOException {
			//if (!isSupported(READ))
			//	throw new UnsupportedOperationException();
			long pos = super.position();
			super.position(0);
			int amt = super.read(dst);
			super.position(pos);
			return amt;
		}
		
		long mSize = 0;
		
		//long mPosition = 0;
		@Override
		public SeekableByteChannel position(long newPosition) throws IOException {
			if (isSupported(POSITIONSET))
				return super.position(newPosition);
			throw new UnsupportedOperationException();
		}
		
		@Override
		public long position() throws IOException {
			if (isSupported(POSITIONGET))
				return super.position();
			throw new UnsupportedOperationException();
		}

}
