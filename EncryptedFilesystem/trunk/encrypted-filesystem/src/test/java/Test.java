
import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.spi.FileSystemProvider;
import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;


import com.bm.nio.file.ConfigEncrypted;
import com.bm.nio.file.FileAttributesEncrypted;
import com.bm.nio.file.IntegrationTest;
import com.bm.nio.file.PathEncrypted;
import com.bm.nio.file.utils.TestUtils;
import com.sun.nio.zipfs.ZipFileSystem;
import com.sun.nio.zipfs.ZipFileSystemProvider;
import com.sun.nio.zipfs.ZipPath;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		//new Test().testStartsForZip();
		//new Test().testRelativeResolve();
		//new Test().testFileName();
		//new Test().testIsAbsoluteResolve();
		//new Test().testIterator();
		//new Test().testXML();
//		Cipher encipher = Cipher.getInstance("AES/CFB/NoPadding");
//		System.out.println(encipher.getProvider().getInfo());
//		System.out.println(encipher.getProvider().getName());
		
		//new Test().testSynchronized();
		//System.out.println(new String(new byte [] {50, 49, 69, 69, 70, 50, 69, 66, 47, 50, 48, 69, 70, 70, 51, 69, 65, 47}));
		//new Test().test2Methods();
		//new Test().testBarrier();
//		System.out.println(Paths.get("").relativize(Paths.get("1.txt")));
//		System.out.println(Paths.get("").toAbsolutePath().relativize(Paths.get("1.txt").toAbsolutePath()));
		
//		ConfigEncrypted ce = new ConfigEncrypted();
//		TestUtils.startTime("test");
//		for (int i = 0; i < 10000; i ++){
//			ConfigEncrypted ce1 = ConfigEncrypted.newConfig(ce);
////			if (!ce1.equals(ce))
////				throw new Exception();
//		}
//		TestUtils.endTime("test");
//		System.out.println(TestUtils.printTime("test"));
		
