package com.bm.nio.file;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import javax.crypto.Cipher;

import com.bm.nio.utils.CipherUtils;

public class FileAttributesEncrypted <T extends BasicFileAttributes> implements BasicFileAttributes{

	private final T mAttrs;
	private final PathEncrypted mPath;
	protected FileAttributesEncrypted(T attr, PathEncrypted path){
		mAttrs = attr;
		mPath = path;
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
		//DONE; the same as underlying size for stream ciphers
		//throw unsupported for block ciphers
		//should return decrypted size. Better calculated than cached.
		final Cipher encipher = mPath.getFileSystem().ciphers.get().getEncipher();
		//have to reduce to int because getEncAmount doesn't accept longs
		if (CipherUtils.getEncAmt(encipher, 1) == 1)//otherwise getEncAmount may overflow for MAX_VALUE
			if (CipherUtils.getEncAmt(encipher, Integer.MAX_VALUE) == Integer.MAX_VALUE){
				final long encSize = sizeEncryted();
				final int encIntRemaindr = (int)(encSize%(long)Integer.MAX_VALUE);
				if (CipherUtils.getEncAmt(encipher, encIntRemaindr) == encIntRemaindr)
					return encSize;
			}
		throw new UnsupportedOperationException("Unable to compute decrypted size for non-stream cipher");
	}
	
	public long sizeEncryted(){
		return mAttrs.size();
	}

	@Override
	public Object fileKey() {
		return mAttrs.fileKey();
	}

}
