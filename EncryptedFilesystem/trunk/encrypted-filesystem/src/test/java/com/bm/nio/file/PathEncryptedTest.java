package com.bm.nio.file;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.bm.nio.file.utils.TestUtils;

public class PathEncryptedTest {

	private final FileSystemProviderEncrypted mFspe = TestUtils.getEncryptedProvider();
	
	@Test
	public void testNamesIterator() throws Exception {
		String basePath = TestUtils.SANDBOX_PATH + "/enc1";
		FileSystem fs = TestUtils.newTempFieSystem(mFspe, basePath);
		Path p = fs.getPath("dir", "dir1");
		//=== ===
		List<String> namesExpect = Arrays.asList(new String[]{"dir", "dir1"});
		Iterator<Path> iterator = p.iterator();
		Assert.assertEquals(p.getNameCount(), 2);
		for (int i = 0; i < namesExpect.size() || i < p.getNameCount() || iterator.hasNext(); i ++){
			Assert.assertEquals(namesExpect.get(i), p.getName(i).toString());
			Assert.assertEquals(namesExpect.get(i), iterator.next().toString());
		}
		
	}
	
	@Test
	public void testCompareToEquals() throws Exception {
		String basePath = TestUtils.SANDBOX_PATH + "/enc1";
		FileSystem fs = TestUtils.newTempFieSystem(mFspe, basePath);
		Path p = fs.getPath("dir", "dir1");
		List<Path> paths = Arrays.asList(new Path[]{
				fs.getPath("dir", "dir1"),
				fs.getPath("dir", "dir3"),
				fs.getPath("dir", "dir0"),
				fs.getPath("dir")
		});
		List<Integer> resultExpect = Arrays.asList(new Integer[]{
				0,
				-1,
				1,
				1
		});
		//=== ===
		for (int i = 0; i < paths.size(); i ++){
			Integer res = p.compareTo(paths.get(i));
			if (res == 0)
				Assert.assertTrue(p.equals(paths.get(i)));
			else
				Assert.assertFalse(p.equals(paths.get(i)));
			Assert.assertTrue(Integer.signum(res) == resultExpect.get(i));
		}
		
	}
	
	@Test
	public void testToStringNormalizeAbsoluteSubpath() throws Exception {
		String basePath = TestUtils.SANDBOX_PATH + "/enc1";
		FileSystem fs = TestUtils.newTempFieSystem(mFspe, basePath);
		List<Path> paths = Arrays.asList(new Path[]{
				fs.getPath("dir", "dir1"),
				fs.getPath("dir", "dir1").toAbsolutePath(),
				fs.getPath("dir", "dir3").subpath(0, 1),
				fs.getPath(".\\dir").subpath(0, 1),
				fs.getPath(".\\dir").normalize().toAbsolutePath(),
				fs.getPath(".\\dir").toAbsolutePath().normalize(),
				fs.getPath("..\\dir").normalize().toAbsolutePath()
		});
		List<String> namesExpect = Arrays.asList(new String[]{
				"dir\\dir1",
				Paths.get(basePath, "dir", "dir1").normalize().toAbsolutePath().toString(),
				"dir",
				".",
				fs.getPath(".\\dir").normalize().toAbsolutePath().toString(),
				fs.getPath(".\\dir").toAbsolutePath().normalize().toString(),
				Paths.get(basePath).toAbsolutePath().normalize().resolve("..\\dir").toString()
		});
		//=== ===
		// preferable order: .toAbsolutePath().normalize()
		boolean exception = false;
		try {
			fs.getPath("..\\dir").toAbsolutePath().normalize();
		} catch (Exception e) {
			exception = true;
		}
		Assert.assertTrue(exception);
		
		//
		for (int i = 0; i < namesExpect.size(); i ++){
			Assert.assertEquals(namesExpect.get(i), paths.get(i).toString());
		}

	}
	
	
	@After
	public void clean() throws IOException {
		TestUtils.deleteFilesystems(mFspe);
	}
	
}
