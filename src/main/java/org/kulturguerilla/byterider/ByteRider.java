package org.kulturguerilla.byterider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper Class to simplify handling of bit-fuddling for storing
 * multiple different values inside a single primitive long.
 */
public class ByteRider {
	private ArrayList<BitField> fields = new ArrayList<>();

	private static final Logger log = LoggerFactory.getLogger(ByteRider.class);

	private final Size size;

	public ByteRider(Size size) {
		this.size = size;
	}

	public ByteRider() {
		this(Size.LONG_SET);
	}

	/**
	 * Creates a {@link BoolField} with the provided name.
	 *
	 * Reserves the lowest currently unused bit to hold information about a single
	 * boolean value.
	 *
	 * @param name the name of the field (for providing better error messages)
	 * @return bool field representing storage for a single boolean value
	 */
	public BoolField addBool(String name) {
		int offset = lowestUnusedOffset(fields);
		log.debug("initializing bool starting at offset: " + offset);
		BoolField b = createBoolField(offset, name);
		addField(b);
		return b;
	}

	private void addField(BitField bf) {
		log.debug("{} <> {}", bf.highestBit(), size.size);
		if (bf.highestBit() >= size.size) {
			throw new IllegalArgumentException("field overflows availabile bits: " +
					bf.name());
		} else {
			this.fields.add(bf);
		}
	}

	/**
	 * creates an {@link IntField} with the provided name, values ranging from
	 * zero to maxValue, inclusive.
	 *
	 * Reserves the required number of bits to store an int value in the desired range.
	 *
	 * @param maxValue maximum valid value for the field.
	 * @param name field name
	 * @return int field representing storage for the int in the given range.
	 */
	public IntField addInt(int maxValue, String name) {
		return addInt(0, maxValue, name);
	}

	/**
	 * creates an {@link IntField} with the provided name, values ranging from
	 * minValue to maxValue, inclusive.
	 *
	 * Reserves the required number of bits to store an int value in the desired range.
	 *
	 * @param minValue minimum valid value for the field
	 * @param maxValue maximum valid value for the field
	 * @param name field name
	 * @return int field representing storage for the int in the given range.
	 */
	public IntField  addInt(int minValue, int maxValue, String name) {
		int offset = lowestUnusedOffset(fields);
		log.debug("initializing int field starting at offset: " + offset);
		IntField i = createIntField(offset, minValue, maxValue, name);
		addField(i);
		return i;
	}

	public LongField addLong(long maxValue) {
		return addLong(0, maxValue, "");
	}

	public LongField addLong(long maxValue, String name) {
		return addLong(0, maxValue, name);
	}

	public LongField addLong(long minValue, long maxValue, String name) {
		int offset = lowestUnusedOffset(fields);
		LongField i = createLongField(offset, minValue, maxValue, name);
		addField(i);
		return i;
	}



	/**
	 * creates a field for storing objects of type T, with the given number of
	 * distinct values and functions for converting between int and T.
	 *
	 * TODO: add use cases, e.g. LengthInMetres(100), or enum(?) or map-lookup
	 *   without providing access to the map directly, using the functions.
	 *
	 * @param <T> the type of the object to be stored
	 * @param cardinality the number of different objects to be stored
	 * @param fromObject function that accepts an object of type T and returns the
	 * 	  canonical Integer representation.
	 * @param toObject function that accepts an Integer and returns an object
	 *    of type T.
	 * @param name field name
	 * @return int field representing storage for the int in the given range.
	 */
	public <T> IntMappedObjField<T> addObj(int cardinality,
			Function<T, Integer> fromObject,
			Function<Integer, T> toObject, String name)
	{
		int offset = lowestUnusedOffset(fields);
		log.debug("initializing obj field starting at offset: " + offset);
		IntMappedObjField<T> e = new ObjFieldImpl<T>(offset, cardinality,
				fromObject, toObject, name);
		addField(e);
		return e;
	}

	/**
	 * creates a field for storing an enum into the bit store.
	 *
	 * Automatically copmutes the required number of bits and uses the
	 * Enums ordinal function to convert to integer under the hood.
	 *
	 * @param <T> the type of the enum to be stored.
	 * @param enumClass class representation of the enum to be stored
	 * @param name field name
	 * @return enum field providing access to storage for the provided enum.
	 */
	public <T extends Enum<T>> IntMappedObjField<T> addEnum(
			Class<T> enumClass, String name)
	{
		int offset = lowestUnusedOffset(fields);
		log.debug("initializing enum field starting at offset: " + offset);
		T [] candidates = enumClass.getEnumConstants();
		IntMappedObjField<T> e = new ObjFieldImpl<T>(offset, candidates.length,
				en -> en.ordinal(), i -> candidates[i], name);
		addField(e);
		return e;
	}

	public List<BitField> fields() { return this.fields; }

	public void checkFields() {
		checkFields(fields.toArray(new BitField[0]));
	}

	public static BoolField createBoolField(int idx, String name) {
		return new BoolImpl(idx, name);
	}

