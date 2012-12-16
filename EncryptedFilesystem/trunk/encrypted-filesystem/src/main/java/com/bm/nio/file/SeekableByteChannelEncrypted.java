package com.bm.nio.file;

import java.io.IOException;
import java.io.OutputStream;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

//TODO: implement based on underlying channel
class SeekableByteChannelEncrypted implements SeekableByteChannel{
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
    protected byte buf[];
    private int plainDataBlockSize;
    private int encryptedDataBlockSize;
    private long mPosition = 0;
    //protected byte bufEnc[];//encrypted data for buf after flush
    //protected int count;
    public static final String PLAIN_BLOCK_SIZE = "block.size";
    public static final String PASSWORD = "password";
    public static final String TRANSFORMATION = "transformation";

	protected final SeekableByteChannel mChannel;
	
    public SeekableByteChannelEncrypted(SeekableByteChannel channel) throws GeneralSecurityException {
        this(channel, null);
    }
	
	public SeekableByteChannelEncrypted(SeekableByteChannel channel, Map<String, ?> props) throws GeneralSecurityException {
    	if (props == null)
    		props = new HashMap<String, Object>();
        initProps(props);
        //final int defaultBlockSize = (8192 / encipher.getBlockSize()) * encipher.getBlockSize()
        //		                  + ((8192 % encipher.getBlockSize()) == 0 ? 0 : encipher.getBlockSize());
        plainDataBlockSize = props.containsKey(PLAIN_BLOCK_SIZE) ?
        			(Integer)props.get(PLAIN_BLOCK_SIZE) : 8192; //encipher.getOutputSize(8192);
        if (plainDataBlockSize <= 0) {
            throw new IllegalArgumentException("Block size <= 0");
        }
        
        final int interEncryptedBlockSize = encipher.getOutputSize(plainDataBlockSize);//before second encryption
        encryptedDataBlockSize = encipher.getOutputSize(interEncryptedBlockSize);
        //if (encipher.getOutputSize(blockSize) != blockSize){
        	//throw new IllegalArgumentException("Stream block size should be multiplier of cipher block size");
        //}
        this.buf = new byte[Math.max(interEncryptedBlockSize, Math.max(plainDataBlockSize, encryptedDataBlockSize))];
		mChannel = channel;
		mIsOpen = true;
	}
	
	public int getPlainDataBlockSize(){
		return plainDataBlockSize;
	}
	
	public int getEncryptedDataBlockSize(){
		return encryptedDataBlockSize;
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
        //dcipher = Cipher.getInstance("AES/CFB/NoPadding");
        encipher = Cipher.getInstance(transformation);
        decipher = Cipher.getInstance(transformation);
        iv = initEncipher(encipher, key);
        initDecipher(decipher, key, iv);
    }

    protected byte[] initEncipher(Cipher encipher, SecretKey key) throws GeneralSecurityException{
        encipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(new byte[16]));
        AlgorithmParameters params = encipher.getParameters();
        return params.getParameterSpec(IvParameterSpec.class).getIV();
    }
    
    protected void initDecipher(Cipher decipher, SecretKey key, byte [] iv) throws GeneralSecurityException{
        decipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
    }

    public byte [] encryptBlock(byte [] bufPlain) throws GeneralSecurityException {
    	try{
	        return encipher.doFinal(bufPlain);
    	} catch (GeneralSecurityException e){
    		initEncipher(encipher, key);
    		throw e;
    	}
    }

    public byte [] decryptBlock(byte [] bufEnc) throws GeneralSecurityException {
    	try {
            return decipher.doFinal(bufEnc);
		} catch (GeneralSecurityException e) {
			initDecipher(decipher, key, iv);
			throw e;
		}
    }
    
    //=== OLD ===
