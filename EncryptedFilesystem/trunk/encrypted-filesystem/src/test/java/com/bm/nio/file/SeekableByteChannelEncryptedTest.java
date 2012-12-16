package com.bm.nio.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
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

	@Test
	public void sizeDecryptedTest() throws Exception {
		HashMap<String, Object> props = new HashMap<String, Object>();
		props.put(OutputStreamCrypto.BLOCK_SIZE, new Integer(8));
		SeekableByteChannel underChannel = new SeekableByteChannel() {
			
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
				long big = Integer.MAX_VALUE;
				return big * 9 + 150;
			}
			
			@Override
			public int read(ByteBuffer dst) throws IOException {
				return 0;
			}
			
			@Override
			public SeekableByteChannel position(long newPosition) throws IOException {
				return null;
			}
			
			@Override
			public long position() throws IOException {
				return 0;
			}
		};
		props.put(SeekableByteChannelEncrypted.TRANSFORMATION, "AES/CBC/PKCS5Padding");
		SeekableByteChannelEncrypted ce = new SeekableByteChannelEncrypted(underChannel, props);
		Assert.assertEquals(4831838253L, ce.sizeDecrypted());
		//=== no padding ===
		props.put(SeekableByteChannelEncrypted.TRANSFORMATION, "AES/CFB/NoPadding");
		ce = new SeekableByteChannelEncrypted(underChannel, props);
		Assert.assertEquals(underChannel.size(), ce.sizeDecrypted());
	}
	
	@Test
	public void seekableByteChannelTest() throws Exception{
		String text = "12345678901234567890";
		HashMap<String, Object> props = new HashMap<String, Object>();
		//props.put(OutputStreamCrypto.BLOCK_SIZE, new Integer(8));
		props.put(OutputStreamCrypto.PASSWORD, "pwd".toCharArray());
		ByteOutputStream bo = new ByteOutputStream();
		
		
		SeekableByteChannelEncrypted ce = new SeekableByteChannelEncrypted(null);
		System.out.println(DatatypeConverter.printHexBinary(ce.encryptBlock("123456789012345".getBytes())));
		System.out.println(new String(ce.decryptBlock(ce.encryptBlock(text.getBytes()))));
	}
}
