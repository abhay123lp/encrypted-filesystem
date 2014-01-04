package com.bm.nio.file;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import sun.nio.fs.WindowsFileSystemProvider;

import com.bm.nio.channels.SeekableByteChannelEncrypted;
import com.bm.nio.file.ConfigEncrypted.Ciphers;
import com.bm.nio.utils.CacheLocal;
import com.bm.nio.utils.CipherUtils;
import com.sun.nio.zipfs.ZipFileSystem;


/**
 * @author Mike
 *	Single Filesystem usually corresponds to encrypted folder 
 */
public class FileSystemEncrypted extends FileSystem {

	
	private Path mRoot;
	private FileSystemProviderEncrypted mProvider;
	//file names that should not be encrypted, i.e. ".." or "."
	private Set<String> plainNames = new HashSet<String>(Arrays.asList(new String[] {"..", ".", "~"}));//Collections.emptySet();
	
	public static final class FileSystemEncryptedEnvParams{
		/**
		 * Name of configuration file for the filesystem. <br>
		 * Should be present either {@value #ENV_CONFIG_FILE} or {@value #ENV_CONFIG}<br>
		 * Value Type: {@link String}
		 */
		public static final String ENV_CONFIG_FILE = "env.config.file";
		/**
		 * Encryption configuration.  <br>
		 * Should be present either {@value #ENV_CONFIG_FILE} or {@value #ENV_CONFIG}. <br>
		 * Value Type: {@link ConfigEncrypted}
		 */
		public static final String ENV_CONFIG = "env.config";
		/**
		 * Set of names that should not be encoded.<br>
		 * Value Type: {@link Set} of {@link String} 
		 */
		public static final String ENV_PLAIN_NAMES = "env.plain.names";
		/**
		 * Used by Provider, instructs to create underlying filesystem if missing.
		 * Value Type: {@link boolean} 
		 */
		public static final String ENV_CREATE_UNDERLYING_FILE_SYSTEM = "env.createUnderlyingFileSystem";
		/**
		 * Password for file system
		 * Value Type: {@link char []} 
		 */
		public static final String ENV_PASSWORD = "env.password";
		public static final String ENV_CIPHERS = "env.ciphers";
	}
	
	private String configFile = "config.xml";
	private Path configPath;
	private ConfigEncrypted config = new ConfigEncrypted();
	//TODO: 14. consider taking PWD every time as a parameter
	//char [] pwd;
	SecretKeySpec key;
	//cache is required to make ciphers multithreaded (as son as filesystem should be is threadsafe)
	CacheLocal<Ciphers> ciphers;//DONE: fix to make work correctly
//	Ciphers ciphers;
	
	/**
	 * @param provider
	 * @param path - path of underlying filesystem, i.e. D:/enc1
	 * @param env
	 */
	@SuppressWarnings("unchecked")
	FileSystemEncrypted(FileSystemProviderEncrypted provider,
            Path path,
            Map<String, ?> env) throws IOException, GeneralSecurityException {
		if (!Files.isDirectory(path))//DONE: test with ZipFileSystem - is it's root defined as file or folder?
			throw new InvalidPathException(path.toString(), path + " can not be used as encrypted storage - not a directory");
		// parse env
		Object o;
		//
		o = env.get(FileSystemEncryptedEnvParams.ENV_PLAIN_NAMES);
		if (o != null){
			if (!(o instanceof Set)){
				throw new IllegalArgumentException("Parameter " + FileSystemEncryptedEnvParams.ENV_PLAIN_NAMES + " must be type of " + Set.class.getSimpleName());
			} else{
				plainNames = (Set<String>)o;
			}
		}
		
		//DONE: create common functions to encryps/decrypt file by password (store cipher in FileSystemEncrypted)
    	mRoot = path.toAbsolutePath();
    	mProvider = provider;
		config = loadConfig(mRoot, env);
    }
	
	/**
	 * @return encryption configuration
	 */
	public ConfigEncrypted getConfig(){
		return config;
	}
	
	/**
	 * @return path to the config file
	 */
	public Path getConfigPath(){
		return configPath;
	}
	
