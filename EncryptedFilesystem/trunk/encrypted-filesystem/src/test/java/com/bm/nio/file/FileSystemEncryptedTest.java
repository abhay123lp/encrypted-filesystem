package com.bm.nio.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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

import com.sun.nio.zipfs.ZipFileSystem;
import com.sun.nio.zipfs.ZipFileSystemProvider;
import com.sun.nio.zipfs.ZipPath;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

public class FileSystemEncryptedTest {

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		Assert.assertTrue(true);
	}
	
	
	String sandboxPath = "./src/test/sandbox/";
	
	private void delete(File file){
		if (file.isFile())
			file.delete();
		if (file.isDirectory()){
			for (File f : file.listFiles())
				delete(f);
			file.delete();
		}
	}
	private File newTempDir(String path){
		File dir = new File(path);
		if (dir.exists())
			//dir.delete();
			delete(dir);
		dir.mkdirs();
		return dir;
	}
	
	private URI fileToURI(File f) throws IOException{
		return Paths.get(f.getCanonicalPath()).toUri();
	}
	
	private URI pathToURI(String path) throws IOException{
		return fileToURI(new File(path));
	}
	
	private URI uriEncrypted(URI u) throws URISyntaxException{
		return new URI("encrypted:" + u);
	}
	
	private FileSystem newTempFieSystem(FileSystemProviderEncrypted fpe, String path) throws IOException, URISyntaxException{
		File file = newTempDir(path); 
		//URI uri1File = Paths.get(file.getCanonicalPath()).toUri();
		URI uri1Encrypted = uriEncrypted(fileToURI(file));
		return fpe.newFileSystem(uri1Encrypted, new HashMap<String, Object>());
	}
	
	private final FileSystemProviderEncrypted mFspe = getEncryptedProvider();
	
	private FileSystemProviderEncrypted getEncryptedProvider(){
		final String scheme = new FileSystemProviderEncrypted().getScheme();
		for (FileSystemProvider f : FileSystemProvider.installedProviders()){
			if (f.getScheme().endsWith(scheme) && f instanceof FileSystemProviderEncrypted)
				return (FileSystemProviderEncrypted)f;
		}
		return new FileSystemProviderEncrypted();
	}
	
	/**
	 * Checks encrypted provider is among installed 
	 */
	@Test
	public void listInstalled(){
		String scheme = new FileSystemProviderEncrypted().getScheme();
		boolean found = false;
		for (FileSystemProvider fsp : FileSystemProvider.installedProviders())
			found = found || fsp.getScheme().equals(scheme);
		Assert.assertTrue(found);
	}
	
	
	/**
	 * Test functions of creating and getting filesystems
	 */
	@Test
	public void newGetCloseFilesystem(){

		FileSystemProviderEncrypted fpe = mFspe;
		
//		try {
//			for (int i = 0; i < Paths.get(pathToURI(".")).getNameCount(); i ++)
//				System.out.println(Paths.get(pathToURI(".")).getName(i));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		try {
//			File sandbox = new File(sandboxPath);
//			String currDir = sandbox.getCanonicalPath();
//			Path p = Paths.get(currDir);
//			URI uriFile = p.toUri();
//			URI uriEncrypted = new URI("encrypted:" + uriFile);
			for (int i = 0; i < 100; i ++)
				newTempFieSystem(fpe, sandboxPath + "/enc" + i);
			// === duplication ===
			boolean duplicateError = false;
			try {
				newTempFieSystem(fpe, sandboxPath + "/enc3");
			} catch (FileSystemAlreadyExistsException e) {
				duplicateError = true;
			}
			Assert.assertTrue(duplicateError);//check error in case of duplication
			// === === ===
			
			// === nested encrypted filesystem not allowed ===
			boolean nestedException = false;
			try {
				newTempFieSystem(fpe, sandboxPath + "/enc3/dir");
			} catch (FileSystemAlreadyExistsException e) {
				nestedException = true;
			}
			Assert.assertTrue(nestedException);//check error in case of duplication
			// === nested - high level filesystem not allowed ===
			nestedException = false;
			try {
				newTempFieSystem(fpe, sandboxPath + "/enc300/dir");
				//TOD1O: implement this check in filesystemprovider!
				newTempFieSystem(fpe, sandboxPath + "/enc300");
			} catch (FileSystemAlreadyExistsException e) {
				nestedException = true;
			}
			Assert.assertTrue(nestedException);//check error in case of duplication
			// === === ===
			
			String encSubPath = sandboxPath + "/enc23/dir";
			newTempDir(encSubPath);
			FileSystem fs = fpe.getFileSystem(uriEncrypted(pathToURI(encSubPath)));
			for (Path p : fs.getRootDirectories()){
				Assert.assertTrue(p.toString().endsWith("enc23"));
				//System.out.println(p);//D:\prog\workspace\encrypted-filesystem-trunk\src\test\sandbox\enc23
			}
			//enc1.delete();
			
			// === closing - should not be exception ===
			boolean exception = false;
			try {
				FileSystem fsClose = newTempFieSystem(fpe, sandboxPath + "/encClose/dir");
				fsClose.close();
				newTempFieSystem(fpe, sandboxPath + "/encClose/dir");
			} catch (Exception e) {
				exception = true;
			}
			Assert.assertFalse(exception);
			// === === ===
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
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
	
	//@Test
//	public void crypterTest() throws Exception{
////        Crypter decrypter = new Crypter("t5fbrxrb");
////        String encrypted = decrypter.encrypt("12345");//decrypter.encrypt("the quick brown fox jumps over the lazy dog");
////        String decrypted = decrypter.decrypt(encrypted);
////        System.out.println(decrypted);
//		String text = "12345678901234567890";
//		HashMap<String, Object> props = new HashMap<String, Object>();
//		props.put(OutputStreamCrypto.BLOCK_SIZE, new Integer(8));
//		props.put(OutputStreamCrypto.PASSWORD, "pwd".toCharArray());
//		ByteOutputStream bo = new ByteOutputStream();
//		
//		OutputStreamCrypto os = new OutputStreamCrypto(bo, props);
//		os.write(text.getBytes());
//		os.close();
//		//-------
//		String res = new String(bo.getBytes(), 0, bo.getCount());
//		System.out.println(res);
//		System.out.println("Encrypted Data " + DatatypeConverter.printHexBinary(bo.getBytes()));
//		//7E6FFA1B8478C294766DF81D827AC09A7865F01F827AC09A0
//		//7E6FFA1B8478C294ADC05465B7788091C01B83944D9DC915DA2FE570
//		//7E6FFA1B8478C294ADC05465B7788091C01B8394
//		Assert.assertTrue(text.equals(res));
//	}
	
	@After
	public void clean(){
		File f = new File(sandboxPath);
		if (f.isDirectory())
			deleteFolderContents(f);
	}
	
	public static void deleteFolderContents(File folder) {
		deleteFolderInternal(folder, true);
	}
	
	private static void deleteFolderInternal(File folder, boolean isContentsOnly) {
	    File[] files = folder.listFiles();
	    if(files!=null) {
	        for(File f: files) {
	            if(f.isDirectory()) {
	            	deleteFolderInternal(f, false);
	            } else {
	                f.delete();
	            }
	        }
	    }
	    if (!isContentsOnly)
	    	folder.delete();
	}
}
