package com.bm.nio.file.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;

import com.bm.nio.file.FileSystemProviderEncrypted;
import com.sun.nio.zipfs.ZipFileSystem;

public class SeekableByteChannelTestList implements SeekableByteChannel {
		
		@Override
		public synchronized boolean isOpen() {
			return true;
		}
		
		@Override
		public synchronized void close() throws IOException {
			
		}
		
		byte [] mBufRaw = new byte [10000];
		ByteBuffer mBuf = ByteBuffer.wrap(mBufRaw);
		@Override
		public synchronized int write(ByteBuffer src) throws IOException {
			int len = src.remaining();
			mBuf.put(src);
//			byte [] buf = new byte [src.remaining()];
//			src.get(buf);
//			this.mBuf = ByteBuffer.wrap(buf);//src.duplicate();
//			return buf.length;
			if (mBuf.position() > mSize)
				mSize = mBuf.position();
			return len;
		}
		
		@Override
		public synchronized SeekableByteChannel truncate(long size) throws IOException {
			mSize = size;
			mBuf.position((int)size);
			//mPosition = 0;
			return this;
		}
		
		@Override
		public synchronized long size() throws IOException {
			return mSize;
		}
		
		@Override
		public synchronized int read(ByteBuffer dst) throws IOException {
			//mBuf.position(0);
			//int len = dst.remaining();
			long left = mSize - mBuf.position();
			if (left <= 0)
				return -1;
			int len = (int)Math.min(dst.remaining(), left);
			dst.put(mBufRaw, mBuf.position(), len);
			mBuf.position(mBuf.position() + len);
//			int len = (int)Math.min(dst.remaining(), mSize);
//			dst.put(mBufRaw, mBuf.position(), len);
//			mBuf.position(mBuf.position() + len);
			return len;
		}
		
		long mSize = 0;
		
		//long mPosition = 0;
		@Override
		public synchronized SeekableByteChannel position(long newPosition) throws IOException {
//			mPosition = newPosition;
//			mBuf.position((int)mPosition);
			mBuf.position((int)newPosition);
			return this;
		}
		
		@Override
		public synchronized long position() throws IOException {
			return mBuf.position();
		}

}
