package com.bm.nio.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.bm.nio.file.utils.TestUtils;

public class FileSystemProviderEncryptedTest {
	static{
		//delete everything before starting tests
		File f = new File(TestUtils.SANDBOX_PATH);
		if (f.isDirectory())
			TestUtils.deleteFolderContents(f);
	}
	
	private final FileSystemProviderEncrypted mFspe = TestUtils.getEncryptedProvider();

	@Test
	public void testReadAttributesGetSetAttribute() throws Exception {
		String basePath = TestUtils.SANDBOX_PATH + "/testReadAttributes";
		try {
			FileSystem lfs = TestUtils.newTempFieSystem(mFspe, basePath);
			Path f1 = lfs.getPath("testFile1").toAbsolutePath().normalize();
			Path d1 = lfs.getPath("testDir1").toAbsolutePath().normalize();
			//encrypted files
			Files.createFile(f1);
			Files.createDirectory(d1);
			
			//the same as underlying
			System.out.println(Files.readAttributes(f1, "*"));
			Assert.assertEquals(Files.readAttributes(f1, "*"), Files.readAttributes(((PathEncrypted)f1).getUnderPath(), "*"));
			//
			FileTime expected = FileTime.fromMillis(System.currentTimeMillis());
			Files.setAttribute(f1, "lastModifiedTime", expected);
			FileTime actual = (FileTime)Files.getAttribute(f1, "lastModifiedTime");
			Assert.assertEquals(expected, actual);
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
