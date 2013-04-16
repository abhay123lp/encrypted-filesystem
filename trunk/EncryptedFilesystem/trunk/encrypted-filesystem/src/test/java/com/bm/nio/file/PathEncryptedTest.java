package com.bm.nio.file;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.bm.nio.file.utils.TestUtils;

public class PathEncryptedTest {

	private final FileSystemProviderEncrypted mFspe = TestUtils.getEncryptedProvider();
	private String basePath = TestUtils.SANDBOX_PATH + "/enc1";
	private final FileSystem fs;
	{
		FileSystem lfs = null;
		try {
			lfs = TestUtils.newTempFieSystem(mFspe, basePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
		fs = lfs;
	}
	
	private final List<Path> pathsSet = Arrays.asList(new Path[]{
			fs.getPath("dir", "dir1"),
			fs.getPath("dir", "dir1").toAbsolutePath(),
			fs.getPath("dir", "dir3").subpath(0, 1),
			fs.getPath(".\\dir").subpath(0, 1),
			fs.getPath(".\\dir").normalize().toAbsolutePath(),
			fs.getPath(".\\dir").toAbsolutePath().normalize(),
			fs.getPath("..\\dir").normalize().toAbsolutePath(),
			fs.getPath(".").toAbsolutePath(),
			fs.getPath(".").normalize()
	});
	
//	List<Path> paths = Arrays.asList(new Path[]{
//	fs.getPath("dir", "dir1"),
//	fs.getPath("dir", "dir1").toAbsolutePath(),
//	fs.getPath("dir", "dir3").subpath(0, 1),
//	fs.getPath(".\\dir").subpath(0, 1),
//	fs.getPath(".\\dir").normalize().toAbsolutePath(),
//	fs.getPath(".\\dir").toAbsolutePath().normalize(),
//	fs.getPath("..\\dir").normalize().toAbsolutePath(),
//	fs.getPath(".").toAbsolutePath(),
//	fs.getPath(".").normalize(),
//});
	
	@Test
	public void testNamesIterator() throws Exception {
		//String basePath = TestUtils.SANDBOX_PATH + "/enc1";
		//FileSystem fs = TestUtils.newTempFieSystem(mFspe, basePath);
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
		//String basePath = TestUtils.SANDBOX_PATH + "/enc1";
		//FileSystem fs = TestUtils.newTempFieSystem(mFspe, basePath);
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
//		FileSystem fs = TestUtils.newTempFieSystem(mFspe, basePath);
		List<Path> paths = pathsSet;
//		List<Path> paths = Arrays.asList(new Path[]{
//				fs.getPath("dir", "dir1"),
//				fs.getPath("dir", "dir1").toAbsolutePath(),
//				fs.getPath("dir", "dir3").subpath(0, 1),
//				fs.getPath(".\\dir").subpath(0, 1),
//				fs.getPath(".\\dir").normalize().toAbsolutePath(),
//				fs.getPath(".\\dir").toAbsolutePath().normalize(),
//				fs.getPath("..\\dir").normalize().toAbsolutePath(),
//				fs.getPath(".").normalize()
//		});
		//toString
		List<String> namesExpect = Arrays.asList(new String[]{
				"dir\\dir1",
				Paths.get(basePath, "dir", "dir1").normalize().toAbsolutePath().toString(),
				"dir",
				".",
				fs.getPath(".\\dir").normalize().toAbsolutePath().toString(),
				fs.getPath(".\\dir").toAbsolutePath().normalize().toString(),
				Paths.get(basePath).toAbsolutePath().normalize().resolve("..\\dir").toString(),
				Paths.get(basePath).toAbsolutePath().normalize().resolve(".").toString(),
				""
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
	
	
	@Test
	public void testIsAbsoluteFileNameParent() throws Exception {
		String basePath = TestUtils.SANDBOX_PATH + "/enc1";
//		FileSystem fs = TestUtils.newTempFieSystem(mFspe, basePath);
		List<Path> paths = pathsSet;
//		List<Path> paths = Arrays.asList(new Path[]{
//				fs.getPath("dir", "dir1"),
//				fs.getPath("dir", "dir1").toAbsolutePath(),
//				fs.getPath("dir", "dir3").subpath(0, 1),
//				fs.getPath(".\\dir").subpath(0, 1),
//				fs.getPath(".\\dir").normalize().toAbsolutePath(),
//				fs.getPath(".\\dir").toAbsolutePath().normalize(),
//				fs.getPath("..\\dir").normalize().toAbsolutePath(),
//				fs.getPath(".").toAbsolutePath(),
//				fs.getPath(".").normalize(),
//		});
		List<Boolean> isAbsoluteExpect = Arrays.asList(new Boolean[]{
				false,
				true,
				false,
				false,
				true,
				true,
				true,
				true,
				false,
		});
		List<String> fileNameExpect = Arrays.asList(new String[]{
				"dir1",
				"dir1",
				"dir",
				".",
				"dir",
				"dir",
				"dir",
				".",
				"",
		});
		List<String> parentNameExpect = Arrays.asList(new String[]{
				"dir",
				Paths.get(basePath, "dir", "dir1").normalize().toAbsolutePath().getParent().toString(),
				"null",
				"null",
				Paths.get(basePath, ".\\dir").toAbsolutePath().normalize().getParent().toString(),
				Paths.get(basePath, ".\\dir").normalize().toAbsolutePath().getParent().toString(),
				Paths.get(basePath).toAbsolutePath().normalize().resolve("..\\dir").getParent().toString(),
				Paths.get(basePath).toAbsolutePath().normalize().resolve(".").getParent().toString(),
				"null"
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
		//System.out.println(Paths.get(basePath, "dir", ".").toAbsolutePath().getFileName());
		//System.out.println(Paths.get("\\").toAbsolutePath().getParent());
		for (int i = 0; i < paths.size(); i ++){
			Assert.assertEquals(isAbsoluteExpect.get(i), paths.get(i).isAbsolute());
			Assert.assertEquals(fileNameExpect.get(i), String.valueOf(paths.get(i).getFileName()));
			Assert.assertEquals(parentNameExpect.get(i), String.valueOf(paths.get(i).getParent()));
		}
	}
	
	
	@After
	public void clean() throws IOException {
		TestUtils.deleteFilesystems(mFspe);
	}
	
}
