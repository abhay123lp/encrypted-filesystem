package com.bm.nio.file;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Encrypted filesystem should exist before creating FileEncrypted.
 * @author Mike
 *
 */
/**
 * Encrypted filesystem should exisist before creating FileEncrypted
 * This is required because Encrypted filesystem responds to name encryption
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
	
	@Override
	public PathEncrypted toPath() {
		return mPath;
	}

	/**
	 * Checks mFile - if this is real underlying file or file with decrypted name
	 * Firstly check if this is real file by decrypting name. If fail trying to encrypt to check if mFile has decrypted name
	 * @return
	 */
	private static InitResult initPath(final File underFile){
		return initPath(underFile, null);
	}
	private static InitResult initPath(final File underFile, char[] password){
//		try {
			//find corresponding encrypted filesystem
			final Path underPath = underFile.toPath();
			final Path absoluteUnderPath = underPath.toAbsolutePath().normalize();
//			final Boolean isDirectory = underFile.isDirectory();
//			FileSystemEncrypted fse = fs.getFileSystemInternal(Paths.get(underPath.toString()));
			//getting filesystem
			//TODO: handle relative paths:. Decided to go with Root
			//1. consider whether to create filesystem from parent or from relative root for relative paths
			//2. For root filesystem should be searched against root
			//3. For parent filesystem should be searched against parent but underlying path should be frpm parent.
			FileSystemEncrypted fse = fs.getFileSystemInternal(absoluteUnderPath);
			if (fse == null){
				if (password == null)
					throw new RuntimeException("Encrypted filesystem was not found for path " + underPath.toString());
				final Path fsRoot;
				if (!underFile.isAbsolute()){
					fsRoot = underPath.subpath(0, 1).toAbsolutePath().normalize();
				}
				else if (underFile.isDirectory())
					fsRoot = absoluteUnderPath;
				else
					fsRoot = absoluteUnderPath.getParent();
				try {
					fse = fs.newFileSystem(fsRoot, newEnv(password));
				} catch (FileSystemAlreadyExistsException e) {
					fse = fs.getFileSystemInternal(fsRoot);
					//below will be checked in above getFileSystem for parent
//					//if fse is still null it means that there is Encrypted filesystem that
//					//is not appropriate for this path, 
//					//for example there is filesystem: D:/dir/enc1 and path is: D:/dir
//					if (fse == null)
//						throw new RuntimeException("Filesystem cannot be created for path " + underPath.toString() + ". There is nested filesystem exists", e);
				} catch (IOException e) {
					throw new RuntimeException("Exception creating encrypted filesystem for path " + underPath.toString(), e);
				}
			}
			// ========= deriving encrypted path =========
			PathEncrypted res;
			try {
				fse.decryptUnderPath(absoluteUnderPath);
				//mUnderPath - real underlying encrypted path, i.e. D:\enc1\1EE1
				res = new PathEncrypted(fse, underPath);
//				return new PathEncrypted(fse, underPath);
			} catch (Exception e) {
				try {
					final Path encryptedPath = fse.encryptUnderPath(absoluteUnderPath);
					//mUnderPath - real decrypted path, i.e. D:\enc1\DIR1.
					//Need to encrypt it first, to real path D:\enc1\1EE1
					if (underPath.isAbsolute())
						res = new PathEncrypted(fse, encryptedPath);
					else
						res = new PathEncrypted(fse, fse.getRootDir().relativize(encryptedPath));
				} catch (GeneralSecurityException e1) {
					throw new RuntimeException("Unable to locate encrypted or decrypted path " + underPath.toString(), e1);
				}
			}
			return new InitResult(res.getUnderPath().toUri(), res);
	}
	
	private static URI toUnderUri(URI uri){
		if (uri.getScheme().equals(fs.getScheme()))
			return fs.getPath(uri).getUnderPath().toUri();
		else
			return uri;
	}
	
	private static HashMap<String, Object> newEnv(char[] password){
		HashMap<String, Object> env = new HashMap<String, Object>();
		env.put(FileSystemEncrypted.FileSystemEncryptedEnvParams.ENV_PASSWORD, password);
		env.put(FileSystemEncrypted.FileSystemEncryptedEnvParams.ENV_CREATE_CONFIG, false);
		env.put(FileSystemEncrypted.FileSystemEncryptedEnvParams.ENV_CREATE_UNDERLYING_FILE_SYSTEM, true);
		return env;
	}

	private static class InitResult{
		final URI uri;
		final PathEncrypted path;
		public InitResult(URI uri, PathEncrypted path) {
			super();
			this.uri = uri;
			this.path = path;
		}
	}
	
	private FileEncrypted(final InitResult res){
		super(res.uri);
		mPath = res.path;
	}
	/**
	 * Corresponding encrypted path
	 */
	private final PathEncrypted mPath;
	/**
	 * Creates encrypted file and if no encryptedfilesystem exists creates a new one with a root in parent folder
	 * with default configuration and given password
	 * @param file
	 * @param password
	 */
	public FileEncrypted(File file, char [] password){
		this(initPath(file, password));
	}
	public FileEncrypted(File parent, String child) {
		this(initPath(new File(parent, child)));
	}

	public FileEncrypted(String parent, String child) {
		this(initPath(new File(parent, child)));
	}

	//DONE: should accept real underlying path name i.e. D:\enc1\1EE1 or decrypted path name D:\enc1\DIR1
	//TEST that both types are accepted and resolved
	public FileEncrypted(String pathname) {
		this(initPath(new File(pathname)));
	}

	public FileEncrypted(URI uri) {
		this(initPath(new File(toUnderUri(uri))));
	}
	
	public File getUnderlyingFile(){
		return mPath.getUnderPath().toFile();
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
	public FileEncrypted getParentFile() {
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
	public FileEncrypted getAbsoluteFile() {
		return new FileEncrypted(mPath.toAbsolutePath().toUri());
	}

	@Override
	public String getCanonicalPath() throws IOException {
		//DONE: consider returning decrypted path
		// canonical should be normalized and absolute
		return this.getCanonicalFile().toString();
	}

	@Override
	public FileEncrypted getCanonicalFile() throws IOException {
		//considering constructor can take real path, not only decrypted one
		return new FileEncrypted(super.getCanonicalPath());
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
		return super.canRead();
	}

	@Override
	public boolean canWrite() {
		return super.canWrite();
	}

	@Override
	public boolean exists() {
		return super.exists();
	}

	@Override
	public boolean isDirectory() {
		return super.isDirectory();
	}

	@Override
	public boolean isFile() {
		return super.isFile();
	}

	@Override
	public boolean isHidden() {
		return super.isHidden();
	}

	@Override
	public long lastModified() {
		return super.lastModified();
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
		return super.delete();
	}

	@Override
	public void deleteOnExit() {
		super.deleteOnExit();
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
		return super.mkdir();
	}

	@Override
	public boolean mkdirs() {
		return super.mkdirs();//considering underling file has encrypted names
	}

	@Override
	public synchronized boolean renameTo(File dest) {
		try {
			final FileEncrypted newFile = toCompatible(dest);
			//not atomic operation
			Files.move(mPath, newFile.mPath);
//			mFile = newFile.mFile;
			super.renameTo(newFile.getUnderlyingFile());
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public boolean setLastModified(long time) {
		return super.setLastModified(time);
	}

	@Override
	public boolean setReadOnly() {
		return super.setReadOnly();
	}

	@Override
	public boolean setWritable(boolean writable, boolean ownerOnly) {
		return super.setWritable(writable, ownerOnly);
	}

	@Override
	public boolean setWritable(boolean writable) {
		return super.setWritable(writable);
	}

	@Override
	public boolean setReadable(boolean readable, boolean ownerOnly) {
		return super.setReadable(readable, ownerOnly);
	}

	@Override
	public boolean setReadable(boolean readable) {
		return super.setReadable(readable);
	}

	@Override
	public boolean setExecutable(boolean executable, boolean ownerOnly) {
		return super.setExecutable(executable, ownerOnly);
	}

	@Override
	public boolean setExecutable(boolean executable) {
		return super.setExecutable(executable);
	}

	@Override
	public boolean canExecute() {
		return super.canExecute();
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
		return super.compareTo(toCompatible(pathname));
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
