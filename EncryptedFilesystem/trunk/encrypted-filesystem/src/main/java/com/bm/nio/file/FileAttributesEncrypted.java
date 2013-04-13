package com.bm.nio.file;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class FileAttributesEncrypted <T extends BasicFileAttributes> implements BasicFileAttributes{

	private T mAttrs;
	protected FileAttributesEncrypted(T attr){
		mAttrs = attr;
	}
	
	@Override
	public FileTime lastModifiedTime() {
		return mAttrs.lastModifiedTime();
	}

	@Override
	public FileTime lastAccessTime() {
		return mAttrs.lastAccessTime();
	}

	@Override
	public FileTime creationTime() {
		return mAttrs.creationTime();
	}

	@Override
	public boolean isRegularFile() {
		return mAttrs.isRegularFile();
	}

	@Override
	public boolean isDirectory() {
		return mAttrs.isDirectory();
	}

	@Override
	public boolean isSymbolicLink() {
		return mAttrs.isSymbolicLink();
	}

	@Override
	public boolean isOther() {
		return mAttrs.isOther();
	}

	@Override
	public long size() {
		// TODO Auto-generated method stub
		//should return decrypted size. Better calculated than cached.
		return 0;
	}
	
	public long sizeEncryted(){
		return mAttrs.size();
	}

	@Override
	public Object fileKey() {
		return mAttrs.fileKey();
	}

}
