package com.bm.nio.channels;

import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.zip.ZipException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.bm.nio.file.ConfigEncrypted;
import com.bm.nio.file.ConfigEncrypted.Ciphers;
import com.bm.nio.file.FileSystemEncrypted.FileSystemEncryptedEnvParams;
import com.bm.nio.utils.CipherUtils;

/**
 * @author Mike
 * implementation based on underlying channel
 */
public class SeekableByteChannelEncrypted extends AbstractInterruptibleChannel implements SeekableByteChannel {
	//================
	//MAC is additional hash (AAD), that may append to encrypted text to verify correct decryption
	//Hash function depends on the secret key
	//TODO: use 2-byte MAC to identify filename or block correctness 
	// !create factory to return 1 channel for 1 under channel! 
	// or allow multiple channels for 1 under channel
	//================
	
    //===
    protected byte decBlock[];
    protected byte encBlock[];
    private final int decBlockSize;
    private final int encBlockSize;
    private long mDecPos = 0;
    private long mDecSize = 0;
    //changes to false when writing is not completed (writen to buffer but not flushed to disk)
    // and to true when saving current block
    private boolean mIsDecFlushed = true; //if current bloch was flushed to under channel
    // if current block was loaded from under channel
    // changes to false when changing Decrypted position and to true when loading current block
    private boolean mIsDecCached = false;
    //===
    Cipher encipher;
    Cipher decipher;
    private ConfigEncrypted mConfig;
    //===
    
	protected final SeekableByteChannel mChannel;
	private final Object mLock;
	private static WeakHashMap<Channel, Object> locks = new WeakHashMap<Channel, Object>();
	
	// === factory functions ===
	private static WeakHashMap<Channel, SeekableByteChannelEncrypted> channels = new WeakHashMap<Channel, SeekableByteChannelEncrypted>();
	//
	public static synchronized SeekableByteChannelEncrypted newChannel(
			SeekableByteChannel channel, Map<String, ?> props)
			throws ChannelExistsException, GeneralSecurityException {
		//return newChannel(channel, props, null);
		if (channels.get(channel) != null)
			throw new ChannelExistsException();
		final SeekableByteChannelEncrypted ce = new SeekableByteChannelEncrypted(channel, props);
		channels.put(channel, ce);
		return ce;
	}

	public static synchronized SeekableByteChannelEncrypted getChannel(
			SeekableByteChannel channel) {
		return channels.get(channel);
	}
	
	private static synchronized void remove(Channel underChannel){
		channels.remove(underChannel);
	}
	// === ===
	
	
    protected SeekableByteChannelEncrypted(SeekableByteChannel channel, Map<String, ?> props) throws GeneralSecurityException {
    	if (props == null)
    		props = new HashMap<String, Object>();
    	initProps(props);
        decBlockSize = mConfig.getBlockSize(); //encipher.getOutputSize(8192);
//        decBlockSize = props.containsKey(ConfigEncrypted.PROPERTY_PLAIN_BLOCK_SIZE) ?
//    			(Integer)props.get(ConfigEncrypted.PROPERTY_PLAIN_BLOCK_SIZE) : 8192; //encipher.getOutputSize(8192);
	    if (decBlockSize <= 0) {
	        throw new IllegalArgumentException("Block size <= 0");
	    }

        //encBlockSize = getEncAmt(decBlockSize);
	    encBlockSize = CipherUtils.getEncAmt(encipher, decBlockSize);
	    decBlock = new byte [decBlockSize];
	    encBlock = new byte [encBlockSize];
		mChannel = channel;
		//lock by under channel, which is more narrow than separate lock for each encrypted channel instance
		synchronized (locks) {
			Object lock = locks.get(channel);
			if (lock == null){
				lock = new Object();
				locks.put(channel, lock);
			}
			mLock = lock;
		}
		//
		mIsOpen = true;

    }
    
	public int getPlainDataBlockSize(){
		return decBlockSize;
	}
	
	public int getEncryptedDataBlockSize(){
		return encBlockSize;
	}
	
