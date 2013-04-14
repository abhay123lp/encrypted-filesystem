package com.bm.nio.file;

import java.io.File;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.IllegalBlockSizeException;

import sun.font.CreatedFontTracker;


/**
 * @author Mike
 *	Single Filesystem usually corresponds to encrypted folder 
 */
public class FileSystemEncrypted extends FileSystem {

	
	private Path mRoot;
	private FileSystemProviderEncrypted mProvider; 
	
	public static final class FileSystemEncryptedEnvParams{
		public static final String ENV_CONFIG_FILE = "env.config.file";
	}
	
	private String configFile = "config.properties";
	/**
	 * @param provider
	 * @param path - path of underlying filesystem, i.e. D:/enc1
	 * @param env
	 */
	FileSystemEncrypted(FileSystemProviderEncrypted provider,
            Path path,
            Map<String, ?> env) throws IOException{
		if (!Files.isDirectory(path))//TODO: test with ZipFileSystem - is it's root defined as file or folder?
			throw new InvalidPathException(path.toString(), path + " can not be used as encrypted storage - not a directory");
		
		//TODO: create common functions to encryps/decrypt file by password (store cipher in FileSystemEncrypted)
		//read encrypted properties from file
		Object o = env.get(FileSystemEncryptedEnvParams.ENV_CONFIG_FILE);
		if (o != null)
			configFile = o.toString();
		Path config = path.resolve(configFile);
		if (!Files.exists(config))
			Files.createFile(config);
    	mRoot = path.toAbsolutePath();
    	mProvider = provider;
    }
	
	/**
	 * @param uri - underlying uri, i.e. file://D:/enc1/F11A
	 * @return - encrypted path
	 * <p> See {@link #toEncrypted(Path)}
	 */
	protected Path toEncrypted(URI uri){
		return toEncrypted(mProvider.uriToPath(uri));
	}
	
	
	/**
	 * @param path - underlying path, i.e. D:/enc1/F11A
	 * @return - encrypted path
	 * <p> See {@link #toEncrypted(URI)}
	 */
	protected PathEncrypted toEncrypted(Path path){
		//TOD1O: create correct transformation
		//14/04/2013 - not sure what above means?? Transform (decrypt) name?
		//TODO: think about using non absolute path
		if (!isSubPath(path))
			throw new IllegalArgumentException("path " + path + " does not belong filesystem path " + mRoot);
		return new PathEncrypted(this, path);//path.toAbsolutePath()?
	}

	/**
	 * Changing names for underPath - encrypts and transforms it to the real path.
	 * @param plainUnderPath - under path with decrypted names, i.e. Path(file:///D:/enc1/dir) or Path(file:///dir)
	 * @return - real under path with encrypted names, i.e. Path(file:///D:/enc1/F11A) or Path(file:///F11A)
	 * @throws GeneralSecurityException 
	 */
	protected Path encryptUnderPath(Path plainUnderPath) throws GeneralSecurityException{
		if (!isSubPath(plainUnderPath))
			throw new IllegalArgumentException("path " + plainUnderPath + " does not belong filesystem path " + mRoot);
		Path res = mRoot;
		Path remainderPath;
		if (plainUnderPath.isAbsolute())//Path(file:///D:/enc1/dir)
			remainderPath = mRoot.relativize(plainUnderPath);//Path(file:///dir)
		else//Path(file:///dir)
			remainderPath = plainUnderPath;
		for (int i = 0; i < remainderPath.getNameCount(); i ++){
			String currName = remainderPath.getName(i).toString();
			res = res.resolve(encryptName(currName));
		}
		return res;
	}

	/**
	 * Changing names for underPath - decrypts and transforms it to the virtual path.
	 * @param encUnderPath - under path with encrypted names, i.e. Path(file:///D:/enc1/F11A) or Path(file:///F11A)
	 * @return - virtual under path with decrypted names, i.e. Path(file:///D:/enc1/dir) or Path(file:///dir)
	 * @throws GeneralSecurityException
	 */
	protected Path decryptUnderPath(Path encUnderPath) throws GeneralSecurityException{
		if (!isSubPath(encUnderPath))
			throw new IllegalArgumentException("path " + encUnderPath + " does not belong filesystem path " + mRoot);
		Path res = mRoot;
		Path remainderPath;
		if (encUnderPath.isAbsolute())//Path(file:///D:/enc1/F11A)
			remainderPath = mRoot.relativize(encUnderPath);//Path(file:///F11A)
		else//Path(file:///F11A)
			remainderPath = encUnderPath;
		for (int i = 0; i < remainderPath.getNameCount(); i ++){
			String currName = remainderPath.getName(i).toString();
			res = res.resolve(decryptName(currName));
		}
		return res;
	}
	//returns root as PathEncrypted
	/**
	 * @return - encrypted root paths
	 */
	@Override
	public Iterable<Path> getRootDirectories() {
		final List<Path> roots = new ArrayList<Path>();
		roots.add(toEncrypted(mRoot));//should it be roots.add(mRoot);? Should be PathEncrypted, according to zipPath
		return roots;
	}
	
