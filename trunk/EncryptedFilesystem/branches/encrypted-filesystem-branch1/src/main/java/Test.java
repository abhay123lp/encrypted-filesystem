import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		new Test().testStartsForZip();
	}
	
	public void testStartsForZip() throws Exception{
		URI uri = URI.create("jar:file:/zipfstest.zip!/BAR");
		Map<String, String> env = new HashMap<>(); 
	    env.put("create", "true");
	    System.out.println(uri.getScheme());
	    //com.sun.nio.zipfs.ZipFileSystemProvider z;
		FileSystem zipfs = FileSystems.newFileSystem(uri, env);
		
		Path p = zipfs.provider().getPath(uri);
		System.out.println(p.toString());
		
		Path p1 = zipfs.provider().getPath(URI.create("jar:file:/zipfstest.zip!/BAR/PAR"));
		System.out.println (p1.startsWith(p));//should be true
		Path p2 = zipfs.provider().getPath(URI.create("jar:file:/zipfstest.zip!/PAR/PAR1"));
		System.out.println(p2.startsWith(p));//should be false
		
		
		//checking problem when mRoot =  jar:file:/zipfstest.zip!/BAR/PAR, fileSystem = jar:file:/zipfstest.zip
		//and received "/BAR" as getPath function
		Path p3 = p.getFileSystem().getPath("/PAR");
		System.out.println(p2.resolve("PAR"));//should be true
		
	}

}
