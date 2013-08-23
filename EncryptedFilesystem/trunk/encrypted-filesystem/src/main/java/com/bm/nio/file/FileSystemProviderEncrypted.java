package com.bm.nio.file;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderMismatchException;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import sun.nio.fs.WindowsFileSystemProvider;


import com.bm.nio.channels.SeekableByteChannelEncrypted;
import com.bm.nio.file.utils.TestUtils;
import com.sun.nio.zipfs.ZipFileSystem;
import com.sun.nio.zipfs.ZipFileSystemProvider;

import static com.bm.nio.file.FileSystemEncrypted.FileSystemEncryptedEnvParams.ENV_CREATE_UNDERLYING_FILE_SYSTEM;



public class FileSystemProviderEncrypted extends FileSystemProvider {

	//private final Map<Path, FileSystemEncrypted> filesystems = new HashMap<>();
	
	//A correspondence between encrypted folder root of underlying filesystem and
	//encrypted filesystem object
	private Object lock = new Object();
	private final TreeMap<Path, FileSystemEncrypted> filesystems = new TreeMap<Path, FileSystemEncrypted>(new ComparatorPath());
	@Override
	public String getScheme() {
		return "encrypted";
	}
	
//	public void test(){
//		for (Path p : filesystems.navigableKeySet())
//			System.out.println(p);
//	}
	
	//correct Path comparison
	//sync3\
	//sync3\dir
	//sync39\
	//sync4\
	//below is incorrect order:
	//.\enc29
	//.\enc2\test
	//.\enc3
	//correct:
	//.\enc2
	//.\enc2\test
	//.\enc20	
	protected static class ComparatorPath implements Comparator<Path> {

		@Override
		public int compare(Path o1, Path o2) {
			return o1.toUri().compareTo(o2.toUri());
//			final int o1Cnt = o1.getNameCount();
//			final int o2Cnt = o2.getNameCount();
//			final int oMin = Math.min(o1Cnt, o2Cnt);
//			for (int i = 0; i < oMin; i ++){
//				final int compare = o1.getName(i).compareTo(o2.getName(i));
//				if (compare != 0)
//					return compare;
//			}
//			if (o1Cnt == o2Cnt)
//				return 0;
//			else if (o1Cnt > o2Cnt)//longer-->greater
//				return 1;
//			else 
//				return -1;
		}
		
	}
	
	@Override
	public SeekableByteChannel newByteChannel(Path path,
			Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		if (!(path instanceof PathEncrypted))
			throw new ProviderMismatchException();
		try {
			synchronized (lock) {
				//corresponding fileSystemEncrypted should have encrypted configuration to open channel
				return ((PathEncrypted)path).getFileSystem().newByteChannel(path, options, attrs);
			}
		} catch (GeneralSecurityException e) {
			throw new IOException(e);
		}
	}
	

