package com.bm.nio.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.crypto.Cipher;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.bm.nio.channels.SeekableByteChannelEncrypted;
import com.bm.nio.file.utils.TestUtils;
import com.bm.nio.utils.CipherUtils;
import com.bm.nio.utils.CipherUtils.CipherUtilsImpl;
import com.bm.nio.utils.impl.CipherUtilsImplFast;
import com.bm.nio.utils.impl.CipherUtilsImplStandard;

public class IntegrationTest {
	private final FileSystemProviderEncrypted mFspe = TestUtils.getEncryptedProvider();
	
	
	public static final String TEST_COPY_SRC = "./src/test/copy_src/";
	public static final String TEST_COPY_TARGET = "./src/test/copy_target/";
	
	static{
		//delete everything before starting tests
		File f = new File(TestUtils.SANDBOX_PATH);
		if (f.isDirectory())
			TestUtils.deleteFolderContents(f);
	}

//	@Test
//	public void testCopy(){
//		System.out.println(System.getProperty("java.version"));
//	}
	
	@Test
	public void testCopy() throws Exception {
		generateTestFiles(TEST_COPY_SRC);

		ConfigEncrypted conf = new ConfigEncrypted();
//		conf.setBlockSize(3);//TODO: also try 11
		conf.setTransformation("AES/CBC/PKCS5Padding");//TODO: try another encryptions 
		testCopyInternal(TEST_COPY_SRC, TestUtils.SANDBOX_PATH + "/testCopy",
				TestUtils.SANDBOX_PATH + "/testCopy1", TEST_COPY_TARGET, conf);

	}
	
	/**
	 * Creates test files and directory structure, hardcoded.
	 */
	private void generateTestFiles(String path) throws Exception {
		//TODO:
		TestUtils.deleteFolderContents(new File(path));
		Random r = new Random();
		for (int i = 0; i < TEST_FILES.length; i +=2){
			String name = TEST_FILES[i];
			int len = Integer.valueOf(TEST_FILES[i + 1]) * 10;
			Path p = Paths.get(path + name).normalize();
	
			try {
			Files.createDirectories(p);
			Files.delete(p);
			p = Files.createFile(p);
			} catch (Exception e) {
				System.out.println(e);
			}
			
			try(SeekableByteChannel bc = Files.newByteChannel(p, StandardOpenOption.WRITE)){
				byte [] data = new byte[len];
				for (int j = 0; j < len; j ++){
					data[j] = (byte)r.nextInt(Byte.MAX_VALUE);
				}
//					os.write(r.nextInt(Byte.MAX_VALUE));
//					os.write(data);
				bc.write(ByteBuffer.wrap(data));
			}
				
		}

	}
	
	//@Test
	public void generateRandomFiles(){
		generateRandomFilesInternal(200, 10, 100000, 10, 10);
	}
	
	private void generateRandomFilesInternal(int filesCnt, int maxNameLen, int maxFileLen, int maxDepth, int maxFilesInOneFolder){
		final String CHARS = "abcdefjhijklmnopqrstuvwxyzABCDEFJHIGKLMNOPQRSTUVWXYZ1234567890-_";
		Random r = new Random();
		//
		int filesProcesed = 0;
		while(filesProcesed < filesCnt)
		{
			String path = getRandomPath(CHARS, maxDepth, maxNameLen);
			int filesToCreate = r.nextInt(maxFilesInOneFolder);
			if (filesToCreate > filesCnt - filesProcesed){//limit is reached
				filesToCreate = filesCnt - filesProcesed;
			}
			//do create
			for (int j = 0; j < filesToCreate; j ++){
				String file = path + getRandomString(CHARS, maxNameLen);
				System.out.println("\"" + file + "\", \"" + r.nextInt(maxFileLen) + "\", ");
			}
			//
			filesProcesed += filesToCreate;
		}
	}
	
	private String getRandomPath(String chars, int maxDepth, int maxNameLen){
		String res = "./";
		int len = new Random().nextInt(maxDepth);
		for (int i = 0; i < len; i ++){
			res += getRandomString(chars, maxNameLen) + "/";
		}
		return res;
	}
	
	private String getRandomString(String chars, int maxLen){
		String res = "";
		Random r = new Random();
		int len = r.nextInt(maxLen - 1) + 1;
		for (int i = 0; i < len; i ++){
			res += chars.charAt(r.nextInt(chars.length() - 1));
		}
		return res;
	}
	
	
	
