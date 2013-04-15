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
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
	 * @return underlying path, i.e. D:\enc1\F11A or \F11A (relative path)
	 */
	protected Path getUnderPath(){
		return mUnderPath;
	}
	
	/**
	 * @return path with decrypted names, equals to underlying path, i.e. D:\enc1\F11A or \F11A (relative path)
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

	//+ Done
	@Override
	public int compareTo(Path other) {
		validatePath(other);
		final Path o1 = this;
		final Path o2 = other;
		final int o1Cnt = o1.getNameCount();
		final int o2Cnt = o2.getNameCount();
		final int oMin = Math.min(o1Cnt, o2Cnt);
		for (int i = 0; i < oMin; i ++){
			final int compare = o1.getName(i).toString().compareTo(o2.getName(i).toString());
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

	//+ Done
	@Override
	public String toString() {
		try {
			return mFs.decryptUnderPath(mUnderPath).toString();//just return decrypted path
		} catch (GeneralSecurityException e) {
			throw new RuntimeException("Unable to decode path " + mUnderPath, e);
		}
		
//		if (this.isAbsolute())
//			//DONE consider returning decrypted path. Returning decrypted path
//			return mUnderPath.toString();
//		else
//			return mUnderPath.toString();
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
	public PathEncrypted getRoot() {
		if (this.isAbsolute())
			return getFsRoot();
		else
			return null;
	}

	/**
	 * @return - default root for current filesystem, i.e. PathEncrypted(D:\enc)
	 * This path is always absolute
	 */
	public PathEncrypted getFsRoot() {
		return mFs.toEncrypted(mFs.getRootDir());
	}

	@Override
	public PathEncrypted getFileName() {
		return mFs.toEncrypted(mUnderPath.getFileName());
	}

	@Override
	public PathEncrypted getParent() {
		if (mUnderPath.getParent() == null)
			return null;
		return mFs.toEncrypted(mUnderPath.getParent());
	}

	/**
	 * Path(xxx\dir).getNameCount() = 1<br>
	 * Path(\dir).getNameCount() = 1<br>
	 * @return
	 */
	//+ Done
	@Override
	public int getNameCount() {
		//root should not be included by specification
		//need count only relative paths - starting from the root
		if (isAbsolute())
			return getRelativePath().getNameCount();
		else
			return getUnderPath().getNameCount();
	}

	//+ Done
	@Override
	public PathEncrypted getName(int index) {
		//root should not be included by specification
		//need count only relative paths - starting from the root
		if (isAbsolute())
			return getRelativePath().getName(index);
		else
			return mFs.toEncrypted(getUnderPath().getName(index));
	}

	/**
	 * Return PathEncrypted that is relative to the root, i.e.<br>
	 * Path(D:\enc1\dir).getRelativePath() = Path(\dir)
	 * Path(\dir).getRelativePath() = Path(\dir)
	 * @return
	 */
	protected PathEncrypted getRelativePath(){
		if (this.isAbsolute())
			return getFsRoot().relativize(this);
		else
			return this;
	}
	
	//+ Done
	@Override
	public PathEncrypted subpath(int beginIndex, int endIndex) {
		//TODO:
		if (isAbsolute())
			return getRelativePath().subpath(beginIndex, endIndex);
		else
			return mFs.toEncrypted(mUnderPath.subpath(beginIndex, endIndex));
//		Path relative = getRelativePath();
//		validatePath(other);
//		return mFs.toEncrypted(mUnderPath.subpath(beginIndex, endIndex));
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

	/**
	 * Path(.\dir).normalize() = Path(.\dir) 
	 * Path(..\dir).normalize() = Path(..\dir) 
	 * Path(D:\enc1\dir).normalize() = Path(D:\enc1\dir) 
	 * Path(D:\enc1\dir\..\dir1).normalize() = Path(D:\enc1\dir1) 
	 * Path(D:\enc1\dir\.\dir1).normalize() = Path(D:\enc1\dir\dir1) 
	 * @return
	 */
	//+ Done
	@Override
	public PathEncrypted normalize() {
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
	public PathEncrypted resolve(Path other) {
		validatePath(other);
		if (other.isAbsolute())
			return (PathEncrypted)other;
		if (other.toString().length() == 0)
			return this;
		//resolve under path for both - then transform to encrypted
		Path pathThis = this.getDecryptedPath();
		Path pathOther = ((PathEncrypted)other).getDecryptedPath();
		Path resolved = pathThis.resolve(pathOther);
		return mFs.toEncrypted(resolved);
	}

	@Override
	public PathEncrypted resolve(String other) {
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
	public PathEncrypted resolveSibling(Path other) {
		if (other.isAbsolute())
			return (PathEncrypted)other;
		if (other.toString().length() == 0)
			return this.getParent();
		if (this.getParent() == null)
			return (PathEncrypted)other;
		
		return this.getParent().resolve(other);
	}

	@Override
	public PathEncrypted resolveSibling(String other) {
		return resolveSibling(mFs.getPath(other));
	}

	@Override
	public PathEncrypted relativize(Path other) {
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

	/**
	 * Method trivially adds root component if missing
	 * xxx - filesystem root
	 * Path(.\dir).toAbsolutePath() = Path(xxx\dir) 
	 * Path(..\dir).toAbsolutePath() = exception 
	 * Path(D:\enc1\dir).toAbsolutePath() = Path(D:\enc1\dir) 
	 * Path(D:\enc1\dir\..\dir1).toAbsolutePath() = Path(D:\enc1\dir\..\dir1) 
	 * @return
	 */
	//+ Done
	@Override
	public PathEncrypted toAbsolutePath() {
		if (this.isAbsolute())
			return this;
		else
			return getFsRoot().resolve(this);
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

	//+ Done
	@Override
	public Iterator<Path> iterator() {
		final PathEncrypted relative = getRelativePath();
        return new Iterator<Path>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < relative.getNameCount();
            }

            @Override
            public Path next() {
                if (i < relative.getNameCount())
                    return relative.getName(i++);
                else
                    throw new NoSuchElementException();
            }

            @Override
            public void remove() {
    			throw new UnsupportedOperationException();
            }
        };
	}

}
