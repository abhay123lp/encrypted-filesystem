import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
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
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.spi.FileSystemProvider;
import java.security.GeneralSecurityException;
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
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import sun.nio.fs.WindowsFileSystemProvider;

import com.bm.nio.file.FileAttributesEncrypted;
import com.bm.nio.file.PathEncrypted;
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
		Path p = Paths.get("/books");
		WatchService w = FileSystems.getDefault().newWatchService();
		p.register(w, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_CREATE);
		WatchKey wk = w.take();
		//wk.pollEvents();
		//boolean b = wk.reset();
		wk = w.take();
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

}
