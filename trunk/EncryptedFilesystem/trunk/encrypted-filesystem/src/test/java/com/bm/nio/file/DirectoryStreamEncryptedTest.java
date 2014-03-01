package com.bm.nio.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.bm.nio.file.utils.TestUtils;

public class DirectoryStreamEncryptedTest {
	static{
		//delete everything before starting tests
		File f = new File(TestUtils.SANDBOX_PATH);
		if (f.isDirectory())
			TestUtils.deleteFolderContents(f);
	}
	
	private final FileSystemProviderEncrypted mFspe = TestUtils.getEncryptedProvider();

	@Test
	public void testGetNext() throws Exception {
		String basePath = TestUtils.SANDBOX_PATH + "/testGetNext";
		final File fTest1 = new File(basePath, "testFile1");
		final File dTest1 = new File(basePath, "testDir1");
		try {
			FileSystem lfs = TestUtils.newTempFieSystem(mFspe, basePath);
			Path f1 = lfs.getPath("testFile1");
			Path f2 = lfs.getPath("testFile2");
			Path d1 = lfs.getPath("testDir1");
			//encrypted files
			Files.createFile(f1);
			Files.createFile(f2);
			Files.createDirectory(d1);
			
			//unencrypted files
			fTest1.createNewFile();
			dTest1.mkdir();
			//test that directory stream returns only encrypted files
			Set<Path> expected = new HashSet<Path>(Arrays.asList(new Path [] {f1, f2, d1}));
			Set<Path> actual = new HashSet<Path>();
			try(DirectoryStream<Path> ds = Files.newDirectoryStream(f1.toAbsolutePath().getParent())){
				for (Path p : ds){
					actual.add(p);
				}
			}
			Assert.assertEquals(expected, actual);
		} finally{
			fTest1.delete();
			dTest1.delete();
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
