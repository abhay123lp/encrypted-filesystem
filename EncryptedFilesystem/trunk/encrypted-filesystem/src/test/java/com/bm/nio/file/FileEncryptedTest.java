package com.bm.nio.file;

import java.io.File;
import java.io.FileOutputStream;
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
			//FileEncrypted(URI uri) TODO: test encrypted URI
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
			FileSystem fse = TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/testFileEncryptedName/fse");
			
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
			FileSystem fse = TestUtils.newTempFieSystem(fpe, TestUtils.SANDBOX_PATH + "/testFileEncryptedName");
			
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

		FileSystemProviderEncrypted fpe = mFspe;

		try {
			File fNotCanonical = new File(TestUtils.SANDBOX_PATH + "/../testFileEncryptedCanonical", ENC_NAME);
			
			FileEncrypted feNotCanonical = new FileEncrypted(fNotCanonical, TestUtils.DEFAULT_PASSWORD);
			FileEncrypted feCanonical = new FileEncrypted(fNotCanonical.getCanonicalFile(), TestUtils.DEFAULT_PASSWORD);
			
			System.out.println(fNotCanonical);
			System.out.println(feNotCanonical);
			Assert.assertEquals(feNotCanonical.getCanonicalFile(), feCanonical);
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