	class CipherUtilsImplMeasure extends CipherUtilsImplStandard {
		private long decAmt = 0;
		private long encAmt = 0;
		
		@Override
		public byte[] decryptBlockImpl(Cipher decipher, byte[] bufEnc,
				int start, int len) throws GeneralSecurityException {
			TestUtils.startTime("decrypt", "group");
			byte [] res = super.decryptBlockImpl(decipher, bufEnc, start, len);
			decAmt += len;
			TestUtils.endTime("decrypt");
			return res;
		}

		@Override
		public byte[] encryptBlockImpl(Cipher encipher, byte[] bufPlain,
				int start, int len) throws GeneralSecurityException {
			TestUtils.startTime("encrypt", "group");
			byte [] res = super.encryptBlockImpl(encipher, bufPlain, start, len);
			encAmt += len;
			TestUtils.endTime("encrypt");
			return res;
		}

		@Override
		public int getEncAmtImpl(Cipher encipher, int decAmt) {
			return super.getEncAmtImpl(encipher, decAmt);
		}

//		@Override
//		public String encryptNameImpl(String plainName, Cipher encipher)
//				throws GeneralSecurityException {
//			return super.encryptNameImpl(plainName, encipher);
//		}
//
//		@Override
//		public String decryptName(String encName, Cipher decipher)
//				throws GeneralSecurityException {
//			return super.decryptName(encName, decipher);
//		}
	};	
	/**
	 * Copies between folders: copySrc(unencrypted)-->enc1(encrypted)-->enc2(encrypted)-->copyTarget(unencrypted)
	 * @param conf2 - configuration for enc2 filesystem
	 * @throws Exception
	 */
	public void testCopyInternal(final String srcPath, final String enc1Path,
			final String enc2Path, final String targetPath,
			ConfigEncrypted conf2) throws Exception {
		
		CipherUtilsImplMeasure impl = new CipherUtilsImplMeasure();
		CipherUtils.setImpl(impl);
		
		//=== INIT ===
		Map<String, Object> env2 = new HashMap<String, Object>();
		env2.put(FileSystemEncrypted.FileSystemEncryptedEnvParams.ENV_CONFIG, conf2);
		env2.put(FileSystemEncrypted.FileSystemEncryptedEnvParams.ENV_PASSWORD, "password1".toCharArray());

		ConfigEncrypted ce = new ConfigEncrypted();
		Map<String, Object> env1 = new HashMap<String, Object>();
		env1.put(FileSystemEncrypted.FileSystemEncryptedEnvParams.ENV_CONFIG, ce);
		env1.put(FileSystemEncrypted.FileSystemEncryptedEnvParams.ENV_PASSWORD, "password1".toCharArray());
		
//		ce.setBlockSize(100);
		
		Path src = Paths.get(srcPath);
		Path enc = TestUtils.newTempFieSystem(mFspe, enc1Path, env1).getPath("/");
		//another type of encryption
		Path enc1 = TestUtils.newTempFieSystem(mFspe, enc2Path, env2).getPath("/");
		
		Path target = Paths.get(targetPath);
		//Path 
		//prepare
		//TestUtils.deleteFolderContents(enc.toFile());
		//TestUtils.deleteFolderContents(enc1.toFile());
		TestUtils.startTime("Copy0");
		copyDirectory(src, target);
		TestUtils.endTime("Copy0");
		System.out.println(TestUtils.printTime("Copy0"));
		TestUtils.deleteFolderContents(target.toFile());
		System.out.println(TestUtils.printTimeGroup(true, "group"));
		//

		TestUtils.startTime("Copy1");
		impl.decAmt = 0;
		copyDirectory(src, enc);//DONE: does not work correctly with block ciphers. Fixed bug in write function
		TestUtils.endTime("Copy1");
		System.out.println(TestUtils.printTime("Copy1"));
		System.out.println(TestUtils.printTimeGroup(true, "group"));
		
		TestUtils.startTime("Copy2");
		impl.decAmt = 0;
		copyDirectory(enc, enc1);//DONE: does not copying correctly. Fixed bug in write function
		TestUtils.endTime("Copy2");
		System.out.println(TestUtils.printTime("Copy2"));
		System.out.println(TestUtils.printTimeGroup(true, "group"));
		
		TestUtils.startTime("Copy3");
		impl.decAmt = 0;
		copyDirectory(enc1, target);//DONE: make it work
		TestUtils.endTime("Copy3");
		System.out.println(TestUtils.printTime("Copy3"));
		System.out.println(TestUtils.printTimeGroup(true, "group"));
		
		
		TestUtils.startTime("Compare1");
		Assert.assertTrue(equals(src, target));
		TestUtils.endTime("Compare1");
		System.out.println(TestUtils.printTime("Compare1"));
		System.out.println(TestUtils.printTimeGroup(true, "group"));
		
		TestUtils.startTime("Compare2");
		Assert.assertTrue(equals(src, enc));
		TestUtils.endTime("Compare2");
		System.out.println(TestUtils.printTime("Compare2"));
		System.out.println(TestUtils.printTimeGroup(true, "group"));
		
		TestUtils.startTime("Compare3");
		impl.decAmt = 0;
		Assert.assertTrue(equals(enc, target));
		TestUtils.endTime("Compare3");
		System.out.println(TestUtils.printTime("Compare3"));
		System.out.println(impl.decAmt);
		System.out.println(TestUtils.printTimeGroup(true, "group"));
		
		TestUtils.startTime("Compare4");
		Assert.assertTrue(equals(enc, enc1));
		TestUtils.endTime("Compare4");
		System.out.println(TestUtils.printTime("Compare4"));
		System.out.println(TestUtils.printTimeGroup(true, "group"));
	}
	
