package com.bm.nio.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
import java.security.GeneralSecurityException;
import java.util.Iterator;

import com.sun.nio.zipfs.ZipDirectoryStream;
import com.sun.nio.zipfs.ZipPath;

/**
 * @author Mike
 * For most functions it works as a proxy to underlying paths
 */
public class PathEncrypted implements Path {

	private final FileSystemEncrypted mFs;
	private final Path mUnderPath;
	/**
	 * @param fs - encrypted filesystem (i.e. folder of zip file etc.)
	 * @param path - underlying path (belongs to underlying filesystem)
	 */
	protected PathEncrypted(FileSystemEncrypted fs, Path path) throws InvalidPathException {
		mFs = fs;
		mUnderPath = path;
		if (!validateUnderPath(path))
			throw new InvalidPathException(path.toString(), "Path " + path.toString() + " is not encrypted path");
	}
	
	/**
	 * @param path - underlying Path(file:///D:/enc1/F11A) or Path(file:///F11A)
	 * @return
	 */
	private boolean validateUnderPath(Path path){
		try {
			mFs.decryptUnderPath(path);
		} catch (GeneralSecurityException e1) {
			return false;
		}
//		if (!mFs.isSubPath(path))
//			return false;
//		//calculate encrypted path = F11A
//		Path encPath = mFs.getRootDir().relativize(path);//= path - root
//		for (int i = 0; i < encPath.getNameCount(); i ++){
//			String encName = path.getName(i).getFileName().toString();
//			try {
//				mFs.decryptName(encName);
//			} catch (GeneralSecurityException e) {
//				return false;
//			}
//		}
		return true;
	}
	
	private void validatePath(Path path){
		if (!(path instanceof PathEncrypted) ||
				!path.getFileSystem().provider().getScheme().equals(
				   this.getFileSystem().provider().getScheme()))
			throw new ProviderMismatchException(path + " provider scheme mismatch");
		if (!((PathEncrypted)path).getFileSystem().equals(this.getFileSystem()))
			throw new IllegalArgumentException("File systems mismatch");
	}
	
	/**
	 * @return underlying path, i.e. D:\enc1
	 */
	protected Path getUnderPath(){
		return mUnderPath;
	}
	
	/**
	 * @return path with decrypted names, equals to underlying path, i.e. D:\F11A
	 */
	public Path getDecryptedPath(){
		return mUnderPath;
	}
	
	//+ Done
	@Override
	public boolean equals(Object obj) {
        return obj != null &&
                obj instanceof PathEncrypted &&
                this.mFs == ((PathEncrypted)obj).mFs &&
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
		return mFs;
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
		return mUnderPath.isAbsolute();
	}

	@Override
	public Path getRoot() {
		return mFs.toEncrypted(mFs.getRootDir());
	}

	@Override
	public Path getFileName() {
		return mFs.toEncrypted(mUnderPath.getFileName());
	}

	@Override
	public Path getParent() {
		if (mUnderPath.getParent() == null)
			return null;
		return mFs.toEncrypted(mUnderPath.getParent());
	}

	@Override
	public int getNameCount() {
		//TOREVIEW
		return mUnderPath.getNameCount();
	}

	@Override
	public Path getName(int index) {
		//TOREVIEW
		return mFs.toEncrypted(mUnderPath.getName(index));
	}

	@Override
	public Path subpath(int beginIndex, int endIndex) {
		//TOREVIEW
		return mFs.toEncrypted(mUnderPath.subpath(beginIndex, endIndex));
	}

	@Override
	public boolean startsWith(Path other) {
		//TOREVIEW
		validatePath(other);
		return mUnderPath.startsWith(((PathEncrypted)other).getUnderPath());
	}

	@Override
	public boolean startsWith(String other) {
		//TOREVIEW
		return mUnderPath.startsWith(mFs.getPath(other));
	}

	@Override
	public boolean endsWith(Path other) {
		//TOREVIEW
		validatePath(other);
		return mUnderPath.endsWith(((PathEncrypted)other).getUnderPath());
	}

	@Override
	public boolean endsWith(String other) {
		//TOREVIEW
		return mUnderPath.endsWith(mFs.getPath(other));
	}

	@Override
	public Path normalize() {
		return mFs.toEncrypted(mUnderPath.normalize());
	}

	/**
	 * Adds other path to this path by below rules:<br>
	 * Path(xxx).resolve(Path(D:/enc1/dir)) = Path(D:/enc1/dir)<br>
	 * Path(xxx).resolve(Path()) = Path(xxx)<br>
	 * Path(xxx).resolve(Path(/dir2)) = Path(xxx/dir2)<br>
	 * @param other
	 * @return
	 */
	@Override
	public Path resolve(Path other) {
		validatePath(other);
		if (other.isAbsolute())
			return other;
		if (other.toString().length() == 0)
			return this;
		//resolve under path for both - then transform to encrypted
		Path pathThis = this.getDecryptedPath();
		Path pathOther = ((PathEncrypted)other).getDecryptedPath();
		Path resolved = pathThis.resolve(pathOther);
		return mFs.toEncrypted(resolved);
	}

	@Override
	public Path resolve(String other) {
		return resolve(mFs.getPath(other));
	}

	/**
	 * Adds other path to this path's parent by below rules (xxx is a parent path):<br>
	 * Path(xxx/enc1).resolve(Path(D:/enc1/dir)) = Path(D:/enc1/dir)<br>
	 * Path(xxx/enc1).resolve(Path()) = Path(xxx)<br>
	 * Path(xxx/enc1).resolve(Path(/dir2)) = Path(xxx/dir2)<br>
	 * Path(xxx/enc1).resolve(Path(YYY)) = Path(YYY) if xxx == null<br>
	 * @param other
	 * @return
	 */
	@Override
	public Path resolveSibling(Path other) {
		if (other.isAbsolute())
			return other;
		if (other.toString().length() == 0)
			return this.getParent();
		if (this.getParent() == null)
			return other;
		
		return this.getParent().resolve(other);
	}

	@Override
	public Path resolveSibling(String other) {
		return resolveSibling(mFs.getPath(other));
	}

	@Override
	public Path relativize(Path other) {
		//TOREVIEW
		validatePath(other);
		Path pathThis = this.getDecryptedPath();
		Path pathOther = ((PathEncrypted)other).getDecryptedPath();
		Path relative = pathThis.relativize(pathOther);
		return mFs.toEncrypted(relative);
	}

	@Override
	public URI toUri() {
		URI res;
		try {
			//file:///D:/enc1/F11A --> file:///D:/enc1/dir
			res = mFs.decryptUnderPath(mUnderPath).toUri();
		} catch (GeneralSecurityException e) {
			throw new RuntimeException("Unable to decode path " + mUnderPath, e);
		}
		
		try {
			//file:///D:/enc1/dir --> encrypted:file:///D:/enc1/dir
			res = new URI(mFs.provider().getScheme(), res.toString(), null);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Unable to derive from URI: " + res + " for path: " + mUnderPath, e);
		}
		return res;
	}

	@Override
	public Path toAbsolutePath() {
		if (this.isAbsolute())
			return this;
		else
			return resolve(this);
	}

	@Override
	public Path toRealPath(LinkOption... options) throws IOException {
		// TODO Auto-generated method stub
		// TODO Consider returning decrypted path 
		return null;
	}

	@Override
	public File toFile() {
		// TOREVIEW
		return mUnderPath.toFile();
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
