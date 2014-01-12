package com.bm.nio.file;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystemAlreadyExistsException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.bm.nio.file.FileSystemEncrypted.FileSystemEncryptedEnvParams;
import com.bm.nio.file.utils.TestUtils;

public class FileEncryptedTest {

	private final String DEC_NAME = "FileEncrypted1";
	private final String ENC_NAME = "E8E0B8FA640C2F76F6A54DAD0630";
	
	static{
		//delete everything before starting tests
		File f = new File(TestUtils.SANDBOX_PATH);
		if (f.isDirectory())
			TestUtils.deleteFolderContents(f);
	}
	
	private final FileSystemProviderEncrypted mFspe = TestUtils.getEncryptedProvider();
	
	
	@Test
	public void testCreate() throws Exception {

		FileSystemProviderEncrypted fpe = mFspe;

		try {
			File rootFile = new File(TestUtils.SANDBOX_PATH + "/testFileEncryptedCreate").getCanonicalFile();
			TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/testFileEncryptedCreate");
			//test filesystemencrypted not found
			boolean exception;
			exception = false;
			try {
				new FileEncrypted(rootFile.getParent() + "\\FileEncrypted1");
			} catch (RuntimeException e) {
				exception = true;
			}
			Assert.assertTrue(exception);
			
			//allow encrypted and decrypted paths
			//test all constructors
			FileEncrypted ffeDec = new FileEncrypted(rootFile.getPath() + "\\" + DEC_NAME);
			FileEncrypted ffeEnc = new FileEncrypted(rootFile.getPath() + "\\" + ENC_NAME);
			File ffeDecUnder = ffeDec.getUnderlyingFile();
			//FileEncrypted(String pathname)
			Assert.assertEquals(ffeDecUnder.compareTo(ffeEnc.getUnderlyingFile()), 0);
			//FileEncrypted(URI uri) DONE: test encrypted URI
			Assert.assertEquals(ffeDecUnder.compareTo(new FileEncrypted(rootFile.toURI().resolve(DEC_NAME)).getUnderlyingFile()), 0);
			Assert.assertEquals(ffeDecUnder.compareTo(new FileEncrypted(rootFile.toURI().resolve(ENC_NAME)).getUnderlyingFile()), 0);
			Assert.assertEquals(ffeDecUnder.compareTo(new FileEncrypted(ffeEnc.toURI()).getUnderlyingFile()), 0);
			//FileEncrypted(File parent, String child)
			Assert.assertEquals(ffeDecUnder.compareTo(new FileEncrypted(ffeDec, "").getUnderlyingFile()), 0);
			Assert.assertEquals(ffeDecUnder.compareTo(new FileEncrypted(ffeDecUnder, "").getUnderlyingFile()), 0);
			Assert.assertEquals(ffeDecUnder.compareTo(new FileEncrypted(rootFile, DEC_NAME).getUnderlyingFile()), 0);
			Assert.assertEquals(ffeDecUnder.compareTo(new FileEncrypted(rootFile, ENC_NAME).getUnderlyingFile()), 0);
			//FileEncrypted(String parent, String child)
			Assert.assertEquals(ffeDecUnder.compareTo(new FileEncrypted(rootFile.getPath(), DEC_NAME).getUnderlyingFile()), 0);
			Assert.assertEquals(ffeDecUnder.compareTo(new FileEncrypted(rootFile.getPath(), ENC_NAME).getUnderlyingFile()), 0);
			//FileEncrypted(String parent, String child)
			FileEncrypted ffeWrap = new FileEncrypted(new File(rootFile.getParent() + "/testFileEncryptedName1/file1"), TestUtils.DEFAULT_PASSWORD);
			exception = false;
			try {
				ffeWrap.createNewFile();
			} catch (Exception e) {
				exception = true;
			}
			Assert.assertFalse(exception);

		} finally{
			clean();
		}
	}
	
	@Test
	public void testGetNamePath() throws Exception {

		FileSystemProviderEncrypted fpe = mFspe;

		try {
			File rootFile = new File(TestUtils.SANDBOX_PATH + "/testFileEncryptedName/fse").getCanonicalFile();
			TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/testFileEncryptedName/fse");
			
			FileEncrypted fe = new FileEncrypted(rootFile, ENC_NAME);
			Assert.assertEquals(fe.getName(), DEC_NAME);
			Assert.assertEquals(fe.getPath(), rootFile.getPath() + File.separator + DEC_NAME);
		} finally{
			clean();
		}
	}