	public boolean equals(Path sourceLocation , Path targetLocation) throws IOException{
		try {
			Files.walkFileTree(sourceLocation, new DirCompareVisitor(sourceLocation, targetLocation));
			Files.walkFileTree(targetLocation, new DirCompareVisitor(targetLocation, sourceLocation));
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
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
		                ch1.read(buf1);
		                ch2.read(buf2);
		                int n1 = buf1.position();
		                int n2 = buf2.position();
		                
		                if (n1 == 0 || n2 == 0) return n1 == n2;

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
	 
	public void copyDirectory(Path sourceLocation , Path targetLocation)
		    throws IOException {
		Files.walkFileTree(sourceLocation, new CopyDirVisitor(sourceLocation, targetLocation));
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
				TestUtils.startTime("CopyDirectoryResolve", "group");

		    	final Path p1 = toPath.resolve(fromPath.relativize(dir).toString());
				TestUtils.endTime("CopyDirectoryResolve");
		        if(!Files.exists(p1)){
		            Files.createDirectory(p1);
		        }
		        return FileVisitResult.CONTINUE;
		    }

		    @Override
		    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				TestUtils.startTime("CopyFilesResolve", "group");
		    	final Path p1 = toPath.resolve(fromPath.relativize(file).toString());
				TestUtils.endTime("CopyFilesResolve");
				FileSystemProviderEncrypted fpe1 = new FileSystemProviderEncrypted();
				fpe1.copy(file, p1, copyOption);
//		        Files.copy(file, p1, copyOption);
		        return FileVisitResult.CONTINUE;
		    }
		}
	 
	@After
	public void clean() throws IOException {
		TestUtils.deleteFilesystems(mFspe);
		File f = new File(TestUtils.SANDBOX_PATH);
		if (f.isDirectory())
			TestUtils.deleteFolderContents(f);
	}
	
