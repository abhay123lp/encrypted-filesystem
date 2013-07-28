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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

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
		return newTempFieSystem(fpe, path, newEnv());
	}
	public static FileSystem newTempFieSystem(FileSystemProviderEncrypted fpe, String path, Map<String, ?> env) throws IOException, URISyntaxException{
		File file = TestUtils.newTempDir(path); 
		URI uri1Encrypted = TestUtils.uriEncrypted(TestUtils.fileToURI(file));
		return fpe.newFileSystem(uri1Encrypted, env);
	}
	
	//=== TIMER UTILS ===
	private static class PerformanceBean {
		AtomicLong time = new AtomicLong(0);
		AtomicLong hits = new AtomicLong(0);
	}
	private static final Map<String, PerformanceBean> timers = new HashMap<String, PerformanceBean>();
	private static final ThreadLocal<Map<String, Long>> timersLocal= new ThreadLocal<Map<String, Long>>(){
		protected java.util.Map<String,Long> initialValue() {
			return new HashMap<String, Long>();
		};
		};
		
	private static void createTimerIfMissing(String timer){
		if (timers.get(timer) == null){
			synchronized (timers) {
				if (timers.get(timer) == null)
					timers.put(timer, new PerformanceBean());
			}
		}
	}
		
	private static void addTime(String timer, long time){
		createTimerIfMissing(timer);
		timers.get(timer).time.getAndAdd(time);
		timers.get(timer).hits.getAndIncrement();
	}

	public static void resetTime(String timer){
		createTimerIfMissing(timer);
		final Long time = - timers.get(timer).time.get();
		timers.get(timer).time.getAndAdd(time);
		final Long hits = - timers.get(timer).hits.get();
		timers.get(timer).hits.addAndGet(hits);
	}

	public static void startTime(String timer, long time){
		Map<String, Long> timers = timersLocal.get();
		if (timers.get(timer) == null){
			timers.put(timer, new Long(0));
		}
		timers.put(timer, time);
	}
	
	public static void startTime(String timer){
		startTime(timer, System.currentTimeMillis());
	}

	public static void endTime(String timer, long time){
		Map<String, Long> timers = timersLocal.get();
		if (timers.get(timer) == null){
			timers.put(timer, new Long(0));
		}
		final long l = timers.get(timer);
		timers.put(timer, time);
		addTime(timer, time - l);
	}
	
	public static void endTime(String timer){
		endTime(timer, System.currentTimeMillis());
	}
	
	
	public static Long getTime(String timer){
		createTimerIfMissing(timer);
		return timers.get(timer).time.get();
	}
	
	public static Long getHits(String timer){
		createTimerIfMissing(timer);
		return timers.get(timer).hits.get();
	}
	
	public static String printTime(String timer){
		return timer + ": " + getTime(timer) + "; hits: " + getHits(timer);
	}

}
