package cz.muni.fi.randgka.library;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.util.Log;

public class ByteableList<E> extends ArrayList<E> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6084608088882060290L;
	
	public byte[] getBytes() {
		try {
			ByteArrayOutputStream bStream = new ByteArrayOutputStream();
			Iterator<E> it = this.iterator();
			while (it.hasNext()) {
				Byteable b = (Byteable)it.next();
				bStream.write(b.getBytes());
				bStream.write(0);
				Log.d("b.bytes", new String(b.getBytes()));
			}
			return bStream.toByteArray();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public <T extends Byteable> void fromBytes(Class<T> targetClass, byte[] bytes, int length) {
		ByteArrayOutputStream bStream = new ByteArrayOutputStream();
		int i = 0;
		try {
			while (bytes[i] < length) {
				if (bytes[i] == (byte)0) {
						T t = targetClass.newInstance();
						t.fromBytes(bStream.toByteArray());
						bStream = new ByteArrayOutputStream();
						Log.d("t.bytes", new String(t.getBytes()));
						this.add((E)t);
				}
				i++;
			}
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int length() {
		int length = 0;
		Iterator<E> it = this.iterator();
		while (it.hasNext()) {
			Byteable b = (Byteable)it.next();
			length += b.length();
		}
		return length;
	}
	
}
