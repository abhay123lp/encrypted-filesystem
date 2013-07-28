package com.bm.nio.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

import javax.xml.bind.DatatypeConverter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import sun.nio.fs.WindowsFileSystemProvider;

import com.bm.nio.file.utils.TestUtils;
import com.sun.nio.zipfs.ZipFileSystem;
import com.sun.nio.zipfs.ZipFileSystemProvider;
import com.sun.nio.zipfs.ZipPath;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

public class FileSystemEncryptedTest {

	static{
		//delete everything before starting tests
		File f = new File(TestUtils.SANDBOX_PATH);
		if (f.isDirectory())
			TestUtils.deleteFolderContents(f);
	}
	
	//@Test
	public void encryptDecrypt(){
		//FileSystemProviderEncrypted f = new FileSystemProviderEncrypted();
		
		try {
			URI uri = URI.create("jar:file:/zipfstest.zip!/BAR");
			Map<String, String> env = new HashMap<>(); 
		    env.put("create", "true");
		    System.out.println(uri.getScheme());
		    //com.sun.nio.zipfs.ZipFileSystemProvider z;
			FileSystem zipfs = FileSystems.newFileSystem(uri, env);
			
			Path p = zipfs.provider().getPath(uri);
			System.out.println(p.toString());
			//Path pathInZipfile = zipfs.getPath("/SomeTextFile.txt");  
			/*URI u = new URI("file:///");
			System.out.println(u.getAuthority());
			for (FileSystemProvider provider: FileSystemProvider.installedProviders()) {
	            
			}*/
			
			//FileSystem f = FileSystems.newFileSystem(u, new HashMap<String, Object>());
			//FileSystem f = FileSystems.getFileSystem(u);
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		Assert.assertTrue(true);
	}
	
	
	
//	private void delete(File file){
//		if (file.isFile())
//			file.delete();
//		if (file.isDirectory()){
//			for (File f : file.listFiles())
//				delete(f);
//			file.delete();
//		}
//	}
//	private File newTempDir(String path){
//		File dir = new File(path);
//		if (dir.exists())
//			//dir.delete();
//			delete(dir);
//		dir.mkdirs();
//		return dir;
//	}
//	
//	private URI fileToURI(File f) throws IOException{
//		return Paths.get(f.getCanonicalPath()).toUri();
//	}
//	
//	private URI pathToURI(String path) throws IOException{
//		return fileToURI(new File(path));
//	}
//	
//	private URI uriEncrypted(URI u) throws URISyntaxException{
//		return new URI("encrypted:" + u);
//	}
	
//	private FileSystem newTempFieSystem(FileSystemProviderEncrypted fpe, String path) throws IOException, URISyntaxException{
//		File file = TestUtils.newTempDir(path); 
//		//URI uri1File = Paths.get(file.getCanonicalPath()).toUri();
//		URI uri1Encrypted = TestUtils.uriEncrypted(TestUtils.fileToURI(file));
//		return fpe.newFileSystem(uri1Encrypted, new HashMap<String, Object>());
//	}
//	
	private final FileSystemProviderEncrypted mFspe = TestUtils.getEncryptedProvider();
	
//	private FileSystemProviderEncrypted getEncryptedProvider(){
//		final String scheme = new FileSystemProviderEncrypted().getScheme();
//		for (FileSystemProvider f : FileSystemProvider.installedProviders()){
//			if (f.getScheme().endsWith(scheme) && f instanceof FileSystemProviderEncrypted)
//				return (FileSystemProviderEncrypted)f;
//		}
//		return new FileSystemProviderEncrypted();
//	}
//	
	/**
	 * Checks encrypted provider is among installed 
	 */
	@Test
	public void listInstalled() throws IOException {
		String scheme = new FileSystemProviderEncrypted().getScheme();
		boolean found = false;
		for (FileSystemProvider fsp : FileSystemProvider.installedProviders())
			found = found || fsp.getScheme().equals(scheme);
		Assert.assertTrue(found);
		//
		clean();
	}
	
	
	/**
	 * Test functions of creating and getting filesystems
	 */
	@Test
	public void newGetCloseFilesystem() throws IOException {

		FileSystemProviderEncrypted fpe = mFspe;
		
//		try {
//			for (int i = 0; i < Paths.get(pathToURI(".")).getNameCount(); i ++)
//				System.out.println(Paths.get(pathToURI(".")).getName(i));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		try {
//			File sandbox = new File(TestUtils.SANDBOX_PATH);
//			String currDir = sandbox.getCanonicalPath();
//			Path p = Paths.get(currDir);
//			URI uriFile = p.toUri();
//			URI uriEncrypted = new URI("encrypted:" + uriFile);
			for (int i = 0; i < 100; i ++)
				TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/enc" + i);
			// === duplication ===
			boolean duplicateError = false;
			try {
				TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/enc3");
			} catch (FileSystemAlreadyExistsException e) {
				duplicateError = true;
			}
			Assert.assertTrue(duplicateError);//check error in case of duplication
			// === === ===
			
			// === nested encrypted filesystem not allowed ===
			boolean nestedException = false;
			try {
				TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/enc3/dir");
			} catch (FileSystemAlreadyExistsException e) {
				nestedException = true;
			}
			Assert.assertTrue(nestedException);//check error in case of duplication
			TestUtils.delete(new File(TestUtils.SANDBOX_PATH + "/enc3/dir"));//housekeeping, otherwise it will throw DirectoryNotEmptyException when will be deleting filesystem
			// === nested - high level filesystem not allowed ===
			nestedException = false;
			try {
				TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/enc300/dir");
				//TOD1O: implement this check in filesystemprovider!
				TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/enc300");
			} catch (FileSystemAlreadyExistsException e) {
				nestedException = true;
			}
			Assert.assertTrue(nestedException);//check error in case of duplication
			// === === ===
			
			String encSubPath = TestUtils.SANDBOX_PATH + "/enc23/dir";
			File f = TestUtils.newTempDir(encSubPath);
			FileSystem fs = fpe.getFileSystem(TestUtils.uriEncrypted(TestUtils.pathToURI(encSubPath)));
			for (Path p : fs.getRootDirectories()){
				Assert.assertTrue(p.toString().endsWith("enc23"));
				//System.out.println(p);//D:\prog\workspace\encrypted-filesystem-trunk\src\test\sandbox\enc23
			}
			TestUtils.delete(f);//housekeeping, otherwise it will throw DirectoryNotEmptyException when will be deleting filesystem
			//enc1.delete();
			
			// === closing - should not be exception ===
			boolean exception = false;
			try {
				FileSystem fsClose = TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/encClose/dir");
				fsClose.close();
				TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/encClose/dir");
			} catch (Exception e) {
				exception = true;
			}
			Assert.assertFalse(exception);
			// === === ===
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} finally{
			clean();
		}
		
		//TODO: in progress - test creation of encrypted properties file
		
//		try {
//			Path p = Paths.get(new URI("file:///test"));
//			Path p1 = Paths.get(new URI("file:///test/test1"));
//			System.out.println(p1.startsWith(p));
//		} catch (URISyntaxException e) {
//			e.printStackTrace();
//		}
	}
	