	final String [] TEST_FILES = new String [] {//"./1.txt", "100"
			
			
			"./ujsl9Jw7G/wW/v/5/doKsk0HC/XGfUt4Y", "58724", 
			"./ujsl9Jw7G/wW/v/5/doKsk0HC/D4LHx", "36085", 
			"./ujsl9Jw7G/wW/v/5/doKsk0HC/6Cqn2sQ", "6839", 
			"./ujsl9Jw7G/wW/v/5/doKsk0HC/WTeqS40N", "39527", 
			"./ujsl9Jw7G/wW/v/5/doKsk0HC/33h", "2009", 
			"./ujsl9Jw7G/wW/v/5/doKsk0HC/IHws", "63008", 
			"./ujsl9Jw7G/wW/v/5/doKsk0HC/w9BqBtra", "85672", 
			"./ujsl9Jw7G/wW/v/5/doKsk0HC/CIvFf", "51198", 
			"./P/IR-sXwql/i/IuCCa7/-1zUb/FTRs-bKqZ/OXbpjTG/pNGGXl/Ut", "47450", 
			"./P/IR-sXwql/i/IuCCa7/-1zUb/FTRs-bKqZ/OXbpjTG/pNGGXl/pb05", "6113", 
			"./P/IR-sXwql/i/IuCCa7/-1zUb/FTRs-bKqZ/OXbpjTG/pNGGXl/N1", "73662", 
			"./P/IR-sXwql/i/IuCCa7/-1zUb/FTRs-bKqZ/OXbpjTG/pNGGXl/ej", "60686", 
			"./P/IR-sXwql/i/IuCCa7/-1zUb/FTRs-bKqZ/OXbpjTG/pNGGXl/x", "54900", 
			"./P/IR-sXwql/i/IuCCa7/-1zUb/FTRs-bKqZ/OXbpjTG/pNGGXl/Eq3y", "48990", 
			"./1lNZIEW/EseCRIa/CjEa/Woe/GL/e5/qM3/7b71/FIMlEH/ZqOjWN", "21706", 
			"./1lNZIEW/EseCRIa/CjEa/Woe/GL/e5/qM3/7b71/FIMlEH/iUX2k0", "47056", 
			"./1lNZIEW/EseCRIa/CjEa/Woe/GL/e5/qM3/7b71/FIMlEH/G", "2858", 
			"./1lNZIEW/EseCRIa/CjEa/Woe/GL/e5/qM3/7b71/FIMlEH/7W96Kq", "78202", 
			"./1lNZIEW/EseCRIa/CjEa/Woe/GL/e5/qM3/7b71/FIMlEH/1VQLPa73", "5045", 
			"./1lNZIEW/EseCRIa/CjEa/Woe/GL/e5/qM3/7b71/FIMlEH/Bb7ArTlF", "93865", 
			"./1lNZIEW/EseCRIa/CjEa/Woe/GL/e5/qM3/7b71/FIMlEH/UcONbOC", "73238", 
			"./1lNZIEW/EseCRIa/CjEa/Woe/GL/e5/qM3/7b71/FIMlEH/4FpJR", "85002", 
			"./D/LJDb0Z1rY/HX/Ua/oPi/T3/f7e4", "65559", 
			"./D/LJDb0Z1rY/HX/Ua/oPi/T3/EPhhsOV", "25312", 
			"./D/LJDb0Z1rY/HX/Ua/oPi/T3/9XV", "19938", 
			"./D/LJDb0Z1rY/HX/Ua/oPi/T3/JbLG1", "13752", 
			"./D/LJDb0Z1rY/HX/Ua/oPi/T3/454t", "84455", 
			"./D/LJDb0Z1rY/HX/Ua/oPi/T3/UMS1", "68197", 
			"./D/LJDb0Z1rY/HX/Ua/oPi/T3/EOXiu", "24496", 
			"./P/8DCh/vB-7OuU22/ZaF3vDVO/96JGTU2/hb/2uXj/7cKoQf", "42923", 
			"./P/8DCh/vB-7OuU22/ZaF3vDVO/96JGTU2/hb/2uXj/GfvsXpc", "90156", 
			"./P/8DCh/vB-7OuU22/ZaF3vDVO/96JGTU2/hb/2uXj/qaeNSELj", "80095", 
			"./P/8DCh/vB-7OuU22/ZaF3vDVO/96JGTU2/hb/2uXj/ikzJy7Iv", "35131", 
			"./P/8DCh/vB-7OuU22/ZaF3vDVO/96JGTU2/hb/2uXj/Z", "46742", 
			"./P/8DCh/vB-7OuU22/ZaF3vDVO/96JGTU2/hb/2uXj/98", "32829", 
			"./P/8DCh/vB-7OuU22/ZaF3vDVO/96JGTU2/hb/2uXj/C", "72045", 
			"./P/8DCh/vB-7OuU22/ZaF3vDVO/96JGTU2/hb/2uXj/VwtFNO4", "98265", 
			"./P/8DCh/vB-7OuU22/ZaF3vDVO/96JGTU2/hb/2uXj/rj6qn", "35235", 
			"./hpQ/z7os1WNd/v91UFCW1/i/D/W/TlRQsrE1/XFAFo", "19674", 
			"./hpQ/z7os1WNd/v91UFCW1/i/D/W/TlRQsrE1/i", "43304", 
			"./hpQ/z7os1WNd/v91UFCW1/i/D/W/TlRQsrE1/wjJm", "86582", 
			"./hpQ/z7os1WNd/v91UFCW1/i/D/W/TlRQsrE1/PVCWF7I", "81818", 
			"./hpQ/z7os1WNd/v91UFCW1/i/D/W/TlRQsrE1/as", "6108", 
			"./hpQ/z7os1WNd/v91UFCW1/i/D/W/TlRQsrE1/s", "63012", 
			"./hpQ/z7os1WNd/v91UFCW1/i/D/W/TlRQsrE1/lIQKqh", "62970", 
			"./hpQ/z7os1WNd/v91UFCW1/i/D/W/TlRQsrE1/75", "93543", 
			"./hpQ/z7os1WNd/v91UFCW1/i/D/W/TlRQsrE1/8KDf", "4944", 
			"./m-JIAS", "68618", 
			"./uEdE", "85524", 
			"./vW", "74442", 
			"./DiS0/xtWrqmK9d/hPcSk9X/je/m", "54443", 
			"./DiS0/xtWrqmK9d/hPcSk9X/je/TfCRa", "63476", 
			"./DiS0/xtWrqmK9d/hPcSk9X/je/tR", "46990", 
			"./TO/B1ocftS/m", "6838", 
			"./rEzjs4/fG5b5H2-/919/JKRvpk/YUMJS99U0/q/F/Pc4Hj3D", "12021", 
			"./rEzjs4/fG5b5H2-/919/JKRvpk/YUMJS99U0/q/F/k", "8660", 
			"./CNVXi2c/cn/Np-hKHXQ/XoB5hE", "88405", 
			"./CNVXi2c/cn/Np-hKHXQ/J7DNB", "23893", 
			"./zoKW", "36286", 
			"./DAEbi9/r0/c3NuEhJ7/3rF/iCo/Qbi/yaJ/U/wR/5pktD8", "60952", 
			"./DAEbi9/r0/c3NuEhJ7/3rF/iCo/Qbi/yaJ/U/wR/AAQ", "31251", 
			"./DAEbi9/r0/c3NuEhJ7/3rF/iCo/Qbi/yaJ/U/wR/tF4en", "28221", 
			"./DAEbi9/r0/c3NuEhJ7/3rF/iCo/Qbi/yaJ/U/wR/sB395O4U", "50999", 
			"./DAEbi9/r0/c3NuEhJ7/3rF/iCo/Qbi/yaJ/U/wR/XtI", "34567", 
			"./DAEbi9/r0/c3NuEhJ7/3rF/iCo/Qbi/yaJ/U/wR/6UW", "83316", 
			"./DAEbi9/r0/c3NuEhJ7/3rF/iCo/Qbi/yaJ/U/wR/XvXCYr", "13215", 
			"./DAEbi9/r0/c3NuEhJ7/3rF/iCo/Qbi/yaJ/U/wR/tDshq", "24931", 
			"./Hm52h9y/f/6V/Pmyq/s4eEZU/GL/jfX/071vs/w/R-2iT8GF", "35379", 
			"./Hm52h9y/f/6V/Pmyq/s4eEZU/GL/jfX/071vs/w/3O5H1Y", "25367", 
			"./Hm52h9y/f/6V/Pmyq/s4eEZU/GL/jfX/071vs/w/h", "36741", 
			"./Hm52h9y/f/6V/Pmyq/s4eEZU/GL/jfX/071vs/w/xUnxB", "9210", 
			"./Hm52h9y/f/6V/Pmyq/s4eEZU/GL/jfX/071vs/w/N2IitMQVT", "30271", 
			"./Hm52h9y/f/6V/Pmyq/s4eEZU/GL/jfX/071vs/w/A1", "69146", 
			"./Hm52h9y/f/6V/Pmyq/s4eEZU/GL/jfX/071vs/w/Q", "50392", 
			"./s/ZPEwiZhj/aaWNnX8bL/BkoLcrjI/AuuoHEli8/7Cu/s", "34685", 
			"./s/ZPEwiZhj/aaWNnX8bL/BkoLcrjI/AuuoHEli8/7Cu/sQyAnP0NS", "46701", 
			"./s/ZPEwiZhj/aaWNnX8bL/BkoLcrjI/AuuoHEli8/7Cu/bJDheH", "6644", 
			"./s/ZPEwiZhj/aaWNnX8bL/BkoLcrjI/AuuoHEli8/7Cu/1fY-G", "59485", 
			"./s/ZPEwiZhj/aaWNnX8bL/BkoLcrjI/AuuoHEli8/7Cu/5T", "69877", 
			"./s/ZPEwiZhj/aaWNnX8bL/BkoLcrjI/AuuoHEli8/7Cu/admJB", "28543", 
			"./IdChN-4jn/Le903TUe/RfZVs0u7/f8IU7lq/-/FnlU0A/vehEaBW-/JeL/QKmIHNM4", "57910", 
			"./IdChN-4jn/Le903TUe/RfZVs0u7/f8IU7lq/-/FnlU0A/vehEaBW-/JeL/bQ3IOuX1", "82616", 
			"./IdChN-4jn/Le903TUe/RfZVs0u7/f8IU7lq/-/FnlU0A/vehEaBW-/JeL/uR2JJN", "85887", 
			"./hdQFyuZa/0jp/5W82ArArV/jUM9jpT/8OWsYvTaJ/OdW", "48143", 
			"./hdQFyuZa/0jp/5W82ArArV/jUM9jpT/8OWsYvTaJ/jw", "50263", 
			"./hdQFyuZa/0jp/5W82ArArV/jUM9jpT/8OWsYvTaJ/p0sSF", "29196", 
			"./hdQFyuZa/0jp/5W82ArArV/jUM9jpT/8OWsYvTaJ/sbVce", "67244", 
			"./hdQFyuZa/0jp/5W82ArArV/jUM9jpT/8OWsYvTaJ/-PT", "48949", 
			"./tCrlAIO/skjzhV/VV-/4svkhkJ/QQ9WXCNf", "93959", 
			"./tCrlAIO/skjzhV/VV-/4svkhkJ/Dd55nQG5", "17470", 
			"./tCrlAIO/skjzhV/VV-/4svkhkJ/CA", "81617", 
			"./tCrlAIO/skjzhV/VV-/4svkhkJ/m-bzCbMh-", "27564", 
			"./tCrlAIO/skjzhV/VV-/4svkhkJ/fLjW3m", "27685", 
			"./tCrlAIO/skjzhV/VV-/4svkhkJ/lBRH", "60135", 
			"./tCrlAIO/skjzhV/VV-/4svkhkJ/3c7bX", "4065", 
			"./7Zu9pz1Xe/kn-/W9t3wKQj/SmzteLcL5/EAT/wrwIJkZo/47T9VqjSr/S2sJoFm", "81735", 
			"./7Zu9pz1Xe/kn-/W9t3wKQj/SmzteLcL5/EAT/wrwIJkZo/47T9VqjSr/oI-A", "79901", 
			"./7Zu9pz1Xe/kn-/W9t3wKQj/SmzteLcL5/EAT/wrwIJkZo/47T9VqjSr/PcZDjky", "93610", 
			"./7Zu9pz1Xe/kn-/W9t3wKQj/SmzteLcL5/EAT/wrwIJkZo/47T9VqjSr/ph", "14684", 
			"./7Zu9pz1Xe/kn-/W9t3wKQj/SmzteLcL5/EAT/wrwIJkZo/47T9VqjSr/2B5Mjt1sb", "56219", 
			"./owj/Qo6qhxEKC/E5DWja/vSL/qj4LlAnD", "66483", 
			"./owj/Qo6qhxEKC/E5DWja/vSL/DV9lXjX", "92171", 
			"./owj/Qo6qhxEKC/E5DWja/vSL/tvUWY", "70689", 
			"./owj/Qo6qhxEKC/E5DWja/vSL/pt-slwQ", "47430", 
			"./Wb/T4xnRi8ej/ZsujE59/Y/bmhkU/1lSS4o", "13683", 
			"./Wb/T4xnRi8ej/ZsujE59/Y/bmhkU/wn", "3649", 
			"./r/uD8zF/dUZ/hcBhwnN/MdE73AJk/lbyQM/UIhKNsn", "57828", 
			"./r/uD8zF/dUZ/hcBhwnN/MdE73AJk/lbyQM/-p", "79586", 
			"./r/uD8zF/dUZ/hcBhwnN/MdE73AJk/lbyQM/OSKwq", "36923", 
			"./r/uD8zF/dUZ/hcBhwnN/MdE73AJk/lbyQM/I7M", "69675", 
			"./r/uD8zF/dUZ/hcBhwnN/MdE73AJk/lbyQM/52R12qJb", "80921", 
			"./r/uD8zF/dUZ/hcBhwnN/MdE73AJk/lbyQM/NR2kKq", "92420", 
			"./Pos/7zvZX/vIzYnQho/GVYWtq/f4nxUdoRt", "54333", 
			"./Pos/7zvZX/vIzYnQho/GVYWtq/qajuYL", "85635", 
			"./Pos/7zvZX/vIzYnQho/GVYWtq/GJz3GP", "13293", 
			"./Pos/7zvZX/vIzYnQho/GVYWtq/liXf3X", "45275", 
			"./ARxJ/fKUR43/y/rYD/QI/lHb/Hdam3F/uHISlPwl", "16113", 
			"./ARxJ/fKUR43/y/rYD/QI/lHb/Hdam3F/j0TedU1jG", "73870", 
			"./ARxJ/fKUR43/y/rYD/QI/lHb/Hdam3F/FUjDHq", "99758", 
			"./ARxJ/fKUR43/y/rYD/QI/lHb/Hdam3F/kS0", "52997", 
			"./ARxJ/fKUR43/y/rYD/QI/lHb/Hdam3F/jOSE", "6459", 
			"./ARxJ/fKUR43/y/rYD/QI/lHb/Hdam3F/KQUp4Kh", "22853", 
			"./ARxJ/fKUR43/y/rYD/QI/lHb/Hdam3F/mo4xHo4c", "35740", 
			"./ARxJ/fKUR43/y/rYD/QI/lHb/Hdam3F/hKkUOuK", "11190", 
			"./d18/sFJo/ZyUc-/LbvF", "19772", 
			"./d18/sFJo/ZyUc-/G", "39793", 
			"./d18/sFJo/ZyUc-/x-7RfYdIH", "30279", 
			"./d18/sFJo/ZyUc-/HHWD", "89385", 
			"./d18/sFJo/ZyUc-/IK4Xj5y1S", "18062", 
			"./d18/sFJo/ZyUc-/ed4z6u", "63377", 
			"./d18/sFJo/ZyUc-/o3", "48923", 
			"./d18/sFJo/ZyUc-/X8eJ", "45250", 
			"./d18/sFJo/ZyUc-/M", "78666", 
			"./HlIYKUiA/eVLPp/1TZBSuco/Pan/iM1/6jf/KdKvGW4/HbwSvd/3jZHt", "65667", 
			"./Bqbc2feOa/x/-/d/5KcS/ROcb/s/7b8KmJ44K/VCEGj6B3e/YRkwNb", "7408", 
			"./Bqbc2feOa/x/-/d/5KcS/ROcb/s/7b8KmJ44K/VCEGj6B3e/wMXAjfDx", "52645", 
			"./kxPt5jk3/7/c/cvNui/x", "73849", 
			"./IemObNX", "97508", 
			"./sedijvdHN", "57483", 
			"./IbEo-dHjI", "30957", 
			"./BsuJdo9B", "84978", 
			"./tqJW8q", "10044", 
			"./UOq", "19643", 
			"./3Gft", "43069", 
			"./HSoDBT", "66440", 
			"./tTyixU-/4b2-y/fVF2UkFL", "78786", 
			"./tTyixU-/4b2-y/K15h8CRH", "10926", 
			"./tTyixU-/4b2-y/7QkWivj1", "68575", 
			"./tTyixU-/4b2-y/eY", "88823", 
			"./tTyixU-/4b2-y/J9", "81357", 
			"./tTyixU-/4b2-y/RwxkPvJZ", "91591", 
			"./tTyixU-/4b2-y/5", "27613", 
			"./tTyixU-/4b2-y/T4Clo3m", "6115", 
			"./tTyixU-/4b2-y/jcvWlOa", "28928", 
			"./73/ePMs/7UyjOs/hScJo1e/1j", "94756", 
			"./73/ePMs/7UyjOs/hScJo1e/vAjp", "80686", 
			"./73/ePMs/7UyjOs/hScJo1e/Ln5OjR6", "62062", 
			"./73/ePMs/7UyjOs/hScJo1e/afCn", "1017", 
			"./73/ePMs/7UyjOs/hScJo1e/h", "58908", 
			"./w22ukuV/99/szs/urkZXdvp/j0s/rwOQ1wi/fP-oO7WOf/jvZ/qQiR-Oo", "49067", 
			"./ApGYOv/N5Ao0/qf/eszXjSrUI/W3xuuVdc9", "13487", 
			"./ApGYOv/N5Ao0/qf/eszXjSrUI/XKj", "79637", 
			"./ApGYOv/N5Ao0/qf/eszXjSrUI/yVmW", "31929", 
			"./s1v2K/Gw/AfSqEOIX/BYwWyj9/z/lrAX/sCcmL74m/AWMYdJ", "77102", 
			"./s1v2K/Gw/AfSqEOIX/BYwWyj9/z/lrAX/sCcmL74m/icSjz", "85783", 
			"./s1v2K/Gw/AfSqEOIX/BYwWyj9/z/lrAX/sCcmL74m/v", "28926", 
			"./s1v2K/Gw/AfSqEOIX/BYwWyj9/z/lrAX/sCcmL74m/mWR55", "62599", 
			"./fTRnHMq/s1/Yrrh/wxxnky/zaqmIuo/ZkLfI", "25977", 
			"./fTRnHMq/s1/Yrrh/wxxnky/zaqmIuo/1RKhjU", "74903", 
			"./fTRnHMq/s1/Yrrh/wxxnky/zaqmIuo/eBDP2Jb", "65963", 
			"./fTRnHMq/s1/Yrrh/wxxnky/zaqmIuo/FlZpsZ", "38268", 
			"./fTRnHMq/s1/Yrrh/wxxnky/zaqmIuo/4qj", "450", 
			"./X/4pOS2/RJ/aekBI", "57278", 
			"./X/4pOS2/RJ/KjOQTjx", "74077", 
			"./X/4pOS2/RJ/6", "4226", 
			"./bms/x2kUEm6m/aoPHfF/T-S-N", "97378", 
			"./bms/x2kUEm6m/aoPHfF/a0-Poo", "72177", 
			"./bms/x2kUEm6m/aoPHfF/wx3oxT", "19340", 
			"./bms/x2kUEm6m/aoPHfF/mkmqy1Q", "22463", 
			"./bms/x2kUEm6m/aoPHfF/6VUY", "69146", 
			"./bms/x2kUEm6m/aoPHfF/X0qp83", "79590", 
			"./bms/x2kUEm6m/aoPHfF/0e", "84433", 
			"./bms/x2kUEm6m/aoPHfF/F", "37077", 
			"./M1uAmV33i", "19513", 
			"./6p4D", "33915", 
			"./RRqV9eIA9", "14193", 
			"./wu9KV9U", "19684", 
			"./e1cT8F", "23040", 
			"./Db", "33477", 
			"./5ZbQHN4WG", "55356", 
			"./K", "23754", 
			"./G3", "6252", 
			"./u-", "54159", 
			"./k-GNcx", "82183", 
			"./F", "7721", 
			"./FdzGGAI1R/f/Kdne/PUdIN/7WKWYb4Qq/0ZK8by/VSiR5/XA/VrDOB3", "31261", 
			"./FdzGGAI1R/f/Kdne/PUdIN/7WKWYb4Qq/0ZK8by/VSiR5/XA/7e4qkKq", "62771", 
			"./FdzGGAI1R/f/Kdne/PUdIN/7WKWYb4Qq/0ZK8by/VSiR5/XA/3DmzBss", "85854", 
			"./FdzGGAI1R/f/Kdne/PUdIN/7WKWYb4Qq/0ZK8by/VSiR5/XA/1u2X", "97314", 
			"./FdzGGAI1R/f/Kdne/PUdIN/7WKWYb4Qq/0ZK8by/VSiR5/XA/1u2X2", "97314" 
	};
	
//	http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Cipher
//
//		Algorithms:
//		AES
//		AESWrap
//		ARCFOUR
//		Blowfish
//		CCM
//		DES
//		DESede
//		DESedeWrap
//		ECIES
//		GCM
//		RC2
//		RC4
//		RC5
//		RSA
//
//		Modes:
//		NONE
//		CBC
//		CFB, CFBx
//		CTR
//		CTS
//		ECB
//		OFB, OFBx
//		PCBC
//
//		Paddings:
//		NoPadding
//		ISO10126Padding
//		OAEPPadding, OAEPWith<digest>And<mgf>Padding
//		PKCS1Padding
//		PKCS5Padding
//		SSL3Padding
//
//		672 cases

}
