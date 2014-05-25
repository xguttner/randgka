package cz.muni.fi.randgka.tools;

import java.io.Serializable;

/**
 * Class for processing of long sequences of data.
 */
public class ByteSequence implements Serializable {
	
	private static final long serialVersionUID = -4124621094801727313L;
	
	private byte[] sequence; // sequence carrying the randomness
	private int byteLength; // length in bytes (with possible redundancy
	private int bitLength; // exact length of sequence in bits
	
	public ByteSequence() {
		this.bitLength = 0;
		this.byteLength = 0;
	}
	
	public ByteSequence(byte[] sequence) {
		this(sequence, sequence.length*8);
	}
	
	public ByteSequence(byte[] sequence, int bitLength) {
		this.sequence = sequence;
		this.setSequence(sequence, bitLength);
	}
	
	/**
	 * rotates bits in sequence one position to the left
	 */
	public void rotateBitsLeft() {
		if (this.bitLength > 0) {
			byte leftmostBitOld = (byte)((0x80 & sequence[0]) >>> (this.bitLength-1)%8);
			byte leftmostBitNew = 0;
			for (int i = this.byteLength - 1; i >=0; i--) {
				leftmostBitNew = (byte)(0x01 &((0x80 & sequence[i]) >>> 7));
				sequence[i] = (byte)((int)(sequence[i] << 1) ^ (int)leftmostBitOld);
				leftmostBitOld = leftmostBitNew;
			}
		}
	}
	
	/**
	 * Scalar product of two sequences resulting in one bit b stored in a byte in a form 
	 * 0000000b
	 * 
	 * @param seq2
	 * @return 
	 * @throws LengthsNotEqualException
	 */
	public byte scalarProduct(ByteSequence seq2) throws LengthsNotEqualException {
		
		if (this.bitLength != seq2.bitLength) throw new LengthsNotEqualException();
		
		byte[] seq2s = seq2.getSequence();
		
		byte result = 0;
		for (int i = 0; i < this.bitLength; i++) {
			result = (byte)((int)result ^ ((int)(0x01 & (this.sequence[(int)(i/8)] >> i%8)) & (int)(0x01 & (seq2s[(int)(i/8)] >> i%8))));
		}
		return result;
	}
	
	/**
	 * adds new bit at the end of the bit string
	 * 
	 * @param bitCarryingByte in a form 0000000b
	 */
	public void addBit(byte bitCarryingByte) {
		if (this.bitLength%8 == 0 && (this.bitLength+1)/8 == this.byteLength) {
			byte[] newArray = new byte[this.byteLength+1];
            for (int i = 0; i < this.byteLength; i++) {
                newArray[i] = this.sequence[i];
            }
            for (int i = 0; i < 1; i++) {
            	newArray[this.byteLength + i] = 0x00;
            }
            this.sequence = newArray;
            this.byteLength = this.byteLength+1;
		}
		
		this.sequence[(int)(this.bitLength/8)] = (byte)((int)this.sequence[(int)(this.bitLength/8)] ^ (int)(bitCarryingByte << (7 - (this.bitLength)%8)));
		this.bitLength++;
	}
	
	/**
	 * addition of seq2 to this sequence with respect to their bit lengths, without any gap between them
	 * @param seq2
	 */
	public void add(ByteSequence seq2) {
		if (seq2.getByteLength() > 0) {
			int oldSequenceLength = this.byteLength;
			int toFill = this.byteLength*8 - this.bitLength;
			if (this.byteLength > 0) {
				this.sequence[this.byteLength-1] = (byte)(this.sequence[this.byteLength-1] ^ ((0xff & seq2.getSequence()[0]) >>> (8-toFill)));
			}
			this.setBitLength(this.bitLength+seq2.getBitLength());
			for (int i = 0; i < seq2.getByteLength()-1; i++) {
				this.sequence[oldSequenceLength + i] = (byte)((seq2.getSequence()[i] << toFill) ^ ((0xff & seq2.getSequence()[i+1]) >>> (8-toFill)));
			}
			if (this.byteLength == oldSequenceLength + seq2.getByteLength()) this.sequence[this.byteLength-1] = (byte)(seq2.getSequence()[seq2.getByteLength()-1] << toFill);
		}
	}

	public int getByteLength() {
		return this.byteLength;
	}
	
	public int getBitLength() {
		return this.bitLength;
	}
	
	public byte[] getSequence() {
		return sequence;
	}

	/**
	 * set bit length, allocate larger array if need be
	 * 
	 * @param bitLength
	 */
	public void setBitLength(int bitLength) {
		int newByteLength = bitLength/8 + ((bitLength%8>0)?1:0);
		if (this.bitLength == 0) {
			this.sequence = new byte[newByteLength];
		} else if (newByteLength > this.byteLength) {
			byte[] newSequence = new byte[newByteLength];
			System.arraycopy(this.sequence, 0, newSequence, 0, this.byteLength);
			this.sequence = newSequence;
		}
		this.byteLength = newByteLength;
		this.bitLength = bitLength;
	}
	
	public void setSequence(byte[] sequence) {
		this.setSequence(sequence, sequence.length*8);
	}

	public void setSequence(byte[] sequence, int bitLength) {
		this.sequence = sequence;
		this.bitLength = bitLength;
		this.byteLength = (int)Math.ceil((double)bitLength/8);
		this.cleanLastByte();
	}
	
	public ByteSequence getSubSequence(int start, int length) {
		int byteLength = (int) Math.ceil((double)length/8);
		int byteStart = (int) Math.ceil((double)start/8);
		if (this.bitLength < byteStart*8 + length) length = this.bitLength - byteStart*8;

		byte[] bytes = new byte[byteLength];
		System.arraycopy(sequence, byteStart, bytes, 0, byteLength);
		ByteSequence bs = new ByteSequence(bytes, length);
		return bs;
	}
	
	/**
	 * set the unused bits of the last byte to be 0
	 */
	private void cleanLastByte() {
		if (byteLength > 0) this.sequence[byteLength-1] = (byte)(this.sequence[byteLength-1] & (0xff << (8 - bitLength%8)));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bitLength;
		for (int i=0; i < ((int)(bitLength/8) + ((bitLength%8>0)?1:0)); i++) {
			result = prime * result + this.sequence[i];
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true; 
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ByteSequence other = (ByteSequence) obj;
		if (bitLength != other.bitLength)
			return false;
		
	    for (int i=0; i < ((int)(bitLength/8) + ((bitLength%8>0)?1:0)); i++) {
	        Object o1 = this.sequence[i];
	        Object o2 = other.sequence[i];
	        if (!(o1==null ? o2==null : o1.equals(o2)))
	            return false;
	    }
		
		return true;
	}

	@Override
	public String toString() {
		String bitValue = new String();
		if (this.byteLength > 0) {
			for (int i = 0; i < this.byteLength; i++) {
				bitValue += String.format("%08d", Integer.parseInt(Integer.toBinaryString(0xff & this.sequence[i])))+" ";
			}
		}
		return bitValue+" "+bitLength+" "+byteLength;
	}
	
}