	@Test
	public void testGetParent() throws Exception {

		FileSystemProviderEncrypted fpe = mFspe;

		try {
			File rootFile = new File(TestUtils.SANDBOX_PATH + "/testFileEncryptedName").getCanonicalFile();
			TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/testFileEncryptedName");
			
			FileEncrypted feChild = new FileEncrypted(rootFile.getPath() + "/child", DEC_NAME);
			FileEncrypted feParent = new FileEncrypted(rootFile.getPath(), "child");
			Assert.assertEquals(feChild.getParentFile(), feParent);
			Assert.assertEquals(feChild.getParent(), feParent.getPath());
		} finally{
			clean();
		}
	}

	@Test
	public void testAbsolute() throws Exception {

		FileSystemProviderEncrypted fpe = mFspe;

		try {
			final File underFileException = new File("./child");
			final File underFile = new File("./childFs/chilfFile");
			boolean exception;
			//creates new filesystem in current dir (projectRoot)
			FileEncrypted feRelative = new FileEncrypted(underFile, TestUtils.DEFAULT_PASSWORD);
			FileEncrypted feAbsolute = new FileEncrypted(underFile.getAbsoluteFile(), TestUtils.DEFAULT_PASSWORD);
			
			Assert.assertFalse(feRelative.isAbsolute());
			Assert.assertTrue(feRelative.getAbsoluteFile().isAbsolute());
			Assert.assertEquals(feRelative.getAbsoluteFile(), feAbsolute);
			Assert.assertEquals(feRelative.getAbsoluteFile().getPath(), feAbsolute.getPath());
			//test exception for existing filesystem
			clean();//delete all filesystems
			//create new one in projectRoot/sandbox
			TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/testFileEncryptedName");
			exception = false;
			try {
				//will try to create new filesystem in projectRoot and throw exception
				//because projectRoot/sandbox already exists
				new FileEncrypted(underFileException, TestUtils.DEFAULT_PASSWORD);
			} catch (FileSystemAlreadyExistsException e) {
				exception = true;
			}
			Assert.assertTrue(exception);
			
			
		} finally{
			clean();
		}
	}	
	
	@Test
	public void testGetCanonical() throws Exception {
		try {
			File fNotCanonical = new File(TestUtils.SANDBOX_PATH + "/../../testFileEncryptedCanonical", ENC_NAME);
			
			FileEncrypted feNotCanonical = new FileEncrypted(fNotCanonical, TestUtils.DEFAULT_PASSWORD);
			FileEncrypted feCanonical = new FileEncrypted(fNotCanonical.getCanonicalFile(), TestUtils.DEFAULT_PASSWORD);
			Assert.assertEquals(feNotCanonical.getCanonicalFile(), feCanonical);
			Assert.assertEquals(feNotCanonical.getCanonicalPath(), feCanonical.getPath());
		} finally{
			clean();
		}
	}

	@Test
	public void testToURI() throws Exception {

		FileSystemProviderEncrypted fpe = mFspe;

		try {
			File rootFile = new File(TestUtils.SANDBOX_PATH + "/testFileEncryptedName/fse").getCanonicalFile();
			TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/testFileEncryptedName/fse");
			FileEncrypted fe = new FileEncrypted(rootFile, ENC_NAME);
			Assert.assertEquals(fe.toPath().toUri(), fe.toURI());
		} finally{
			clean();
		}
	}

