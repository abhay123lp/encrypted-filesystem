package com.bm.nio.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.NoSuchElementException;


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
                this.mFs.equals(((PathEncrypted)obj).mFs) &&
                compareTo((Path) obj) == 0;
	}

	//+ Done
	@Override
	public int hashCode() {
		return toUri().hashCode();
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
	
	//+ Done
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

	//+ Done
	@Override
	public PathEncrypted getFileName() {
		return mFs.toEncrypted(mUnderPath.getFileName());
	}

	//+ Done
	@Override
	public PathEncrypted getParent() {
		if (mUnderPath.getParent() == null)
			return null;
		if (mUnderPath.equals(mFs.getRootDir()))//check if path is root the don't have parent.
			return null;
		final PathEncrypted parent = mFs.toEncrypted(mUnderPath.getParent());
		return parent;
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
		if (isAbsolute())
			return getRelativePath().subpath(beginIndex, endIndex);
		else
			return mFs.toEncrypted(mUnderPath.subpath(beginIndex, endIndex));
	}

	//+ Done
	@Override
	public boolean startsWith(Path other) {
		validatePath(other);
		return mUnderPath.startsWith(((PathEncrypted)other).getUnderPath());
	}

	//+ Done
	@Override
	public boolean startsWith(String other) {
		return startsWith(mFs.getPath(other));
	}

	//+ Done
	@Override
	public boolean endsWith(Path other) {
		validatePath(other);
		return mUnderPath.endsWith(((PathEncrypted)other).getUnderPath());
	}

	//+ Done
	@Override
	public boolean endsWith(String other) {
		return endsWith(mFs.getPath(other));
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
	 * Adds other path to this (this + other) path by below rules:<br>
	 * Path(xxx).resolve(Path(D:/enc1/dir)) = Path(D:/enc1/dir)<br>
	 * Path(xxx).resolve(Path()) = Path(xxx)<br>
	 * Path(xxx).resolve(Path(/dir2)) = Path(xxx/dir2)<br>
	 * @param other
	 * @return
	 */
	//+ Done
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

	//+ Done
	@Override
	public PathEncrypted resolve(String other) {
		return resolve(mFs.getPath(other));
	}

	/**
	 * Adds other path to this path's parent by below rules (xxx is a parent path):<br>
	 * Path(xxx/enc1).resolveSibling(Path(D:/enc1/dir)) = Path(D:/enc1/dir)<br>
	 * Path(xxx/enc1).resolveSibling(Path()) = Path(xxx)<br>
	 * Path(xxx/enc1).resolveSibling(Path(/dir2)) = Path(xxx/dir2)<br>
	 * Path(xxx/enc1).resolveSibling(Path(YYY)) = Path(YYY) if xxx == null<br>
	 * @param other
	 * @return
	 */
	//+ Done
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

	//+ Done
	@Override
	public PathEncrypted resolveSibling(String other) {
		return resolveSibling(mFs.getPath(other));
	}

	/**
	 * other - this
	 * @param other
	 * @return
	 */
	//+ Done
	@Override
	public PathEncrypted relativize(Path other) {
		validatePath(other);
		Path pathThis = this.getDecryptedPath();
		Path pathOther = ((PathEncrypted)other).getDecryptedPath();
		Path relative = pathThis.relativize(pathOther);
		return mFs.toEncrypted(relative);
	}

	//Covered +
	@Override
	public URI toUri() {
		URI res;
		try {
			//file:///D:/enc1/F11A --> file:///D:/enc1/dir
			//file:///./F11A --> file:///D:/enc1/./dir
			res = mFs.getRootDir().resolve(mFs.decryptUnderPath(mUnderPath)).toUri();
		} catch (GeneralSecurityException e) {
			throw new RuntimeException("Unable to decode path " + mUnderPath, e);
		}
		
		try {
			//file:///D:/enc1/dir --> encrypted:file:///D:/enc1/dir
			//file:///D:/enc1/./dir --> encrypted:file:///D:/enc1/./dir
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

	//+ Done
	@Override
	public Path toRealPath(LinkOption... options) throws IOException {
		// DONE Consider returning decrypted path
		Path realUnderPath = mFs.getRootDir().resolve(mUnderPath).toRealPath(options);
		//note: in case of links Path D:/enc2/dir can belong to filesystem D:/enc1/ !
		return mFs.toEncrypted(realUnderPath);
	}

	/**
	 * Returns physical file associated with this path. Name and contents are encrypted.
	 * @return
	 */
	//+ Done
	@Override
	public File toFile() {
		return  mFs.getRootDir().resolve(mUnderPath).toFile();
	}

	//+ Done
	@Override
	public WatchKey register(WatchService watcher, Kind<?>[] events,
			Modifier... modifiers) throws IOException {
		if (!(watcher instanceof WatchServiceEncrypted))
			throw new ProviderMismatchException("Incompatible watch service to register");
		final Path wholeUnderPath = mFs.getRootDir().resolve(mUnderPath);
		final WatchServiceEncrypted wse = (WatchServiceEncrypted) watcher;
		final WatchKey underKey = wholeUnderPath.register(wse.getUnderWatcher(), events, modifiers);
		//DONE: here if someone will call watcher.poll in another tread - watcher can't find key because it
		// still don't have it
		// Resolution: as soon as we use new WatchKeyEncrypted every time - watcher.poll will 
		//return correct result
		//final WatchKey key = new WatchServiceEncrypted.WatchKeyEncrypted(underKey, wse, this);
		return wse.toWatchKeyEncrypted(underKey);
	}

	//+ Done
	@Override
	public WatchKey register(WatchService watcher, Kind<?>... events)
			throws IOException {
		return register(watcher, events, new WatchEvent.Modifier[0]);
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
