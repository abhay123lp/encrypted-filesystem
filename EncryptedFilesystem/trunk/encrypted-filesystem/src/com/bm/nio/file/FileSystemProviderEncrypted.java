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
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipError;

import com.sun.nio.zipfs.ZipFileSystem;


public class FileSystemProviderEncrypted extends FileSystemProvider {

	//private FileSystemProvider mBaseFSP;
	/*public FileSystemProviderEncrypted(FileSystemProvider baseFSP, char[] pwd){
		mBaseFSP = baseFSP;
	}*/
	
	private final Map<Path, FileSystemEncrypted> filesystems = new HashMap<>();
	
	@Override
	public String getScheme() {
		return "encrypted";
	}

	
	
	@Override
	public SeekableByteChannel newByteChannel(Path path,
			Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		
		return new SeekableByteChannelEncrypted();
	}

	/**
	 * Generates filesystem and creates configuration file by default
	 * @param uri
	 * @param env
	 * @return
	 * @throws IOException
	 */
	@Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env)
            throws IOException
        {
			//TODO: create configuration file
            Path path = uriToPath(uri);
            //path.
            synchronized(filesystems) {
                Path realPath = null;
                if (validatePath(path)) {
                    realPath = path.toRealPath();
                    if (filesystems.containsKey(realPath))
                        throw new FileSystemAlreadyExistsException();
                }
                FileSystemEncrypted encfs = null;
               	encfs = new FileSystemEncrypted(this, path, env);
                filesystems.put(realPath, encfs);
                return encfs;
            }
        }
	
	
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

	
	@Override
	public FileSystem getFileSystem(URI uri) {
        synchronized (filesystems) {
            FileSystemEncrypted encfs = null;
            try {
            	encfs = filesystems.get(uriToPath(uri).toRealPath());
            } catch (IOException x) {
                // ignore the ioe from toRealPath(), return FSNFE
            }
            if (encfs == null)
                throw new FileSystemNotFoundException();
            return encfs;
        }
	}

	@Override
	public Path getPath(URI uri) {
        String spec = uri.getSchemeSpecificPart();
        return getFileSystem(uri).getPath(spec);
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
