package org.kulturguerilla.byterider;

import java.io.ByteArrayOutputStream;

/**
 * tooling to encode / decode sequences (arrays) of integers as a byte [].
 *
 * The encoding consists of two steps:
 * - delta compression
 * - store individual entries as variable size integers
 *
 * This is useful and efficient for situations where long sequences of values
 * with little variation (or slowly changing) have to be stored efficiently.
 */
public class DeltaCompression {

	public static byte [] encode (int [] is) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(is.length * 4);
		for (int v : is) {
			int normalized = v < 0 ? (-v << 1) | 0x1 : v << 1;
			int size = Math.max(1, (38 - Integer.numberOfLeadingZeros(normalized)) / 7);
			for(int x = 1; x <= size; ++x) {
				if (x == size) {
					baos.write((byte) ((normalized >> (((size - x) * 7))) & 0x7F));
				} else {
					baos.write((byte) (normalized >> (((size - x) * 7) & 0x7F) | 0x80));
				}
			}
		}
		return baos.toByteArray();
	}

	public static int [] decode(byte [] bs) {
		int numInts = 0;
		for (byte b: bs) {
			numInts += ((b & 0x80) == 0) ? 1 : 0;
		}
		int idx = 0;
		int [] result = new int[numInts];
		for(int i = 0; i < numInts; ++i) {
			int tmp = 0;
			while (true) {
				tmp = (tmp << 7) | (bs[idx] & 0x7F);
				if ((bs[idx++] & 0x80) == 0) {
					result[i] = (tmp & 0x1) == 1 ? -(tmp >> 1) : tmp >> 1;;
					break;
				}
			}
		}
		return result;
	}

	public static byte [] encodeInt(int v) {
		int normalized = v < 0? (-v << 1) | 0x1 : v << 1;
		int size       = Math.max(1, (38 - Integer.numberOfLeadingZeros(normalized))/7);
		byte [] result = new byte[size];
		for(int x = 0; x < size; ++x) {
			result[x] = (byte) (normalized >> (((size-x-1)*7) & 0x7F) | 0x80);
		}
		result[size-1] &= 0x7F;
		return result;
	}

	public static int decodeInt(byte [] bs) {
		int result = 0;
		for(int i = 0; i < bs.length; ++i) {
			result = (result << 7) | (bs[i] & 0x7F);
			if((bs[i] & 0x80) == 0) break;
		}
		return (result & 0x1) == 1
				? -(result >> 1)
				: result >> 1;
	}
}
