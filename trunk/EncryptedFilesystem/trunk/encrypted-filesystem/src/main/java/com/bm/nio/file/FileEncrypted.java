package com.bm.nio.file;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Encrypted filesystem should exist before creating FileEncrypted.
 * @author Mike
 *
 */
public class FileEncrypted extends File {

	private static final long serialVersionUID = -8051062756068390323L;
	private static final FileSystemProviderEncrypted fs = getEncryptedProvider();
	private static FileSystemProviderEncrypted getEncryptedProvider(){
		final String scheme = new FileSystemProviderEncrypted().getScheme();
		for (FileSystemProvider f : FileSystemProvider.installedProviders()){
			if (f.getScheme().endsWith(scheme) && f instanceof FileSystemProviderEncrypted)
				return (FileSystemProviderEncrypted)f;
		}
		return new FileSystemProviderEncrypted();
	}
	
//	private static URI fileToURI(File f) throws IOException{
//		return Paths.get(f.getCanonicalPath()).toUri();
//	}
//	
//	private static URI uriEncrypted(URI u) throws URISyntaxException{
//		return new URI(fs.getScheme() + ":" + u);
//	}
	
	@Override
	public PathEncrypted toPath() {
		return mPath;
	}

	/**
	 * Checks mFile - if this is real underlying file or file with decrypted name
	 * Firstly check if this is real file by decrypting name. If fail trying to encrypt to check if mFile has decrypted name
	 * @return
	 */
	private PathEncrypted initPath(final Path underPath){
//		try {
			//find corresponding encrypted filesystem
//			final Path mUnderPath = mFile.toPath();
			FileSystemEncrypted fse = fs.getFileSystemInternal(Paths.get(underPath.toString()));
			if (fse == null)
				throw new RuntimeException("Encrypted filesystem was not found for path " + underPath.toString());
			try {
				fse.decryptUnderPath(underPath);
				//mUnderPath - real underlying encrypted path, i.e. D:\enc1\1EE1
				return new PathEncrypted(fse, underPath);
			} catch (Exception e) {
				try {
					final Path encryptedPath = fse.encryptUnderPath(underPath);
					//mUnderPath - real decrypted path, i.e. D:\enc1\DIR1.
					//Need to encrypt it first, to real path D:\enc1\1EE1
					return new PathEncrypted(fse, encryptedPath);
				} catch (GeneralSecurityException e1) {
					throw new RuntimeException("Unable to locate encrypted or decrypted path " + this.getPath(), e1);
				}
			}
	}
	/**
	 * Corresponding encrypted path
	 */
	private final PathEncrypted mPath;
	private File mFile;
	public FileEncrypted(File parent, String child) {
		super(parent, child);
		
//		mFile = new File(parent, child);
		mPath = initPath(new File(parent, child).toPath());
		mFile = mPath.getUnderPath().toFile();
	}

	public FileEncrypted(String parent, String child) {
		super(parent, child);
		
//		mFile = new File(parent, child);
		mPath = initPath(new File(parent, child).toPath());
		mFile = mPath.getUnderPath().toFile();
	}

	//DONE: should accept real underlying path name i.e. D:\enc1\1EE1 or decrypted path name D:\enc1\DIR1
	//TEST that both types are accepted and resolved
	public FileEncrypted(String pathname) {
		super(pathname);
		
//		mFile = new File(pathname);
		mPath = initPath(new File(pathname).toPath());
		mFile = mPath.getUnderPath().toFile();
	}

	public static URI test(){
		return null;
	}
	public FileEncrypted(URI uri) {
		super(uri);
//		mFile = new File(uri);
		mPath = initPath(new File(uri).toPath());
		mFile = mPath.getUnderPath().toFile();
	}
	
	public File getUnderlyingFile(){
		return mFile;
	}
	
	@Override
	public String getName() {
		return mPath.getFileName().toString();
	}

	@Override
	public String getParent() {
		return mPath.getParent().toString();
	}

	@Override
	public File getParentFile() {
		return new FileEncrypted(mPath.getParent().toUri());
	}

	@Override
	public String getPath() {
		return mPath.toString();
	}

	@Override
	public boolean isAbsolute() {
		return mPath.isAbsolute();
	}

	@Override
	public String getAbsolutePath() {
		return mPath.toAbsolutePath().toString();
	}

	@Override
	public File getAbsoluteFile() {
		return new FileEncrypted(mPath.toAbsolutePath().toUri());
	}

	@Override
	public String getCanonicalPath() throws IOException {
		//TODO: consider returning decrypted path
		// canonical should be normalized and absolute
		return this.getCanonicalFile().toString();
	}

	@Override
	public File getCanonicalFile() throws IOException {
		//considering constructor can take real path, not only decrypted one
		return new FileEncrypted(mFile.getCanonicalPath());
	}

	@Override
	public URL toURL() throws MalformedURLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public URI toURI() {
		return mPath.toUri();
	}

	@Override
	public boolean canRead() {
		return mFile.canRead();
	}

	@Override
	public boolean canWrite() {
		return mFile.canWrite();
	}

	@Override
	public boolean exists() {
		return mFile.exists();
	}

	@Override
	public boolean isDirectory() {
		return mFile.isDirectory();
	}

	@Override
	public boolean isFile() {
		return mFile.isFile();
	}

	@Override
	public boolean isHidden() {
		return mFile.isHidden();
	}

	@Override
	public long lastModified() {
		return mFile.lastModified();
	}

	@Override
	public long length() throws RuntimeException {
		try {
			return Files.readAttributes(mPath, BasicFileAttributes.class).size();
		} catch (IOException e) {
			throw new RuntimeException("Unable to get size for pathname " + this.getPath(), e);
		}
	}

