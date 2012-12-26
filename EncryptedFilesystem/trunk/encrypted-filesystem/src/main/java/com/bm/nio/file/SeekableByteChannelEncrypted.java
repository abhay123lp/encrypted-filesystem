package com.bm.nio.file;

import java.io.IOException;
import java.io.OutputStream;

import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.hamcrest.core.IsEqual;

import sun.nio.fs.WindowsFileSystemProvider;

import com.sun.nio.zipfs.ZipFileSystem;
import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

//TODO: implement based on underlying channel
class SeekableByteChannelEncrypted extends AbstractInterruptibleChannel implements SeekableByteChannel {
	//================
	//MAC is additional hash (AAD), that may append to encrypted text to verify correct decryption
	//Hash function depends on the secret key
	//TODO: use 2-byte MAC to identify filename correctness 
	//================
	
    Cipher encipher;
    Cipher decipher;

    //=== Cipher params ===
    String transformation = "AES/CFB/NoPadding";//"AES/CBC/PKCS5Padding";
    //String transformation = "AES/CBC/PKCS5Padding";
    byte[] salt = new String("12345678").getBytes();
    int iterationCount = 1024;
    int keyStrength = 128;
    SecretKey key;
    byte[] iv;
    //===
    protected byte block[];
    protected byte blockEnc[];
    private final int decBlockSize;
    private final int encBlockSize;
    private long mDecPos = 0;
    private long mDecSize = 0;
    //protected byte bufEnc[];//encrypted data for buf after flush
    //protected int count;
    public static final String PLAIN_BLOCK_SIZE = "block.size";
    public static final String PASSWORD = "password";
    public static final String TRANSFORMATION = "transformation";

	protected final SeekableByteChannel mChannel;
	private final Object mLock;
	private static WeakHashMap<Channel, Object> locks = new WeakHashMap<Channel, Object>();
	
    public SeekableByteChannelEncrypted(SeekableByteChannel channel) throws GeneralSecurityException {
        this(channel, new HashMap<String, Object>());
    }
	
    public SeekableByteChannelEncrypted(SeekableByteChannel channel, Map<String, ?> props, Cipher c) throws GeneralSecurityException {
    	//TODO:
    	if (props == null)
    		props = new HashMap<String, Object>();
    	initProps(props);
    	if (c == null)
    		c = getDefaultCipher();
        //this(channel, new HashMap<String, Object>());
        encipher = Cipher.getInstance(c.getAlgorithm(), c.getProvider());
        decipher = Cipher.getInstance(c.getAlgorithm(), c.getProvider());
        iv = initEncipher(encipher, key);
        initDecipher(decipher, key, iv);
        
        decBlockSize = props.containsKey(PLAIN_BLOCK_SIZE) ?
    			(Integer)props.get(PLAIN_BLOCK_SIZE) : 8192; //encipher.getOutputSize(8192);
	    if (decBlockSize <= 0) {
	        throw new IllegalArgumentException("Block size <= 0");
	    }

        encBlockSize = getEncAmt(decBlockSize);
	    block = new byte [decBlockSize];
	    blockEnc = new byte [encBlockSize];
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
//		try {
//			mDecSize = getDecSize();
//		} catch (IOException e) {
//			mDecSize = 0;
//		}

    }
    
	public SeekableByteChannelEncrypted(SeekableByteChannel channel, Map<String, ?> props) throws GeneralSecurityException {
		this(channel, props, null);
//    	if (props == null)
//    		props = new HashMap<String, Object>();
//        Cipher c = initProps(props);
        //final int defaultBlockSize = (8192 / encipher.getBlockSize()) * encipher.getBlockSize()
        //		                  + ((8192 % encipher.getBlockSize()) == 0 ? 0 : encipher.getBlockSize());
//        decBlockSize = props.containsKey(PLAIN_BLOCK_SIZE) ?
//        			(Integer)props.get(PLAIN_BLOCK_SIZE) : 8192; //encipher.getOutputSize(8192);
//        if (decBlockSize <= 0) {
//            throw new IllegalArgumentException("Block size <= 0");
//        }
//        
////        final int interEncryptedBlockSize = encipher.getOutputSize(decBlockSize);//before second encryption
////        encBlockSize = encipher.getOutputSize(interEncryptedBlockSize);
//        //if (encipher.getOutputSize(blockSize) != blockSize){
//        	//throw new IllegalArgumentException("Stream block size should be multiplier of cipher block size");
//        //}
//        //this.buf = new byte[Math.max(interEncryptedBlockSize, Math.max(decBlockSize, encBlockSize))];
//        encBlockSize = getEncSize(decBlockSize);
//		mChannel = channel;
//		//lock by under channel, which is more narrow than separate lock for each encrypted channel instance
//		synchronized (locks) {
//			Object lock = locks.get(channel);
//			if (lock == null){
//				lock = new Object();
//				locks.put(channel, lock);
//			}
//			mLock = lock;
//		}
//		//
//		mIsOpen = true;
//		try {
//			mDecSize = getPlainSize();
//		} catch (IOException e) {
//			mDecSize = 0;
//		}
	}
	
