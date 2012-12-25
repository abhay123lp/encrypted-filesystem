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
		public boolean isOpen() {
			return true;
		}
		
		@Override
		public void close() throws IOException {
			
		}
		
		byte [] mBufRaw = new byte [10000];
		ByteBuffer mBuf = ByteBuffer.wrap(mBufRaw);
		@Override
		public int write(ByteBuffer src) throws IOException {
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
		public SeekableByteChannel truncate(long size) throws IOException {
			mSize = 0;
			mBuf.position(0);
			//mPosition = 0;
			return this;
		}
		
		@Override
		public long size() throws IOException {
			return mSize;
		}
		
		@Override
		public int read(ByteBuffer dst) throws IOException {
			//mBuf.position(0);
			
			int len = dst.remaining();
			dst.put(mBufRaw, mBuf.position(), len);
			mBuf.position(mBuf.position() + len);
			return len;
		}
		
		long mSize = 0;
		
		//long mPosition = 0;
		@Override
		public SeekableByteChannel position(long newPosition) throws IOException {
//			mPosition = newPosition;
//			mBuf.position((int)mPosition);
			mBuf.position((int)newPosition);
			return this;
		}
		
		@Override
		public long position() throws IOException {
			return mBuf.position();
		}

}