	@Test
	public void deleteTest() throws Exception {
		//TODO:
		//1st test: delete all filesystems by directory walker
		//2nd test: create non-encrypted fordel and catch Directory not empty exception (delet should only tuch encrypted objects).
		
		//create and delete encrypted folders and files
		try {
			FileSystemProviderEncrypted fpe = mFspe;
			
			//TEST!
//			Path path = mFspe.getFileSystems().iterator().next().getRootDirectories().iterator().next();
//			DirectoryStream<Path> stream = fpe.newDirectoryStream(path, new DirectoryStream.Filter<Path>() {
//
//				@Override
//				public boolean accept(Path entry) throws IOException {
//					return true;
//				}
//			});
//			for (Path p : stream){
//				System.out.println(p);//null!!
//			}
			//
			
			// === deleting should not be exception ===
			for (int i = 0; i < 10; i ++){
				String basePath = TestUtils.SANDBOX_PATH + "/enc" + i;
				TestUtils.newTempFieSystem(fpe, basePath);
			}
			
			boolean exception = false;
			try {
					TestUtils.deleteFilesystems(fpe);
					TestUtils.deleteFilesystems(fpe);//checking that filesystems are closed
			} catch (Exception e) {
				exception = true;
			}
			Assert.assertFalse(exception);
			// === === ===
			
			// === deleting with not encrypted folder inside - should be exception (not encrypted won't be deleted) ===
			for (int i = 0; i < 10; i ++){
				String basePath = TestUtils.SANDBOX_PATH + "/enc" + i;
				TestUtils.newTempFieSystem(fpe, basePath);
			}
			
			File plain = TestUtils.newTempDir(TestUtils.SANDBOX_PATH + "/enc0/enc01");
			
			//TODO: should work correctly 
			//when implemented encrypted name check (in DirectoryIteratorEncrypted.hasNext(), new PathEncrypted())
			
			boolean passed = false;
			try {
				TestUtils.deleteFilesystems(fpe);
			} catch (Exception e) {
				TestUtils.delete(plain);//remove plain directory
				try {
					TestUtils.deleteFilesystems(fpe);
					passed = true;//deletes correctly after plain is removed
				} catch (Exception e2) {
					
				}
			}
			Assert.assertTrue(passed);
			// === === ===
			
		} catch (Exception e) {
			throw e;
		} finally{
			clean();
		}
	}	
	
