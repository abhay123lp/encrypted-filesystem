package com.bm.nio.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

import javax.management.RuntimeErrorException;
import javax.swing.plaf.basic.BasicOptionPaneUI;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import sun.nio.fs.WindowsFileSystemProvider;
import sun.org.mozilla.javascript.internal.ast.WithStatement;

import com.bm.nio.file.utils.TestUtils;
import com.sun.nio.zipfs.ZipPath;

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
			//dir\dir1
			fs.getPath("dir", "dir1"),
			//D:\enc1\dir\dir1
			fs.getPath("dir", "dir1").toAbsolutePath(),
			//dir
			fs.getPath("dir"),
			//D:\enc1\dir
			fs.getPath("dir").toAbsolutePath(),
			//dir\..
			fs.getPath("dir", ".."),
			//D:\enc1\..\dir
			fs.getPath("\\..\\dir").toAbsolutePath(),
			//.
			fs.getPath("."),
			//D:\enc1\.
			fs.getPath(".").toAbsolutePath(),
			//
			fs.getPath("")
	});
	
	@Test
	public void testEquals() throws Exception {
		List<Path> paths = pathsSet;
		List<Path> pathsExpect = Arrays.asList(new Path[]{
				fs.getPath("dir", "dir1"),
				fs.getPath("dir", "dir1").toAbsolutePath(),
				fs.getPath("dir", "dir3").subpath(0, 1),
				fs.getPath(".\\dir").toAbsolutePath().normalize(),
				fs.getPath("dir").resolve(".."),
				fs.getPath("..\\dir").normalize().toAbsolutePath(),
				fs.getPath(".\\dir").subpath(0, 1),
				fs.getPath(".").toAbsolutePath(),
				fs.getPath(".").normalize()
		});
		// ================= 
		for (int i = 0; i < paths.size(); i ++){
			Assert.assertEquals(pathsExpect, paths);
		}
	}
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
		//String basePath = TestUtils.SANDBOX_PATH + "/enc1";
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
				fs.getPath(".\\dir").normalize().toAbsolutePath().toString(),
				"dir\\..",
				Paths.get(basePath).toAbsolutePath().normalize().resolve("..\\dir").toString(),
				".",
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
		// ===
		Assert.assertEquals(fs.getPath(".\\dir").toAbsolutePath().normalize(),
				            fs.getPath(".\\dir").normalize().toAbsolutePath());
		
		//
		for (int i = 0; i < namesExpect.size(); i ++){
			Assert.assertEquals(namesExpect.get(i), paths.get(i).toString());
		}

	}
	
	
	@Test
	public void testIsAbsoluteFileNameParent() throws Exception {
		//String basePath = TestUtils.SANDBOX_PATH + "/enc1";
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
				true,
				false,
				true,
				false,
				true,
				false,
		});
		List<String> fileNameExpect = Arrays.asList(new String[]{
				"dir1",
				"dir1",
				"dir",
				"dir",
				"..",
				"dir",
				".",
				".",
				"",
		});
		List<String> parentNameExpect = Arrays.asList(new String[]{
				"dir",
				Paths.get(basePath, "dir", "dir1").normalize().toAbsolutePath().getParent().toString(),
				"null",
				Paths.get(basePath, ".\\dir").toAbsolutePath().normalize().getParent().toString(),
				"dir",
				Paths.get(basePath).toAbsolutePath().normalize().resolve("..\\dir").getParent().toString(),
				"null",
				Paths.get(basePath).toAbsolutePath().normalize().resolve(".").getParent().toString(),
				"null"
		});
		//=== ===
		//System.out.println(Paths.get(basePath, "dir", ".").toAbsolutePath().getFileName());
		//System.out.println(Paths.get("\\").toAbsolutePath().getParent());
		for (int i = 0; i < paths.size(); i ++){
			Assert.assertEquals(isAbsoluteExpect.get(i), paths.get(i).isAbsolute());
			Assert.assertEquals(fileNameExpect.get(i), String.valueOf(paths.get(i).getFileName()));
			Assert.assertEquals(parentNameExpect.get(i), String.valueOf(paths.get(i).getParent()));
		}
	}
	
	