//		new IntegrationTest().testCopy();
//		new Test().measureEncSpeed();
		new Test().checkKey();

		
	}
	
	public void testStartsForZip() throws Exception{
		URI uri = URI.create("jar:file:/zipfstest.zip!/BAR");
		Map<String, String> env = new HashMap<>(); 
	    env.put("create", "true");
	    System.out.println(uri.getScheme());
	    //com.sun.nio.zipfs.ZipFileSystemProvider z;
		FileSystem zipfs = FileSystems.newFileSystem(uri, env);
		
		Path p = zipfs.provider().getPath(uri);
		System.out.println(p.toString());
		
		Path p1 = zipfs.provider().getPath(URI.create("jar:file:/zipfstest.zip!/BAR/PAR"));
		System.out.println (p1.startsWith(p));//should be true
		Path p2 = zipfs.provider().getPath(URI.create("jar:file:/zipfstest.zip!/PAR/PAR1"));
		System.out.println(p2.startsWith(p));//should be false
		
		
		//checking problem when mRoot =  jar:file:/zipfstest.zip!/BAR/PAR, fileSystem = jar:file:/zipfstest.zip
		//and received "/BAR" as getPath function
		Path p3 = p.getFileSystem().getPath("/PAR");
		System.out.println(p2.resolve("RAR").startsWith(p2));//should be true
		System.out.println(p2.resolve("/RAR").startsWith(p2));//should be false
		System.out.println(p2.getFileSystem().getPath("/PAR2", "PAR3", "PAR4").toAbsolutePath());
		
		System.out.println(p2.startsWith(p2));//should be true
		
		Path pathDir = Files.createDirectory(p3);
		System.out.println(Files.isDirectory(pathDir));

		
		long longSize = 0xFFFFFFFFFFFFFFFFl;//Integer.MAX_VALUE;
		//longSize *= 2;
		int intSize = (int)longSize;
		System.out.println(longSize);
		System.out.println (intSize == longSize);
		// ===
		testWindows();
		testZIP();
		//
	}
	
	
	public void testWindows() throws Exception {
		// ==== Testing Windows filesystem ===
		//two options to write - WRITE, WRITE + APPEND. Simple writes just rewrite current values
		SeekableByteChannel s = Files.newByteChannel(Paths.get("test.txt"), StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE);//, StandardOpenOption.APPEND);
		ByteBuffer bb = ByteBuffer.wrap("12345".getBytes());
		s.truncate(0);//clear file
		s.position(0);//start position
		s.write(bb);
		bb.position(2);
		s.write(bb);
		//
		s.position(0);
		byte [] bbrb = new byte[(int)s.size()];
		s.read(ByteBuffer.wrap(bbrb));
		System.out.println(new String(bbrb));//should be 12345345
		s.close();
	}

	public static void testZIP() throws Exception {
		// ==== Testing ZIP filesystem ===
		//two options to write - WRITE, WRITE + APPEND. Simple writes just rewrite current values
		URI uri = URI.create("jar:file:/test.zip!/test.txt");
		Map<String, String> env = new HashMap<>(); 
	    env.put("create", "true");
		FileSystem zipfs = FileSystems.newFileSystem(uri, env);
		Path pz = zipfs.provider().getPath(uri);
		SeekableByteChannel s = Files.newByteChannel(pz, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);//, StandardOpenOption.APPEND);
		ByteBuffer bb = ByteBuffer.wrap("12345".getBytes());
		//s.truncate(0);//clear file - unsupported
		//s.position(0);//start position - unsupported
		s.write(bb);
		bb.position(2);
		s.write(bb);
		
//		SeekableByteChannel s1 = Files.newByteChannel(pz, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);//, StandardOpenOption.APPEND);
//		s1.write(ByteBuffer.wrap("bb".getBytes()));
		//
		s.close();
//		s1.close();
		s = Files.newByteChannel(pz, StandardOpenOption.READ, StandardOpenOption.CREATE);//, StandardOpenOption.APPEND);
		byte [] bbrb = new byte[(int)s.size()];
		s.read(ByteBuffer.wrap(bbrb));
		System.out.println(new String(bbrb));//should be 12345345
		s.close();
	}
	
	public void testRelativeResolve(){
		String sa = "file:///D:/a/a";
		String sb = "file:///D:/a/a/b";
		URI ua = URI.create(sa);
		URI ub = URI.create(sb);
		Path a = Paths.get(ua);
		Path b = Paths.get(ub);
		System.out.println(sa + " vs " + sb);
		System.out.println("Relativize: " + a.relativize(b));//= b - a 
		System.out.println("Resolve: " + a.resolve(b));//= a + b
		
		System.out.println(ua.relativize(ub));
	}
	
	public void testGetPath(){
		ZipFileSystemProvider z = new ZipFileSystemProvider();
		try {
			z.newFileSystem(URI.create("jar:file:/zipfstest.zip!/BAR"), new HashMap());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Path p = z.getPath(URI.create("jar:file:/zipfstest.zip!/BAR/par"));
		try {
			Files.createDirectories(p);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(p.toString());
	}

	public void testFileName(){
		String sa = "file:///D:/a/a";
		String sb = "file:///D:/a/a/b/c";
		URI ua = URI.create(sa);
		URI ub = URI.create(sb);
		Path a = Paths.get(ua);
		Path b = Paths.get(ub);
		Path part = a.relativize(b);
		for (int i = 0; i < part.getNameCount(); i ++){
			System.out.println(part.getName(i).toString() + part.getName(i).getFileName().toString());
		}
	}

	public void testIsAbsoluteResolve(){
		String sa = "file:///D:/a/a";
		String sb = "file:///D:/a/a/b/c";
		URI ua = URI.create(sa);
		URI ub = URI.create(sb);
		Path a = Paths.get(ua);
		Path b = Paths.get(ub);
		Path part = a.relativize(b);
		System.out.println(part.isAbsolute());
		System.out.println(b.resolve(part));
		System.out.println(part.resolve(part));
		System.out.println(part.resolve(b));
	}

	public void testIterator(){
		String sa = "file:///D:/a/a";
		String sb = "file:///D:/a/a/b/c";
		URI ua = URI.create(sa);
		URI ub = URI.create(sb);
		Path a = Paths.get(ua);
		Path b = Paths.get(ub);
		Path part = a.relativize(b);
		Iterator<Path> iterator = b.iterator();
		while(iterator.hasNext()){
			System.out.println(iterator.next());
		}
		for (int i = 0; i < b.getNameCount(); i ++){
			System.out.println(b.getName(i));
		}
	}

	public void testXML() throws Exception {
		@Root
		@Namespace
		class C1 {
			@Element
			private int i;
			@Element
			private int j;
		}
		C1 c1 = new C1();
		Persister serializer = new Persister(new Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>"));
		StringWriter sw = new StringWriter();
		serializer.write(c1, sw);
		System.out.println(sw.getBuffer().toString());
		String str = "<c1><i>1</i><j>2</j></c1>";
		serializer.read(c1, str);
	}
	
	public void testSynchronized() throws Exception {
		class C0{
			private int i = 0;
			public int getI(){
				return i;
			}
			public void setI(int i){
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println(i + " was set");
				this.i = i;
			}
		}
		class C1{
			private C0 c0 = new C0();
			private Object o = new Object();
			public C0 getC0(){
				synchronized (o) {
					return c0;
				}
			}
		}
		
		final C1 c1 = new C1();
		
		c1.getC0().setI(1);
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				c1.getC0().setI(2);
			}
		});
		t.start();
		t.join();
		System.out.println(c1.getC0().getI());
		
	}
	
	//testing if it's possible to know if method belongs to child or parent
	//needed for CipherUtils - getEncAmount and enc/dec block
	public void test2Methods() throws Exception {
		class C1 {
			public void meth1(){};
			public void meth2(){};
		}
		class C2 extends C1 {
			@Override
			public void meth1() {};
			@Override
			public void meth2() {};
		};
		
		C1 c = new C2();
		System.out.println(c.getClass().getMethod("meth1").getDeclaringClass().equals(c.getClass().getMethod("meth2").getDeclaringClass()));
//		Path srcPath, dstPath;
//		srcPath = Paths.get("");
//		Files.copy(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING);
	}
	
	public void testBarrier() throws Exception {
		Random r1 = new Random(50);
		System.out.println(r1.nextInt(50));
		
		final CyclicBarrier cb = new CyclicBarrier(5);
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				try {
					cb.await();
					System.out.println(Thread.currentThread() + " " + System.currentTimeMillis());
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		
		for (int i = 0; i < 10; i ++){
			new Thread(r).start();
			Thread.sleep(500);
		}
	}
	
	public void testThreadLocal() throws Exception {
		final ThreadLocal<Integer> tl = new ThreadLocal<Integer>(){
			volatile int i = 0;
			Queue<Integer> freeObjects = new ConcurrentLinkedQueue<Integer>();
			@Override
			protected Integer initialValue() {
				Integer il = freeObjects.poll();
				if (il == null)
					il = new Integer(i ++);//creating new
				final Integer i1 = il;
			    final Thread godot = Thread.currentThread();
			    new Thread() {
			      @Override public void run() {
			        try {
			          godot.join();
			          freeObjects.offer(i1);
			        } catch (InterruptedException e) {
			          // thread dying; ignore
			        }// finally {
			        //  remove();
			        //}
			      }
			    }.start();
				return i1;
			}
			
		};
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				tl.get();
				
			}
		}).start();

		Thread.sleep(10000);
	}
	
	public void measureEncSpeed() throws Exception {
		ConfigEncrypted ce = new ConfigEncrypted();
//		ce.setTransformation("AES/CBC/NoPadding");

		Cipher c = ce.newCiphers("123".toCharArray()).getDecipher();
		byte [] data = new byte [90000000];
//		byte [] data = new byte [9];
		long l = System.currentTimeMillis();
		c.doFinal(data);
		c.doFinal(data);
		l = System.currentTimeMillis() - l;
		System.out.println(l);
	}
	
	public void checkKey() throws Exception {
	    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
	    KeySpec spec = new PBEKeySpec("123".toCharArray(), "123".getBytes(), 10, 128);
	    SecretKey tmp = factory.generateSecret(spec);
	    SecretKeySpec key = new SecretKeySpec(tmp.getEncoded(), "AES");// TODO: get appropriate key
	    key = new SecretKeySpec(tmp.getEncoded(), "Blowfish");// TODO: get appropriate key
	    key = new SecretKeySpec(tmp.getEncoded(), "DES");// TODO: get appropriate key
	    //Blowfish, DES
	}

}
