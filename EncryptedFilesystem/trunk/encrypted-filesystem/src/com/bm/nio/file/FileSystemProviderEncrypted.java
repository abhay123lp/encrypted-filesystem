package com.bm.nio.file;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class FileSystemProviderEncrypted extends FileSystemProvider {

	private FileSystemProvider mBaseFSP;
	public FileSystemProviderEncrypted(FileSystemProvider baseFSP, char[] pwd){
		mBaseFSP = baseFSP;
	}
	
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

	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env)
			throws IOException {
		return new FileSystemEncrypted();
	}

	@Override
	public FileSystem getFileSystem(URI uri) {
		return new FileSystemEncrypted();
	}

	@Override
	public Path getPath(URI uri) {
		return new PathEncrypted();
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