    protected void initProps(Map<String, ?> props) throws GeneralSecurityException{
    	final Object envConfig, envPwd, envCiphers;
    	envConfig = props.get(FileSystemEncryptedEnvParams.ENV_CONFIG);
		envPwd = props.get(FileSystemEncryptedEnvParams.ENV_PASSWORD);
		envCiphers = props.get(FileSystemEncryptedEnvParams.ENV_CIPHERS);
		final char [] pwd;
		final SecretKeySpec key;
		final Ciphers c;
    	if (envConfig != null)
    		mConfig = ConfigEncrypted.newConfig((ConfigEncrypted)envConfig);
    	else
    		mConfig = new ConfigEncrypted();
		
		if (envCiphers != null){
			if (envCiphers instanceof Ciphers){
				c = (Ciphers)envCiphers;
			} else {
					throw new IllegalArgumentException("Parameter " + FileSystemEncryptedEnvParams.ENV_CIPHERS + " must be type of " + (new char [0]).getClass().getSimpleName() + " or " + Ciphers.class.getSimpleName());
				}
		} else if (envPwd != null){
			if (envPwd instanceof char []){
				c = mConfig.newCiphers((char []) envPwd);
			} else if (envPwd instanceof SecretKeySpec){
					c = mConfig.newCiphers((SecretKeySpec) envPwd);
			} else {
					throw new IllegalArgumentException("Parameter " + FileSystemEncryptedEnvParams.ENV_PASSWORD + " must be type of " + (new char [0]).getClass().getSimpleName() + " or " + SecretKeySpec.class.getSimpleName());
				}
		} else
			throw new IllegalArgumentException("Parameter " + FileSystemEncryptedEnvParams.ENV_CIPHERS + " or " + FileSystemEncryptedEnvParams.ENV_PASSWORD + " must be present");
		
    	encipher = c.getEncipher();
    	decipher = c.getDecipher();
    }
    
//    /**
//     * Should be overridden together with encrypt/decrypt block
//     * to return correct encrypted size
//     * Reverse function getEncAmt(int encSize) cannot be made because of padding in encrypted data
//     * @param decAmt
//     * @return
//     */
//    protected int getEncAmt(int decAmt){
//        return encipher.getOutputSize(encipher.getOutputSize(decAmt));
//    }
    
	volatile boolean mIsOpen;

	private void checkOpen() throws IOException{
		if (!isOpen())
			throw new ClosedChannelException();
	}
	
	public long sizeEncrypted() throws IOException {
		checkOpen();
		synchronized (mLock) {
			return mChannel.size();
		}
	}

	
	//long mSizeEncTmp = 0;
	//long mPlainSizeTmp = 0;
	/**
	 * Derive plain size from underChannel size by decrypting last block
	 * @return
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 */
	private long getDecSize() throws IOException, GeneralSecurityException {
		//decode latest block and return it's size
		//returns 0 on exception
		//long lastBlock = 0;
		//byte [] lastBlockDec = new byte [0];
		//try {
//		if (encBlockSize == decBlockSize)
//			return sizeEnc;
		
		//if encrypted size didn't change from last time 
		//if (mSizeEncTmp == sizeEnc)
		//	return mPlainSize;
		
		synchronized (mLock) {
			// Encrypted: blockEnc1|blockEnc2|... |blockEncN|lastBlockEnc
			// Decrypted: bockDec1|blockDec2|...|blockDecN|lastBlockDec
			// SizeDec = blockDecSize*N + lastBlockDec
			//			|before last bl.| |in last block|
			final long sizeEnc = mChannel.size(); 
			if (sizeEnc == 0)
				return 0;
			//- 1 - size before last block
			long lastBlock = getBlockNum(sizeEnc - 1, encBlockSize);
			long sizeDec =  lastBlock * decBlockSize;
			//- 2 - size in block
			//long lastIndex = sizeEnc - 1;
			//lastBlock = getBlockNum(lastIndex, (long)encryptedDataBlockSize);
			int lastBlockRemains = getBlockPos(sizeEnc - 1, encBlockSize) + 1;//bytes remain in the last block
//			if (getEncAmt(lastBlockRemains) == lastBlockRemains)//check if remainder decrypts to the same amount of bytes 
			if (CipherUtils.getEncAmt(encipher, lastBlockRemains) == lastBlockRemains)//check if remainder decrypts to the same amount of bytes 
				return sizeDec + lastBlockRemains;
			byte [] remainderArray = new byte[lastBlockRemains];
			//if (lastBlockRemainds == 0)
			//	return sizeEnc;
			
			final long posTmp = mChannel.position(); 
			long lastBlockStart = lastBlock * encBlockSize;
			mChannel.position(lastBlockStart);
			mChannel.read(ByteBuffer.wrap(remainderArray));
			mChannel.position(posTmp);
			//byte [] lastBlockDec = decryptBlock(remainderArray);
			byte [] lastBlockDec = CipherUtils.decryptBlock(decipher, remainderArray);
			sizeDec += (long)lastBlockDec.length;
			return sizeDec;
		}
		//} catch (Exception e) {
		//	return 0;
		//}
	}
	