	public int getPlainDataBlockSize(){
		return decBlockSize;
	}
	
	public int getEncryptedDataBlockSize(){
		return encBlockSize;
	}
	
    protected void initProps(Map<String, ?> props) throws GeneralSecurityException{
    	//TODO: write correct initialization and parse properties
        char [] pwd = props.containsKey(PASSWORD) ?
    			(char [] )props.get(PASSWORD) : new char[3];
    	transformation = props.containsKey(TRANSFORMATION) ?
    					 (String)props.get(TRANSFORMATION) : transformation;
    	//---
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(pwd, salt, iterationCount, keyStrength);
        SecretKey tmp = factory.generateSecret(spec);
        key = new SecretKeySpec(tmp.getEncoded(), "AES");
    }
    
    protected Cipher getDefaultCipher() throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(transformation);
        return cipher;
    }
    
//    protected void initProps(Map<String, ?> props) throws GeneralSecurityException{
//    	//TODO: write correct initialization and parse properties
//        char [] pwd = props.containsKey(PASSWORD) ?
//    			(char [] )props.get(PASSWORD) : new char[3];
//    	transformation = props.containsKey(TRANSFORMATION) ?
//    					 (String)props.get(TRANSFORMATION) : transformation;
//    	//---
//        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
//        KeySpec spec = new PBEKeySpec(pwd, salt, iterationCount, keyStrength);
//        SecretKey tmp = factory.generateSecret(spec);
//        key = new SecretKeySpec(tmp.getEncoded(), "AES");
//        //dcipher = Cipher.getInstance("AES/CFB/NoPadding");
//        encipher = Cipher.getInstance(transformation);
//        decipher = Cipher.getInstance(transformation);
//        iv = initEncipher(encipher, key);
//        initDecipher(decipher, key, iv);
//    }

    protected byte[] initEncipher(Cipher encipher, SecretKey key) throws GeneralSecurityException{
        encipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(new byte[16]));
        AlgorithmParameters params = encipher.getParameters();
        return params.getParameterSpec(IvParameterSpec.class).getIV();
    }
    
    protected void initDecipher(Cipher decipher, SecretKey key, byte [] iv) throws GeneralSecurityException{
        decipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
    }

    byte [] encryptBlock(byte [] bufPlain) throws GeneralSecurityException {
    	return encryptBlock(bufPlain, 0, bufPlain.length);
    }
    byte [] encryptBlock(byte [] bufPlain, int start, int len) throws GeneralSecurityException {
    	try{
	        byte [] tmp = new byte[len - start]; 
	        System.arraycopy(bufPlain, start, tmp, 0, len); 
	        xor(tmp);
	        tmp = encipher.doFinal(tmp);
	        flip(tmp);
	        xor(tmp);
	        tmp = encipher.doFinal(tmp);
	        return tmp;
    	} catch (GeneralSecurityException e){
    		initEncipher(encipher, key);
    		throw e;
    	}
    }
    
    static void flip(byte [] a){
    	for (int i = 0, j = a.length - 1; i < a.length/2; i ++, j --){
    		byte tmp = a[i];
    		a[i] = a[j];
    		a[j] = tmp;
    	}
    }

    static void xor(byte [] a){
    	for(int i = 0; i < a.length - 1; i++)
    		a[i + 1] ^= a[i];
    }

    static void unxor(byte [] a){
    	for(int i = a.length - 1; i > 0; --i)
    		a[i] ^= a[i-1];
    }

    byte [] decryptBlock(byte [] bufEnc) throws GeneralSecurityException {
    	return decryptBlock(bufEnc, 0, bufEnc.length);
    }
    byte [] decryptBlock(byte [] bufEnc, int start, int len) throws GeneralSecurityException {
    	try {
            //return decipher.doFinal(bufEnc, start, len);
	        byte [] tmp = new byte[len - start]; 
	        System.arraycopy(bufEnc, start, tmp, 0, len); 
	        tmp = decipher.doFinal(tmp);
	        unxor(tmp);
	        flip(tmp);
	        tmp = decipher.doFinal(tmp);
	        unxor(tmp);
	        return tmp;
    		
		} catch (GeneralSecurityException e) {
			initDecipher(decipher, key, iv);
			throw e;
		}
    }
    
    /**
     * Should be overridden together with encrypt/decrypt block
     * to return correct encrypted size
     * Reverse function getEncAmt(int encSize) cannot be made because of padding in encrypted data
     * @param decAmt
     * @return
     */
    protected int getEncAmt(int decAmt){
        return encipher.getOutputSize(encipher.getOutputSize(decAmt));
    }
    
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
			if (getEncAmt(lastBlockRemains) == lastBlockRemains)//check if remainder decrypts to the same amount of bytes 
				return sizeDec + lastBlockRemains;
			byte [] remainderArray = new byte[lastBlockRemains];
			//if (lastBlockRemainds == 0)
			//	return sizeEnc;
			
			final long posTmp = mChannel.position(); 
			long lastBlockStart = lastBlock * encBlockSize;
			mChannel.position(lastBlockStart);
			mChannel.read(ByteBuffer.wrap(remainderArray));
			mChannel.position(posTmp);
			byte [] lastBlockDec = decryptBlock(remainderArray);
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
	