	@Test
	public void testDelegates() throws Exception {

		FileSystemProviderEncrypted fpe = mFspe;

		try {
			File rootFile = new File(TestUtils.SANDBOX_PATH + "/testFileEncryptedName/fse").getCanonicalFile();
			TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/testFileEncryptedName/fse");
			FileEncrypted fe = new FileEncrypted(rootFile, DEC_NAME);
			File f = new File(rootFile, ENC_NAME);
			//readonly
			Assert.assertEquals(f.canRead(), fe.canRead());
			fe.createNewFile();
			fe.setReadable(false, false);
			Assert.assertEquals(f.canRead(), fe.canRead());
			fe.delete();
			//execute
			Assert.assertEquals(f.canExecute(), fe.canExecute());
			fe.createNewFile();
			fe.setExecutable(false, false);
			Assert.assertEquals(f.canExecute(), fe.canExecute());
			fe.delete();
			//
			Assert.assertEquals(f.canWrite(), fe.canWrite());
			Assert.assertEquals(f.exists(), fe.exists());
			Assert.assertEquals(f.isDirectory(), fe.isDirectory());
			Assert.assertEquals(f.isFile(), fe.isFile());
			Assert.assertEquals(f.isHidden(), fe.isHidden());
			//lastmodified
			Assert.assertEquals(f.lastModified(), fe.lastModified());
			fe.createNewFile();
			fe.setLastModified(100L);
			Assert.assertEquals(100L, fe.lastModified());
			Assert.assertEquals(100L, f.lastModified());
			fe.delete();
			//delete
			fe.createNewFile();
			Assert.assertEquals(true, fe.exists());
			Assert.assertEquals(true, f.exists());
			fe.delete();
			Assert.assertEquals(false, fe.exists());
			Assert.assertEquals(false, f.exists());
			//mkdir
			fe.mkdir();
			Assert.assertEquals(true, fe.exists());
			Assert.assertEquals(true, f.exists());
			Assert.assertEquals(true, fe.isDirectory());
			Assert.assertEquals(true, f.isDirectory());
			fe.delete();
			Assert.assertEquals(false, fe.exists());
			Assert.assertEquals(false, f.exists());
			//mkdirs
			fe.toPath().resolve("nestedDir").toFile().mkdirs();
			Assert.assertEquals(true, fe.exists());
			Assert.assertEquals(true, fe.isDirectory());
			Assert.assertFalse(fe.delete());//nested dir exists
		} finally{
			clean();
		}
	}

	@Test
	public void testLength() throws Exception {

		FileSystemProviderEncrypted fpe = mFspe;

		try {
			boolean exception;
			File rootFile = new File(TestUtils.SANDBOX_PATH + "/testFileEncryptedName/fse").getCanonicalFile();
			TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/testFileEncryptedName/fse");
			FileEncrypted fe = new FileEncrypted(rootFile, ENC_NAME);
			//test stream cipher
			exception = false;
			try {
				fe.length();
			} catch (RuntimeException e) {
				//throws exception for not created file
				exception = true;
			}
			Assert.assertTrue(exception);
			//
			final String data = "testtest";
			fe.createNewFile();
			writeToFile(fe, data);
			Assert.assertEquals(data.length(), fe.length());
			//test block cipher
			clean();
			ConfigEncrypted ce = new ConfigEncrypted();
			ce.setTransformation("AES/CBC/PKCS5Padding");
			Map<String, Object> env = TestUtils.newEnv();
			env.put(FileSystemEncryptedEnvParams.ENV_CONFIG, ce);
			TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/testFileEncryptedName/fse", env);
			//NOTE: if not creating file again then it will use old filesystem!
			fe = new FileEncrypted(rootFile, ENC_NAME);
			exception = false;
			try {
				writeToFile(fe, data);
				fe.length();
			} catch (UnsupportedOperationException e) {
				exception = true;
			}
			Assert.assertTrue(exception);
		} finally{
			clean();
		}
	}

	public static void writeToFile(FileEncrypted fe, String data) throws IOException{
		try (OutputStream os = fe.toPath().getFileSystem().provider().newOutputStream(fe.toPath())){
			os.write(data.getBytes());
		}
	}
	
	@Test
	public void testSpace() throws Exception {

		FileSystemProviderEncrypted fpe = mFspe;

		try {
			File rootFile = new File(TestUtils.SANDBOX_PATH + "/testFileEncryptedName/fse").getCanonicalFile();
			TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/testFileEncryptedName/fse");
			FileEncrypted fe = new FileEncrypted(rootFile, DEC_NAME);
			boolean exception;
			//
			exception = false;
			try {
				fe.getTotalSpace();
			} catch (UnsupportedOperationException e) {
				exception = true;
			}
			Assert.assertTrue(exception);
			//
			exception = false;
			try {
				fe.getFreeSpace();
			} catch (UnsupportedOperationException e) {
				exception = true;
			}
			Assert.assertTrue(exception);
			//
			exception = false;
			try {
				fe.getUsableSpace();
			} catch (UnsupportedOperationException e) {
				exception = true;
			}
			Assert.assertTrue(exception);
		} finally{
			clean();
		}
	}

