package com.bm.nio.file;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

import org.junit.Assert;
import org.junit.Test;

public class FileSystemEncryptedTest {

	@Test
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
}