	/**
	 * Generates filesystem and creates configuration file by default
	 * @param uri - encrypted path, encrypted:file:///D:/enc1 or encrypted:jar:///D:/enc1.zip 
	 * @param env - list of parameters for the filesystem
	 * @return
	 * @throws IOException
	 */
	@Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env)
            throws IOException
        {
            Path path = null;
            if (Boolean.TRUE.equals(env.get(ENV_CREATE_UNDERLYING_FILE_SYSTEM)))
				try {
				    path = uriToPath(uri);
				} catch (FileSystemNotFoundException e) {
					FileSystems.newFileSystem(toUnderUri(uri), env);
				    path = uriToPath(uri);
				}
			else
				path = uriToPath(uri);
            return newFileSystem(path, env);
        }
	
	// === URI vs PATH ===
	// URI contains filesystem predicate, like file:///D:/
	// PATH is just a plain path, but with a link to filesystem it belongs, i.e. D:/ and Path.getFileSystem = WindowsFileSystem
	// ===
	
	// === absolute vs real paths ===
	//	real = absolute + real names in Path (not synonyms, case sensitive)
	// real = D:/Users
	// absolute = D:/Пользователи
	// ===
	
	/**
	 * @param path - underlyng path, D:/enc1 or D:/enc1.zip
	 * @param env - list of parameters for the filesystem, See {@link FileSystemEncrypted.FileSystemEncryptedEnvParams}
	 * @return
	 * @throws IOException
	 */
	@Override
	public FileSystem newFileSystem(Path path, Map<String, ?> env)
			throws IOException {
		//if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS));//TODO: consider using link options
		if (!Files.exists(path))
			throw new InvalidPathException(path.toString(),
					" Path does not exist and can not be used as encrypted storage");
		//TOD1O: create configuration file
		// or choose already existing one
		// configuration file is managed by filesystem!
        synchronized(filesystems) {
            Path realPath = null;
            if (validatePath(path)) {
            	//TODO: consider using toAbstractPath().normalize()
            	//otherwise it will work incorrectly with links
                realPath = path.toRealPath();
                //if (filesystems.containsKey(realPath))
                if (getFileSystemInternal(realPath) != null)
                    throw new FileSystemAlreadyExistsException("Path: " + path);
            } else
            	throw new InvalidPathException(path.toString(), path + " can not be used as encrypted storage");
            FileSystemEncrypted encfs = null;
           	try {
				encfs = new FileSystemEncrypted(this, path, env);
			} catch (GeneralSecurityException e) {
				throw new IOException(e);
			}
            filesystems.put(realPath, encfs);
            return encfs;
        }
	}
	
	/**
	 * @param p - encrypted filesystem, encrypted:file:///D:/enc1
	 * @return filesystem, or null if not found
	 * <p> See {@link #getFileSystemInternal(Path)}
	 */
	protected FileSystemEncrypted getFileSystemInternal(PathEncrypted p){
		return getFileSystemInternal(((PathEncrypted)p).getUnderPath());
	}
	/**
	 * Find path in filesystem and return encrypted filesystem if already exists 
	 * Path should be from underlying filesystem 
	 * @param p - underlyng path, D:/enc1 or D:/enc1.zip
	 * @return filesystem, or null if not found
	 * <p> See {@link #getFileSystemInternal(URI)}
	 */
	//Covered +
	protected FileSystemEncrypted getFileSystemInternal(Path p){
		
		//should find root folder 
		//added filesystem check p.getFileSystem().equals(h.getKey().getFileSystem())
		//to prevent errors when comparing pah from different providers or filesystems. They should not be equal
		final Entry<Path, FileSystemEncrypted> h = filesystems.ceilingEntry(p);
		if (h != null && p.getFileSystem().equals(h.getKey().getFileSystem())){
			final Path ceiling = h.getKey();
			if (p.startsWith(ceiling))
				return h.getValue();
			if (ceiling.startsWith(p))
				return h.getValue();
		}
		final Entry<Path, FileSystemEncrypted> l = filesystems.floorEntry(p);
		if (l != null && p.getFileSystem().equals(l.getKey().getFileSystem())){
			final Path floor = l.getKey();
			if (p.startsWith(floor))
				return l.getValue();
			if (floor.startsWith(p))
				return l.getValue();
		}		
		return null;
		
//		final String pStr = p.toUri().getPath();
//		//should find root folder 
//		//compare using URI's path
//		final Entry<Path, FileSystemEncrypted> h = filesystems.ceilingEntry(p);
//		if (h != null && p.getFileSystem().equals(h.getKey().getFileSystem())){
//			final String ceiling = h.getKey().toUri().getPath();
//			if (ceiling == null || pStr == null){
//				if (ceiling == null && pStr == null)
//					return h.getValue();
//				else
//					return null;
//			}
//			if (pStr.startsWith(ceiling))
//				return h.getValue();
//			if (ceiling.startsWith(pStr))
//				return h.getValue();
//		}
//		final Entry<Path, FileSystemEncrypted> l = filesystems.floorEntry(p);
//		if (l != null && p.getFileSystem().equals(l.getKey().getFileSystem())){
//			final String floor = l.getKey().toUri().getPath();
//			if (floor == null || pStr == null){
//				if (floor == null && pStr == null)
//					return l.getValue();
//				else
//					return null;
//			}
//			if (pStr.startsWith(floor))
//				return l.getValue();
//			if (floor.startsWith(pStr))
//				return l.getValue();
//		}		
//		return null;

//		//should find root folder 
//		final Entry<Path, FileSystemEncrypted> h = filesystems.ceilingEntry(p);
//		if (h != null){
//			final Path ceiling = h.getKey();
//			if (p.startsWith(ceiling))
//				return h.getValue();
//			if (ceiling.startsWith(p))
//				return h.getValue();
//		}
//		final Entry<Path, FileSystemEncrypted> l = filesystems.floorEntry(p);
//		if (l != null){
//			final Path floor = l.getKey();
//			if (p.startsWith(floor))
//				return l.getValue();
//			if (floor.startsWith(p))
//				return l.getValue();
//		}		
//		return null;
	}
	
    /**
     * Gets encrypted URI, returns underlying filesystem path 
     * @param uri - encrypted path, encrypted:file:///D:/enc1
     * @return Path in Underlying filesystem, Path(file:///D:/enc1)
     */
    //IMPORTANT: it will return underlying path with DECRYPTED path name! 
	//i.e.(file:///D:/enc1/dir, instead of file:///D:/enc1/F11A)
    protected Path uriToPath(URI uri) {
        String scheme = uri.getScheme();
        if ((scheme == null) || !scheme.equalsIgnoreCase(getScheme())) {
            throw new IllegalArgumentException("URI scheme is not '" + getScheme() + "'");
        }
        Path underPath = Paths.get(toUnderUri(uri)).toAbsolutePath();
        return underPath;
    }
    
    /**
     * Transforms 
     * @param encUri - encrypted URI, i.e. encrypted:file:///D:/enc1
     * @return - underlying URI, i.e. file:///D:/enc1
     */
    protected URI toUnderUri(URI encUri){
        // only support legacy URL syntax encrypted:{uri}
        final String spec = encUri.getSchemeSpecificPart();
        return URI.create(spec);
    }
    
    private boolean validatePath(Path path) {
        try {
            BasicFileAttributes attrs =
                Files.readAttributes(path, BasicFileAttributes.class);
            if (!attrs.isRegularFile() && !attrs.isDirectory())
                throw new UnsupportedOperationException();
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }
    
	//Gets encrypted URI, encrypted:file:///D:/enc1/dir
    //returns FileSystemEncrypted, FileSystemEncrypted(D:/enc1)    
	@Override
	public FileSystem getFileSystem(URI uri) {
		return getFileSystemInternal(uri);
//        synchronized (filesystems) {
//            FileSystem encfs = null;
//            try {
//            	encfs = getFileSystem(uriToPath(uri).toRealPath());
//            } catch (IOException x) {
//                // ignore the ioe from toRealPath(), return FSNFE
//            	//System.out.println(x);
//            }
//            if (encfs == null)
//                throw new FileSystemNotFoundException();
//            return encfs;
//        }
	}
	//Gets encrypted URI, encrypted:file:///D:/enc1/dir
    //returns FileSystemEncrypted, FileSystemEncrypted(D:/enc1)	
	/**
	 * @param uri - encrypted URI, encrypted:file:///D:/enc1/dir
	 * @return FileSystemEncrypted, FileSystemEncrypted(D:/enc1)
	 * <p> See {@link #getFileSystemInternal(Path)}}
	 */
	protected FileSystemEncrypted getFileSystemInternal(URI uri) {
        synchronized (filesystems) {
            FileSystemEncrypted encfs = null;
            try {
            	encfs = getFileSystemInternal(uriToPath(uri).toRealPath());
            } catch (IOException x) {
                // ignore the ioe from toRealPath(), return FSNFE
            	//System.out.println(x);
            }
            if (encfs == null)
                throw new FileSystemNotFoundException();
            return encfs;
        }
	}

	
	//Gets encrypted URI, encrypted:file:///D:/enc1/dir  (corresponds to underlying file:///D:/enc1/F11A)
    //returns PathEncrypted, PathEncrypted(dir)
	@Override
	public Path getPath(URI uri){
		//1
		//final String underSpec = uri.getSchemeSpecificPart();//here underlying spec, like file://D:/enc1/dir
		//final URI underUri = new URI(spec);//here underlying URI, like file://D:/enc1/dir
		//final String spec = underUri.getSchemeSpecificPart();//here spec, like D:/enc1/dir
        //return getFileSystem(uri).getPath(spec);
		
		//2
		//final Path underPath = uriToPath(uri);
		//return getFileSystem(underPath).toEncrypted(underPath);
		//3
		//TOD1O: can be exception if no filesystem exists for thsi URI. Resolution - the same way works zip filesystem (correct for our case) 
		//but windowsfilesystem can do that

		//DONE: toEncrypted(uri) takes underlying URI, not encrypted!
		//TODO: what if URI == file://./enc1/dir???
		Path lPath;
		FileSystemEncrypted fs = getFileSystemInternal(uri);
		lPath = uriToPath(uri);//Path(file:///D:/enc1/dir) - incorrect under path
		try {
			lPath = fs.encryptUnderPath(lPath);//Path(file:///D:/enc1/F11A)
		} catch (GeneralSecurityException e) {
			throw new RuntimeException("Unable to encode path " + lPath, e);
		}
		lPath = fs.toEncrypted(lPath);//Path(encrypted:file:///D:/enc1/dir)
        return lPath;
	}

	
	protected void closeFilesystem(FileSystem fs){
		final ArrayList<Path> toRemove = new ArrayList<Path>();
		for(Entry<Path, FileSystemEncrypted> e : filesystems.entrySet()){
			if (e.getValue().equals(fs))
				toRemove.add(e.getKey());
		}
		synchronized (filesystems) {
			for (Path p : toRemove)
				filesystems.remove(p);
		}
	}
	
	/**
	 * @param dir - encrypted path (encrypted:file:///D:/enc1/dir)
	 * @param filter
	 * @return
	 * @throws IOException
	 */
	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir,
			Filter<? super Path> filter) throws IOException {
		//TOTEST:
		if (!(dir instanceof PathEncrypted))//analogy with other providers
			throw new ProviderMismatchException();
		if (!Files.isDirectory(dir))
			throw new NotDirectoryException(dir.toString());
		return ((PathEncrypted)dir).newDirectoryStream(filter);
		
		
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs)
			throws IOException {
		if (!(dir instanceof PathEncrypted))//analogy with other providers
			throw new ProviderMismatchException();
//		if (!Files.isDirectory(dir))
//			throw new NotDirectoryException(dir.toString());
		//TOTEST: can contain errors!
		//Files.createDirectory(((PathEncrypted)dir.toAbsolutePath().normalize()).getUnderPath(), attrs);
		Files.createDirectory(((PathEncrypted)dir.toAbsolutePath()).getUnderPath(), attrs);
	}

	/**
	 * @param path - encrypted path (encrypted:file:///D:/enc1/dir)
	 * @throws IOException
	 */
	@Override
	public void delete(Path path) throws IOException {
		if (!(path instanceof PathEncrypted))
			throw new ProviderMismatchException();
		final PathEncrypted p = (PathEncrypted)path;
		FileSystemEncrypted fs = p.getFileSystem();//getFileSystemInternal(p);
		if (fs == null)
			throw new FileSystemNotFoundException();
		fs.delete(p);
		//ZipFileSystemProvider zs; zs.delete(path)
	}

	
	//private byte [] copyBuffer = new byte [4*1024];
	//private ByteBuffer copyBufferB = ByteBuffer.wrap(copyBuffer);
	@Override
	public void copy(Path source, Path target, CopyOption... options)
			throws IOException {
		boolean replaceExisting = false, copyAttributes = false;
		LinkOption [] linkOptions = new LinkOption[0];
		for (CopyOption co : options)
			if (co.equals(StandardCopyOption.REPLACE_EXISTING))
				replaceExisting = true;
			else if (co.equals(LinkOption.NOFOLLOW_LINKS))
				linkOptions = new LinkOption[] {LinkOption.NOFOLLOW_LINKS};
			else if (co.equals(StandardCopyOption.COPY_ATTRIBUTES))
				copyAttributes = true;
		
		
		if (replaceExisting)
			Files.deleteIfExists(target);
		if (Files.isDirectory(source, linkOptions)){
			Files.createDirectory(target);
		} else {
			
			int bufferSize = 4*1024;//12
			if (source.getFileSystem() instanceof FileSystemEncrypted)
				bufferSize = Math.max(bufferSize, ((FileSystemEncrypted)source.getFileSystem()).getConfig().getBlockSize());
			if (target.getFileSystem() instanceof FileSystemEncrypted)
				bufferSize = Math.max(bufferSize, ((FileSystemEncrypted)target.getFileSystem()).getConfig().getBlockSize());
			//making threadsafe. There is more sophisticated way using new buffer with weak refs for every concurrent thread
			final byte [] copyBuffer = new byte [bufferSize];
			final ByteBuffer copyBufferB = ByteBuffer.wrap(copyBuffer);
			
			final Set<OpenOption> srcOpts = new HashSet<>();
			srcOpts.add(StandardOpenOption.READ);
			final Set<OpenOption> trgOpts = new HashSet<>();
			trgOpts.add(StandardOpenOption.WRITE);
			trgOpts.add(StandardOpenOption.CREATE_NEW);
			//StandardOpenOption.APPEND
			try(SeekableByteChannel srcChannel = Files.newByteChannel(source, srcOpts)){
				try(SeekableByteChannel trgChannel = Files.newByteChannel(target, trgOpts)){
					while (srcChannel.read(copyBufferB) >= 0 || copyBufferB.position() != 0){
						copyBufferB.flip();
						copyBufferB.position(trgChannel.write(copyBufferB));
						copyBufferB.compact();
					}
				}
			}
		}
		
		if (copyAttributes){
			//TODO:  copy attributes as by StandardCopyOption.COPY_ATTRIBUTES
			//ZipFileSystemProvider zp; zp.copy(src, target, options)
		}
		
	}