	protected Path getRootDir(){
		return mRoot;
	}
	
	/**
	 * Checks if given path belongs to the filesystem. Fon non absolute paths assume that it belongs to this filesystem
	 * @param path - underlying path, i.e. D:/enc1/F11A
	 * @return
	 */
	protected boolean isSubPath(Path path){
		return (!path.isAbsolute()) || path.startsWith(mRoot);
	}
	
	//Gets path string RELATIVE TO THE ROOT of filesystem, [D:/enc1/]dir, or together with filesystem root D:/enc1/dir
	//file:///D:/prog/workspace/encrypted-filesystem/src/test/sandbox/enc23/
	//Returns PathEncrypted 
	//example1: root=D:/enc1, first = dir, return D:/enc/dir
	//example2: root=D:/enc1, first = /dir, throws IllegalArgumentException (D:/dir is not part of D:/enc1)
	//TOTEST
	//DONE: decide string path should be encrypted or decrypted???
	/**
	 * Gets path string RELATIVE TO THE ROOT of filesystem, [D:/enc1/]dir, or together with filesystem root D:/enc1/dir.
	 * Parameters should be in plain decrypted string, i.e. D:/enc1/dir, not D:/enc1/F11A
	 * @param first - first path of path. i.e. D:/enc1/dir
	 * @param more - additional path part, i.e. dir1
	 * @return Encrypted path, i.e. Path(encrypted:file:///D:/enc1/dir/dir1)
	 */
	@Override
	public Path getPath(String first, String... more) {
		// DONE: make to work properly
		//let underlying fs do all stick work. Create absolute path to not deal with .., ./ etc
		Path lPath = mRoot.resolve(mRoot.getFileSystem().getPath(first, more));//.toAbsolutePath()?
		try {
			lPath = encryptUnderPath(lPath);//Path(file:///D:/enc1/F11A)
		} catch (GeneralSecurityException e) {
			throw new RuntimeException("Unable to encode path " + lPath, e);
		}
		//DONE: lPath here should be D:/enc1/F11A, not D:/enc1/dir
		return toEncrypted(lPath);
	}
	
	
	protected String encryptName(String plainName) throws GeneralSecurityException {
		//TODO:
		//throw new IllegalArgumentException();
		return plainName;
	}
	
	protected String decryptName(String encName) throws GeneralSecurityException {
		//TODO:
		return encName;
	}
	
	/**
	 * Sets custom decoder, for example base64 to decode filenames and file contents
	 */
	public void setNameDecoder(){
		//TODO: implement
		//TODO: make it to save/load implementing class from properties
	}
	
	@Override
	public FileSystemProvider provider() {
		return mProvider;
	}

	
	private boolean isClosed = false;
	@Override
	public synchronized void close() throws IOException {
		// TODO remove itself from filesystemprovider
		//also need to close channels and streams
		mProvider.closeFilesystem(this);
		isClosed = true;
	}

	@Override
	public synchronized boolean isOpen() {
		return !isClosed;
	}

	/**
	 * @param path - encrypted path, encrypted:file:///D:/enc1/dir
	 * do not check since used internally, assume path belongs to this filesystem
	 */
	protected void delete(PathEncrypted path) throws IOException {
		//DONE: test that deletes correctly
		synchronized (this) {
			Files.delete(path.getUnderPath());
			if (path.getUnderPath().equals(mRoot)) {//root was deleted - close
				close();
			}
		}
		//delete some file within filesystem
	}
	
	public void delete() throws IOException {
		this.delete(toEncrypted(mRoot));
	}
	
	@Override
	public boolean isReadOnly() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getSeparator() {
		return mRoot.getFileSystem().getSeparator();
	}

	@Override
	public Iterable<FileStore> getFileStores() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PathMatcher getPathMatcher(String syntaxAndPattern) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WatchService newWatchService() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