	@Override
	public long size() throws IOException {
		checkOpen();
		return sizeInternal();
	}
	
	/**
	 * @return Actual decrypted size
	 * Can return incorrect if under channel does not support position(long)
	 */
	private long sizeInternal(){
		synchronized (mLock) {
			try {
				long decSizeUnder = getDecSize();
				if (decSizeUnder > mDecSize)
					mDecSize = decSizeUnder;
			} catch (Exception e) {
			}			
			return mDecSize;
		}
	}
	
	
	/**
	 * @return position in decrypted data
	 * @throws IOException
	 */
	@Override
	public long position() throws IOException {
		checkOpen();
		synchronized (mLock) {
			return mDecPos;
		}
	}
	
	/**
	 * @return position in encrypted data. For block Ciphers will equal to beginning of the block.
	 * @throws IOException
	 */
	public long positionEncrypted() throws IOException {
		checkOpen();
		synchronized (mLock) {
			return mChannel.position();
		}
	}
	/**
	 * pos and blockSize should be both encrypted or plain
	 * @param pos - position in encrypted or plain data
	 * @param blockSize size of encrypted or plain block, where position is located in
	 * @return numbel of block starting from 0
	 */
	private long getBlockNum(long pos, long blockSize){
		//final long blockSize = (long)plainDataBlockSize;
		final long num = pos / blockSize;
		return num;
	}

	/**
	 * @param pos - position in plain (decrypted) data
	 * @return position within block
	 */
	private int getBlockPos(long pos, long blockSize){
		//final long blockSize = (long)plainDataBlockSize;
		long tmp = pos % blockSize;
		return (int)tmp;
	}

	private int loadBlock(long pos, BlockOperationOptions ...props) throws IOException, GeneralSecurityException {
		final Set<BlockOperationOptions> sOps = new HashSet<BlockOperationOptions>(Arrays.asList(props));
		return loadBlock(pos, sOps);
	}
	
	/**
	 * Decrypts block and load to buffer from the given decrypted position
	 * @return number of bytes were read to buffer
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 */
	private int loadBlock(long pos, final Set<BlockOperationOptions> props) throws IOException, GeneralSecurityException {
		// Encrypted: blockEnc1|blockEnc2|... |blockEncN|lastBlockEnc
		// Decrypted: bockDec1|blockDec2|...|blockDecN|lastBlockDec
		// load block where pos is located in

		synchronized (mLock) {
			//load decrypted size in case under channel was updated 
			//long decSize = sizeInternal();
			//DONE: consider using below
			long decSize = mDecSize;
			//
			if (pos > decSize)
				pos = decSize;
			int len = decBlockSize;
			long currBlock = getBlockNum(pos, decBlockSize);
			long lastBlock = getBlockNum(decSize, decBlockSize);
			if (currBlock == lastBlock)//last block - load part
				len = getBlockPos(decSize, decBlockSize);
			if (len == 0)
				//return 0;
				len = decBlockSize; // load whole block if "pos" points to the first byte
			long posEnc = currBlock * (long)encBlockSize;
			try {
				mChannel.position(posEnc);
			} catch (UnsupportedOperationException e) {
				// read from the current position, unless need to halt
				if (props.contains(BlockOperationOptions.STOPONPOSITIONERROR))
					return -1;
			}
			//int lenEnc = getEncAmt(len);
			int lenEnc = CipherUtils.getEncAmt(encipher, len);
			ByteBuffer bufEnc = ByteBuffer.wrap(encBlock, 0, lenEnc);
			int readAmt = 0;
			int readOverall = 0;
			while (readOverall < lenEnc && readAmt > -1) {
				try {
					begin();
					readAmt = mChannel.read(bufEnc);
					if (readAmt == -1)
						break;
					readOverall += readAmt;
				} finally {
					//readOverall += readAmt;
					end(bufEnc.remaining() > 0);
				}
			}
			if (readOverall <= 0)
				return 0;

			//byte [] dec = decryptBlock(blockEnc, 0, readOverall);
			byte [] dec = CipherUtils.decryptBlock(decipher, encBlock, 0, readOverall);

			System.arraycopy(dec, 0, decBlock, 0, dec.length);
//			byte [] dec = decryptBlock(blockEnc, 0, lenEnc);
//			System.arraycopy(dec, 0, block, 0, dec.length);
			
			//placing position back to the beginning of encrypted block
//			try {
//				mChannel.position(posEnc);
//			} catch (UnsupportedOperationException e) {
//			}
			mIsDecCached = true;
			return dec.length;
		}
	}
	
