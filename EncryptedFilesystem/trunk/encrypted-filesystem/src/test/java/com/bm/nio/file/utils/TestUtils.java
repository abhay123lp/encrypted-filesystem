package com.bm.nio.file.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;

import javax.management.RuntimeErrorException;

import com.bm.nio.file.FileSystemEncrypted;
import com.bm.nio.file.FileSystemProviderEncrypted;

public class TestUtils {
	
	public static final String SANDBOX_PATH = "./src/test/sandbox/";
	public static final char[] DEFAULT_PASSWORD = "12345".toCharArray();
	
	public static HashMap<String, Object> newEnv(){
		HashMap<String, Object> env = new HashMap<String, Object>();
		env.put(FileSystemEncrypted.FileSystemEncryptedEnvParams.ENV_PASSWORD, DEFAULT_PASSWORD);
		return env;
	}
	
	public static void deleteFilesystems(FileSystemProviderEncrypted provider) throws IOException {
		for (FileSystemEncrypted fe : provider.getFileSystems()){
			for (Path p : fe.getRootDirectories())
				Files.walkFileTree(p, new FileVisitor<Path>() {

					@Override
					public FileVisitResult preVisitDirectory(Path dir,
							BasicFileAttributes attrs) throws IOException {
						//System.out.println(dir);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file,
							BasicFileAttributes attrs) throws IOException {
			            Files.delete(file);
			            return FileVisitResult.CONTINUE;
			        }

					@Override
					public FileVisitResult visitFileFailed(Path file,
							IOException exc) throws IOException {
						//consider using deleteIfExists
						//otherwise will throw an error if file is missing
						Files.delete(file);
			            return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir,
							IOException exc) throws IOException {
				           if (exc == null)
				            {
				        	   Files.delete(dir);
				        	   return FileVisitResult.CONTINUE;
				            }
				            else
				            {
				                // directory iteration failed; propagate exception
				                throw exc;
				            }
					}

				});
		}
	}
	
	
	public static void delete(File file){
		if (file.isFile())
			file.delete();
		if (file.isDirectory()){
			for (File f : file.listFiles())
				delete(f);
			file.delete();
		}
	}
	
	// ===
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
	// ===
	
	public static File newTempDir(String path){
		File dir = new File(path);
		if (dir.exists())
			return dir;
//			delete(dir);
		//can return false if Far manager has previously opened path
		//when it was recently deleted
		int i = 0;
		while (!dir.mkdirs()){
			i ++;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			if (i == 10)
				throw new RuntimeException("Can't create directory for testing: " + path);
		}
		return dir;
	}
	
	public static URI fileToURI(File f) throws IOException{
		return Paths.get(f.getCanonicalPath()).toUri();
	}
	
	public static URI pathToURI(String path) throws IOException{
		return fileToURI(new File(path));
	}
	
	public static URI uriEncrypted(URI u) throws URISyntaxException{
		return new URI("encrypted:" + u);
	}
	
	public static FileSystemProviderEncrypted getEncryptedProvider(){
		final String scheme = new FileSystemProviderEncrypted().getScheme();
		for (FileSystemProvider f : FileSystemProvider.installedProviders()){
			if (f.getScheme().endsWith(scheme) && f instanceof FileSystemProviderEncrypted)
				return (FileSystemProviderEncrypted)f;
		}
		return new FileSystemProviderEncrypted();
	}
	
	public static FileSystem newTempFieSystem(FileSystemProviderEncrypted fpe, String path) throws IOException, URISyntaxException{
		File file = TestUtils.newTempDir(path); 
		URI uri1Encrypted = TestUtils.uriEncrypted(TestUtils.fileToURI(file));
		return fpe.newFileSystem(uri1Encrypted, newEnv());
	}

}