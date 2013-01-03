package com.bm.nio.file.channel;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

public class SeekableByteChannelTestFixed implements SeekableByteChannel {
		
		@Override
		public boolean isOpen() {
			return true;
		}
		
		@Override
		public void close() throws IOException {
			
		}
		
		ByteBuffer src;
		@Override
		public int write(ByteBuffer src) throws IOException {
//			byte [] buf = new byte [src.remaining()];
//			src.get(buf);
//			this.src = ByteBuffer.wrap(buf);
//			return buf.length;
			int len = src.remaining();
			src.position(src.capacity());
			return len;
		}
		
		public int setSrc(ByteBuffer src){
			byte [] buf = new byte [src.remaining()];
			src.get(buf);
			this.src = ByteBuffer.wrap(buf);
			return buf.length;
		}
		
		@Override
		public SeekableByteChannel truncate(long size) throws IOException {
			return null;
		}
		
		@Override
		public long size() throws IOException {
			return mSize;
		}
		
		@Override
		public int read(ByteBuffer dst) throws IOException {
			src.position(0);
			int len = Math.min(dst.remaining(), src.remaining());
			while (src.hasRemaining()&&dst.hasRemaining())
		         dst.put(src.get()); 
			return len;
		}
		
		long mSize = 0;
		public void setSize(long newSize){
			mSize = newSize;
		}
		
		long mPosition = 0;
		@Override
		public SeekableByteChannel position(long newPosition) throws IOException {
			mPosition = newPosition;
			return null;
		}
		
		@Override
		public long position() throws IOException {
			return mPosition;
		}

}