//	public long sizeOld() throws IOException {
//		checkOpen();
//		//position may be higher that underChannel size, when writing to buffer		
//		if ((getBlockNum(mPlainPosition, plainDataBlockSize) * encryptedDataBlockSize) >= mChannel.size())
//			return mPlainPosition;
//		//long size measured in encrypted size
//		long longSize = mChannel.size();
//		int intSize = (int)longSize;
//		if (intSize == longSize)//if size is measured in int
//			return decipher.getOutputSize(intSize);
//		else{ //if size is measured in long
//			//long blockSize = encryptedDataBlockSize;
//			//long blocksCnt = getBlockNum(longSize, encryptedDataBlockSize);//(longSize / blockSize);
//			long size1 = getBlockNum(longSize, encryptedDataBlockSize) * plainDataBlockSize;//blocksCnt * plainDataBlockSize;
//			//remainder will be inaccurate if padding is used in encryption
//			//because encrypting 10 bytes will occupy 32 bytes, and remainder will return 32 (when actually 10 data + 22 padding)
//			int remainder = (int) (longSize % (long)encryptedDataBlockSize);//(int) (longSize - (blockSize * blocksCnt)); 
//			long size2 = decipher.getOutputSize(remainder);
//			return size1 + size2;
//		}
//		
//	}
	
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
	 * @return position in encrypted data.
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

	private int loadBlock(long pos, BlockOperationOptions ...ops) throws IOException, GeneralSecurityException {
		final Set<BlockOperationOptions> sOps = new HashSet<BlockOperationOptions>(Arrays.asList(ops));
		return loadBlock(pos, sOps);
	}
	
	/**
	 * Decrypts block and load to buffer from the given decrypted position
	 * @return number of bytes were read to buffer
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 */
	private int loadBlock(long pos, final Set<BlockOperationOptions> ops) throws IOException, GeneralSecurityException {
		//TODO:
		// Encrypted: blockEnc1|blockEnc2|... |blockEncN|lastBlockEnc
		// Decrypted: bockDec1|blockDec2|...|blockDecN|lastBlockDec
		// load block where pos is located in
		synchronized (mLock) {
			//update decrypted size in case under channel was updated 
//			try {
//				long sizeDec = getDecSize();
//				if (mDecSize < sizeDec)
//					mDecSize = sizeDec;
//			} catch (IOException e) {
//				// assume mDecSize is up to date
//			}
			long decSize = sizeInternal();
			//
			if (pos > decSize)
				pos = decSize;
			int len = decBlockSize;
			long currBlock = getBlockNum(pos, decBlockSize);
			long lastBlock = getBlockNum(decSize, decBlockSize);
			if (currBlock == lastBlock)//last block - load part
				len = getBlockPos(decSize, decBlockSize);
			if (len == 0)
				return 0;
			long posEnc = currBlock * (long)encBlockSize;
			try {
				mChannel.position(posEnc);
			} catch (IOException e) {
				// read from the current position, unless need to halt
				if (ops.contains(BlockOperationOptions.STOPONPOSITIONERROR))
					return 0;
			}
			int lenEnc = getEncAmt(len);
			ByteBuffer bufEnc = ByteBuffer.wrap(blockEnc, 0, lenEnc);
			int readAmt = 0;
			int readOverall = 0;
			while (readOverall < lenEnc && readAmt > -1) {
				try {
					begin();
					readAmt = mChannel.read(bufEnc);
					if (readAmt == -1)
						break;
				} finally {
					readOverall += readAmt;
					end(bufEnc.remaining() > 0);
				}
			}
			byte [] dec = decryptBlock(blockEnc, 0, lenEnc);
			System.arraycopy(dec, 0, block, 0, dec.length);
			//placing position back to the beginning of encrypted block
			try {
				mChannel.position(posEnc);
			} catch (IOException e) {
			}
			return dec.length;
		}
	}
	
	private enum BlockOperationOptions
	{
		/**
		 * non interruptible read/write
		 */
		NONINTERRUPTIBLE,
		/**
		 * do not perform read/write if unable to set position in encrypted (underlying) channel
		 */
		STOPONPOSITIONERROR
	}
	
	private int saveBlock(long pos, BlockOperationOptions ...ops) throws IOException, GeneralSecurityException {
		final Set<BlockOperationOptions> sOps = new HashSet<BlockOperationOptions>(Arrays.asList(ops));
		//return saveBlock(pos, true, true);
		return saveBlock(pos, sOps);
	}

	/**
	 * Encrypts block and puts buffer to underChannel from the current decrypted position
	 * @param pos - position in block (decrypted)
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 */
//	private int saveBlock(long pos) throws IOException, GeneralSecurityException {
//		return saveBlock(pos, true, true);
//	}
	
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
	private int saveBlock(long pos, final Set<BlockOperationOptions> ops) throws IOException, GeneralSecurityException {
		//See implementation of write for Channels.WritableByteChannelImpl
		// Encrypted: blockEnc1|blockEnc2|... |blockEncN|lastBlockEnc
		// Decrypted: bockDec1|blockDec2|...|blockDecN|lastBlockDec
		// save block where pos is located in
		synchronized (mLock) {
			long decSize = sizeInternal();
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
			} catch (IOException e) {
				//if (!isIgnorePositionError)
				if (ops.contains(BlockOperationOptions.STOPONPOSITIONERROR))
					return 0;
				// write from the current position
			}
			byte [] enc = encryptBlock(block, 0, len);
			len = enc.length;
			ByteBuffer buf = ByteBuffer.wrap(enc);
			while (buf.remaining() > 0) {
//				if (interruptible){
				if (!ops.contains(BlockOperationOptions.NONINTERRUPTIBLE)){
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
				try {
					saveBlock(mDecPos);
				} catch (GeneralSecurityException | IOException e) {
					//throw new IOException("Unable to set new position: unable to flush buffer");
				}
				//load to buffer if required
				// === 2 - load new block. Continue if load is not supported (write only)
				positionInternal(newPosition);
				try {
					loadBlock(mDecPos);
				} catch (GeneralSecurityException | IOException e) {
					//throw new IOException("Unable to set new position: unable to decrypt");
				}
				// === 3 - set new enc position.
				try {
					mChannel.position(posEnc);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else{
				//set position in plain data
				positionInternal(newPosition);
			}
			return this;
		}
	}
	
	private void positionInternal(long newPosition){
		mDecPos = newPosition;
		if (mDecPos > mDecSize)
			mDecSize = mDecPos;
	}

	//TOTEST
	@Override
	public int write(ByteBuffer src) throws IOException {
		checkOpen();
		synchronized (mLock) {
			int blockPos = getBlockPos(mDecPos, decBlockSize);
			int remains = decBlockSize - blockPos;
			final int len = src.remaining();
			//no block overflow
			if (len < remains){
				src.get(block, blockPos, len);
				positionInternal(mDecPos + len);
				try {
					saveBlock(mDecPos, BlockOperationOptions.STOPONPOSITIONERROR);//, false, true);
				} catch (GeneralSecurityException e) {
					//Do nothing as it was optional flush at any write to under channel 
				}
				return len;
			}

			//block overflow
			//start
			try {
				src.get(block, blockPos, remains);
				positionInternal(mDecPos + remains);
				saveBlock(mDecPos - 1);//mDecPos here is the beginning of next block
			} catch (GeneralSecurityException e) {
				IOException ie = new IOException("Unable to write middle block at position " + (mDecPos - 1));
				ie.initCause(e);
				throw ie;
			}
			//middle
			while(src.remaining() > decBlockSize){
				try {
					src.get(block, 0, decBlockSize);
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
			src.get(block, blockPos, lenEnd);
			positionInternal(mDecPos + lenEnd);
			try {
				saveBlock(mDecPos, BlockOperationOptions.STOPONPOSITIONERROR);
			} catch (GeneralSecurityException e) {
				//Do nothing as it was optional to keep under channel updated 
			}
			return len;
		}
	}

	//TOTEST
	@Override
	public int read(ByteBuffer dst) throws IOException {
		checkOpen();
		// TODO Auto-generated method stub
		// flush buffer if reading beyond current block
		synchronized (mLock) {
			final long decPosStart = mDecPos;
			try {
				loadBlock(mDecPos, BlockOperationOptions.STOPONPOSITIONERROR);
			} catch (GeneralSecurityException e) {
				//Do nothing as it was optional fill buffer at any read operation 
			}
			long sizeDec = sizeInternal();
			long remainsToEnd = sizeDec - mDecPos;//additional check how - much remains to read in this (dec) channel
			int blockPos = getBlockPos(mDecPos, decBlockSize);
			int remains = decBlockSize - blockPos;//remains from mDecPos to the end of block
			int len = dst.remaining();//remains in dst.
			//in case when not enough bytes remain in the channel 
			//to reach either dst size (remainsToEnd < len) or block end (remainsToEnd < remains)
			if (remainsToEnd < remains && remainsToEnd < len){
				dst.put(block, blockPos, (int) remainsToEnd);
				positionInternal(mDecPos + remainsToEnd);
				return len;
			}
			// === no block overflow ===
			//can fit within current block
			if (len < remains){
				dst.put(block, blockPos, len);
				positionInternal(mDecPos + len);
				return len;
			}
			
			// === block overflow ===
			//start
			dst.put(block, blockPos, remains);
			positionInternal(mDecPos + remains);
			//middle
			while(dst.remaining() > decBlockSize){
				try {
					int amt = loadBlock(mDecPos);//loading new block
					dst.put(block, 0, amt);
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
				dst.put(block, 0, amt);
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
			//check - if lucky and remainder decrypts to the same amount of bytes (usually for stream cipher)
			//the same, if fits the end of block (newBlockPos == 0) no need to calculate last block
			if (getEncAmt(newBlockPos) == newBlockPos || newBlockPos == 0){
				mDecSize = size;
				mDecPos = size;
				mChannel.truncate(posEnc + newBlockPos);
				return this;
			}
			//not last block - need to truncate under channel
			if (newBlock < lastBlock){
				mChannel.truncate(posEnc + encBlockSize);
				mChannel.position(posEnc);
			}
			//put dec position and size to the end of last block
			mDecSize = posDec + decBlockSize;
			mDecPos = mDecSize;
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
			mDecPos = size;
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
		//TODO: flush from the buffer, 
		//... or alternatively flush it after each write operation
		try {
			//saveBlock(mDecPos, true, false);
			saveBlock(mDecPos, BlockOperationOptions.NONINTERRUPTIBLE);
		} catch (GeneralSecurityException e) {
			IOException ie = new IOException("Unable to close: cannot flush the buffer");
			ie.initCause(e);
			throw ie;
		}
		mIsOpen = false;
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
//	@Override
//	public void close() throws IOException {
//		//TODO: flush from the buffer, 
//		//... or alternatively flush it after each write operation
//		mIsOpen = false;
//	}
//
//	@Override
//	public boolean isOpen() {
//		return mIsOpen;
//	}
	
}
