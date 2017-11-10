package org.kulturguerilla.byterider.examples;

import static org.assertj.core.api.Assertions.assertThat;

import org.kulturguerilla.byterider.examples.ExampleInterface.SampleEnum;
import org.junit.Test;

/**
 * Tests that exercise the two implementations of {@link ExampleInterface}
 * and make sure they both behave properly for all valid configurations.
 */
public class ExampleTests {

	@Test public void exerciseBitMasked() {
		exerciseAllAttributes(new BitMaskBasedExample(true, 0, SampleEnum.ZERO));
	}

	@Test public void exerciseByteRider() {
		exerciseAllAttributes(new ByteRiderExample(true, 0, SampleEnum.ZERO));
	}

	public void exerciseAllAttributes(ExampleInterface instance) {
		exerciseBoolField(instance);
		exerciseIntField(instance);
		exerciseEnumField(instance);
	}

	public void exerciseBoolField(ExampleInterface instance) {
		instance.clearBooleanField();
		assertThat(instance.booleanField()).isFalse();
		instance.setBooleanField();
		assertThat(instance.booleanField()).isTrue();
	}

	public void exerciseIntField(ExampleInterface instance) {
		for(int i = -4; i <= 4; ++i) {
			instance.setIntField(i);
			assertThat(instance.intField()).isEqualTo(i);
		}
	}

	public void exerciseEnumField(ExampleInterface instance) {
		for(int i = 0; i < SampleEnum.values().length; ++i) {
			instance.setSampleEnum(SampleEnum.values()[i]);
			assertThat(instance.sampleEnum()).isEqualTo(SampleEnum.values()[i]);
		}
	}
}
