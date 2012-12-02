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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sun.font.CreatedFontTracker;


/**
 * @author Mike
 *	Single Filesystem usually corresponds to encrypted folder 
 */
public class FileSystemEncrypted extends FileSystem {

	
	private Path mRoot;
	private FileSystemProviderEncrypted mProvider; 
	/**
	 * @param provider
	 * @param path - path of underlying filesystem, i.e. D:/enc1
	 * @param env
	 */
	FileSystemEncrypted(FileSystemProviderEncrypted provider,
            Path path,
            Map<String, ?> env) throws IOException{
		if (!Files.isDirectory(path))
			throw new InvalidPathException(path.toString(), path + " can not be used as encrypted storage - not a directory");
		
		//TODO: create common functions to encryps/decrypt file by password
		//read encrypted properties from file
		Path config = path.resolve("config.properties");
		if (!Files.exists(config))
			Files.createFile(config);
    	mRoot = path.toAbsolutePath();
    	mProvider = provider;
    }
	
	/**
	 * @param uri - underlying uri, i.e. file://D:/enc1/dir
	 * @return - encrypted path
	 * <p> See {@link #toEncrypted(Path)}
	 */
	protected Path toEncrypted(URI uri){
		return toEncrypted(mProvider.uriToPath(uri));
	}
	
	/**
	 * @param path - underlying path, i.e. D:/enc1/dir
	 * @return - encrypted path
	 * <p> See {@link #toEncrypted(URI)}
	 */
	protected Path toEncrypted(Path path){
		//TODO: create correct transformation 
		//try {
			//return Paths.get(new URI(provider().getScheme() + ":" + p.toUri()));
		if (!path.startsWith(mRoot))
			throw new IllegalArgumentException("path " + path + " does not belong filesystem path " + mRoot);
		return new PathEncrypted(this, path.toAbsolutePath());
		//} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
		//	e.printStackTrace();
		//	return null;
		//}
	}

	//returns root as PathEncrypted
	@Override
	public Iterable<Path> getRootDirectories() {
		final List<Path> roots = new ArrayList<Path>();
		roots.add(toEncrypted(mRoot));
		return roots;
	}
	
	//Gets path string RELATIVE TO THE ROOT of filesystem, [D:/enc1/]dir, or together with filesystem root D:/enc1/dir
	//file:///D:/prog/workspace/encrypted-filesystem/src/test/sandbox/enc23/
	//Returns PathEncrypted 
	//example1: root=D:/enc1, first = dir, return D:/enc/dir
	//example2: root=D:/enc1, first = /dir, throws IllegalArgumentException (D:/dir is not part of D:/enc1)
	//TOTEST
	@Override
	public Path getPath(String first, String... more) {
		// TODO: make to work properly
		//let underlying fs do all stick work. Create absolute path to not deal with .., ./ etc
		final Path lPath = mRoot.resolve(mRoot.getFileSystem().getPath(first, more).toAbsolutePath());
		//if root is D:/enc1, and furst = D:/enc2/dir, then throw exception
		//if (!lPath.startsWith(mRoot))
		//	throw new IllegalArgumentException("path " + lPath + " does not belong filesystem path " + mRoot);
		//return new PathEncrypted(this, lPath.toAbsolutePath());
		return toEncrypted(lPath);
		//return null;
	}
	
	@Override
	public FileSystemProvider provider() {
		return mProvider;
	}

	
	private boolean isClosed = false;
	//TOTEST
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
