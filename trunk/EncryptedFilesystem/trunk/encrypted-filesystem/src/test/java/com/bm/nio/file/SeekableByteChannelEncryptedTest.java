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

import com.bm.nio.file.channel.SeekableByteChannelTestFixed;
import com.bm.nio.file.channel.SeekableByteChannelTestList;
import com.bm.nio.file.stream.OutputStreamCrypto;
import com.bm.nio.file.utils.Crypter;
import com.sun.nio.zipfs.ZipFileSystem;
import com.sun.nio.zipfs.ZipFileSystemProvider;
import com.sun.nio.zipfs.ZipPath;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

public class SeekableByteChannelEncryptedTest {
	
	private SeekableByteChannelTestFixed getUnderChannelFixed(){
		return new SeekableByteChannelTestFixed();
	}
	
	private SeekableByteChannelTestList getUnderChannelList(){
		return new SeekableByteChannelTestList();
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
		SeekableByteChannelTestFixed underChannel = getUnderChannelFixed();
		SeekableByteChannelEncrypted ce;
		//
		long size = Integer.MAX_VALUE;
		size = size * 9 + 169;//remainder 32
		underChannel.setSize(size);
		//plain {1, 2}
		underChannel.setSrc(ByteBuffer.wrap(new byte [] {-105, -7, -116, -10, 103, -9, -23, 61, -19, -90, -75, -63, -31, 31, 15, 48, 119, 94, -52, -83, -40, -128, -69, -117, 67, 81, 98, 123, 111, -42, 105, -68}));
		//=== with padding, 8 bytes dec block ===
		ce = getSeekableByteChannelEncrypted(underChannel, "AES/CBC/PKCS5Padding", 8);
		Assert.assertEquals(((size - 1)/32)*8 + 2, ce.size());
		//=== with padding, 128 bytes dec block ===
		ce = getSeekableByteChannelEncrypted(underChannel, "AES/CBC/PKCS5Padding", 128);
		Assert.assertEquals(((size - 1)/160)*128 + 2, ce.size());
		//=== no padding, 8 bytes dec block ===
		underChannel.setSize(size + 2);
		underChannel.setSrc(ByteBuffer.wrap(new byte [] {-77, 68}));//plain {1, 2}
		ce = getSeekableByteChannelEncrypted(underChannel, "AES/CFB/NoPadding", 8);
		Assert.assertEquals(underChannel.size(), ce.size());
		//=== no padding, 128 bytes dec block ===
		ce = getSeekableByteChannelEncrypted(underChannel, "AES/CFB/NoPadding", 128);
		Assert.assertEquals(underChannel.size(), ce.size());
	}
	
	@Test
	public void positionTest() throws Exception {
		SeekableByteChannelTestFixed underChannel = getUnderChannelFixed();
		//
		long size = Integer.MAX_VALUE;
		size = size * 9 + 169;
		underChannel.setSize(size);//size of encrypted data = 4 * decrypted size, for test with padding
		//underChannel.write(ByteBuffer.wrap(new byte [] {-105, -7, -116, -10, 103, -9, -23, 61, -19, -90, -75, -63, -31, 31, 15, 48, 119, 94, -52, -83, -40, -128, -69, -117, 67, 81, 98, 123, 111, -42, 105, -68}));
		underChannel.setSrc(ByteBuffer.wrap(new byte [] {-105, -7, -116, -10, 103, -9, -23, 61, -19, -90, -75, -63, -31, 31, 15, 48, 119, 94, -52, -83, -40, -128, -69, -117, 67, 81, 98, 123, 111, -42, 105, -68}));
		//=== with padding ===
		SeekableByteChannelEncrypted ce = getSeekableByteChannelEncrypted(underChannel, "AES/CBC/PKCS5Padding", 8);
		//position above bounds
		ce.position(size * 2);
		//Assert.assertEquals((size/32)*8, ce.position());//32 - encrypted block, 8 - plain block; size/32 - block num
		Assert.assertEquals(((size - 1)/32)*8 + 2, ce.position());//32 - encrypted block, 8 - plain block; size-1 - last index;size-1/32 - block num
		Assert.assertEquals(((size - 1)/32)*32, ce.positionEncrypted());//
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
	public void writeTest(){
		
	}
	
	@Test
	public void seekableByteChannelReadWriteTruncateTest() throws Exception{
		//case1: 
		SeekableByteChannelTestList underChannel = getUnderChannelList();
		SeekableByteChannelEncrypted ce;
		//
		//=== with padding, 8 bytes dec block ===
		ce = getSeekableByteChannelEncrypted(underChannel, "AES/CBC/PKCS5Padding", 8);
		ce.write(ByteBuffer.wrap("12345678123556789".getBytes()));
		ce.position(11);//correcting 5 to 4
		ce.write(ByteBuffer.wrap("4".getBytes()));
		//ce.close();
		//ce.flush();
		ce.position(0);
		byte [] b = new byte [100];
		int len = ce.read(ByteBuffer.wrap(b));
		Assert.assertEquals("12345678123456789", new String(b, 0, len));
		
		//check truncate
		ce.truncate(16);
		ce.position(8);
		len = ce.read(ByteBuffer.wrap(b));
		//System.out.println(new String(b, 0, len));
		Assert.assertEquals("12345678", new String(b, 0, len));
	}
	@Test
	public void seekableByteChannelTest() throws Exception{
		// get encrypted data for test
//		SeekableByteChannelEncrypted sbe = getSeekableByteChannelEncrypted(getUnderChannel(), "AES/CFB/NoPadding", 8);
//		byte [] buf = sbe.encryptBlock(new byte [] {1, 2});
		//
		
		String text = "12345678901234567890";
		HashMap<String, Object> props = new HashMap<String, Object>();
		//props.put(OutputStreamCrypto.BLOCK_SIZE, new Integer(8));
		props.put(OutputStreamCrypto.PASSWORD, "pwd".toCharArray());
		ByteOutputStream bo = new ByteOutputStream();
		
		
		SeekableByteChannelEncrypted ce = getSeekableByteChannelEncrypted(getUnderChannelFixed(), "AES/CFB/NoPadding", 8);//new SeekableByteChannelEncrypted(null);
		//System.out.println(DatatypeConverter.printHexBinary(ce.encryptBlock("123456789012345".getBytes())));
		//System.out.println(new String(ce.decryptBlock(ce.encryptBlock(text.getBytes()))));
		Assert.assertEquals(text, new String(ce.decryptBlock(ce.encryptBlock(text.getBytes()))));
	}
}
