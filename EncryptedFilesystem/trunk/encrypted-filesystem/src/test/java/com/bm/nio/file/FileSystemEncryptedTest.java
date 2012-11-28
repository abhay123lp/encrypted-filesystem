package com.bm.nio.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.ByteChannel;
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

import org.junit.Assert;
import org.junit.Test;

import com.sun.nio.zipfs.ZipFileSystem;
import com.sun.nio.zipfs.ZipFileSystemProvider;

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
	public void newGetFilesystem(){

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
			// === === ===
			
			String encSubPath = sandboxPath + "/enc23/dir";
			newTempDir(encSubPath);
			FileSystem fs = fpe.getFileSystem(uriEncrypted(pathToURI(encSubPath)));
			System.out.println(fs.getRootDirectories());
			//enc1.delete();
			
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
//		try {
//			Path p = Paths.get(new URI("file:///test"));
//			Path p1 = Paths.get(new URI("file:///test/test1"));
//			System.out.println(p1.startsWith(p));
//		} catch (URISyntaxException e) {
//			e.printStackTrace();
//		}
	}
	
	//@Test
	public void test(){
		System.out.println("39".compareTo("3d"));
	}
	
}
