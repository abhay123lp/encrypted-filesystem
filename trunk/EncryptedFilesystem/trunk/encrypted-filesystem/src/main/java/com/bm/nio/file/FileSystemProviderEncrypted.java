package com.bm.nio.file;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderMismatchException;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.bm.nio.file.channel.SeekableByteChannelEncrypted;




public class FileSystemProviderEncrypted extends FileSystemProvider {

	//private final Map<Path, FileSystemEncrypted> filesystems = new HashMap<>();
	
	//A correspondence between encrypted folder root of underlying filesystem and
	//encrypted filesystem object
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
		
	}
	
	@Override
	public SeekableByteChannel newByteChannel(Path path,
			Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		if (!(path instanceof PathEncrypted))
			throw new ProviderMismatchException();
		//TODO:
		try {
			final Path underPath =  ((PathEncrypted)path).getUnderPath();
			//underPath.getFileSystem().provider().checkAccess(underPath, AccessMode.READ);
			//Files.isReadable(underPath);
			//Files.isWritable(underPath);
			SeekableByteChannelEncrypted ch = new SeekableByteChannelEncrypted(Files.newByteChannel(underPath, options));
			if (options.contains(StandardOpenOption.APPEND))
				ch.position(ch.size());
			return ch;
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
            Path path = uriToPath(uri);
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
	 * @param env - list of parameters for the filesystem
	 * @return
	 * @throws IOException
	 */
	@Override
	public FileSystem newFileSystem(Path path, Map<String, ?> env)
			throws IOException {
		//TODO: create configuration file
		// or choose already existing one		
        synchronized(filesystems) {
            Path realPath = null;
            if (validatePath(path)) {
                realPath = path.toRealPath();
                //if (filesystems.containsKey(realPath))
                if (getFileSystemInternal(realPath) != null)
                    throw new FileSystemAlreadyExistsException();
            } else
            	throw new InvalidPathException(path.toString(), path + " can not be used as encrypted storage");
            FileSystemEncrypted encfs = null;
           	encfs = new FileSystemEncrypted(this, path, env);
            filesystems.put(realPath, encfs);
            return encfs;
        }
	}
	
	/**
	 * Find path in filesystem and return encrypted filesystem if already exists 
	 * Path should be from underlying filesystem 
	 * @param p - underlyng path, D:/enc1 or D:/enc1.zip
	 * @return
	 * <p> See {@link #getFileSystemInternal(URI)}}
	 * Covered +
	 */
	protected FileSystemEncrypted getFileSystemInternal(Path p){
		//should find root folder 
		final Entry<Path, FileSystemEncrypted> h = filesystems.ceilingEntry(p);
		if (h != null){
			final Path ceiling = h.getKey();
			if (p.startsWith(ceiling))
				return h.getValue();
			if (ceiling.startsWith(p))
				return h.getValue();
		}
		final Entry<Path, FileSystemEncrypted> l = filesystems.floorEntry(p);
		if (l != null){
			final Path floor = l.getKey();
			if (p.startsWith(floor))
				return l.getValue();
			if (floor.startsWith(p))
				return l.getValue();
		}		
		return null;
	}
	
    /**
     * Gets encrypted URI, returns underlying filesystem path 
     * @param uri - encrypted path, encrypted:file:///D:/enc1
     * @return Path in Underlying filesystem, Path(file:///D:/enc1)
     */
    protected Path uriToPath(URI uri) {
        String scheme = uri.getScheme();
        if ((scheme == null) || !scheme.equalsIgnoreCase(getScheme())) {
            throw new IllegalArgumentException("URI scheme is not '" + getScheme() + "'");
        }
        try { 
            // only support legacy URL syntax encrypted:{uri}
            String spec = uri.getSchemeSpecificPart();
            return Paths.get(new URI(spec)).toAbsolutePath();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
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

	
	//Gets encrypted URI, encrypted:file:///D:/enc1/dir
    //returns PathEncrypted, PathEncrypted(dir)
	@Override
	public Path getPath(URI uri) {
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
        return getFileSystemInternal(uri).toEncrypted(uri);
		
	}

	
	protected void closeFilesystem(FileSystem fs){
		final ArrayList<Path> toRemove = new ArrayList<Path>();
		for(Entry<Path, FileSystemEncrypted> e : filesystems.entrySet()){
			if (e.getValue().equals(fs))
				toRemove.add(e.getKey());
		}
		for (Path p : toRemove)
			filesystems.remove(p);
			
	}
	
	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir,
			Filter<? super Path> filter) throws IOException {
		return new DirectoryStreamEncrypted();
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs)
			throws IOException {
	}

	@Override
	public void delete(Path path) throws IOException {
	}

	
	private byte [] copyBuffer = new byte [4*1024];
	private ByteBuffer copyBufferB = ByteBuffer.wrap(copyBuffer);
	@Override
	public void copy(Path source, Path target, CopyOption... options)
			throws IOException {
		final Set<OpenOption> srcOpts = new HashSet<>();
		srcOpts.add(StandardOpenOption.READ);
		final Set<OpenOption> trgOpts = new HashSet<>();
		trgOpts.add(StandardOpenOption.WRITE);
		//StandardOpenOption.APPEND
		SeekableByteChannel srcChannel = this.newByteChannel(source, srcOpts, (FileAttribute<Object>)null);
		SeekableByteChannel trgChannel = this.newByteChannel(target, trgOpts, (FileAttribute<Object>)null);
		while (srcChannel.read(copyBufferB) > 0){
			trgChannel.write(copyBufferB);
		}
		
	}

	@Override
	public void move(Path source, Path target, CopyOption... options)
			throws IOException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isHidden(Path path) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public FileStore getFileStore(Path path) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path,
			Class<V> type, LinkOption... options) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path,
			Class<A> type, LinkOption... options) throws IOException {
		// TODO Auto-generated method stub
		return null;
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
