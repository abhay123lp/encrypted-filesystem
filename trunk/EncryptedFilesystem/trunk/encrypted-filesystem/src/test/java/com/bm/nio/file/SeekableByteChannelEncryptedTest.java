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
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.zip.ZipFile;

import javax.xml.bind.DatatypeConverter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.bm.nio.channels.SeekableByteChannelEncrypted;
import com.bm.nio.file.channel.SeekableByteChannelTestFixed;
import com.bm.nio.file.channel.SeekableByteChannelTestList;
import com.bm.nio.file.channel.SeekableByteChannelTestListUnsupported;

public class SeekableByteChannelEncryptedTest {
	
	private SeekableByteChannelTestFixed getUnderChannelFixed(){
		return new SeekableByteChannelTestFixed();
	}
	
	private SeekableByteChannelTestList getUnderChannelList(){
		return new SeekableByteChannelTestList();
	}
	
	private SeekableByteChannelTestListUnsupported getUnderChannelListUnsupported(){
		return new SeekableByteChannelTestListUnsupported();
	}
	
	private SeekableByteChannelEncrypted getSeekableByteChannelEncrypted(
			SeekableByteChannel underChannel, String transformation, Integer blockSize) throws Exception {
		HashMap<String, Object> props = new HashMap<String, Object>();
		props.put(SeekableByteChannelEncrypted.EncryptedConfig.PLAIN_BLOCK_SIZE, blockSize);
		props.put(SeekableByteChannelEncrypted.EncryptedConfig.TRANSFORMATION, transformation);
		return new SeekableByteChannelEncrypted(underChannel, props);
//		SeekableByteChannelEncrypted ce = SeekableByteChannelEncrypted.getChannel(underChannel);
//		if (ce != null && ce.isOpen())
//			ce.close();
//		return SeekableByteChannelEncrypted.newChannel(underChannel, props);
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
	public void seekableByteChannelReadWriteTruncateTest() throws Exception{
		String [] transformations = new String [] {"AES/CBC/PKCS5Padding", "AES/CFB/NoPadding"};
		for (String transformation : transformations){
			//case1: 
			SeekableByteChannelTestList underChannel = getUnderChannelList();
			SeekableByteChannelEncrypted ce;
			String txt = "12345678abccefghij";
			String txtNew = "12345678abcdefghij";
			//
			//=== with padding, 8 bytes dec block ===
			ce = getSeekableByteChannelEncrypted(underChannel, transformation, 8);
			ce.write(ByteBuffer.wrap(txt.getBytes()));
			ce.position(11);//correcting 5 to 4
			ce.write(ByteBuffer.wrap("d".getBytes()));
			//ce.close();
			//ce.flush();
			ce.position(0);
			byte [] b = new byte [100];
			int len = ce.read(ByteBuffer.wrap(b));
			Assert.assertEquals(txtNew, new String(b, 0, len));
			
			//truncate before len --> 0
			txtNew = txtNew.substring(8);
			while ((txtNew = txtNew.substring(0, txtNew.length() - 1)).length() > 0){
				ce.truncate(ce.size() - 1);
				ce.position(8);
				len = ce.read(ByteBuffer.wrap(b));
				Assert.assertEquals(txtNew, new String(b, 0, len));
			}
		}
		
		//check truncate
//		ce.truncate(17);
//		ce.position(8);
//		len = ce.read(ByteBuffer.wrap(b));
//		Assert.assertEquals("123456789", new String(b, 0, len));
//		
//		ce.truncate(16);
//		ce.position(8);
//		len = ce.read(ByteBuffer.wrap(b));
//		Assert.assertEquals("12345678", new String(b, 0, len));
	}
	
	@Test
	public void seekableByteChannelUnsupportedTest() throws Exception{
		SeekableByteChannelTestListUnsupported underChannel;
		SeekableByteChannelEncrypted ce;
		//
		//=== with padding, 8 bytes dec block ===
		underChannel = getUnderChannelListUnsupported();
		ce = getSeekableByteChannelEncrypted(underChannel, "AES/CBC/PKCS5Padding", 8);
		
		class UnsupportedTest{
			public void testWrite(SeekableByteChannelEncrypted ce, SeekableByteChannelTestListUnsupported underChannel) throws Exception {
				int i = 0;
				//WRITE TEST
				ByteBuffer dec = ByteBuffer.wrap("12345678abcdefghi".getBytes());
				ByteBuffer enc = ByteBuffer.wrap(new byte [100]);
				ByteBuffer encFirst = null;
				underChannel.first();
				underChannel.reset();
				i = 0;
				while (underChannel.next()){
					i ++;
					//init channels
					underChannel.setSupported();
					underChannel.position(0);
					ce.position(0);//if not set then remaining of previous write will change the result
					//
					underChannel.setSupported(SeekableByteChannelTestListUnsupported.WRITE);
					dec.position(0);
					ce.write(dec);//write unsupported
					//enable everything to read
//					underChannel.setSupported();
//					underChannel.position(0);
//					ce.position(0);//if not set then remaining of previous write will change the result
//					underChannel.read(enc);
					underChannel.readNoChange(enc);//safe read without changing channel's state
//					underChannel.position(0);
					enc.position(0);
					if (i == 1)
						encFirst = SeekableByteChannelEncryptedTest.clone(enc);
					Assert.assertEquals(encFirst, enc);
					underChannel.reset();
				}
			}
			
			public void testRead(SeekableByteChannelEncrypted ce, SeekableByteChannelTestListUnsupported underChannel) throws Exception {
				int i = 0;
				ByteBuffer dec = ByteBuffer.wrap("12345678abcdefghi".getBytes());
				//ByteBuffer enc = ByteBuffer.wrap(new byte [100]);
				ByteBuffer decTmp = ByteBuffer.wrap(new byte [100]);
				underChannel.first();
				underChannel.reset();
				i = 0;
				int len = 0;
				while (underChannel.next()){
					i ++;
					underChannel.setSupported();
					ce.position(0);
					underChannel.reset();
					underChannel.setSupported(SeekableByteChannelTestListUnsupported.READ);
					//underChannel.setUnsupported(SeekableByteChannelTestListUnsupported.POSITIONSET);
					decTmp.position(0);
					len = ce.read(decTmp);
					decTmp.position(0);
					byte [] decResRaw = new byte [len];
					ByteBuffer decRes = ByteBuffer.wrap(decResRaw);
					decTmp.get(decResRaw, 0, len);
					Assert.assertEquals(decRes, dec);
					//underChannel.reset();
				}
			}
		}
		
		UnsupportedTest ut = new UnsupportedTest();
		ut.testWrite(ce, underChannel);
		ut.testRead(ce, underChannel);
		//only read, from the beginning
		underChannel.setSupported();
		underChannel.position(0);
		ce = getSeekableByteChannelEncrypted(underChannel, "AES/CBC/PKCS5Padding", 8);
		ut.testRead(ce, underChannel);
		//=== no padding ===
		underChannel = getUnderChannelListUnsupported();
		ce = getSeekableByteChannelEncrypted(underChannel, "AES/CFB/NoPadding", 8);
		ut.testWrite(ce, underChannel);
		ut.testRead(ce, underChannel);
		//only read, from the beginning
		underChannel.setSupported();
		underChannel.position(0);
		ce = getSeekableByteChannelEncrypted(underChannel, "AES/CFB/NoPadding", 8);
		ut.testRead(ce, underChannel);
	}
	
	public static ByteBuffer clone(ByteBuffer original) {
	       ByteBuffer clone = ByteBuffer.allocate(original.capacity());
	       original.rewind();//copy from the beginning
	       clone.put(original);
	       original.rewind();
	       clone.flip();
	       return clone;
	}	
	
	@Test
	public void seekableByteChannelParallelTest() throws Exception{
		//TODO:
		String [] transformations = new String [] {"AES/CBC/PKCS5Padding", "AES/CFB/NoPadding"};
		for (final String transformation : transformations){
			//init
			final SeekableByteChannelTestList underChannel = getUnderChannelList();
			//SeekableByteChannelEncrypted ce;
			String txt = "12345678abccefghij";
			final ByteBuffer data = ByteBuffer.wrap(txt.getBytes());
			//
			//=== with padding, 8 bytes dec block ===
			final int THREADS_CNT = 13;
			final CyclicBarrier starter = new CyclicBarrier(THREADS_CNT);
			final CyclicBarrier stopper = new CyclicBarrier(THREADS_CNT + 1);
			final SeekableByteChannelEncrypted ce = getSeekableByteChannelEncrypted(underChannel, transformation, 8);
			for (int i = 0; i < THREADS_CNT; i ++){
				new Thread(){
					@Override
					public void run() {
						try {
							System.out.println("waiting " + this.getId());
							starter.await();
							// do work
							//SeekableByteChannelEncrypted ce = getSeekableByteChannelEncrypted(underChannel, transformation, 8);
							byte b;
							while (true){
							synchronized (data) {
								if (data.position() == data.capacity())
									break;
								b = data.get();
							}
								ce.write(ByteBuffer.wrap(new byte [] {b}));
								Thread.yield();
							}
							//ce.close();
							//
							System.out.println("finished " + this.getId());
							stopper.await();
						} catch (Exception e) {
							e.printStackTrace();
						}
					};
				}.start();
			}
			
			stopper.await();
			System.out.println("finished All");
			SeekableByteChannelEncrypted ce1 = getSeekableByteChannelEncrypted(underChannel, transformation, 8);
			ByteBuffer bb = ByteBuffer.allocate(data.capacity());
			ce1.read(bb);
			System.out.println(new String(bb.array()));
			
			/*
			
			ce = getSeekableByteChannelEncrypted(underChannel, transformation, 8);
			ce.write(ByteBuffer.wrap(txt.getBytes()));
			ce.position(11);//correcting 5 to 4
			ce.write(ByteBuffer.wrap("d".getBytes()));
			//ce.close();
			//ce.flush();
			ce.position(0);
			byte [] b = new byte [100];
			int len = ce.read(ByteBuffer.wrap(b));
			Assert.assertEquals(txtNew, new String(b, 0, len));
			
			//truncate before len --> 0
			txtNew = txtNew.substring(8);
			while ((txtNew = txtNew.substring(0, txtNew.length() - 1)).length() > 0){
				ce.truncate(ce.size() - 1);
				ce.position(8);
				len = ce.read(ByteBuffer.wrap(b));
				Assert.assertEquals(txtNew, new String(b, 0, len));
			}*/
		}		
	}
	
//	@Test
//	public void seekableByteChannelTest() throws Exception{
//		String text = "12345678901234567890";
//		HashMap<String, Object> props = new HashMap<String, Object>();
//		props.put(OutputStreamCrypto.PASSWORD, "pwd".toCharArray());
//		ByteOutputStream bo = new ByteOutputStream();
//		
//		
//		SeekableByteChannelEncrypted ce = getSeekableByteChannelEncrypted(getUnderChannelFixed(), "AES/CFB/NoPadding", 8);//new SeekableByteChannelEncrypted(null);
//		Assert.assertEquals(text, new String(ce.decryptBlock(ce.encryptBlock(text.getBytes()))));
//	}
}