	public static IntField createIntField(int offset, int minValue, int maxValue, String name) {
		return IntImpl.create(offset, minValue, maxValue, name);
	}

	public static LongField createLongField(int offset, long minValue, long maxValue, String name) {
		return LongImpl.create(offset, minValue, maxValue, name);
	}

	public enum Size {
		BYTE_SET(8), SHORT_SET(16), INT_SET(32), LONG_SET(64);

		final byte size;

		final long mask() {
			// return (1L << size)-1;
			return (1L << (size-1)) - 1 + (1L << (size-1));
		}

		private Size(int size) {
			this.size = (byte) size;
		}
	};

	interface BitField {
		long clear(long field);
		long mask();
		int highestBit();
		String name();
	}

	static abstract class BaseBitField implements BitField
	{
		protected final long mask;
		protected final long clear;

		private final String name;
		private final int highestBit;

		BaseBitField(String name, long mask, int highestBit) {
			this.name = name;
			this.mask  = mask;
			this.clear= ~mask;
			this.highestBit = highestBit;
		}

		@Override public String name() { return name; }

		@Override public long clear(long field) { return field & clear; }

		@Override public long mask() { return mask; }

		@Override public int highestBit() { return this.highestBit; }
	}

	/**
	 * Represents a {@link BitField} containing a single, non-nullable,
	 * primitive boolean.
	 */
	public interface BoolField extends BitField {
		boolean get(long field);
		long set(long field);
		long set(long field, boolean val);
	}

	/**
	 * Represents a {@link BitField} containing a primitive, non-nullable
	 * int value.
	 */
	public interface IntField extends BitField {
		int get(long field);
		long set(long field, int value);
		int maxValue();
		int minValue();
	}

	/**
	 * Represents a {@link BitField} containing a primitive, non-nullable
	 * long value.
	 */
	public interface LongField extends BitField {
		long get(long field);
		long set(long field, long value);
		long minValue();
		long maxValue();
	}

	/**
	 * Represents a {@link BitField} containing an non-nullable object.
	 */
	public interface IntMappedObjField<T> extends BitField {
		T get(long field);
		long set(long field, T x);
	}

	/**
	 * Represents a {@link BitField} containing a single, nullable,
	 * primitive boolean.
	 */
	public interface IntegerField extends BitField {
		Integer get(long field);
		long set(long field, Integer x);
	}

	/**
	 * represents a bool as a single bit.
	 */
	public static class BoolImpl extends BaseBitField implements BoolField {

		public BoolImpl(int idx, String name) {
			super(name, 1L << idx, idx);
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
			int bits = bitsRequired(maxValue+1l-minValue);
			long mask = ((1L << bits) - 1) << offset;
			return new IntImpl(mask, offset, minValue, maxValue, name, offset + bits - 1);
		}

		IntImpl(long mask, int offset, int minValue, int maxValue, String name, int highestBit) {
			super(name, mask, highestBit);
			this.offset = offset;
			this.minValue = minValue;
			this.maxValue = maxValue;
		}

		@Override public int get(long field) {
			return (int) (((field & mask) >> offset) + minValue);
		}

		@Override public long set(long field, int value) {
			if (value < minValue || value > maxValue) {
				throw new IllegalArgumentException(String.format
						("value %d out of range [%d, %d]", value, minValue, maxValue));
			} else {
				return field & clear | ((value - minValue) << offset);
			}
		}

		@Override public int minValue() {
			return (int) minValue;
		}

		@Override public int maxValue() {
			return maxValue;
		}
	}

	/**
	 * represents an int covering certain bits of a long.
	 */
	public static class LongImpl extends BaseBitField implements LongField {

		final private int offset;
		final private long minValue; // int misbehaves on crossing 32bit boundaries.
		final private long maxValue;

		public static LongImpl create(int offset, long minValue, long maxValue, String name) {
			int bits = bitsRequired(maxValue+1l-minValue);
			long mask = ((1L << bits) - 1) << offset;
			return new LongImpl(mask, offset, minValue, maxValue, name, offset + bits - 1);
		}

		LongImpl(long mask, int offset, long minValue, long maxValue, String name, int highestBit) {
			super(name, mask, highestBit);
			this.offset = offset;
			this.minValue = minValue;
			this.maxValue = maxValue;
		}

		@Override public long get(long field) {
			return ((field & mask) >> offset) + minValue;
		}

		@Override public long set(long field, long value) {
			if (value < minValue || value > maxValue) {
				throw new IllegalArgumentException(String.format
						("value %d out of range [%d, %d]", value, minValue, maxValue));
			} else {
				return field & clear | ((value - minValue) << offset);
			}
		}

		@Override public long minValue() {
			return (int) minValue;
		}

		@Override public long maxValue() {
			return maxValue;
		}
	}

	public static class ObjFieldImpl<T> implements IntMappedObjField<T> {
		final Function<Integer, T> toObject;
		final Function<T, Integer> fromObject;
		final IntField intField;

		public ObjFieldImpl(int offset, int values, Function<T, Integer> fromObject,
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

		@Override public int highestBit() { return intField.highestBit(); }
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
