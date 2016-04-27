package org.kulturguerilla.byterider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper Class to simplify handling of bit-fuddling for storing
 * multiple different values inside a single short, int or long.
 *
 * Note: currently only long is supported.
 *
 * Currently implemented as an interface to allow mixin-style usage.
 */
public class ByteRider {
	private ArrayList<BitField> fields = new ArrayList<>();

	private static final Logger log = LoggerFactory.getLogger(ByteRider.class);

	public BoolField addBool(String name) {
		int offset = lowestUnusedOffset(fields);
		log.debug("initializing bool starting at offset: " + offset);
		BoolField b = createBoolField(offset, name);
		fields.add(b);
		return b;
	}

	public IntField addInt(int maxValue, String name) {
		return addInt(0, maxValue, name);
	}

	public IntField  addInt(int minValue, int maxValue, String name) {
		int offset = lowestUnusedOffset(fields);
		log.debug("initializing int field starting at offset: " + offset);
		IntField i = createIntField(offset, minValue, maxValue, name);
		fields.add(i);
		return i;
	}

	public <T> IntMappedObjField<T> addObj(int values, Function<T, Integer> fromObject,
			Function<Integer, T> toObject, String name) {
		int offset = lowestUnusedOffset(fields);
		log.debug("initializing enum field starting at offset: " + offset);
		IntMappedObjField<T> e = new EnumFieldImpl<T>(offset, values, fromObject, toObject, name);
		fields.add(e);
		return e;
	}

	public List<BitField> fields() { return this.fields; }

	public void checkFields() {
		checkFields(fields.toArray(new BitField[0]));
	}

	static BoolField createBoolField(int idx, String name) {
		return new LongBool(idx, name);
	}

	static IntField createIntField(int offset, int minValue, int maxValue, String name) {
		return IntImpl.create(offset, minValue, maxValue, name);
	}

	public enum Size {
		BYTE_SET(8), SHORT_SET(16), INT_SET(32), LONG_SET(64);

		final byte size;

		private Size(int size) {
			this.size = (byte) size;
		}
	};

	interface BitField {
		long clear(long field);
		long mask();
		String name();
	}

	static class BaseBitField implements BitField
	{
		protected final long mask;
		protected final long clear;

		private final String name;

		BaseBitField(String name, long mask) {
			this.name = name;
			this.mask  = mask;
			this.clear= ~mask;
		}

		@Override public String name() { return name; }

		@Override public long clear(long field) { return field & clear; }

		@Override public long mask() { return mask; }
	}

	public interface BoolField extends BitField {
		boolean get(long field);
		long set(long field);
		long set(long field, boolean val);
	}

	public interface IntField extends BitField {
		int get(long field);
		long set(long field, int value);
	}

	public interface IntMappedObjField<T> extends BitField {
		T get(long field);
		long set(long field, T x);
	}

	/**
	 * represents a bool as a single bit inside a long.
	 */
	public static class LongBool extends BaseBitField implements BoolField {

		public LongBool(int idx, String name) {
			this(idx, name, Size.LONG_SET);
		}

		public LongBool(int idx, String name, Size size) {
			super(name, 1L << idx);
			if (idx >= size.size || idx < 0) {
				throw new IllegalArgumentException("bit index out of range: idx="
						+ idx + "; available=" + size.size + " for field " + name);
			}
		}

		@Override public boolean get(long field) {
			return (mask & field) == mask;
		}

		@Override public long set(long field) {
			return field | mask;
		}

		@Override public long set(long field, boolean val) {
			return val ? set(field) : clear(field);
		}
	}

	/**
	 * represents an int covering certain bits of a long.
	 */
	public static class IntImpl extends BaseBitField implements IntField {

		final private int offset;
		final private long minValue; // int misbehaves on crossing 32bit boundaries.
		final private int maxValue;

		public static IntImpl create(int offset, int minValue, int maxValue, String name) {
			long bits = bitsRequired(maxValue+1l-minValue);
			long mask = ((1L << bits) - 1) << offset;
			return new IntImpl(mask, offset, minValue, maxValue, name);
		}

		IntImpl(long mask, int offset, int minValue, int maxValue, String name) {
			super(name, mask);
			this.offset = offset;
			this.minValue = minValue;
			this.maxValue = maxValue;
		}

		public int get(long field) {
			return (int) (((field & mask) >> offset) + minValue);
		}

		public long set(long field, int value) {
			if (value < minValue || value > maxValue) {
				throw new IllegalArgumentException(String.format
						("value %d out of range [%d, %d]", value, minValue, maxValue));
			} else {
				return field & clear | ((value - minValue) << offset);
			}
		}
	}

	public static class EnumFieldImpl<T> implements IntMappedObjField<T> {
		final Function<Integer, T> toObject;
		final Function<T, Integer> fromObject;
		final IntField intField;

		public EnumFieldImpl(int offset, int values, Function<T, Integer> fromObject,
				Function<Integer, T> toObject, String name)
		{
			this.intField = IntImpl.create(offset, 0, values+1, name);
			this.toObject = toObject;
			this.fromObject = fromObject;
		}

		@Override public T get(long field) {
			return toObject.apply(intField.get(field));
		}

		@Override public long set(long field, T x) {
			return intField.set(field, fromObject.apply(x));
		}

		@Override public String name() { return intField.name(); }

		@Override public long clear(long field) { return intField.clear(field); }

		@Override public long mask() { return intField.mask(); }
	}

	/**
	 * computes the number of bits required to represent n different
	 * values.
	 */
 	static int bitsRequired(long size) {
		return 64-Long.numberOfLeadingZeros(size-1);
	}

	/**
	 * verify the correctness of multiple BitFields.
	 *
	 * @param fields varargs of BitField instances.
	 * @throws IllegalArgumentException when fields are inconsisent and overlap.
	 */
	public static void checkFields(BitField... fields) {
		long init = 0;
		for (int i = 0; i < fields.length; ++i) {
			log.debug("{} | {} -> {}", Long.toBinaryString(init), Long.toBinaryString(fields[i].mask()), Long.toBinaryString(init | fields[i].mask()));
			// failure case: go back through processed field to find contradicting pair.
			if ((init & fields[i].mask()) != 0) {
				for(int j=0; j<i; ++j) {
					if((fields[i].mask() & fields[j].mask()) != 0) {
						throw new IllegalArgumentException("overlapping bitfields: "
								+ fields[i].name() + " <> " + fields[j].name());
					}
				}
			}
			init |= fields[i].mask();
		}
	}

	public static int lowestUnusedOffset(List<BitField> fields) {
		long mask = 0;
		for (BitField f : fields) {
			mask |= f.mask();
		}
		return 64 - Long.numberOfLeadingZeros(mask);
	}

}