//	public void getFileSystems(Collection<FileSystemEncrypted> fss){
//		//consider using read lock
//		for(Entry<Path, FileSystemEncrypted> e : filesystems.entrySet()){
//			fss.add(e.getValue());
//		}
//	}
//	
	public Iterable<FileSystemEncrypted> getFileSystems(){
		final List<FileSystemEncrypted> ar = new ArrayList<FileSystemEncrypted>();
		//consider using read lock
		for(Entry<Path, FileSystemEncrypted> e : filesystems.entrySet()){
			ar.add(e.getValue());
		}
		return ar;
	}
	
	@Override
	public void move(Path source, Path target, CopyOption... options)
			throws IOException {
		//TODO: implement later
		//should be able to move between different filesystems, not only encrypted
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException {
		// TOTEST
		if (!(path instanceof PathEncrypted) || !(path2 instanceof PathEncrypted))
			throw new ProviderMismatchException();
		return Files.isSameFile(((PathEncrypted)path).getFullUnderPath(), ((PathEncrypted)path2).getFullUnderPath());
	}

	@Override
	public boolean isHidden(Path path) throws IOException {
		if (!(path instanceof PathEncrypted))
			throw new ProviderMismatchException();
		// TOTEST
		return Files.isHidden(((PathEncrypted)path).getFullUnderPath());
	}

	@Override
	public FileStore getFileStore(Path path) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		// TODO Auto-generated method stub
		//TOTEST: method could be incorrect!
		if (!(path instanceof PathEncrypted))
			throw new ProviderMismatchException();
		//Path p = ((PathEncrypted)path.toAbsolutePath()).getUnderPath();
		Path p = ((PathEncrypted)path).getFullUnderPath();
		//Path p = ((PathEncrypted)path.toAbsolutePath().normalize()).getUnderPath();
		p.getFileSystem().provider().checkAccess(p, modes);
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path,
			Class<V> type, LinkOption... options) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param path - encrypted filesystem, encrypted:file:///D:/enc1
	 * @param type
	 * @param options
	 * @return
	 * @throws IOException
	 */
	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path,
			Class<A> type, LinkOption... options) throws IOException {
		if (!(path instanceof PathEncrypted))//analogy with other providers
			throw new ProviderMismatchException();
		//TOTEST
		return ((PathEncrypted)path).readAttributes(type, options);
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes,
			LinkOption... options) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value,
			LinkOption... options) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
