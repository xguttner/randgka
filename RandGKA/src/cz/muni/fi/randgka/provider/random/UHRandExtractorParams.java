package cz.muni.fi.randgka.provider.random;

import java.util.Map.Entry;
import java.util.TreeMap;

public class UHRandExtractorParams {

	private static TreeMap<Integer, Integer> lengths;
	private static boolean isSet = false;
	public static final int MAXIMAL_OUTPUT = 2161,
							MAXIMAL_INPUT = 2447;
	
	/**
	 * set a number of different lengths to enable appropriate length retrieval
	 */
	private static void setLengths() {
		lengths = new TreeMap<Integer, Integer>();
		lengths.put(162, 347);
		lengths.put(276, 467);
		lengths.put(516, 719);
		lengths.put(1053, 1283);
		lengths.put(MAXIMAL_OUTPUT, MAXIMAL_INPUT);
	}
	
	/**
	 * @param length - wanted randomness extraction output length
	 * @return least greater tuple (input length x output length) able to produce given output length
	 */
	public static Entry<Integer, Integer> getLengths(int length) {
		if (!isSet) setLengths();
		
		for (Entry<Integer, Integer> e : lengths.entrySet()) {
			if (e.getKey() >= length) return e;
		}
		return null;
	}
}
