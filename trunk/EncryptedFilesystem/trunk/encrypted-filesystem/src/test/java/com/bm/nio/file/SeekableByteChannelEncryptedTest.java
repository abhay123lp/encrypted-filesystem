package com.bm.nio.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

import javax.xml.bind.DatatypeConverter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.bm.nio.file.stream.OutputStreamCrypto;
import com.bm.nio.file.utils.Crypter;
import com.sun.nio.zipfs.ZipFileSystem;
import com.sun.nio.zipfs.ZipFileSystemProvider;
import com.sun.nio.zipfs.ZipPath;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

public class SeekableByteChannelEncryptedTest {

	private static class SeekableByteChannelTest implements SeekableByteChannel{
		
		@Override
		public boolean isOpen() {
			return true;
		}
		
		@Override
		public void close() throws IOException {
			
		}
		
		@Override
		public int write(ByteBuffer src) throws IOException {
			return 0;
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
			return 0;
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
	
	private SeekableByteChannelTest getUnderChannel(){
		return new SeekableByteChannelTest();
	}
	
	private SeekableByteChannelEncrypted getSeekableByteChannelEncrypted(
			SeekableByteChannel underChannel, String transformation, Integer blockSize) throws Exception {
		HashMap<String, Object> props = new HashMap<String, Object>();
		props.put(OutputStreamCrypto.BLOCK_SIZE, blockSize);
		props.put(SeekableByteChannelEncrypted.TRANSFORMATION, transformation);
		return new SeekableByteChannelEncrypted(underChannel, props);
	}
	
	@Test
	public void sizeDecryptedTest() throws Exception {
		SeekableByteChannelTest underChannel = getUnderChannel();
		//
		long size = Integer.MAX_VALUE;
		size = size * 9 + 150;
		underChannel.setSize(size);
		//=== with padding ===
		SeekableByteChannelEncrypted ce = getSeekableByteChannelEncrypted(underChannel, "AES/CBC/PKCS5Padding", 8);
		Assert.assertEquals(4831838253L, ce.size());
		//=== no padding ===
		ce = getSeekableByteChannelEncrypted(underChannel, "AES/CFB/NoPadding", 8);
		//ce = new SeekableByteChannelEncrypted(underChannel, props);
		Assert.assertEquals(underChannel.size(), ce.size());
	}
	
	@Test
	public void positionDecryptedTest() throws Exception {
		SeekableByteChannelTest underChannel = getUnderChannel();
		//
		long size = Integer.MAX_VALUE;
		size = size * 9 + 150;
		underChannel.setSize(size);//size of encrypted data = 4 * decrypted size, for test with padding
		//=== with padding ===
		SeekableByteChannelEncrypted ce = getSeekableByteChannelEncrypted(underChannel, "AES/CBC/PKCS5Padding", 8);
		//position above bounds
		ce.position(size * 2);
		Assert.assertEquals((size/32)*8, ce.position());//32 - encrypted block, 8 - plain block; size/32 - block num
		Assert.assertEquals((size/32)*32, ce.positionEncrypted());
		//position within bounds
		long posTmp = ce.position() - 150;//position in plain text measurement
		ce.position(posTmp);
		Assert.assertEquals(posTmp, ce.position());
		Assert.assertEquals((posTmp/8)*32, ce.positionEncrypted());
		//=== no padding ===
		ce = getSeekableByteChannelEncrypted(underChannel, "AES/CFB/NoPadding", 8);
		ce.position(size * 2);//position above bounds
		Assert.assertEquals((size/8)*8, ce.position());//8 - encrypted block, 8 - plain block; size/8 - block num
		Assert.assertEquals((size/8)*8, ce.positionEncrypted());//size/32 - block num
		//position within bounds
		posTmp = ce.position() - 150;
		ce.position(posTmp);
		Assert.assertEquals(posTmp, ce.position());
		Assert.assertEquals((posTmp/8)*8, ce.positionEncrypted());
	}
	
	@Test
	public void seekableByteChannelTest() throws Exception{
		String text = "12345678901234567890";
		HashMap<String, Object> props = new HashMap<String, Object>();
		//props.put(OutputStreamCrypto.BLOCK_SIZE, new Integer(8));
		props.put(OutputStreamCrypto.PASSWORD, "pwd".toCharArray());
		ByteOutputStream bo = new ByteOutputStream();
		
		
		SeekableByteChannelEncrypted ce = new SeekableByteChannelEncrypted(null);
		//System.out.println(DatatypeConverter.printHexBinary(ce.encryptBlock("123456789012345".getBytes())));
		//System.out.println(new String(ce.decryptBlock(ce.encryptBlock(text.getBytes()))));
		Assert.assertEquals(text, new String(ce.decryptBlock(ce.encryptBlock(text.getBytes()))));
	}
}
