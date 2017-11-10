package org.kulturguerilla.byterider.examples;

/**
 * Simple example for storing multiple fields in a single primitive value.
 */
public class BitMaskBasedExample implements ExampleInterface {


    private static final long ALL_BITS             = ~0L;
	private static final long BOOLEAN_FIELD_OFFSET = 0;
	private static final long INT_FIELD_OFFSET     = 1;
	private static final long INT_FIELD_BITS       = 0xF << INT_FIELD_OFFSET;
	private static final long ENUM_OFFSET          = 5;
	private static final long ENUM_FIELD_BITS      = 0x3 << ENUM_OFFSET;
	private long data;

	public BitMaskBasedExample(boolean booleanField, int intField, SampleEnum e) {
		if (booleanField) {
			this.setBooleanField();
		} else {
			this.clearBooleanField();
		}
		this.setIntField(intField);
		setSampleEnum(e);
	}

	public boolean booleanField() {
		return ( (data >> BOOLEAN_FIELD_OFFSET ) & 1) == 1;
	}

	public void setBooleanField() {
		data |= (1 << BOOLEAN_FIELD_OFFSET);
	}

	public void clearBooleanField() {
		data &= ((1 << BOOLEAN_FIELD_OFFSET) ^ ALL_BITS);
	}

	public int intField() {
		return (int) ( (data & INT_FIELD_BITS) >> INT_FIELD_OFFSET ) - 4;
	}

	public void setIntField(int val) {
		data &= (INT_FIELD_BITS ^ ALL_BITS);
		data |= ((val+4) << INT_FIELD_OFFSET);
	}

	public SampleEnum sampleEnum() {
		return SampleEnum.values()[(int) (data & ENUM_FIELD_BITS) >> ENUM_OFFSET];
	}

	public void setSampleEnum(SampleEnum e) {
		System.out.println("enum: " + e + " (" + e.ordinal() + ")");
		data &= (ENUM_FIELD_BITS ^ ALL_BITS);
		data |= (e.ordinal() << ENUM_OFFSET);
	}
}
