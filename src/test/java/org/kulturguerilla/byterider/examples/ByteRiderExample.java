package org.kulturguerilla.byterider.examples;

import static org.kulturguerilla.byterider.ByteRider.*;

import org.kulturguerilla.byterider.ByteRider;

/**
 * Same as {@link BitMaskBasedExample}, but implemented using byte rider.
 *
 * Advantages:
 * - boundary checks before writes: prevents accidental overwriting of adjacent bits
 * - eases transformation of values; the output of bit operations is usually
 *   some primitive in the range [0, 2^k) (where k is the number of bits used)
 */
public class ByteRiderExample implements ExampleInterface {

	/**
	 * byte rider instance: one instance per class and primitive, abstracts the
	 * bit shifting and attempts to make sure you're not shooting yourself in
	 * the foot.
	 *
	 * If you are running out of bits, to manage a second primitive value a second
	 * instance is needed.
	 */
	static ByteRider br = new ByteRider();

	/** field representing storage for a single primitive boolean. */
	private static final BoolField booleanField = br.addBool("booleanField");

	/** field representing storage for an integer in range -4, 4 inclusive. */
	private static final IntField intField = br.addInt(-4, 4, "intField");

	private static final IntMappedObjField<SampleEnum> eField = br.addEnum(
			SampleEnum.class, "sample enum");
	// private static final IntMappedObjField<SampleEnum> eField = br.addObj(
	// 		SampleEnum.values().length,
	// 		e -> e.ordinal(),
	// 		i -> SampleEnum.values()[i],
	// 	    "sample enum");

	static {
		br.checkFields();
	}

	private long data;

	public ByteRiderExample(boolean booleanField, int intField, SampleEnum e) {
		if (booleanField) {
			this.setBooleanField();
		} else {
			this.clearBooleanField();
		}
		this.setIntField(intField);
		setSampleEnum(e);
	}

	public boolean booleanField() {
		return booleanField.get(data);
	}

	public void setBooleanField() {
		this.data = booleanField.set(data, true);
	}

	public void clearBooleanField() {
		this.data = booleanField.set(data, false);
	}

	public int intField() {
		return intField.get(data);
	}

	public void setIntField(int value) {
		this.data = intField.set(data, value);
	}

	public SampleEnum sampleEnum() {
		return eField.get(data);
	}

	public void setSampleEnum(SampleEnum e) {
		this.data = eField.set(data, e);
	}
}
