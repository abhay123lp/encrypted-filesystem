package com.bm.nio.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchService;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.bm.nio.file.utils.TestUtils;

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
	
	@Test
	public void testSupportedFileAttributeViews() throws Exception {
		FileSystem fsTest = TestUtils.newTempFieSystem(mFspe, TestUtils.SANDBOX_PATH + "/encGetPath");
		Path p = fsTest.getPath("dir1", "dir2");
		Files.createDirectories(p);
		Assert.assertEquals(fsTest.supportedFileAttributeViews(), ((PathEncrypted)p).getFileSystem().supportedFileAttributeViews());
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
}