//	@Test
//	public void testStartsWithEndsWith() throws Exception {
//		List<Path> paths = pathsSet;
//		class TestCase{
//			public Path path;
//			public boolean testCondition; 
//			public TestCase(Path path, boolean testCondition){
//				this.path = path;
//				this.testCondition = testCondition;
//			}
//		}
//		List<TestCase[]> startsExpected = Arrays.asList(new TestCase[][]{
//				{
//					new TestCase(fs.getPath("dir"), true), 
//					new TestCase(fs.getPath("dir").toAbsolutePath(), false),
//					new TestCase(fs.getPath("dir\\.").normalize(), true),
//				},
//				{
//					 new TestCase(fs.getPath("dir"), false), 
//					 new TestCase(fs.getPath("dir").toAbsolutePath(), true),
//					 new TestCase(fs.getPath("dir\\.").normalize(), false),
//					 new TestCase(fs.getPath("dir\\.").normalize().toAbsolutePath(), true),
//				},
//				{
//					new TestCase(fs.getPath("dir"), true), 
//					new TestCase(fs.getPath("dir").toAbsolutePath(), false),
//					new TestCase(fs.getPath("dir\\.").normalize(), true),
//				},
//				{
//					 new TestCase(fs.getPath("dir"), false), 
//					 new TestCase(fs.getPath("dir").toAbsolutePath(), true),
//					 new TestCase(fs.getPath("dir\\.").normalize(), false),
//					 new TestCase(fs.getPath("dir\\.").normalize().toAbsolutePath(), true),
//				},
//				{
//					 new TestCase(fs.getPath("dir"), true),
//					 new TestCase(fs.getPath("dir").toAbsolutePath(), false),
//					 new TestCase(fs.getPath(".."), false),
//				},
//				{
//					 new TestCase(fs.getPath(".."), false),
//					 new TestCase(fs.getPath("..").toAbsolutePath(), true),
//					 new TestCase(fs.getPath("\\..").toAbsolutePath(), true),
//					 new TestCase(fs.getPath("dir"), false),
//				},
//				{
//					 new TestCase(fs.getPath(""), false),
//					 new TestCase(fs.getPath(".").toAbsolutePath(), false),
//				},
//				{
//					 new TestCase(fs.getPath("."), false),
//					 new TestCase(fs.getPath("").toAbsolutePath(), true),
//				},
//				{
//					 new TestCase(fs.getPath(""), true),
//					 new TestCase(fs.getPath("."), false),
//					 new TestCase(fs.getPath(".").normalize(), true),
//				},
//		});
//
//		List<TestCase[]> endsExpected = Arrays.asList(new TestCase[][]{
//				{
//					new TestCase(fs.getPath("dir"), true), 
//					new TestCase(fs.getPath("dir").toAbsolutePath(), false),
//					new TestCase(fs.getPath("dir\\.").normalize(), true),
//				},
//				{
//					 new TestCase(fs.getPath("dir"), false), 
//					 new TestCase(fs.getPath("dir").toAbsolutePath(), true),
//					 new TestCase(fs.getPath("dir\\.").normalize(), false),
//					 new TestCase(fs.getPath("dir\\.").normalize().toAbsolutePath(), true),
//				},
//				{
//					new TestCase(fs.getPath("dir"), true), 
//					new TestCase(fs.getPath("dir").toAbsolutePath(), false),
//					new TestCase(fs.getPath("dir\\.").normalize(), true),
//				},
//				{
//					 new TestCase(fs.getPath("dir"), false), 
//					 new TestCase(fs.getPath("dir").toAbsolutePath(), true),
//					 new TestCase(fs.getPath("dir\\.").normalize(), false),
//					 new TestCase(fs.getPath("dir\\.").normalize().toAbsolutePath(), true),
//				},
//				{
//					 new TestCase(fs.getPath("dir"), true),
//					 new TestCase(fs.getPath("dir").toAbsolutePath(), false),
//					 new TestCase(fs.getPath(".."), false),
//				},
//				{
//					 new TestCase(fs.getPath(".."), false),
//					 new TestCase(fs.getPath("..").toAbsolutePath(), true),
//					 new TestCase(fs.getPath("\\..").toAbsolutePath(), true),
//					 new TestCase(fs.getPath("dir"), false),
//				},
//				{
//					 new TestCase(fs.getPath(""), false),
//					 new TestCase(fs.getPath(".").toAbsolutePath(), false),
//				},
//				{
//					 new TestCase(fs.getPath("."), false),
//					 new TestCase(fs.getPath("").toAbsolutePath(), true),
//				},
//				{
//					 new TestCase(fs.getPath(""), true),
//					 new TestCase(fs.getPath("."), false),
//					 new TestCase(fs.getPath(".").normalize(), true),
//				},
//		});
//
//		// ================= 
//		//System.out.print(Paths.get(".\\dir").startsWith("."));
//		//System.out.print(Paths.get(".\\dir").startsWith(".\\dir"));
//		//System.out.print(Paths.get(".\\dir\\..").toAbsolutePath().normalize().startsWith(Paths.get(".").toAbsolutePath().normalize()));
//		for (int i = 0; i < paths.size(); i ++){
//			final Path p = paths.get(i);
//			Assert.assertTrue(p.startsWith(p));
//			for (TestCase tc : startsExpected.get(i)){
//				Assert.assertEquals(tc.testCondition, p.startsWith(tc.path));
//				Assert.assertEquals(tc.testCondition, p.startsWith(tc.path.toString()));
//			}
//			
//			System.out.println(" ====== " + i + " ====== ");
//			Assert.assertTrue(p.endsWith(p));
//			for (TestCase tc : startsExpected.get(i)){
//				System.out.println(p.endsWith(tc.path));
//			}
//		}
//		//System.out.print(Paths.get(".").normalize().startsWith(Paths.get("")));
//		//System.out.println(fs.getPath("dir", "dir1").startsWith("dir"));
//	}
//

	@Test
	public void testStartsWithEndsWith() throws Exception {
		// === absolute ===
		final Path p1 = fs.getPath("dir1", "dir2", "dir3").toAbsolutePath();
		final Path p11 = fs.getPath("dir2", "dir3");
		final Path p12 = fs.getPath("dir1", "dir2");
		final Path p13 = fs.getPath("dir1", "dir2").toAbsolutePath();
		
		Assert.assertFalse(p1.startsWith(p11));
		Assert.assertFalse(p1.startsWith(p11.toString()));
		Assert.assertTrue(p1.endsWith(p11));
		Assert.assertTrue(p1.endsWith(p11.toString()));
		//
		Assert.assertFalse(p1.startsWith(p12));
		Assert.assertFalse(p1.startsWith(p12.toString()));
		Assert.assertFalse(p1.endsWith(p12));
		Assert.assertFalse(p1.endsWith(p12.toString()));
		//
		Assert.assertTrue(p1.startsWith(p13));
		Assert.assertTrue(p1.startsWith(p13.toString()));
		Assert.assertFalse(p1.endsWith(p13));
		Assert.assertFalse(p1.endsWith(p13.toString()));
		
		// === relative ===
		final Path p2 = fs.getPath(".", "dir2", "dir3");
		final Path p21 = fs.getPath("dir2", "dir3");
		final Path p22 = fs.getPath(".", "dir2");
		final Path p23 = fs.getPath(".", "dir2", "dir3").toAbsolutePath();

		Assert.assertFalse(p2.startsWith(p21));
		Assert.assertFalse(p2.startsWith(p21.toString()));
		Assert.assertTrue(p2.endsWith(p21));
		Assert.assertTrue(p2.endsWith(p21.toString()));
		//
		Assert.assertTrue(p2.startsWith(p22));
		Assert.assertTrue(p2.startsWith(p22.toString()));
		Assert.assertFalse(p2.endsWith(p22));
		Assert.assertFalse(p2.endsWith(p22.toString()));
		//
		Assert.assertFalse(p2.startsWith(p23));
		Assert.assertFalse(p2.startsWith(p23.toString()));
		Assert.assertFalse(p2.endsWith(p23));
		Assert.assertFalse(p2.endsWith(p23.toString()));
	}
	
	@Test
	public void testResolveRelativise() throws Exception {
		final Path a1 = fs.getPath(".", "dir2");
		final Path a2 = fs.getPath("dir2").toAbsolutePath();
		
		final Path b1 = fs.getPath("..", "dir1");
		final Path b2 = fs.getPath("dir1").toAbsolutePath();
		
		// === RESOLVE ===
		Assert.assertTrue(a1.resolve(b1).equals(fs.getPath(".\\dir2\\..\\dir1")));
		Assert.assertTrue(a1.resolve(b1.toString()).equals(fs.getPath(".\\dir2\\..\\dir1")));
		Assert.assertTrue(a1.resolve(b2).equals(b2));
		Assert.assertTrue(a1.resolve(b2.toString()).equals(b2));
		
		Assert.assertTrue(a2.resolve(b1).equals(fs.getPath(a2.toString(), b1.toString())));
		Assert.assertTrue(a2.resolve(b1.toString()).equals(fs.getPath(a2.toString(), b1.toString())));
		Assert.assertTrue(a2.resolve(b2).equals(b2));
		Assert.assertTrue(a2.resolve(b2.toString()).equals(b2));

		// === RESOLVE  SIBLING===
		Assert.assertTrue(a1.resolveSibling(b1).equals(fs.getPath(".\\..\\dir1")));
		Assert.assertTrue(a1.resolveSibling(b1.toString()).equals(fs.getPath(".\\..\\dir1")));
		Assert.assertTrue(a1.resolveSibling(b2).equals(b2));
		Assert.assertTrue(a1.resolveSibling(b2.toString()).equals(b2));

		Assert.assertTrue(a2.resolveSibling(b1).equals(fs.getPath(a2.getParent().toString(), b1.toString())));
		Assert.assertTrue(a2.resolveSibling(b1.toString()).equals(fs.getPath(a2.getParent().toString(), b1.toString())));
		Assert.assertTrue(a2.resolveSibling(b2).equals(b2));
		Assert.assertTrue(a2.resolveSibling(b2.toString()).equals(b2));

		// === RELATIVIZE ===
		Assert.assertTrue(a1.relativize(b1).equals(fs.getPath("..\\..\\..\\dir1")));
		boolean exception = false;
		try {
			a1.relativize(b2);
		} catch (Exception e) {
			exception = true;
		}
		Assert.assertTrue(exception);
		
		exception = false;
		try {
			a2.relativize(b1);
		} catch (Exception e) {
			exception = true;
		}
		Assert.assertTrue(exception);
		Assert.assertTrue(a2.relativize(b2).equals(fs.getPath("..\\dir1")));
	}
	
	@Test
	public void testToUri() throws Exception {
		final Path a1 = fs.getPath(".", "dir2", "dir3");
		final Path a2 = fs.getPath(".", "dir2", "dir3").toAbsolutePath().normalize();
		Assert.assertEquals("encrypted:" + Paths.get(basePath).normalize().toAbsolutePath().toUri() + "./dir2/dir3",
							 a1.toUri().toString());
		Assert.assertEquals("encrypted:" + Paths.get(basePath).normalize().toAbsolutePath().toUri() + "dir2/dir3",
							a2.toUri().toString());
		
	}
	
	@Test
	public void testToRealPath() throws Exception {
		final Path a1 = fs.getPath(".", "dir2", "dir3");
		final Path a2 = fs.getPath(".", "dir2", "dir3").toAbsolutePath();
		Files.createDirectories(a1);
		Files.createDirectories(a2);
		Assert.assertEquals(a1.toRealPath().toString(), a1.toAbsolutePath().normalize().toString());
		Assert.assertEquals(a2.toRealPath().toString(), a2.normalize().toString());
	}
	
	//@Test
	public void testZip() throws Exception {
		//Files.createFile(Paths.get(TestUtils.SANDBOX_PATH, "1.zip"));
		//URI u = URI.create("encrypted:jar:file:/1.zip!/");
		URI u = URI.create("jar:file:/1.zip!/");
		Map<String, Object> env = new HashMap<String, Object>();
		env.put(FileSystemEncrypted.FileSystemEncryptedEnvParams.ENV_CREATE_UNDERLYING_FILE_SYSTEM, true);
		env.put("create", "true");
		FileSystems.newFileSystem(u, env);
		final Path a1 = Paths.get(u);
		
		u = URI.create("jar:file:/2.zip!/");
		FileSystems.newFileSystem(u, env);
		final Path a2 = Paths.get(u);
		System.out.println(a1.startsWith(a2));
		
		URI u1 = URI.create("jar:file:/1.zip!/");
		URI u2 = URI.create("encrypted:jar:file:/2.zip!/");
		
		System.out.println(u1.compareTo(u2));
		//final Path a1 = Paths.get(URI.create("jar:file:/1.zip!/"));
//		FileSystems.newFileSystem(URI.create("file:/"), Collections.EMPTY_MAP);
		//Files.createDirectories(a1);
	}	
	
	
	@Test
	public void testToFile() throws Exception {
		final Path a1 = fs.getPath(".", "dir2", "dir3");
		final Path a2 = fs.getPath(".", "dir2", "file.txt");
		final byte DATA = 31;
		Files.createDirectories(a1);
		Files.createFile(a2);
		
		try (OutputStream os = Files.newOutputStream(a2);) {
			os.write(DATA);
		}
		
		//
		File f1 = a1.toFile();
		boolean exception = false;
		try (FileInputStream fi = new FileInputStream(f1);) {
		} catch (Exception e) {
			exception = true;
		}
		Assert.assertTrue(exception);
		
		//
		File f2 = a2.toFile();
		exception = false;
		try (FileInputStream fi = new FileInputStream(f2);) {
			int i = fi.read();
			Assert.assertEquals(i, DATA);//for default stream cipher one byte is not encoded.
		} catch (Exception e) {
			exception = true;
		}
		Assert.assertFalse(exception);
		//
		
	}
	
	@After
	public void clean() throws IOException {
		TestUtils.deleteFilesystems(mFspe);
	}
	
	public void testStaff() throws Exception {
//		final Path a3 = fs.getPath("dir2.txt");
//		//Files.createFile(Paths.get("dir2.txt").normalize().toAbsolutePath());
		// === ===
		
//		try (SeekableByteChannel os = Files.newByteChannel(a3, StandardOpenOption.WRITE);) {
//		os.write(ByteBuffer.wrap(new byte [] {31, 31, 31}));
//	}
		
//		SeekableByteChannel sb =  Files.newByteChannel(a3.normalize().toAbsolutePath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ);
//		sb.write(ByteBuffer.wrap("123".getBytes()));
//		sb.close();
//		
//		sb = Files.newByteChannel(a3.normalize().toAbsolutePath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
//		ByteBuffer bb = ByteBuffer.allocate(100);
//		sb.position(0);
//		sb.read(bb);
//		sb.close();
//		System.out.println(new String( bb.array()));
//		System.out.println("123");
	}
	
}
