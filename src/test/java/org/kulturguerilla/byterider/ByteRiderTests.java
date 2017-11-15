package org.kulturguerilla.byterider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.kulturguerilla.byterider.ByteRider.Size.*;
import static org.kulturguerilla.byterider.ByteRider.*;

import static java.util.Arrays.asList;

import java.util.List;

import org.junit.Test;

public class ByteRiderTests {

	// tests for helper methods
	@Test public void numBits() {
		assertThat(bitsRequired(2)).isEqualTo(1);
		assertThat(bitsRequired(3)).isEqualTo(2);
		assertThat(bitsRequired(4)).isEqualTo(2);
		assertThat(bitsRequired(5)).isEqualTo(3);
		assertThat(bitsRequired(1l<<30)).isEqualTo(30);
		assertThat(bitsRequired(1l<<31)).isEqualTo(31);
		assertThat(bitsRequired((1l<<31)+1)).isEqualTo(32);
	}

	// tests for Bool
	private BoolField b12 = createBoolField(12, "testField at bit 12");

	@Test public void outOfRangeIndexFails() {
		assertThatThrownBy(() -> new BoolImpl(13, "off", BYTE_SET))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test public void negativeBitFails() {
		assertThatThrownBy(() -> new BoolImpl(-1, "off", BYTE_SET))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test public void getTrue() {
		assertThat(b12.get(1<<12)).as("set bit should yield true").isTrue();
	}

	@Test public void getFalse() {
		assertThat(b12.get(1<<13)).as("unset bit should yield false").isFalse();
	}

	@Test public void set() {
		assertThat(b12.get(b12.set(0L))).as("setting + getting yields true").isTrue();
	}

	@Test public void setBoolVal() {
		assertThat(b12.get(b12.set(0L, true))).as("setting + getting yields true").isTrue();
		assertThat(b12.get(b12.set(0L, false))).as("setting + getting yields true").isFalse();
	}

	@Test public void clear() {
		assertThat(b12.get(b12.clear(b12.set(0L)))).as("clear yields false").isFalse();
	}

	// tests for some enum
	enum TestEnum {
		V1, V2, V3;
	}

	// tests for Int fields...
	@Test public void mask() {
		assertThat(createIntField(0, 0, 3, "test").mask()).isEqualTo(0b11);
		assertThat(createIntField(1, 0, 3, "test").mask()).isEqualTo(0b110);
		assertThat(createIntField(1, 0, 4, "test").mask()).isEqualTo(0b1110);
		assertThat(createIntField(1, 1, 4, "test").mask()).isEqualTo(0b110);
		assertThat(createIntField(0, 0, Integer.MAX_VALUE, "test").mask())
			.isEqualTo(Integer.MAX_VALUE);
		assertThat(createIntField(1, 0, Integer.MAX_VALUE, "test").mask())
			.isEqualTo(Integer.MAX_VALUE*2l);
	}

	IntField f = createIntField(3, 13, 26, "test");

	@Test public void get() {
		assertThat(f.get(0b111)).as("13 is minValue and encoded as 0").isEqualTo(13);
		assertThat(f.get(0b1000)).as("14 is encoded as 1").isEqualTo(14);
	}

	@Test public void setValueTooLarge() {
		assertThatThrownBy(() -> f.set(0L, 27)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test public void setValueTooSmall() {
		assertThatThrownBy(() -> f.set(0L, 12)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test public void setValueKeepsRestConstant() {
		assertThat(f.set(0L, 13)).isEqualTo(0L);
	}

	@Test public void setNewValuesClearsPrevious() {
		assertThat(f.get(f.set(f.set(0L, 14), 15))).isEqualTo(15);
	}

	@Test public void setGetSymmetric() {
		List<Long> testVals = asList(0L, 0b111L, 0b110000100L);
		for(long baseVal: testVals) {
			for (int i=13; i<=26; ++i) {
				assertThat(f.get(f.set(baseVal, i))).as("with baseVal " + baseVal
						+ ", get / seat of " + i + " should yield equal value")
					.isEqualTo(i);
			}
		}
	}

	@Test public void setNegativeValue() {
		IntField f = createIntField(3, -32767, 32768, "16bits");
		long baseVal = 0b110000000000000000111;
		assertThat(f.get(baseVal)).isEqualTo(-32767);
		assertThat(f.get(0xFFFFF)).isEqualTo(32768);
		assertThat(f.get(f.set(baseVal, 0))).isEqualTo(0);
		assertThat(f.get(f.set(0xFFFFF, 0))).isEqualTo(0);
	}

	BitField bits0To2 = createIntField(0, 0, 7, "bits012");
	BitField bits3To6 = createIntField(3, 0, 8, "bits3456");
	BitField bits8To9 = createIntField(8, 0, 3, "bits3456");
	BitField boolb6   = createBoolField(6, "bit6");
	BitField boolb7   = createBoolField(7, "bit7");

	// overlapping fields / inconsistent coverage
	@Test public void adjacentIntFieldsAreOk() {
		checkFields(bits0To2, bits3To6);
		checkFields(bits3To6, bits0To2);
	}

	@Test public void mixedIntBoolFieldsAreOk() {
		checkFields(boolb7, bits3To6, bits0To2, bits8To9);
	}

	@Test public void bit7Overlap() {
		assertThatThrownBy(() -> checkFields(boolb6, bits3To6))
			.isInstanceOf(IllegalArgumentException.class);
	}

	// first unused bit offset
	@Test public void emptyYieldsZero() {
		assertThat(lowestUnusedOffset(asList())).isEqualTo(0);
	}

	@Test public void singleBoolYieldsOne() {
		assertThat(lowestUnusedOffset(asList(createBoolField(0, "bit0")))).isEqualTo(1);
	}

	@Test public void single3IntYieldsTwo() {
		assertThat(lowestUnusedOffset(asList(createIntField(0, 0, 2, "bits12")))).isEqualTo(2);
	}

	@Test public void singleBoolAtPos3YieldsFour() {
		assertThat(lowestUnusedOffset(asList(createBoolField(3, "bit0")))).isEqualTo(4);
	}

	@Test public void singleBoolAtLastPosYields64() {
		assertThat(lowestUnusedOffset(asList(createBoolField(63, "bit0")))).isEqualTo(64);
	}
}


