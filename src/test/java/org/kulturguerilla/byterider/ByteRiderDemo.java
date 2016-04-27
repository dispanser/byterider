package org.kulturguerilla.byterider;

import static org.kulturguerilla.byterider.ByteRider.*;

/**
 * simple demo showing how the {@link ByteRider} is used.
 *
 * Notes:
 * - a ByteRider only ever handles a single long value
 * - it's a good habit to call br.checkFields() in a static block
 *   to make sure no inconsistency exists in the bit masks (this should be
 *   OK if you only use the ByteRider addBool, addInt, addEnum methods, but
 *   if you use the static methods and provide your own offset, it's not).
 * - the EnumField could wrap anything that has a meaningful mapping between
 *   ints and an (enumerable) number of objects.
 * - IntMappedObjField uses IntField under the hood, and just applies the fromObject and
 *   toObject methods as convenience.
 */
public class ByteRiderDemo {

	static ByteRider br = new ByteRider();

	private final static BoolField drivable = br.addBool("isDrivable");
	private final static IntField net2Class = br.addInt(6, "net2Class");
	private final static BoolField freeway = br.addBool("freeway");
	private final static IntField xDecaMicro = br.addInt(-32767, 32768, "xStart");
	private final static IntField yDecaMicro = br.addInt(-16383, 16384, "yStart");
	private final static IntMappedObjField<MyEnum> eField = br.addObj(3, MyEnum::toInt, MyEnum::fromInt, "my enum");

	public enum MyEnum {
		NONE(0), V1(1), V2(2);

		final int ord;

		MyEnum(int o) { this.ord = o; }

		public static MyEnum fromInt(int x) {
			switch(x) {
				case 0: return NONE;
				case 1: return V1;
				case 2: return V2;
				default: throw new IllegalArgumentException("invalid integer constant: " + x);
			}
		}

		public static int toInt(MyEnum me) {
			return me.ord;
		}
	}

	// package-private only to access from ByteRiderDemoTest. don't do this at home!
	long data = 0;

	static {
		br.checkFields();
	}

	public boolean isDrivable() {
		return drivable.get(data);
	}

	public int getNet2Class() {
		return net2Class.get(data);
	}

	public boolean isFreeway() {
		return freeway.get(data);
	}

	public int getX() {
		return xDecaMicro.get(data);
	}

	public int getY() {
		return yDecaMicro.get(data);
	}

	public MyEnum getEnum() {
		return eField.get(data);
	}

	public void setDrivable(boolean isDrivable) {
		data = isDrivable ? drivable.set(data) : drivable.clear(data);
	}

	public void setNet2Class(int n2c) {
		data = net2Class.set(data, n2c);
	}

	public void setFreeway(boolean isFreeway) {
		data = isFreeway ? freeway.set(data) : freeway.clear(data);
	}

	public void setCoords(int x, int y) {
		data = yDecaMicro.set(xDecaMicro.set(data, x), y);
	}

	public void setEnum(MyEnum e) {
		data = eField.set(data, e);
	}

}