	@Test
	public void testNewWatchService() throws Exception {
		FileSystem fs = TestUtils.newTempFieSystem(mFspe, TestUtils.SANDBOX_PATH + "/enc1");
		WatchService wsEq = FileSystems.getDefault().newWatchService();
		WatchService ws1 = new WatchServiceEncrypted(wsEq, (FileSystemEncrypted)fs);
		WatchService ws2 = new WatchServiceEncrypted(wsEq, (FileSystemEncrypted)fs);
		Assert.assertEquals(ws1, ws2);
		Assert.assertEquals(ws1.hashCode(), ws2.hashCode());
	}
	
	@Test
	public void testGetPath() throws Exception {
		FileSystem fsTest = TestUtils.newTempFieSystem(mFspe, TestUtils.SANDBOX_PATH + "/encGetPath");
		Path p = fsTest.getPath("dir1", "dir2");
		Files.createDirectories(p);
		//TODO:
	}
	
	/**
	 * At leas should not throw exception
	 * @throws Exception
	 */
	@Test
	public void testSimple() throws Exception {
		FileSystem fsTest = TestUtils.newTempFieSystem(mFspe, TestUtils.SANDBOX_PATH + "/encGetPath");
		Path p1 = fsTest.getPath("dir1", "dir2", "1.txt");
		Files.createDirectories(p1.subpath(0, 2));
		Files.createFile(p1);
		SeekableByteChannel bc = Files.newByteChannel(p1, StandardOpenOption.WRITE, StandardOpenOption.READ);
		bc.write(ByteBuffer.wrap("1234567890".getBytes()));
		bc.position(3);
		byte [] b = new byte[10];
		bc.read(ByteBuffer.wrap(b));
		bc.close();
	}
	
	
	public static final String TEST_COPY_SRC = "./src/test/copy_src/";
	public static final String TEST_COPY_TARGET = "./src/test/copy_target/";
	