	/**
	 * Loads config either from config class or config file
	 * @param path - root pat of the filesystem
	 * @param env - parameters that may contain config class of config file name
	 * @return config class
	 * @throws IOException
	 */
	protected ConfigEncrypted loadConfig(Path path, Map<String, ?> env) throws IOException, GeneralSecurityException {
		ConfigEncrypted res = config;
		Object envConfFile, envConf, envPwd;
		envConfFile = env.get(FileSystemEncryptedEnvParams.ENV_CONFIG_FILE);
		envConf = env.get(FileSystemEncryptedEnvParams.ENV_CONFIG);
		envPwd = env.get(FileSystemEncryptedEnvParams.ENV_PASSWORD);
		final char [] pwd;
		if (envPwd == null){
			throw new IllegalArgumentException("Parameter " + FileSystemEncryptedEnvParams.ENV_PASSWORD + " must be present");
		} else{
			if (!(envPwd instanceof char [])){
				throw new IllegalArgumentException("Parameter " + FileSystemEncryptedEnvParams.ENV_PASSWORD + " must be type of " + (new char [0]).getClass().getSimpleName());
			} else{
				pwd = (char []) envPwd;
			}
		}
		//
//		if (envConfFile != null && envConf != null)
//			throw new IllegalArgumentException(
//					"Should be present only one parameter of: "
//							+ FileSystemEncryptedEnvParams.ENV_CONFIG_FILE
//							+ ", " + FileSystemEncryptedEnvParams.ENV_CONFIG);
		if (envConf != null)
			res = (ConfigEncrypted)envConf;
		if (envConfFile != null)
			configFile = envConfFile.toString();
		configPath = path.resolve(configFile);
		if (Files.exists(configPath)){
			if (envConf != null)
				throw new IllegalArgumentException("Existing config file " + configFile
						+ " cannot be overriden by "
						+ FileSystemEncryptedEnvParams.ENV_CONFIG
						+ " config parameter");
			//res = ConfigEncrypted.loadConfig(configPath);
			res = new ConfigEncrypted();
			res.loadConfig(configPath);
//		} else {
//			Files.createFile(configPath);
//			res.saveConfig(configPath);
		}
		//
		this.key = res.newSecretKeySpec(pwd);
		
//		ciphers = res.newCiphers(key);
		ciphers = initCiphersCache(res, key);
		// create config file if not exists
		if (!Files.exists(configPath)){
			Files.createFile(configPath);
			res.saveConfig(configPath);
		}
		return res;
	}
	
