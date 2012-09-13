package com.bm.nio.file;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.spi.FileSystemProvider;

import org.junit.Assert;
import org.junit.Test;

public class FileSystemEncryptedTest {

	@Test
	public void encryptDecrypt(){
		//FileSystemProviderEncrypted f = new FileSystemProviderEncrypted();
		
		try {
			URI u = new URI("file://test.txt");
			System.out.println(u.getScheme());
			for (FileSystemProvider provider: FileSystemProvider.installedProviders()) {
	            
	        }			
			FileSystem f = FileSystems.newFileSystem(u, null);
			
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Assert.assertTrue(true);
	}
}