	private static class BlockOperationOptions
	{
		/**
		 * non interruptible read/write
		 */
		public final static BlockOperationOptions NONINTERRUPTIBLE = new BlockOperationOptions();
		/**
		 * do not perform read/write if unable to set position in encrypted (underlying) channel
		 */
		public final static BlockOperationOptions STOPONPOSITIONERROR = new BlockOperationOptions();
	}
	
	
	private int saveBlock(long pos, BlockOperationOptions ...props) throws IOException, GeneralSecurityException {
		final Set<BlockOperationOptions> sOps = new HashSet<BlockOperationOptions>(Arrays.asList(props));
		//return saveBlock(pos, true, true);
		return saveBlock(pos, sOps);
	}

	/**
	 * Encrypts block and puts buffer to underChannel from the current decrypted position
	 * @param pos - position in block (decrypted)
	 * @param isIgnorePositionError do not proceed if underlying channel does not support position(pos), so block cannot 
	 * be written to appropriate space in encrypted channel. If true then block is forced to save.
	 * @param interruptible - make this operation interruptible
	 * @return
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
//	private int saveBlock(long pos, final boolean isIgnorePositionError, final boolean interruptible) throws IOException, GeneralSecurityException {
	private int saveBlock(long pos, final Set<BlockOperationOptions> props) throws IOException, GeneralSecurityException {
		//See implementation of write for Channels.WritableByteChannelImpl
		// Encrypted: blockEnc1|blockEnc2|... |blockEncN|lastBlockEnc
		// Decrypted: bockDec1|blockDec2|...|blockDecN|lastBlockDec
		// save block where pos is located in
		synchronized (mLock) {
			//changed at 02.01.2013 - otherwise size won't change in case of truncate
			//because it will be always updated back
			long decSize = mDecSize;
			//long decSize = sizeInternal();
			if (pos > decSize)
				pos = decSize;
			int len = decBlockSize;//getBlockPos(pos, decBlockSize);
			//if (len == 0)
			//	return 0;
//			if (mDecSize - pos > decBlockSize)//not last block - save whole block
//				len = decBlockSize;
			long currBlock = getBlockNum(pos, decBlockSize);
			long lastBlock = getBlockNum(decSize, decBlockSize);
			if (currBlock == lastBlock)//last block - save whole block
				len = getBlockPos(mDecPos, decBlockSize);
			if (len == 0)
				return 0;
			long posEnc = currBlock * (long)encBlockSize;
			try {
				mChannel.position(posEnc);
			} catch (UnsupportedOperationException e) {
				//if (!isIgnorePositionError)
				if (props.contains(BlockOperationOptions.STOPONPOSITIONERROR))
					return -1;
				// write from the current position
			}
			//byte [] enc = encryptBlock(block, 0, len);
			byte [] enc = CipherUtils.encryptBlock(encipher, decBlock, 0, len);
			len = enc.length;
			ByteBuffer buf = ByteBuffer.wrap(enc);
			while (buf.remaining() > 0) {
//				if (interruptible){
				if (!props.contains(BlockOperationOptions.NONINTERRUPTIBLE)){
					try {
						begin();
						mChannel.write(buf);
					} finally {
						end(buf.remaining() > 0);
					}
				} else{
					mChannel.write(buf);
				}
			}
			mIsDecFlushed = true;			
			return len;
		}
	}
	/**
	 * @param newPosition - sets position in plain (decrypted) data
	 * @return
	 * @throws IOException
	 */
	@Override
	public SeekableByteChannel position(long newPosition) throws IOException {
		//scenario 1: write < blockSize bytes and then move position to previous block
		//result 1 (flush after each write): - no issues
		//result 2 (flush after buffer is full): - loosing current buffer, that is not full
		
		//scenario 2: write < blockSize bytes 2 times
		//result 1 (flush after each write, append mode): - second write should be from the beginning of block, but position
		// change is not supported in append mode 
		//result 2 (flush after buffer is full): - no issues, will write only after buffer is full
		
		checkOpen();
		synchronized (mLock) {
			//if current block (buffer) changes
			long currBlock = getBlockNum(mDecPos, decBlockSize);
			long newBlock = getBlockNum(newPosition, decBlockSize);
			if (newBlock != currBlock){
				//set position in underlying channel to the beginning of block mChannel.position();
				//can be overflow when newPosition > MAX_LONG/(encryptedDataBlockSize/plainDataBlockSize), then 
				//posEnc > MAX_LONG
				//long posEnc = getBlockNum(newPosition, decBlockSize) * (long)encBlockSize;
				long sizeDec = sizeInternal();
				//out of bounds
				//if (posEnc > sizeEncrypted()){//position > that size.
				if (newPosition > sizeDec){//position > that size.

					newPosition = sizeDec;//sizeInternal();
//					try {
//						newPosition = getDecSize();//position = greatest index + 1 = size
//					} catch (GeneralSecurityException e) {
//						throw new IOException("Unable to set new position: unable to decrypt");
//					}
					
					//posEnc = sizeEncrypted();
					//long lastBlock = getBlockNum(sizeDec - 1, decBlockSize);//getBlockNum(sizeEncrypted() - 1, encBlockSize);
					//can only put position to the beginning of the last block as can't say more accurate for plain data
					//posEnc = lastBlock * (long)encBlockSize;
					//newPosition = lastBlock * (long)decBlockSize;
				}
				//can only put position to the beginning of the last block as can't say more accurate for plain data
				long posEnc = getBlockNum(newPosition, decBlockSize) * (long)encBlockSize;
				// === 1 - flush buffer before setting position. Continue if save is not supported (readonly)
				UnsupportedOperationException ue = new UnsupportedOperationException("Unable to change position: allowed to change only within one block size " + decBlockSize);
				try {
					//
					//saveBlock(mDecPos);
					//don't change position if can't save current block - change 31.12.2012
					if (saveBlock(mDecPos, BlockOperationOptions.STOPONPOSITIONERROR) == -1)
						throw ue;
				} catch (GeneralSecurityException e) {
					//throw new IOException("Unable to set new position: unable to flush buffer");
				}
				//load to buffer if required
				// === 2 - load new block. Continue if load is not supported (write only)
				positionInternal(newPosition);
				int amt = 0;
				try {
					//loadBlock(mDecPos);
					amt = loadBlock(mDecPos, BlockOperationOptions.STOPONPOSITIONERROR);
					if (amt == -1)
						throw ue;
				} catch (GeneralSecurityException e) {
					//throw new IOException("Unable to set new position: unable to decrypt");
				}
				// === 3 - set new enc position.
				//TODO: Consider leaving position at the end
//				try {
//					mChannel.position(posEnc);
//				} catch (UnsupportedOperationException e) {
//					//don't care - if underlying channel does not support - use only this channel position
//					//change 31.12.2012 - do not change position if underlying channel does not support it
//					throw ue;
//				}
			} else{
				//set position in plain data
				positionInternal(newPosition);
			}
			return this;
		}
	}
	
