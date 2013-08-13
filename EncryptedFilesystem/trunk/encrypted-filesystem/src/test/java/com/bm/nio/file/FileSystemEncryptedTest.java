package com.bm.nio.file;

import java.io.BufferedWriter;
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
import java.nio.charset.Charset;
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
import java.util.Random;
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
	 * At least should not throw exception
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
	
	// =======================================
	// === below moved to a separate class ===
	// =======================================
//	public static final String TEST_COPY_SRC = "./src/test/copy_src/";
//	public static final String TEST_COPY_TARGET = "./src/test/copy_target/";
//	
//	@Test
//	public void testCopy() throws Exception {
//		generateTestFiles(TEST_COPY_SRC);
//
//		ConfigEncrypted conf = new ConfigEncrypted();
////		conf.setBlockSize(3);//TODO: also try 11
//		conf.setTransformation("AES/CBC/PKCS5Padding");//TODO: try another encryptions 
//		testCopyInternal(TEST_COPY_SRC, TestUtils.SANDBOX_PATH + "/testCopy",
//				TestUtils.SANDBOX_PATH + "/testCopy1", TEST_COPY_TARGET, conf);
//
//	}
//	
//	/**
//	 * Creates test files and directory structure, hardcoded.
//	 */
//	private void generateTestFiles(String path) throws Exception {
//		//TODO:
//		TestUtils.deleteFolderContents(new File(path));
//		Random r = new Random();
//		for (int i = 0; i < TEST_FILES.length; i +=2){
//			String name = TEST_FILES[i];
//			int len = Integer.valueOf(TEST_FILES[i + 1]);
//			Path p = Paths.get(path + name).normalize();
//	
//			try {
//			Files.createDirectories(p);
//			Files.delete(p);
//			p = Files.createFile(p);
//			} catch (Exception e) {
//				System.out.println(e);
//			}
//			
//			try(SeekableByteChannel bc = Files.newByteChannel(p, StandardOpenOption.WRITE)){
//				byte [] data = new byte[len];
//				for (int j = 0; j < len; j ++){
//					data[j] = (byte)r.nextInt(Byte.MAX_VALUE);
//				}
////					os.write(r.nextInt(Byte.MAX_VALUE));
////					os.write(data);
//				bc.write(ByteBuffer.wrap(data));
//			}
//				
//		}
//
//	}
//	
//	//@Test
//	public void generateRandomFiles(){
//		generateRandomFilesInternal(200, 10, 100000, 10, 10);
//	}
//	
//	private void generateRandomFilesInternal(int filesCnt, int maxNameLen, int maxFileLen, int maxDepth, int maxFilesInOneFolder){
//		final String CHARS = "abcdefjhijklmnopqrstuvwxyzABCDEFJHIGKLMNOPQRSTUVWXYZ1234567890-_";
//		Random r = new Random();
//		//
//		int filesProcesed = 0;
//		//for (int i = 0; i < filesCnt; i ++)
//		while(filesProcesed < filesCnt)
//		{
//			String path = getRandomPath(CHARS, maxDepth, maxNameLen);
//			int filesToCreate = r.nextInt(maxFilesInOneFolder);
//			if (filesToCreate > filesCnt - filesProcesed){//limit is reached
//				filesToCreate = filesCnt - filesProcesed;
//			}
//			//do create
//			for (int j = 0; j < filesToCreate; j ++){
//				String file = path + getRandomString(CHARS, maxNameLen);
//				System.out.println("\"" + file + "\", \"" + r.nextInt(maxFileLen) + "\", ");
//			}
//			
//			//
//			filesProcesed += filesToCreate;
//		}
//	}
//	
//	private String getRandomPath(String chars, int maxDepth, int maxNameLen){
//		String res = "./";
//		int len = new Random().nextInt(maxDepth);
//		for (int i = 0; i < len; i ++){
//			res += getRandomString(chars, maxNameLen) + "/";
//		}
//		return res;
//	}
//	
//	private String getRandomString(String chars, int maxLen){
//		String res = "";
//		Random r = new Random();
//		int len = r.nextInt(maxLen - 1) + 1;
//		for (int i = 0; i < len; i ++){
//			res += chars.charAt(r.nextInt(chars.length() - 1));
//		}
//		return res;
//	}
//	
//	/**
//	 * Copies between folders: copySrc(unencrypted)-->enc1(encrypted)-->enc2(encrypted)-->copyTarget(unencrypted)
//	 * @param conf2 - configuration for enc2 filesystem
//	 * @throws Exception
//	 */
//	public void testCopyInternal(final String srcPath, final String enc1Path,
//			final String enc2Path, final String targetPath,
//			ConfigEncrypted conf2) throws Exception {
//		//=== INIT ===
//		Map<String, Object> env2 = new HashMap<String, Object>();
//		env2.put(FileSystemEncrypted.FileSystemEncryptedEnvParams.ENV_CONFIG, conf2);
//		env2.put(FileSystemEncrypted.FileSystemEncryptedEnvParams.ENV_PASSWORD, "password1".toCharArray());
//
//		Path src = Paths.get(srcPath);
//		Path enc = TestUtils.newTempFieSystem(mFspe, enc1Path).getPath("/");
//		//another type of encryption
//		Path enc1 = TestUtils.newTempFieSystem(mFspe, enc2Path, env2).getPath("/");
//		
//		Path target = Paths.get(targetPath);
//		//Path 
//		//prepare
//		//TestUtils.deleteFolderContents(enc.toFile());
//		//TestUtils.deleteFolderContents(enc1.toFile());
//		TestUtils.startTime("Copy0");
//		copyDirectory(src, target);
//		TestUtils.endTime("Copy0");
//		TestUtils.deleteFolderContents(target.toFile());
//		//
//
//		TestUtils.startTime("Copy1");
//		copyDirectory(src, enc);//DONE: does not work correctly with block ciphers. Fixed bug in write function
//		TestUtils.endTime("Copy1");
//		
////		copyDirectory(src, enc);//DONE: test this also, it might just start writing from beginning without deleting the whole file (but maybe just because of different buffer size 12 vs 8k)
//		TestUtils.resetTime("encrypt");
//		TestUtils.startTime("Copy2");
//		copyDirectory(enc, enc1);//DONE: does not copying correctly. Fixed bug in write function
//		TestUtils.endTime("Copy2");
//		System.out.println(TestUtils.printTime("decrypt"));
//		System.out.println(TestUtils.printTime("encrypt"));
//		
//		TestUtils.startTime("Copy3");
//		copyDirectory(enc1, target);//DONE: make it work
//		TestUtils.endTime("Copy3");
//		//copyDirectory(enc, target, true);
//		
//		System.out.println(TestUtils.printTime("Copy0"));
//		System.out.println(TestUtils.printTime("Copy1"));
//		System.out.println(TestUtils.printTime("Copy2"));
////		System.out.println(TestUtils.printTime("saveBlock"));
////		System.out.println(TestUtils.printTime("write"));
//		System.out.println(TestUtils.printTime("read"));
//		System.out.println(TestUtils.printTime("readStart"));
//		System.out.println(TestUtils.printTime("readMiddle"));
//		System.out.println(TestUtils.printTime("loadBlock"));
//		System.out.println(TestUtils.printTime("Copy3"));
//		
//		Assert.assertTrue(equals(src, target));
//		Assert.assertTrue(equals(src, enc));
//		Assert.assertTrue(equals(enc, target));
//		Assert.assertTrue(equals(enc, enc1));		
//	}
//	
//	public boolean equals(Path sourceLocation , Path targetLocation) throws IOException{
//		try {
//			time1 = 0;
//			time2 = 0;
//			Files.walkFileTree(sourceLocation, new DirCompareVisitor(sourceLocation, targetLocation));
//			Files.walkFileTree(targetLocation, new DirCompareVisitor(targetLocation, sourceLocation));
//			System.out.println("time1: " + time1 + "; time2: " + time2);
//		} catch (Exception e) {
//			return false;
//		}
//		return true;
//	}
//	
//    long time1, time2;
//	long t1, t2;
//	 public class DirCompareVisitor extends SimpleFileVisitor<Path> {
//		    private Path fromPath;
//		    private Path toPath;
//		    private StandardCopyOption copyOption = StandardCopyOption.REPLACE_EXISTING;
//		    
//		    public DirCompareVisitor(Path from, Path to){
//		    	fromPath = from;
//		    	toPath = to;
//		    }
//		    @Override
//		    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
//		    	final Path p1 = toPath.resolve(fromPath.relativize(dir).toString());
//		        if(!Files.exists(p1) || !Files.isDirectory(p1)){
//		            throw new RuntimeException("Path " + p1.toString() + " is missing or is not directory");
//		        }
//		        return FileVisitResult.CONTINUE;
//		    }
//
//		    @Override
//		    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//		    	final Path p1 = toPath.resolve(fromPath.relativize(file).toString());
//		        if(!Files.exists(p1) || Files.isDirectory(p1)){
//		            throw new RuntimeException("Path " + p1.toString() + " is missing or is not a file");
//		        }
//
//		        if (!isEqual(Files.newInputStream(file), Files.newInputStream(p1))){
//		        	throw new RuntimeException("Path " + p1.toString() + " is not equals to " + file.toString());
//		        }
//		        //Files.copy(file, p1, copyOption);
//		        return FileVisitResult.CONTINUE;
//		    }
//		    
//		    private boolean isEqual(InputStream i1, InputStream i2)
//		            throws IOException {
//
//		        ReadableByteChannel ch1 = Channels.newChannel(i1);
//		        ReadableByteChannel ch2 = Channels.newChannel(i2);
//
////		        ByteBuffer buf1 = ByteBuffer.allocateDirect(1024);
////		        ByteBuffer buf2 = ByteBuffer.allocateDirect(1024);
//		        ByteBuffer buf1 = ByteBuffer.allocateDirect(16384);
//		        ByteBuffer buf2 = ByteBuffer.allocateDirect(16384);
//		        try {
//		            while (true) {
//		            	t1 = System.currentTimeMillis();
//		                int n1 = ch1.read(buf1);
//		                t2 = System.currentTimeMillis();
//		                time1 += t2 - t1;		                
//		                int n2 = ch2.read(buf2);
//		                t1 = System.currentTimeMillis();
//		                time2 += t1 - t2; 
//
//		                if (n1 == -1 || n2 == -1) return n1 == n2;
//
//		                buf1.flip();
//		                buf2.flip();
//
//		                for (int i = 0; i < Math.min(n1, n2); i++)
//		                    if (buf1.get() != buf2.get())
//		                        return false;
//
//		                buf1.compact();
//		                buf2.compact();
//		            }
//		        } finally {
//		            if (i1 != null) i1.close();
//		            if (i2 != null) i2.close();
//		        }
//		        
//		    }		    
//		}
//	 
//	public void copyDirectory(Path sourceLocation , Path targetLocation)
//		    throws IOException {
//		Files.walkFileTree(sourceLocation, new CopyDirVisitor(sourceLocation, targetLocation));
//    }
//		
//	 
//	 public class CopyDirVisitor extends SimpleFileVisitor<Path> {
//		    private Path fromPath;
//		    private Path toPath;
//		    private StandardCopyOption copyOption = StandardCopyOption.REPLACE_EXISTING;
//		    
//		    public CopyDirVisitor(Path from, Path to){
//		    	fromPath = from;
//		    	toPath = to;
//		    }
//		    @Override
//		    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
//		    	final Path p1 = toPath.resolve(fromPath.relativize(dir).toString());
//		        if(!Files.exists(p1)){
//		            Files.createDirectory(p1);
//		        }
//		        return FileVisitResult.CONTINUE;
//		    }
//
//		    @Override
//		    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//		    	final Path p1 = toPath.resolve(fromPath.relativize(file).toString());
//				FileSystemProviderEncrypted fpe1 = new FileSystemProviderEncrypted();
//				fpe1.copy(file, p1, copyOption);
////		        Files.copy(file, p1, copyOption);
//		        return FileVisitResult.CONTINUE;
//		    }
//		}
//	
//	final String [] TEST_FILES = new String [] {
//			"./ujsl9Jw7G/wW/v/5/doKsk0HC/XGfUt4Y", "58724", 
//			"./ujsl9Jw7G/wW/v/5/doKsk0HC/D4LHx", "36085", 
//			"./ujsl9Jw7G/wW/v/5/doKsk0HC/6Cqn2sQ", "6839", 
//			"./ujsl9Jw7G/wW/v/5/doKsk0HC/WTeqS40N", "39527", 
//			"./ujsl9Jw7G/wW/v/5/doKsk0HC/33h", "2009", 
//			"./ujsl9Jw7G/wW/v/5/doKsk0HC/IHws", "63008", 
//			"./ujsl9Jw7G/wW/v/5/doKsk0HC/w9BqBtra", "85672", 
//			"./ujsl9Jw7G/wW/v/5/doKsk0HC/CIvFf", "51198", 
//			"./P/IR-sXwql/i/IuCCa7/-1zUb/FTRs-bKqZ/OXbpjTG/pNGGXl/Ut", "47450", 
//			"./P/IR-sXwql/i/IuCCa7/-1zUb/FTRs-bKqZ/OXbpjTG/pNGGXl/pb05", "6113", 
//			"./P/IR-sXwql/i/IuCCa7/-1zUb/FTRs-bKqZ/OXbpjTG/pNGGXl/N1", "73662", 
//			"./P/IR-sXwql/i/IuCCa7/-1zUb/FTRs-bKqZ/OXbpjTG/pNGGXl/ej", "60686", 
//			"./P/IR-sXwql/i/IuCCa7/-1zUb/FTRs-bKqZ/OXbpjTG/pNGGXl/x", "54900", 
//			"./P/IR-sXwql/i/IuCCa7/-1zUb/FTRs-bKqZ/OXbpjTG/pNGGXl/Eq3y", "48990", 
//			"./1lNZIEW/EseCRIa/CjEa/Woe/GL/e5/qM3/7b71/FIMlEH/ZqOjWN", "21706", 
//			"./1lNZIEW/EseCRIa/CjEa/Woe/GL/e5/qM3/7b71/FIMlEH/iUX2k0", "47056", 
//			"./1lNZIEW/EseCRIa/CjEa/Woe/GL/e5/qM3/7b71/FIMlEH/G", "2858", 
//			"./1lNZIEW/EseCRIa/CjEa/Woe/GL/e5/qM3/7b71/FIMlEH/7W96Kq", "78202", 
//			"./1lNZIEW/EseCRIa/CjEa/Woe/GL/e5/qM3/7b71/FIMlEH/1VQLPa73", "5045", 
//			"./1lNZIEW/EseCRIa/CjEa/Woe/GL/e5/qM3/7b71/FIMlEH/Bb7ArTlF", "93865", 
//			"./1lNZIEW/EseCRIa/CjEa/Woe/GL/e5/qM3/7b71/FIMlEH/UcONbOC", "73238", 
//			"./1lNZIEW/EseCRIa/CjEa/Woe/GL/e5/qM3/7b71/FIMlEH/4FpJR", "85002", 
//			"./D/LJDb0Z1rY/HX/Ua/oPi/T3/f7e4", "65559", 
//			"./D/LJDb0Z1rY/HX/Ua/oPi/T3/EPhhsOV", "25312", 
//			"./D/LJDb0Z1rY/HX/Ua/oPi/T3/9XV", "19938", 
//			"./D/LJDb0Z1rY/HX/Ua/oPi/T3/JbLG1", "13752", 
//			"./D/LJDb0Z1rY/HX/Ua/oPi/T3/454t", "84455", 
//			"./D/LJDb0Z1rY/HX/Ua/oPi/T3/UMS1", "68197", 
//			"./D/LJDb0Z1rY/HX/Ua/oPi/T3/EOXiu", "24496", 
//			"./P/8DCh/vB-7OuU22/ZaF3vDVO/96JGTU2/hb/2uXj/7cKoQf", "42923", 
//			"./P/8DCh/vB-7OuU22/ZaF3vDVO/96JGTU2/hb/2uXj/GfvsXpc", "90156", 
//			"./P/8DCh/vB-7OuU22/ZaF3vDVO/96JGTU2/hb/2uXj/qaeNSELj", "80095", 
//			"./P/8DCh/vB-7OuU22/ZaF3vDVO/96JGTU2/hb/2uXj/ikzJy7Iv", "35131", 
//			"./P/8DCh/vB-7OuU22/ZaF3vDVO/96JGTU2/hb/2uXj/Z", "46742", 
//			"./P/8DCh/vB-7OuU22/ZaF3vDVO/96JGTU2/hb/2uXj/98", "32829", 
//			"./P/8DCh/vB-7OuU22/ZaF3vDVO/96JGTU2/hb/2uXj/C", "72045", 
//			"./P/8DCh/vB-7OuU22/ZaF3vDVO/96JGTU2/hb/2uXj/VwtFNO4", "98265", 
//			"./P/8DCh/vB-7OuU22/ZaF3vDVO/96JGTU2/hb/2uXj/rj6qn", "35235", 
//			"./hpQ/z7os1WNd/v91UFCW1/i/D/W/TlRQsrE1/XFAFo", "19674", 
//			"./hpQ/z7os1WNd/v91UFCW1/i/D/W/TlRQsrE1/i", "43304", 
//			"./hpQ/z7os1WNd/v91UFCW1/i/D/W/TlRQsrE1/wjJm", "86582", 
//			"./hpQ/z7os1WNd/v91UFCW1/i/D/W/TlRQsrE1/PVCWF7I", "81818", 
//			"./hpQ/z7os1WNd/v91UFCW1/i/D/W/TlRQsrE1/as", "6108", 
//			"./hpQ/z7os1WNd/v91UFCW1/i/D/W/TlRQsrE1/s", "63012", 
//			"./hpQ/z7os1WNd/v91UFCW1/i/D/W/TlRQsrE1/lIQKqh", "62970", 
//			"./hpQ/z7os1WNd/v91UFCW1/i/D/W/TlRQsrE1/75", "93543", 
//			"./hpQ/z7os1WNd/v91UFCW1/i/D/W/TlRQsrE1/8KDf", "4944", 
//			"./m-JIAS", "68618", 
//			"./uEdE", "85524", 
//			"./vW", "74442", 
//			"./DiS0/xtWrqmK9d/hPcSk9X/je/m", "54443", 
//			"./DiS0/xtWrqmK9d/hPcSk9X/je/TfCRa", "63476", 
//			"./DiS0/xtWrqmK9d/hPcSk9X/je/tR", "46990", 
//			"./TO/B1ocftS/m", "6838", 
//			"./rEzjs4/fG5b5H2-/919/JKRvpk/YUMJS99U0/q/F/Pc4Hj3D", "12021", 
//			"./rEzjs4/fG5b5H2-/919/JKRvpk/YUMJS99U0/q/F/k", "8660", 
//			"./CNVXi2c/cn/Np-hKHXQ/XoB5hE", "88405", 
//			"./CNVXi2c/cn/Np-hKHXQ/J7DNB", "23893", 
//			"./zoKW", "36286", 
//			"./DAEbi9/r0/c3NuEhJ7/3rF/iCo/Qbi/yaJ/U/wR/5pktD8", "60952", 
//			"./DAEbi9/r0/c3NuEhJ7/3rF/iCo/Qbi/yaJ/U/wR/AAQ", "31251", 
//			"./DAEbi9/r0/c3NuEhJ7/3rF/iCo/Qbi/yaJ/U/wR/tF4en", "28221", 
//			"./DAEbi9/r0/c3NuEhJ7/3rF/iCo/Qbi/yaJ/U/wR/sB395O4U", "50999", 
//			"./DAEbi9/r0/c3NuEhJ7/3rF/iCo/Qbi/yaJ/U/wR/XtI", "34567", 
//			"./DAEbi9/r0/c3NuEhJ7/3rF/iCo/Qbi/yaJ/U/wR/6UW", "83316", 
//			"./DAEbi9/r0/c3NuEhJ7/3rF/iCo/Qbi/yaJ/U/wR/XvXCYr", "13215", 
//			"./DAEbi9/r0/c3NuEhJ7/3rF/iCo/Qbi/yaJ/U/wR/tDshq", "24931", 
//			"./Hm52h9y/f/6V/Pmyq/s4eEZU/GL/jfX/071vs/w/R-2iT8GF", "35379", 
//			"./Hm52h9y/f/6V/Pmyq/s4eEZU/GL/jfX/071vs/w/3O5H1Y", "25367", 
//			"./Hm52h9y/f/6V/Pmyq/s4eEZU/GL/jfX/071vs/w/h", "36741", 
//			"./Hm52h9y/f/6V/Pmyq/s4eEZU/GL/jfX/071vs/w/xUnxB", "9210", 
//			"./Hm52h9y/f/6V/Pmyq/s4eEZU/GL/jfX/071vs/w/N2IitMQVT", "30271", 
//			"./Hm52h9y/f/6V/Pmyq/s4eEZU/GL/jfX/071vs/w/A1", "69146", 
//			"./Hm52h9y/f/6V/Pmyq/s4eEZU/GL/jfX/071vs/w/Q", "50392", 
//			"./s/ZPEwiZhj/aaWNnX8bL/BkoLcrjI/AuuoHEli8/7Cu/s", "34685", 
//			"./s/ZPEwiZhj/aaWNnX8bL/BkoLcrjI/AuuoHEli8/7Cu/sQyAnP0NS", "46701", 
//			"./s/ZPEwiZhj/aaWNnX8bL/BkoLcrjI/AuuoHEli8/7Cu/bJDheH", "6644", 
//			"./s/ZPEwiZhj/aaWNnX8bL/BkoLcrjI/AuuoHEli8/7Cu/1fY-G", "59485", 
//			"./s/ZPEwiZhj/aaWNnX8bL/BkoLcrjI/AuuoHEli8/7Cu/5T", "69877", 
//			"./s/ZPEwiZhj/aaWNnX8bL/BkoLcrjI/AuuoHEli8/7Cu/admJB", "28543", 
//			"./IdChN-4jn/Le903TUe/RfZVs0u7/f8IU7lq/-/FnlU0A/vehEaBW-/JeL/QKmIHNM4", "57910", 
//			"./IdChN-4jn/Le903TUe/RfZVs0u7/f8IU7lq/-/FnlU0A/vehEaBW-/JeL/bQ3IOuX1", "82616", 
//			"./IdChN-4jn/Le903TUe/RfZVs0u7/f8IU7lq/-/FnlU0A/vehEaBW-/JeL/uR2JJN", "85887", 
//			"./hdQFyuZa/0jp/5W82ArArV/jUM9jpT/8OWsYvTaJ/OdW", "48143", 
//			"./hdQFyuZa/0jp/5W82ArArV/jUM9jpT/8OWsYvTaJ/jw", "50263", 
//			"./hdQFyuZa/0jp/5W82ArArV/jUM9jpT/8OWsYvTaJ/p0sSF", "29196", 
//			"./hdQFyuZa/0jp/5W82ArArV/jUM9jpT/8OWsYvTaJ/sbVce", "67244", 
//			"./hdQFyuZa/0jp/5W82ArArV/jUM9jpT/8OWsYvTaJ/-PT", "48949", 
//			"./tCrlAIO/skjzhV/VV-/4svkhkJ/QQ9WXCNf", "93959", 
//			"./tCrlAIO/skjzhV/VV-/4svkhkJ/Dd55nQG5", "17470", 
//			"./tCrlAIO/skjzhV/VV-/4svkhkJ/CA", "81617", 
//			"./tCrlAIO/skjzhV/VV-/4svkhkJ/m-bzCbMh-", "27564", 
//			"./tCrlAIO/skjzhV/VV-/4svkhkJ/fLjW3m", "27685", 
//			"./tCrlAIO/skjzhV/VV-/4svkhkJ/lBRH", "60135", 
//			"./tCrlAIO/skjzhV/VV-/4svkhkJ/3c7bX", "4065", 
//			"./7Zu9pz1Xe/kn-/W9t3wKQj/SmzteLcL5/EAT/wrwIJkZo/47T9VqjSr/S2sJoFm", "81735", 
//			"./7Zu9pz1Xe/kn-/W9t3wKQj/SmzteLcL5/EAT/wrwIJkZo/47T9VqjSr/oI-A", "79901", 
//			"./7Zu9pz1Xe/kn-/W9t3wKQj/SmzteLcL5/EAT/wrwIJkZo/47T9VqjSr/PcZDjky", "93610", 
//			"./7Zu9pz1Xe/kn-/W9t3wKQj/SmzteLcL5/EAT/wrwIJkZo/47T9VqjSr/ph", "14684", 
//			"./7Zu9pz1Xe/kn-/W9t3wKQj/SmzteLcL5/EAT/wrwIJkZo/47T9VqjSr/2B5Mjt1sb", "56219", 
//			"./owj/Qo6qhxEKC/E5DWja/vSL/qj4LlAnD", "66483", 
//			"./owj/Qo6qhxEKC/E5DWja/vSL/DV9lXjX", "92171", 
//			"./owj/Qo6qhxEKC/E5DWja/vSL/tvUWY", "70689", 
//			"./owj/Qo6qhxEKC/E5DWja/vSL/pt-slwQ", "47430", 
//			"./Wb/T4xnRi8ej/ZsujE59/Y/bmhkU/1lSS4o", "13683", 
//			"./Wb/T4xnRi8ej/ZsujE59/Y/bmhkU/wn", "3649", 
//			"./r/uD8zF/dUZ/hcBhwnN/MdE73AJk/lbyQM/UIhKNsn", "57828", 
//			"./r/uD8zF/dUZ/hcBhwnN/MdE73AJk/lbyQM/-p", "79586", 
//			"./r/uD8zF/dUZ/hcBhwnN/MdE73AJk/lbyQM/OSKwq", "36923", 
//			"./r/uD8zF/dUZ/hcBhwnN/MdE73AJk/lbyQM/I7M", "69675", 
//			"./r/uD8zF/dUZ/hcBhwnN/MdE73AJk/lbyQM/52R12qJb", "80921", 
//			"./r/uD8zF/dUZ/hcBhwnN/MdE73AJk/lbyQM/NR2kKq", "92420", 
//			"./Pos/7zvZX/vIzYnQho/GVYWtq/f4nxUdoRt", "54333", 
//			"./Pos/7zvZX/vIzYnQho/GVYWtq/qajuYL", "85635", 
//			"./Pos/7zvZX/vIzYnQho/GVYWtq/GJz3GP", "13293", 
//			"./Pos/7zvZX/vIzYnQho/GVYWtq/liXf3X", "45275", 
//			"./ARxJ/fKUR43/y/rYD/QI/lHb/Hdam3F/uHISlPwl", "16113", 
//			"./ARxJ/fKUR43/y/rYD/QI/lHb/Hdam3F/j0TedU1jG", "73870", 
//			"./ARxJ/fKUR43/y/rYD/QI/lHb/Hdam3F/FUjDHq", "99758", 
//			"./ARxJ/fKUR43/y/rYD/QI/lHb/Hdam3F/kS0", "52997", 
//			"./ARxJ/fKUR43/y/rYD/QI/lHb/Hdam3F/jOSE", "6459", 
//			"./ARxJ/fKUR43/y/rYD/QI/lHb/Hdam3F/KQUp4Kh", "22853", 
//			"./ARxJ/fKUR43/y/rYD/QI/lHb/Hdam3F/mo4xHo4c", "35740", 
//			"./ARxJ/fKUR43/y/rYD/QI/lHb/Hdam3F/hKkUOuK", "11190", 
//			"./d18/sFJo/ZyUc-/LbvF", "19772", 
//			"./d18/sFJo/ZyUc-/G", "39793", 
//			"./d18/sFJo/ZyUc-/x-7RfYdIH", "30279", 
//			"./d18/sFJo/ZyUc-/HHWD", "89385", 
//			"./d18/sFJo/ZyUc-/IK4Xj5y1S", "18062", 
//			"./d18/sFJo/ZyUc-/ed4z6u", "63377", 
//			"./d18/sFJo/ZyUc-/o3", "48923", 
//			"./d18/sFJo/ZyUc-/X8eJ", "45250", 
//			"./d18/sFJo/ZyUc-/M", "78666", 
//			"./HlIYKUiA/eVLPp/1TZBSuco/Pan/iM1/6jf/KdKvGW4/HbwSvd/3jZHt", "65667", 
//			"./Bqbc2feOa/x/-/d/5KcS/ROcb/s/7b8KmJ44K/VCEGj6B3e/YRkwNb", "7408", 
//			"./Bqbc2feOa/x/-/d/5KcS/ROcb/s/7b8KmJ44K/VCEGj6B3e/wMXAjfDx", "52645", 
//			"./kxPt5jk3/7/c/cvNui/x", "73849", 
//			"./IemObNX", "97508", 
//			"./sedijvdHN", "57483", 
//			"./IbEo-dHjI", "30957", 
//			"./BsuJdo9B", "84978", 
//			"./tqJW8q", "10044", 
//			"./UOq", "19643", 
//			"./3Gft", "43069", 
//			"./HSoDBT", "66440", 
//			"./tTyixU-/4b2-y/fVF2UkFL", "78786", 
//			"./tTyixU-/4b2-y/K15h8CRH", "10926", 
//			"./tTyixU-/4b2-y/7QkWivj1", "68575", 
//			"./tTyixU-/4b2-y/eY", "88823", 
//			"./tTyixU-/4b2-y/J9", "81357", 
//			"./tTyixU-/4b2-y/RwxkPvJZ", "91591", 
//			"./tTyixU-/4b2-y/5", "27613", 
//			"./tTyixU-/4b2-y/T4Clo3m", "6115", 
//			"./tTyixU-/4b2-y/jcvWlOa", "28928", 
//			"./73/ePMs/7UyjOs/hScJo1e/1j", "94756", 
//			"./73/ePMs/7UyjOs/hScJo1e/vAjp", "80686", 
//			"./73/ePMs/7UyjOs/hScJo1e/Ln5OjR6", "62062", 
//			"./73/ePMs/7UyjOs/hScJo1e/afCn", "1017", 
//			"./73/ePMs/7UyjOs/hScJo1e/h", "58908", 
//			"./w22ukuV/99/szs/urkZXdvp/j0s/rwOQ1wi/fP-oO7WOf/jvZ/qQiR-Oo", "49067", 
//			"./ApGYOv/N5Ao0/qf/eszXjSrUI/W3xuuVdc9", "13487", 
//			"./ApGYOv/N5Ao0/qf/eszXjSrUI/XKj", "79637", 
//			"./ApGYOv/N5Ao0/qf/eszXjSrUI/yVmW", "31929", 
//			"./s1v2K/Gw/AfSqEOIX/BYwWyj9/z/lrAX/sCcmL74m/AWMYdJ", "77102", 
//			"./s1v2K/Gw/AfSqEOIX/BYwWyj9/z/lrAX/sCcmL74m/icSjz", "85783", 
//			"./s1v2K/Gw/AfSqEOIX/BYwWyj9/z/lrAX/sCcmL74m/v", "28926", 
//			"./s1v2K/Gw/AfSqEOIX/BYwWyj9/z/lrAX/sCcmL74m/mWR55", "62599", 
//			"./fTRnHMq/s1/Yrrh/wxxnky/zaqmIuo/ZkLfI", "25977", 
//			"./fTRnHMq/s1/Yrrh/wxxnky/zaqmIuo/1RKhjU", "74903", 
//			"./fTRnHMq/s1/Yrrh/wxxnky/zaqmIuo/eBDP2Jb", "65963", 
//			"./fTRnHMq/s1/Yrrh/wxxnky/zaqmIuo/FlZpsZ", "38268", 
//			"./fTRnHMq/s1/Yrrh/wxxnky/zaqmIuo/4qj", "450", 
//			"./X/4pOS2/RJ/aekBI", "57278", 
//			"./X/4pOS2/RJ/KjOQTjx", "74077", 
//			"./X/4pOS2/RJ/6", "4226", 
//			"./bms/x2kUEm6m/aoPHfF/T-S-N", "97378", 
//			"./bms/x2kUEm6m/aoPHfF/a0-Poo", "72177", 
//			"./bms/x2kUEm6m/aoPHfF/wx3oxT", "19340", 
//			"./bms/x2kUEm6m/aoPHfF/mkmqy1Q", "22463", 
//			"./bms/x2kUEm6m/aoPHfF/6VUY", "69146", 
//			"./bms/x2kUEm6m/aoPHfF/X0qp83", "79590", 
//			"./bms/x2kUEm6m/aoPHfF/0e", "84433", 
//			"./bms/x2kUEm6m/aoPHfF/F", "37077", 
//			"./M1uAmV33i", "19513", 
//			"./6p4D", "33915", 
//			"./RRqV9eIA9", "14193", 
//			"./wu9KV9U", "19684", 
//			"./e1cT8F", "23040", 
//			"./Db", "33477", 
//			"./5ZbQHN4WG", "55356", 
//			"./K", "23754", 
//			"./G3", "6252", 
//			"./u-", "54159", 
//			"./k-GNcx", "82183", 
//			"./F", "7721", 
//			"./FdzGGAI1R/f/Kdne/PUdIN/7WKWYb4Qq/0ZK8by/VSiR5/XA/VrDOB3", "31261", 
//			"./FdzGGAI1R/f/Kdne/PUdIN/7WKWYb4Qq/0ZK8by/VSiR5/XA/7e4qkKq", "62771", 
//			"./FdzGGAI1R/f/Kdne/PUdIN/7WKWYb4Qq/0ZK8by/VSiR5/XA/3DmzBss", "85854", 
//			"./FdzGGAI1R/f/Kdne/PUdIN/7WKWYb4Qq/0ZK8by/VSiR5/XA/1u2X", "97314" 
//	};
	
}