	@Test
	public void testEqualsHashcodeCompareTo() throws Exception {

		FileSystemProviderEncrypted fpe = mFspe;
		try {
			File rootFile = new File(TestUtils.SANDBOX_PATH + "/testFileEncryptedName/fse").getCanonicalFile();
			TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/testFileEncryptedName/fse");
			FileEncrypted fe = new FileEncrypted(rootFile, DEC_NAME);
			//
			HashSet<FileEncrypted> set = new HashSet<FileEncrypted>();
			set.add(fe);
			//use another constructor to create file
			FileEncrypted feSame = new FileEncrypted(new File(rootFile, DEC_NAME), TestUtils.DEFAULT_PASSWORD);
			Assert.assertTrue(set.contains(feSame));
			Assert.assertEquals(0, fe.compareTo(feSame));
			//
			FileEncrypted feMore = new FileEncrypted(rootFile, DEC_NAME + "1");
			FileEncrypted feLess = new FileEncrypted(rootFile, DEC_NAME.substring(0, DEC_NAME.length() - 2));
			Assert.assertTrue(fe.compareTo(feMore) < 0);
			Assert.assertTrue(fe.compareTo(feLess) > 0);
		} finally{
			clean();
		}
	}

	@Test
	public void testRenameTo() throws Exception {

		FileSystemProviderEncrypted fpe = mFspe;
		try {
			File rootFile = new File(TestUtils.SANDBOX_PATH + "/testFileEncryptedName/fse").getCanonicalFile();
			TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/testFileEncryptedName/fse");
			FileEncrypted fe = new FileEncrypted(rootFile, DEC_NAME);
			FileEncrypted feNew = new FileEncrypted(rootFile, DEC_NAME + "1");
			//
			Assert.assertFalse(fe.renameTo(feNew));
			fe.createNewFile();
			Assert.assertTrue(fe.renameTo(feNew));
			Assert.assertTrue(feNew.exists());
		} finally{
			clean();
		}
	}

	@Test
	public void testList() throws Exception {

		FileSystemProviderEncrypted fpe = mFspe;
		try {
			File rootFile = new File(TestUtils.SANDBOX_PATH + "/testFileEncryptedName/fse").getCanonicalFile();
			TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/testFileEncryptedName/fse");
			FileEncrypted fe = new FileEncrypted(rootFile, DEC_NAME);
			//
			String [] children = new String [] {"child1", "child2", "child3"};
			Set<String> childrenSet = new HashSet<String>(Arrays.asList(children));
			fe.mkdir();
			FileEncrypted feChild1 = new FileEncrypted(fe, children[0]);
			FileEncrypted feChild2 = new FileEncrypted(fe, children[1]);
			FileEncrypted feChild3 = new FileEncrypted(fe, children[2]);
			FileEncrypted feChild22 = new FileEncrypted(feChild2, "child2");
			Assert.assertTrue(feChild1.createNewFile());
			Assert.assertTrue(feChild2.mkdir());
			Assert.assertTrue(feChild22.createNewFile());
			Assert.assertTrue(feChild3.mkdir());
			for (String str : fe.list()){
				Assert.assertTrue(childrenSet.remove(str));
			}
			Assert.assertTrue(childrenSet.size() == 0);
			Assert.assertArrayEquals(feChild22.list(), new String [] {"child2"});
			//list(FilenameFilter)
			childrenSet = new HashSet<String>(Arrays.asList(children));
			final String filterRemove = "child2";
			childrenSet.remove(filterRemove);
			FilenameFilter ff = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return !name.equalsIgnoreCase(filterRemove);
				}
			};
			for (String str : fe.list(ff)){
				Assert.assertTrue(childrenSet.remove(str));
			}
			Assert.assertTrue(childrenSet.size() == 0);
			//listFiles()
			childrenSet = new HashSet<String>(Arrays.asList(children));
			for (File file : fe.listFiles()){
				Assert.assertTrue(childrenSet.remove(file.getName()));
			}
			Assert.assertTrue(childrenSet.size() == 0);
			//listFiles(FilenameFilter)
			childrenSet = new HashSet<String>(Arrays.asList(children));
			childrenSet.remove(filterRemove);
			for (File file : fe.listFiles(ff)){
				Assert.assertTrue(childrenSet.remove(file.getName()));
			}
			Assert.assertTrue(childrenSet.size() == 0);
			//listFiles(FileFilter)
			FileFilter fff = new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return !pathname.getName().equalsIgnoreCase(filterRemove);
				}
			};
			childrenSet = new HashSet<String>(Arrays.asList(children));
			childrenSet.remove(filterRemove);
			for (File file : fe.listFiles(fff)){
				Assert.assertTrue(childrenSet.remove(file.getName()));
			}
			Assert.assertTrue(childrenSet.size() == 0);
			
		} finally{
			clean();
		}
	}

	@After
	public void clean() throws IOException {
		TestUtils.deleteFilesystems(mFspe);
		File f = new File(TestUtils.SANDBOX_PATH);
		if (f.isDirectory())
			TestUtils.deleteFolderContents(f);
	}	
}