	private static CacheLocal<Ciphers> initCiphersCache(final ConfigEncrypted config, final SecretKeySpec key) throws GeneralSecurityException{
		// check that transformation is supported
		if (config.newCiphers(key) == null)
			throw new RuntimeException("Null ciphers returned for " + config.getTransformation() + " transformation");
		CacheLocal<Ciphers> ciphers = new CacheLocal<Ciphers>(){
			@Override
			protected Ciphers initialValue() {
				try {
					return config.newCiphers(key);
				} catch (GeneralSecurityException e) {
					throw new RuntimeException("Error initializing ciphers for " + config.getTransformation() + " transformation", e);
				}
			}
		};
		return ciphers;
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
		Path remainderPath;//path remainder inside FileSystem
		final String separator = plainUnderPath.getFileSystem().getSeparator();
		if (plainUnderPath.isAbsolute()){//Path(file:///D:/enc1/dir)
			remainderPath = mRoot.relativize(plainUnderPath);//Path(file:///dir)
			res = mRoot;
		}
		else{//Path(file:///dir)
			remainderPath = plainUnderPath;
			res = mRoot.getFileSystem().getPath("");//empty path if plainUnderPath is relative (not absolute)
		}
		// === encrypt ===
		for (int i = 0; i < remainderPath.getNameCount(); i ++){
			String currName = remainderPath.getName(i).toString();
			// fix for filesystems that nclude delimiter in the filename (ZipFileSystem)
			currName = currName.replace(separator, "");
			// === ===
			// fix for filesystems that can't resolve against empty path
			// so first step is to avoid res.resolve below
			if ((!plainUnderPath.isAbsolute()) && i == 0){
				if (plainNames.contains(currName))
					res = res.getFileSystem().getPath(currName);
				else
					res = res.getFileSystem().getPath(encryptName(currName));
				
//				res = remainderPath.getName(i);
				continue;
			}
			// === ===
			
			//should not encrypt ".." or "."
			if (plainNames.contains(currName))
				res = res.resolve(currName);
			else
				res = res.resolve(encryptName(currName));//CipherUtils.encryptName(currName, config.newCiphers(pwd).getEncipher()));
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
		Path res;
		Path remainderPath;
		final String separator = encUnderPath.getFileSystem().getSeparator();
		if (encUnderPath.isAbsolute()){//Path(file:///D:/enc1/F11A)
			remainderPath = mRoot.relativize(encUnderPath);//Path(file:///F11A)
			res = mRoot;
		}
		else{//Path(file:///F11A)
			remainderPath = encUnderPath;
			res = mRoot.getFileSystem().getPath("");//empty path if encUnderPath is relative (not absolute)
		}
		// === decrypt ===
		for (int i = 0; i < remainderPath.getNameCount(); i ++){
			String currName = remainderPath.getName(i).toString();
			// fix for filesystems that nclude delimiter in the filename (ZipFileSystem)
			currName = currName.replace(separator, "");
			// === ===
			// fix for filesystems that can't resolve against empty path
			// so first step is to avoid res.resolve below
			if ((!encUnderPath.isAbsolute()) && i == 0){
				if (plainNames.contains(currName))
					res = res.getFileSystem().getPath(currName);
				else
					res = res.getFileSystem().getPath(decryptName(currName));
//				res = remainderPath.getName(i);
				continue;
			}
			// === ===
			//should not decrypt ".." or "."
			if (plainNames.contains(currName))
				res = res.resolve(currName);
			else
				res = res.resolve(decryptName(currName));//CipherUtils.decryptName(currName, config.newCiphers(pwd).getDecipher()));
		}
		return res;
	}
	
	private String encryptName(String plainName) throws GeneralSecurityException {
		return CipherUtils.encryptName(plainName, ciphers.get().getEncipher());
//		return CipherUtils.encryptName(plainName, ciphers.getEncipher());
	}
	
	private String decryptName(String encName) throws GeneralSecurityException {
		return CipherUtils.decryptName(encName, ciphers.get().getDecipher());
//		return CipherUtils.decryptName(encName, ciphers.getDecipher());
	}


	protected SeekableByteChannel newByteChannel(Path path,
			Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException, GeneralSecurityException {
		final Path underPath = ((PathEncrypted)path).getFullUnderPath();
		
		synchronized (this) {
			final SeekableByteChannel uch = Files.newByteChannel(underPath, options);
			SeekableByteChannelEncrypted ch = SeekableByteChannelEncrypted.getChannel(uch);
			if (ch == null){
				Map<String, Object> props = new HashMap<String, Object>();
				//DONE: 5.5.6. Add password or cipher to parameters. 
				props.put(FileSystemEncryptedEnvParams.ENV_CONFIG, config);
//				props.put(FileSystemEncryptedEnvParams.ENV_PASSWORD, this.key);
				props.put(FileSystemEncryptedEnvParams.ENV_CIPHERS, ciphers.get());
//				props.put(FileSystemEncryptedEnvParams.ENV_CIPHERS, ciphers);
				ch = SeekableByteChannelEncrypted.newChannel(uch, props);//DONE: pass config encrypted
			}
			if (options.contains(StandardOpenOption.APPEND))
				ch.position(ch.size());
			return ch;
		}
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
	 * @return Encrypted path, i.e. Path(encrypted:file:///D:/enc1/dir/dir1) or Path(encrypted:file:///dir/dir1)
	 */
	@Override
	public Path getPath(String first, String... more) {
		// DONE: make to work properly
		//let underlying fs do all stick work. Create absolute path to not deal with .., ./ etc
		//TOREVIEW: use either of below
		//Path lPath = mRoot.resolve(mRoot.getFileSystem().getPath(first, more));//.toAbsolutePath()?
		Path lPath = mRoot.getFileSystem().getPath(first, more);
		try {
			lPath = encryptUnderPath(lPath);//Path(file:///D:/enc1/F11A)
		} catch (GeneralSecurityException e) {
			throw new RuntimeException("Unable to encode path " + lPath, e);
		}
		//DONE: lPath here should be D:/enc1/F11A, not D:/enc1/dir
		return toEncrypted(lPath);
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
		//also need to close channels and streams, consider using by many threads
		mProvider.closeFilesystem(this);
		isClosed = true;
	}

	@Override
	public synchronized boolean isOpen() {
		return !isClosed;
	}

	//+ Done
	/**
	 * @param path - encrypted path, encrypted:file:///D:/enc1/dir
	 * do not check since used internally, assume path belongs to this filesystem
	 */
	protected void delete(PathEncrypted path) throws IOException {
		//DONE: test that deletes correctly
		final Path fullUnderPath = path.getFullUnderPath();//this.getRootDir().resolve(path.getUnderPath());
		synchronized (this) {
			if (fullUnderPath.equals(mRoot)) {
				//check that no objects are left from this filesystem, otherwise throw exception
				//if not checked then it can remove config with some encrypted folders are left
				try (DirectoryStream<Path> ds = Files.newDirectoryStream(mRoot);){
					for (Path p : ds){
						if (p instanceof PathEncrypted)
							if (this.equals(((PathEncrypted)p).getFileSystem()))
								throw new DirectoryNotEmptyException(path.toString());
					}
				}

				//here no filesystem objects left, can safely delete config
				Files.deleteIfExists(configPath);
				try {
					Files.delete(fullUnderPath);
				} catch (DirectoryNotEmptyException e) {
//					//weird behavior - even if delete() was called for all files, they
					//can be deleted milliseconds later if were watched by watch service which wasn't
					//properly closed
//					System.out.println("Exception:");
//					try(DirectoryStream<Path> ds = Files.newDirectoryStream(fullUnderPath)){
//						for (Path p : ds){
//							System.out.println(p);
//						}
//					}
					throw e;
				} catch (Exception e) {
					// some filesystems doesn't allow deleting root, zipfs for example
				}
				close();
			} else{
				Files.delete(fullUnderPath);
			}
		}
		//delete some file within filesystem
	}
	
	//+ Done
	public void delete() throws IOException {
		this.delete(toEncrypted(mRoot));
	}
	
	@Override
	public boolean isReadOnly() {
		return mRoot.getFileSystem().isReadOnly();
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

	//+ Done
	@Override
	public WatchService newWatchService() throws IOException {
		return new WatchServiceEncrypted(mRoot.getFileSystem().newWatchService(), this);
	}

}