	@Test
	public void testCopy() throws Exception {
		ConfigEncrypted conf = new ConfigEncrypted();
//		conf.setBlockSize(3);//TODO: also try 11
		conf.setTransformation("AES/CBC/PKCS5Padding");//try
		Map<String, Object> env = new HashMap<String, Object>();
		env.put(FileSystemEncrypted.FileSystemEncryptedEnvParams.ENV_CONFIG, conf);
		env.put(FileSystemEncrypted.FileSystemEncryptedEnvParams.ENV_PASSWORD, "password1".toCharArray());
		
		//TODO:
		// 1. Use copyDirectory to copy from existing directory
		// 2. Write function to validate copied data

		Path src = Paths.get(TEST_COPY_SRC);//new File(TEST_COPY_SRC);
		Path enc = TestUtils.newTempFieSystem(mFspe, TestUtils.SANDBOX_PATH + "/testCopy").getPath("/");
		//another type of encryption
		Path enc1 = TestUtils.newTempFieSystem(mFspe, TestUtils.SANDBOX_PATH + "/testCopy1", env).getPath("/");
//		Path enc = TestUtils.newTempFieSystem(mFspe, TestUtils.SANDBOX_PATH + "/testCopy").getPath("/").toAbsolutePath();
		//
		
		Path target = Paths.get(TEST_COPY_TARGET);//new File(TestUtils.SANDBOX_PATH + "/testCopy");
		//Path 
		//prepare
		//TestUtils.deleteFolderContents(enc.toFile());
		//TestUtils.deleteFolderContents(enc1.toFile());
		TestUtils.startTime("Copy0");
		copyDirectory(src, target);
		TestUtils.endTime("Copy0");
		TestUtils.deleteFolderContents(target.toFile());
		//

		TestUtils.startTime("Copy1");
		copyDirectory(src, enc);//DONE: does not work correctly with block ciphers. Fixed bug in write function
		TestUtils.endTime("Copy1");
		
//		copyDirectory(src, enc);//DONE: test this also, it might just start writing from beginning without deleting the whole file (but maybe just because of different buffer size 12 vs 8k)
		TestUtils.resetTime("encrypt");
		TestUtils.startTime("Copy2");
		copyDirectory(enc, enc1);//DONE: does not copying correctly. Fixed bug in write function
		TestUtils.endTime("Copy2");
		System.out.println(TestUtils.printTime("decrypt"));
		System.out.println(TestUtils.printTime("encrypt"));
		
		TestUtils.startTime("Copy3");
		copyDirectory(enc1, target);//DONE: make it work
		TestUtils.endTime("Copy3");
		//copyDirectory(enc, target, true);
		
		System.out.println(TestUtils.printTime("Copy0"));
		System.out.println(TestUtils.printTime("Copy1"));
		System.out.println(TestUtils.printTime("Copy2"));
//		System.out.println(TestUtils.printTime("saveBlock"));
//		System.out.println(TestUtils.printTime("write"));
		System.out.println(TestUtils.printTime("read"));
		System.out.println(TestUtils.printTime("readStart"));
		System.out.println(TestUtils.printTime("readMiddle"));
		System.out.println(TestUtils.printTime("loadBlock"));
		System.out.println(TestUtils.printTime("Copy3"));
		
		Assert.assertTrue(equals(src, target));
		Assert.assertTrue(equals(src, enc));
		Assert.assertTrue(equals(enc, target));
		Assert.assertTrue(equals(enc, enc1));
	}
	
