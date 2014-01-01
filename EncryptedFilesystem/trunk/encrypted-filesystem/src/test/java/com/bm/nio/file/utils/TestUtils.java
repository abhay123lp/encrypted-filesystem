package com.bm.nio.file.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	
//	private static void deleteBlocking (File f){
//		try {
//			Files.deleteIfExists(f.toPath());
//			while (Files.exists(f.toPath())){
//				Files.deleteIfExists(f.toPath());
//				Thread.sleep(100);
//			}
//		} catch (IOException|InterruptedException e1) {
//			e1.printStackTrace();
//		}
//	}
//	
	private static void deleteFolderInternal(File folder, boolean isContentsOnly){
	    File[] files = folder.listFiles();
	    if(files!=null) {
	        for(File f: files) {
	            if(f.isDirectory()) {
	            	deleteFolderInternal(f, false);
	            } else {
//	                Files.delete(f.toPath());
	                f.delete();
//	                deleteBlocking(f);
	            }
	        }
	    }
	    if (!isContentsOnly)
//            Files.delete(folder.toPath());	    	
	    	folder.delete();
//	    	deleteBlocking(folder);
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
		return newTempFieSystem(fpe, path, env, false);
	}
	
	public static FileSystem newTempFieSystem(FileSystemProviderEncrypted fpe, String path, Map<String, ?> env, boolean recreateExisting) throws IOException, URISyntaxException{
		File file = TestUtils.newTempDir(path); 
		URI uri1Encrypted = TestUtils.uriEncrypted(TestUtils.fileToURI(file));
		FileSystem res;
		try {
			res = fpe.newFileSystem(uri1Encrypted, env);
		} catch (FileSystemAlreadyExistsException e) {
			if (!recreateExisting)
				throw e;
			res = fpe.getFileSystem(uri1Encrypted);
			res.close();
			res = fpe.newFileSystem(uri1Encrypted, env);
		}
		return res;
	}
	
	//=== TIMER UTILS ===
	private static class TimerBean {
		String name;
		AtomicLong time = new AtomicLong(0);
		AtomicLong hits = new AtomicLong(0);
		String group = null;
	}
	private static final Map<String, List<String>> timerGroups = new HashMap<String, List<String>>();
	private static final Map<String, TimerBean> timersMap = new HashMap<String, TimerBean>();
	private static final ThreadLocal<Map<String, Long>> timersLocal= new ThreadLocal<Map<String, Long>>(){
		protected java.util.Map<String,Long> initialValue() {
			return new HashMap<String, Long>();
		};
		};
		
	private static void createTimerIfMissing(String timer){
		if (timersMap.get(timer) == null){
			synchronized (timersMap) {
				if (timersMap.get(timer) == null){
					final TimerBean tb = new TimerBean();
					tb.name = timer;
					timersMap.put(timer, tb);
				}
			}
		}
	}
		
	private static void addTime(String timer, long time){
		createTimerIfMissing(timer);
		timersMap.get(timer).time.getAndAdd(time);
		timersMap.get(timer).hits.getAndIncrement();
	}

	public static void resetTime(String timer){
		createTimerIfMissing(timer);
		final Long time = - timersMap.get(timer).time.get();
		timersMap.get(timer).time.getAndAdd(time);
		final Long hits = - timersMap.get(timer).hits.get();
		timersMap.get(timer).hits.addAndGet(hits);
	}

	protected static void addToGroup(String timer, String timerGroup){
		createTimerIfMissing(timer);
		if (timersMap.get(timer).group != timerGroup){
			timersMap.get(timer).group = timerGroup;
			List<String> timers = timerGroups.get(timerGroup);
			if (timers == null){
				timers = new ArrayList<String>();
				timerGroups.put(timerGroup, timers);
			}
			timers.add(timer);
		}
	}
	
	public static void startTime(String timer, long time, String timerGroup){
		addToGroup(timer, timerGroup);
		Map<String, Long> timers = timersLocal.get();
		if (timers.get(timer) == null){
			timers.put(timer, new Long(0));
		}
		timers.put(timer, time);
	}
	
	public static void startTime(String timer){
		startTime(timer, null);
	}

	//TODO: consider using parentTimer instead of timerGroup
	public static void startTime(String timer, String timerGroup){
		startTime(timer, System.currentTimeMillis(), timerGroup);
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
		return timersMap.get(timer).time.get();
	}
	
	public static Long getHits(String timer){
		createTimerIfMissing(timer);
		return timersMap.get(timer).hits.get();
	}
	
	public static String printTime(boolean reset, String... timers){
		String res = "";
		Long total = 0L;
		for (String timer : timers){
			total += getTime(timer);
			res += timer + ": " + getTime(timer) + "; hits: " + getHits(timer) + "    ";
			if (reset)
				resetTime(timer);
		}
		return res + "    total: " + total;
	}

	public static String printTime(String... timers){
		final String res = printTime(false, timers);
		return res;
	}

	public static String printTimeGroup(String timerGroup){
		final String res = printTimeGroup(false, timerGroup);
		return res;
	}

	public static String printTimeGroup(boolean reset, String timerGroup){
		List<String> timers = timerGroups.get(timerGroup);
		final String res = printTime(reset, timers.toArray(new String [0]));
		return res;
	}
}