	private void positionInternal(long newPosition){
		if (getBlockNum(mDecPos, decBlockSize) != getBlockNum(newPosition, decBlockSize)){ 
			mIsDecCached = false;
		}
		mDecPos = newPosition;
		if (mDecPos > mDecSize)
			mDecSize = mDecPos;
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		checkOpen();
		synchronized (mLock) {
//			mIsDecFlushed = true;
			int blockPos = getBlockPos(mDecPos, decBlockSize);
			int remains = decBlockSize - blockPos;
			final int len = src.remaining();
			//no block overflow
			if (len < remains){
				src.get(decBlock, blockPos, len);
				positionInternal(mDecPos + len);
				try {
					saveBlock(mDecPos, BlockOperationOptions.STOPONPOSITIONERROR);//, false, true);
				} catch (GeneralSecurityException e) {
					//Do nothing as it was optional flush at any write to under channel
					mIsDecFlushed = false;
				}
				return len;
			}

			//block overflow
			//start
			try {
				src.get(decBlock, blockPos, remains);
				positionInternal(mDecPos + remains);
				saveBlock(mDecPos - 1);//mDecPos here is the beginning of next block
//				positionInternal(mDecPos + remains);
			} catch (GeneralSecurityException e) {
				IOException ie = new IOException("Unable to write middle block at position " + (mDecPos - 1));
				ie.initCause(e);
				throw ie;
			}
			//middle
			while(src.remaining() > decBlockSize){
				try {
					src.get(decBlock, 0, decBlockSize);
					positionInternal(mDecPos + decBlockSize);
					saveBlock(mDecPos - 1);//mDecPos here is the beginning of next block
				} catch (GeneralSecurityException e) {
					IOException ie = new IOException("Unable to write final block at position " + (mDecPos - 1));
					ie.initCause(e);
					throw ie;
				}
			}
			//end
			int lenEnd = src.remaining();
			src.get(decBlock, 0, lenEnd);
			positionInternal(mDecPos + lenEnd);
			if (lenEnd != 0)//have to do border condition, otherwise will spend time for extra write
				try {
					saveBlock(mDecPos - 1, BlockOperationOptions.STOPONPOSITIONERROR);//fixed to mDecPos - 1 28/07/2013
				} catch (GeneralSecurityException e) {
					//Do nothing as it was optional to keep under channel updated
					mIsDecFlushed = false;
				}
			return len;
		}
	}
	