	@Override
	public boolean delete() {
		return mFile.delete();
	}

	@Override
	public void deleteOnExit() {
		mFile.deleteOnExit();
	}

	@Override
	public String[] list() throws RuntimeException {
		final List<String> res = new ArrayList<String>();
		try {
			Files.walkFileTree(mPath, new HashSet<FileVisitOption>(), 1, new FileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir,
						BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file,
						BasicFileAttributes attrs) throws IOException {
					res.add(file.getFileName().toString());
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file,
						IOException exc) throws IOException {
					throw new RuntimeException("Unable to list file " + file.toString());
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir,
						IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new RuntimeException("Unable to list for pathname " + this.getPath(), e);
		}
		return res.toArray(new String [res.size()]);
	}

	@Override
	public String[] list(FilenameFilter filter) {
		List<String> res = new ArrayList<String>();
		String [] list = this.list();
		for (String str : list){
			if (filter.accept(this, str)){
				res.add(str);
			}
		}
		return res.toArray(new String [res.size()]);
	}

	private File [] toFiles(String [] paths){
		File [] res = new File[paths.length];
		for (int i = 0; i < paths.length; i ++){
			res[i] = new FileEncrypted(this, paths[i]);
		}
		return res;
	}
	
	@Override
	public File[] listFiles() {
		return toFiles(this.list());
	}

	@Override
	public File[] listFiles(FilenameFilter filter) {
		return toFiles(this.list(filter));
	}

	@Override
	public File[] listFiles(FileFilter filter) {
		File [] files = this.listFiles();
		List<File> res = new ArrayList<File>();
		for (File f : files){
			if (filter.accept(f)){
				res.add(f);
			}
		}
		return res.toArray(new File[res.size()]);
	}

	@Override
	public boolean mkdir() {
		return mFile.mkdir();
	}

	@Override
	public boolean mkdirs() {
		return mFile.mkdirs();//considering underling file has encrypted names
	}

	@Override
	public synchronized boolean renameTo(File dest) {
		try {
			final FileEncrypted newFile = toCompatible(dest);
			//not atomic operation
			Files.move(mPath, newFile.mPath);
			mFile = newFile.mFile;
			super.renameTo(newFile.getUnderlyingFile());
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public boolean setLastModified(long time) {
		return mFile.setLastModified(time);
	}

	@Override
	public boolean setReadOnly() {
		return mFile.setReadOnly();
	}

	@Override
	public boolean setWritable(boolean writable, boolean ownerOnly) {
		return mFile.setWritable(writable, ownerOnly);
	}

	@Override
	public boolean setWritable(boolean writable) {
		return mFile.setWritable(writable);
	}

	@Override
	public boolean setReadable(boolean readable, boolean ownerOnly) {
		return mFile.setReadable(readable, ownerOnly);
	}

	@Override
	public boolean setReadable(boolean readable) {
		return mFile.setReadable(readable);
	}

	@Override
	public boolean setExecutable(boolean executable, boolean ownerOnly) {
		return mFile.setExecutable(executable, ownerOnly);
	}

	@Override
	public boolean setExecutable(boolean executable) {
		return mFile.setExecutable(executable);
	}

	@Override
	public boolean canExecute() {
		return mFile.canExecute();
	}

	@Override
	public long getTotalSpace() throws RuntimeException {
		try {
			return Files.getFileStore(mPath).getTotalSpace();
		} catch (IOException e) {
			throw new RuntimeException("Unable to get total space for pathname " + this.getPath(), e);
		}
	}

	@Override
	public long getFreeSpace() {
		try {
			return Files.getFileStore(mPath).getUnallocatedSpace();
		} catch (IOException e) {
			throw new RuntimeException("Unable to get total space for pathname " + this.getPath(), e);
		}
	}

	@Override
	public long getUsableSpace() {
		try {
			return Files.getFileStore(mPath).getUsableSpace();
		} catch (IOException e) {
			throw new RuntimeException("Unable to get total space for pathname " + this.getPath(), e);
		}
	}

	@Override
	public boolean equals(Object obj) {
        if ((obj == null) && (!(obj instanceof FileEncrypted))) {
            return false;
        }
        return mPath.equals(((FileEncrypted)obj).mPath);
	}

	@Override
	public int hashCode() {
		return mPath.hashCode();
	}

	@Override
	public String toString() {
		return this.getPath();
	}


	@Override
	public int compareTo(File pathname) {
//		if (!(pathname instanceof FileEncrypted))
//			throw new ProviderMismatchException();
//		final FileEncrypted pathNameEnc = (FileEncrypted)pathname;
//		if (!pathNameEnc.toPath().getFileSystem().equals(this.toPath().getFileSystem()))
//			throw new IllegalArgumentException("path " + pathname + " does not belong filesystem path " + this.toPath().getFileSystem().getRootDir());
//		toCompatible(pathname);
		return mFile.compareTo(toCompatible(pathname));
	}
	
	private FileEncrypted toCompatible(File file){
		if (!(file instanceof FileEncrypted))
			throw new ProviderMismatchException();
		final FileEncrypted fileEnc = (FileEncrypted)file;
		if (!fileEnc.toPath().getFileSystem().equals(this.toPath().getFileSystem()))
			throw new IllegalArgumentException("path " + file + " does not belong filesystem path " + this.toPath().getFileSystem().getRootDir());
		return fileEnc;
	}
	
	@Override
	public boolean createNewFile() throws IOException {
		try {
			Files.createFile(mPath);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
}
