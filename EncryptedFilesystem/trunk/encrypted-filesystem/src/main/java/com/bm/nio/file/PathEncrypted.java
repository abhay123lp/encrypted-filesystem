package com.bm.nio.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;

import com.sun.nio.zipfs.ZipDirectoryStream;
import com.sun.nio.zipfs.ZipPath;

/**
 * @author Mike
 * For most functions it works as a proxy to underlying paths
 */
public class PathEncrypted implements Path {

	private final FileSystemEncrypted pFs;
	private final Path mUnderPath;
	/**
	 * @param fs - encrypted filesystem (i.e. folder of zip file etc.)
	 * @param path - underlying path (belongs to underlying filesystem)
	 */
	protected PathEncrypted(FileSystemEncrypted fs, Path path) throws InvalidPathException {
		pFs = fs;
		mUnderPath = path;
		if (!validate(path))
			throw new InvalidPathException(path.toString(), "Path " + path.toString() + " is not encrypted path");
	}
	
	private static boolean validate(Path path){
		for (int i = 0; i < path.getNameCount(); i ++){
			String name = path.getName(i).getFileName().toString();
			//TODO: consider validating path to have encrypted name and throw exception if not. ()
		}
			
		return true;
	}
	
	/**
	 * @return underlying path, i.e. D:\enc1
	 */
	protected Path getUnderPath(){
		return mUnderPath;
	}
	
	/**
	 * @return path with decrypted names, equals to underlying path, i.e. D:\fa987
	 */
	public Path getDecryptedPath(){
		return mUnderPath;
	}
	
	//+ Done
	@Override
	public boolean equals(Object obj) {
        return obj != null &&
                obj instanceof PathEncrypted &&
                this.pFs == ((PathEncrypted)obj).pFs &&
                compareTo((Path) obj) == 0;
	}

	//TOTEST
	@Override
	public int compareTo(Path other) {
		final Path o1 = this;
		final Path o2 = other;
		final int o1Cnt = o1.getNameCount();
		final int o2Cnt = o2.getNameCount();
		final int oMin = Math.min(o1Cnt, o2Cnt);
		for (int i = 0; i < oMin; i ++){
			final int compare = o1.getName(i).compareTo(o2.getName(i));
			if (compare != 0)
				return compare;
		}
		if (o1Cnt == o2Cnt)
			return 0;
		else if (o1Cnt > o2Cnt)//longer-->greater
			return 1;
		else 
			return -1;
	}

	//TOTEST
	@Override
	public String toString() {
		if (this.isAbsolute())
			//TODO consider returning decrypted path
			return mUnderPath.toString();
		else
			return mUnderPath.toString();
	}
	
	@Override
	public FileSystemEncrypted getFileSystem() {
		return pFs;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <A extends BasicFileAttributes> A readAttributes(Class<A> type, LinkOption... options) throws IOException {
		return (A) new FileAttributesEncrypted(Files.readAttributes(mUnderPath, type, options)); 
	}

    DirectoryStream<Path> newDirectoryStream(Filter<? super Path> filter) throws IOException {
    		return new DirectoryStreamEncrypted(this, filter);
        }
	
	@Override
	public boolean isAbsolute() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Path getRoot() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path getFileName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNameCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Path getName(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path subpath(int beginIndex, int endIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean startsWith(Path other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean startsWith(String other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean endsWith(Path other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean endsWith(String other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Path normalize() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path resolve(Path other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path resolve(String other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path resolveSibling(Path other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path resolveSibling(String other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path relativize(Path other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI toUri() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path toAbsolutePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path toRealPath(LinkOption... options) throws IOException {
		// TODO Auto-generated method stub
		// TODO Consider returning decrypted path 
		return null;
	}

	@Override
	public File toFile() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WatchKey register(WatchService watcher, Kind<?>[] events,
			Modifier... modifiers) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WatchKey register(WatchService watcher, Kind<?>... events)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<Path> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

}