//    public String decrypt(String base64EncryptedData) throws Exception {
//        decipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
//        byte[] decryptedData = DatatypeConverter.parseBase64Binary(base64EncryptedData);//base64EncryptedData.getBytes();//new sun.misc.BASE64Decoder().decodeBuffer(base64EncryptedData);
//        byte[] utf8 = decipher.doFinal(decryptedData);
//        return new String(utf8, "UTF8");
//    }
//
//    protected byte [] encrypt(byte [] bufPlain) throws IOException {
//    	//TODO : implement encryption
//    	//System.arraycopy(bufPlain, 0, bufEnc, 0, count);
//    	try{
//	        encipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(new byte[16]));
//	        AlgorithmParameters params = encipher.getParameters();
//	        iv = params.getParameterSpec(IvParameterSpec.class).getIV();
//	        //TODO check cases for block padding, when encrypted buffer should be more than plain text
//	        bufEnc = encipher.doFinal(bufPlain);
//    	} catch (Exception e){
//    		throw new IOException(e);
//    	}
//    	return bufEnc;
//    }
    //====


	volatile boolean mIsOpen;
	@Override
	public void close() throws IOException {
		//TODO: flush from the buffer, 
		//... or alternatively flush it after each write operation
		mIsOpen = false;
	}

	@Override
	public boolean isOpen() {
		return mIsOpen;
	}

	private void checkOpen() throws IOException{
		if (!isOpen())
			throw new ClosedChannelException();
	}
	
	public long sizeEncrypted() throws IOException {
		checkOpen();
		return mChannel.size();
	}

	@Override
	public long size() throws IOException {
		checkOpen();
		
		long longSize = mChannel.size();
		int intSize = (int)longSize;
		if (intSize == longSize)//if size is measured in int
			return decipher.getOutputSize(intSize);
		else{ //if size is measured in long
			long blockSize = encryptedDataBlockSize;
			long blocksCnt = (longSize / blockSize);
			long size1 = blocksCnt * plainDataBlockSize;
			//remainder will be inaccurate if padding is used in encryption
			//because encrypting 10 bytes will occupy 32 bytes, and remainder will return 32 (when actually 10 data + 22 padding)
			int remainder = (int) (longSize - (blockSize * blocksCnt)); 
			long size2 = decipher.getOutputSize(remainder);
			return size1 + size2;
		}
		
	}
	
	/**
	 * @return position in decrypted data
	 * @throws IOException
	 */
	@Override
	public long position() throws IOException {
		checkOpen();
		return mPosition;
	}
	
	/**
	 * @return position in encrypted data.
	 * @throws IOException
	 */
	public long positionEncrypted() throws IOException {
		checkOpen();
		return mChannel.position();
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
	private int getPosInBlock(long pos){
		final long blockSize = (long)plainDataBlockSize;
		long tmp = pos % blockSize;
		return (int)tmp;
	}

	/**
	 * Decrypts block and load to buffer from the current encrypted position
	 */
	private void loadBlock(){
		//TODO:
	}
	/**
	 * Encrypts block and puts buffer to underChannel from the current encrypted position
	 */
	private void saveBlock(){
		//TODO:
	}
	/**
	 * @param newPosition - sets position in plain (decrypted) data
	 * @return
	 * @throws IOException
	 */
	//TOTEST
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
		//if current block (buffer) changes
		if (getBlockNum(newPosition, plainDataBlockSize) != getBlockNum(mPosition, plainDataBlockSize)){
			//set position in underlying channel to the beginning of block mChannel.position();
			//can be overflow when newPosition > MAX_LONG/(encryptedDataBlockSize/plainDataBlockSize), then 
			//posEnc > MAX_LONG
			long posEnc = getBlockNum(newPosition, plainDataBlockSize) * (long)encryptedDataBlockSize; 
			//out of bounds
			if (posEnc > sizeEncrypted()){//position > that size. Don't rely on size(), it may be inaccurate
				posEnc = sizeEncrypted();
				long lastBlock = getBlockNum(posEnc, encryptedDataBlockSize);
				//can only put position to the beginning of the last block as can't say more accurate for plain data
				posEnc = lastBlock * (long)encryptedDataBlockSize;
				newPosition = lastBlock * (long)plainDataBlockSize;
			}
			mChannel.position(posEnc);
			//load to buffer if required
			//loadBlock(getBlockNum(newPosition));
			loadBlock();
		}
		//set position in plain data
		mPosition = newPosition;
		return this;
	}

	//TOTEST
	@Override
	public int read(ByteBuffer dst) throws IOException {
		checkOpen();
		// TODO Auto-generated method stub
		return 0;
	}

	//TOTEST
	@Override
	public int write(ByteBuffer src) throws IOException {
		checkOpen();
		// TODO Auto-generated method stub
		return 0;
	}

	//TOTEST
	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		checkOpen();
		// TODO Auto-generated method stub
		return null;
	}

	
}
