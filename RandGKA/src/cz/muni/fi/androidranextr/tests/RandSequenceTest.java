package cz.muni.fi.androidranextr.tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import cz.muni.fi.randgka.library.ByteSequence;

public class RandSequenceTest {

	private ByteSequence seq1, seq11, seq2;
	
	@Before
	public void setUp() {
		byte[] byteSeq1 = new byte[] {(byte)0x88, (byte)0xff, (byte)0xfe, (byte)0x00};
		byte[] byteSeq11 = new byte[] {(byte)0x88, (byte)0xff, (byte)0xfe, (byte)0x06, (byte)0x80};  //byteSeq1 + 011010
		byte[] byteSeq2 = new byte[] {(byte)0x11, (byte)0xff, (byte)0xfc, (byte)0x10};
		seq1 = new ByteSequence(byteSeq1, 28);
		seq11 = new ByteSequence(byteSeq11, 34);
		seq2 = new ByteSequence(byteSeq2, 28);
	}
	
	/*@Test
	public void testRotateBitsLeft() {
		seq1.rotateBitsLeft();
		(new RandSequence()).rotateBitsLeft();
		assertEquals(seq1, seq2);
	}

	@Test
	public void testScalarProduct() {
		byte[] ones = new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
		byte[] zeros = new byte[]{(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};
		byte scalarProduct;
		try {
			System.out.println("testScalar1");
			scalarProduct = seq1.scalarProduct(new RandSequence(ones, 28));
			assertEquals((byte)0x01, scalarProduct);
			System.out.println("testScalar2");
			scalarProduct = seq1.scalarProduct(new RandSequence(zeros, 28));
			assertEquals((byte)0x00, scalarProduct);
		} catch (LengthsNotEqualException e) {
			fail("Can't recognize equal lengths.");
		}
		try {
			scalarProduct = seq1.scalarProduct(new RandSequence(ones, 29));
			fail("Can't recognize different lengths.");
		} catch (LengthsNotEqualException e) {}
	}*/

	/*@Test
	public void testAddBit() {
		seq1.addBit((byte)0x00);
		seq1.addBit((byte)0x01);
		seq1.addBit((byte)0x01);
		seq1.addBit((byte)0x00);
		seq1.addBit((byte)0x01);
		seq1.addBit((byte)0x00);
		assertEquals(seq1, seq11);
	}*/

	@Test
	public void testAdd() {
		//System.out.println(Integer.toBinaryString((byte)0xff)+"\n"+Integer.toBinaryString((byte)0xff >>> 2));
		/*System.out.println(seq1+" "+seq2+" "+seq11);
		System.out.println("testAdd1");
		seq1.add(seq2);
		System.out.println(seq1);
		System.out.println("testAdd2");
		seq1.add(seq11);
		System.out.println(seq1);*/
		ByteSequence s0 = new ByteSequence(new byte[]{(byte)0xfe, (byte)0x01, (byte)0x12, (byte)0xbc, (byte)0xd4, (byte)0x02}, 43);
		ByteSequence s1 = new ByteSequence(new byte[]{(byte)0x02, (byte)0x32, (byte)0x12, (byte)0xa1, (byte)0xa8, (byte)0x16, (byte)0x05}, 51);
		ByteSequence s2 = new ByteSequence(new byte[]{(byte)0x07, (byte)0x33}, 16);
		ByteSequence s3 = new ByteSequence(new byte[]{(byte)0x74, (byte)0x33, (byte)0x48}, 22);
		ByteSequence s4 = new ByteSequence(new byte[]{(byte)0xa1, (byte)0x41, (byte)0xac}, 17);
		ByteSequence s5 = new ByteSequence(new byte[]{(byte)0xc5, (byte)0x58, (byte)0x0c, (byte)0x0c, (byte)0x71, (byte)0x4e, (byte)0x15}, 56);
		System.out.println(s0+" "+s1);
		s0.add(s1);
		System.out.println(s0+" "+s2);
		s0.add(s2);
		System.out.println(s0+" "+s3);
		s0.add(s3);
		System.out.println(s0+" "+s4);
		s0.add(s4);
		System.out.println(s0+" "+s5);
		s0.add(s5);
		System.out.println(s0);
	}
}
