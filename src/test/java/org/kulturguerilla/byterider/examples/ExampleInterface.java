package org.kulturguerilla.byterider.examples;

/**
 * interface that is implemented by both {@link BitMaskBasedExample} and
 * {@link ByteRiderExample}.
 */
interface ExampleInterface {

	/** a flag. */
	boolean booleanField();
	void setBooleanField();
	void clearBooleanField();

	/** an int, allowed range [-4, +4], nine values (4 bits). */
	int intField();
	void setIntField(int value);

	/** sample enum, just for demonstration. */
	public static enum SampleEnum {
		ZERO, ONE, TWO, THREE
	}

	/** no setter here, it's nothing new anyway. */
	SampleEnum sampleEnum();
	void setSampleEnum(SampleEnum e);
}