	public boolean equals(Path sourceLocation , Path targetLocation) throws IOException{
		try {
			time1 = 0;
			time2 = 0;
			Files.walkFileTree(sourceLocation, new DirCompareVisitor(sourceLocation, targetLocation));
			Files.walkFileTree(targetLocation, new DirCompareVisitor(targetLocation, sourceLocation));
			System.out.println("time1: " + time1 + "; time2: " + time2);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
    long time1, time2;
	long t1, t2;
	 public class DirCompareVisitor extends SimpleFileVisitor<Path> {
		    private Path fromPath;
		    private Path toPath;
		    private StandardCopyOption copyOption = StandardCopyOption.REPLACE_EXISTING;
		    
		    public DirCompareVisitor(Path from, Path to){
		    	fromPath = from;
		    	toPath = to;
		    }
		    @Override
		    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		    	final Path p1 = toPath.resolve(fromPath.relativize(dir).toString());
		        if(!Files.exists(p1) || !Files.isDirectory(p1)){
		            throw new RuntimeException("Path " + p1.toString() + " is missing or is not directory");
		        }
		        return FileVisitResult.CONTINUE;
		    }

		    @Override
		    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		    	final Path p1 = toPath.resolve(fromPath.relativize(file).toString());
		        if(!Files.exists(p1) || Files.isDirectory(p1)){
		            throw new RuntimeException("Path " + p1.toString() + " is missing or is not a file");
		        }

		        if (!isEqual(Files.newInputStream(file), Files.newInputStream(p1))){
		        	throw new RuntimeException("Path " + p1.toString() + " is not equals to " + file.toString());
		        }
		        //Files.copy(file, p1, copyOption);
		        return FileVisitResult.CONTINUE;
		    }
		    
		    private boolean isEqual(InputStream i1, InputStream i2)
		            throws IOException {

		        ReadableByteChannel ch1 = Channels.newChannel(i1);
		        ReadableByteChannel ch2 = Channels.newChannel(i2);

//		        ByteBuffer buf1 = ByteBuffer.allocateDirect(1024);
//		        ByteBuffer buf2 = ByteBuffer.allocateDirect(1024);
		        ByteBuffer buf1 = ByteBuffer.allocateDirect(16384);
		        ByteBuffer buf2 = ByteBuffer.allocateDirect(16384);
		        try {
		            while (true) {
		            	t1 = System.currentTimeMillis();
		                int n1 = ch1.read(buf1);
		                t2 = System.currentTimeMillis();
		                time1 += t2 - t1;		                
		                int n2 = ch2.read(buf2);
		                t1 = System.currentTimeMillis();
		                time2 += t1 - t2; 

		                if (n1 == -1 || n2 == -1) return n1 == n2;

		                buf1.flip();
		                buf2.flip();

		                for (int i = 0; i < Math.min(n1, n2); i++)
		                    if (buf1.get() != buf2.get())
		                        return false;

		                buf1.compact();
		                buf2.compact();
		            }
		        } finally {
		            if (i1 != null) i1.close();
		            if (i2 != null) i2.close();
		        }
		        
		    }		    
		}
	 
	 
	 public class CopyDirVisitor extends SimpleFileVisitor<Path> {
		    private Path fromPath;
		    private Path toPath;
		    private StandardCopyOption copyOption = StandardCopyOption.REPLACE_EXISTING;
		    
		    public CopyDirVisitor(Path from, Path to){
		    	fromPath = from;
		    	toPath = to;
		    }
		    @Override
		    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		    	final Path p1 = toPath.resolve(fromPath.relativize(dir).toString());
		        if(!Files.exists(p1)){
		            Files.createDirectory(p1);
		        }
		        return FileVisitResult.CONTINUE;
		    }

		    @Override
		    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		    	final Path p1 = toPath.resolve(fromPath.relativize(file).toString());
				FileSystemProviderEncrypted fpe1 = new FileSystemProviderEncrypted();
				fpe1.copy(file, p1, copyOption);
//		        Files.copy(file, p1, copyOption);
		        return FileVisitResult.CONTINUE;
		    }
		}
	 
	public void copyDirectory(Path sourceLocation , Path targetLocation)
		    throws IOException {
		Files.walkFileTree(sourceLocation, new CopyDirVisitor(sourceLocation, targetLocation));
    }
	
	
	@After
	public void clean() throws IOException {
		//DONE: implement directory walker deletion (can take from delete() test)
		//
		TestUtils.deleteFilesystems(mFspe);
		//clean is required for some nested filesystems, 
		// i.e. EncryptedFilesystem with enc300/dir root, will leave enc300 after deletion 
		File f = new File(TestUtils.SANDBOX_PATH);
		if (f.isDirectory())
			TestUtils.deleteFolderContents(f);
//		
//		//close filesystems
//		FileSystemProviderEncrypted fpe = mFspe;
//		for (FileSystemEncrypted fe : fpe.getFileSystems()){
//			fe.close();
//		}

	}
	
}