	@Override
	public int read(ByteBuffer dst) throws IOException {
		int i = readInternal(dst);
		return i;
	}

//	@Override
	public int readInternal(ByteBuffer dst) throws IOException {
		checkOpen();
		synchronized (mLock) {
			final long decPosStart = mDecPos;
			try {
				if (!mIsDecCached)
					loadBlock(mDecPos, BlockOperationOptions.STOPONPOSITIONERROR);//may cause low performance! (pt. 16.3.4.1)
			} catch (GeneralSecurityException e) {
				//Do nothing as it was optional fill buffer at any read operation 
			}
			//TODO: consider using below
			//long sizeDec = mDecSize;
			long sizeDec = sizeInternal();
			long remainsToEnd = sizeDec - mDecPos;//additional check how - much remains to read in this (dec) channel
			
			int blockPos = getBlockPos(mDecPos, decBlockSize);
			int remains = decBlockSize - blockPos;//remains from mDecPos to the end of block
			int len = dst.remaining();//remains in dst.
			//in case when not enough bytes remain in the channel 
			//to reach dst size (remainsToEnd < len) and block end (remainsToEnd < remains)
			if (remainsToEnd < remains && remainsToEnd < len){
				dst.put(decBlock, blockPos, (int) remainsToEnd);
				positionInternal(mDecPos + remainsToEnd);
				
				// check if something remains - try loading one block
				//1. check that we can't setup position (only read is allowed)
				//2. read the block
				//3. if something appeared to be red and position is not in the beginning of block - throw not aligned
				//4. if aligned - read next block and recursively call self
				//simple case of above scenario - firs time read of read-only under channel (position is not working)
				try {
					int amt = loadBlock(mDecPos, BlockOperationOptions.STOPONPOSITIONERROR);
					if (amt != -1)//if not -1 then position is allowed thus assuming that everything was red correctly
						if (remainsToEnd == 0)//if channel reached end
							return -1;
						else
							return (int)remainsToEnd;
					amt = loadBlock(mDecPos);
					if (amt <= 0)//if can't read anyway - it means end of under channel is reached
						//return (int)remainsToEnd;
						return -1;
					if (getBlockPos(mDecPos, decBlockSize) != 0)//
						throw new IOException("Unable to read: last block is not aligned ending at position " + mDecPos);
					mDecSize += amt;
					return read(dst);
				} catch (GeneralSecurityException e) {
					IOException ie = new IOException("Unable to read last block at position " + mDecPos);
					ie.initCause(e);
					throw ie;
				}
				//
				//return (int)remainsToEnd;
			}
			// === no block overflow ===
			//can fit within current block
			if (len < remains){
				dst.put(decBlock, blockPos, len);
				positionInternal(mDecPos + len);
				return len;
			}
			
			// === block overflow ===
			//start
			//if (blockPos != 0){//if new block (==0) then skip this and go loading to the middle
				dst.put(decBlock, blockPos, remains);
				positionInternal(mDecPos + remains);
			//}

			//middle
			while(dst.remaining() > decBlockSize){
				try {
					int amt = loadBlock(mDecPos);//loading new block
					dst.put(decBlock, 0, amt);
					positionInternal(mDecPos + amt);
					if (amt < decBlockSize)//if end is reached
						return (int)(mDecPos - decPosStart);
				} catch (GeneralSecurityException e) {
					IOException ie = new IOException("Unable to read middle block at position " + mDecPos);
					ie.initCause(e);
					throw ie;
				}
			}

			//end
			try {
				int amt = loadBlock(mDecPos);//loading new block
				amt = Math.min(amt, dst.remaining());//read may be not enough to fill dst
				dst.put(decBlock, 0, amt);
				positionInternal(mDecPos + amt);
			} catch (GeneralSecurityException e) {
				IOException ie = new IOException("Unable to read final block at position " + mDecPos);
				ie.initCause(e);
				throw ie;
			}
			
			return (int)(mDecPos - decPosStart);
		}
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		checkOpen();
		synchronized (mLock) {
			long sizeDec = sizeInternal();
			if (size > sizeDec)
				return this;
			long newBlock = getBlockNum(size, decBlockSize);
			long lastBlock = getBlockNum(sizeDec, decBlockSize);
			long posEnc = newBlock * (long)encBlockSize;
			long posDec = newBlock * (long)decBlockSize;
			int newBlockPos = getBlockPos(size, decBlockSize);//position in latest truncated block
			//if fits the end of block (newBlockPos == 0) no need to calculate last block, just cut in the end
			if (newBlockPos == 0){
			//if (getEncAmt(newBlockPos) == newBlockPos || newBlockPos == 0){
				mDecSize = size;
				positionInternal(size);
//				mDecPos = size;
				mChannel.truncate(posEnc + newBlockPos);
				return this;
			}
			
			//stream cypher - truncate every time
//			if (getEncAmt(newBlockPos) == newBlockPos){
			if (CipherUtils.getEncAmt(encipher, newBlockPos) == newBlockPos){
				mChannel.truncate(posEnc + newBlockPos);
			}else
			//block cipher
			//no need to truncate last block, it will always have the same size 
			//if not last block - need to truncate under channel
			if (newBlock < lastBlock){
				mChannel.truncate(posEnc + encBlockSize);
				//change 02.01.2012 - now position is put to the end of block
				//mChannel.position(posEnc);
				mChannel.position(posEnc + encBlockSize);
			}
			
			//put dec position and size to the end of last block
			mDecSize = posDec + decBlockSize;
			positionInternal(mDecSize);
//			mDecPos = mDecSize;
			//read block... 
			try {
				loadBlock(mDecPos);
			} catch (GeneralSecurityException e) {
				IOException ie = new IOException("Unable to truncate: cannot read the final block at position " + mDecPos);
				ie.initCause(e);
				throw ie;
			}
			// ... truncate ... 
			mDecSize = size;
			positionInternal(size);
//			mDecPos = size;
			// ...and save back
			try {
				saveBlock(mDecPos);
			} catch (GeneralSecurityException e) {
				IOException ie = new IOException("Unable to truncate: cannot save the final block at position " + mDecPos);
				ie.initCause(e);
				throw ie;
			}
			
			return this;
		}
		//return null;
	}

	@Override
	protected void implCloseChannel() throws IOException {
		synchronized (this) {
			//TODO: flush from the buffer, 
			//... or alternatively flush it after each write operation
			if (!mIsDecFlushed)
			try {
				//saveBlock(mDecPos, true, false);
				saveBlock(mDecPos, BlockOperationOptions.NONINTERRUPTIBLE);
			} catch (GeneralSecurityException e) {
				IOException ie = new IOException("Unable to close: cannot flush the buffer");
				ie.initCause(e);
				throw ie;
			}
//			mIsDecFlushed = true;
			
			mChannel.close();
			remove(mChannel);
			mIsOpen = false;
		}
	}

	/**
	 * flushes contents of buffer
	 * @return number of decrypted bytes flushed
	 * @throws IOException
	 */
	public int flush() throws IOException {
		checkOpen();
		synchronized (mLock) {
			try {
				return saveBlock(mDecPos);
			} catch (GeneralSecurityException e) {
				IOException ie = new IOException("Unable to flush the buffer");
				ie.initCause(e);
				throw ie;
			}
		}
	}
}
