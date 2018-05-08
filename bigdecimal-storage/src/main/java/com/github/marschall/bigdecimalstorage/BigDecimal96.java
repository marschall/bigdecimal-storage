package com.github.marschall.bigdecimalstorage;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A 96 bit integer can hold a scale of up to 8 (4 bits) and a
 * 84 bit mantissa.
 */
public final class BigDecimal96 implements Serializable {

  // TODO Comparable

  // TODO ONE and ZERO constants

  private static final int COMPACT_ARRAY_LENGTH = 8;

  private static final long serialVersionUID = 2L;

  /**
   * The largest supported scale.
   */
  public static final int MAX_SCALE = 6;

  /**
   * The largest supported value.
   */
  public static final BigDecimal MAX_VALUE = new BigDecimal(new BigInteger(new byte[] {
      (byte) 0x7F,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF
  }));

  /**
   * The smallest supported value.
   */
  public static final BigDecimal MIN_VALUE = new BigDecimal(new BigInteger(new byte[] {
      (byte) 0x80,
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x00
  }));

  // 4 scale bits
  // 4 length bits
  // the first 24 bits
  private final int highBits;
  // the last 64 bits
  private final long lowBits;

  private BigDecimal96(int highBits, long lowBits) {
    this.highBits = highBits;
    this.lowBits = lowBits;
  }

  private int getScale() {
    return (this.highBits >>> 60) & 0b1111;
  }

  private int getArrayLength() {
    return (this.highBits >>> 56) & 0b1111;
  }

  private boolean isCompact() {
    return this.getArrayLength() == COMPACT_ARRAY_LENGTH;
  }

  private static int getHighByte(int scale, int arrayLength) {
    return (Math.abs(scale) << 28) | (arrayLength << 24);
  }

  public static BigDecimal96 valueOf(BigDecimal bigDecimal) {
    if (bigDecimal == null) {
      return null;
    }

    int scale = bigDecimal.scale();
    if (scale > MAX_SCALE) {
      throw new IllegalArgumentException("maximum scale allowed is: " + MAX_SCALE);
    }
    if (fitsInto64bit(bigDecimal, scale)) {
      // we assume that in this case the BigDecimal is compact
      return fromLongValue(bigDecimal, scale);
    } else {
      if (bigDecimal.compareTo(MIN_VALUE) < 0) {
        throw new IllegalArgumentException("value too small");
      }
      if (bigDecimal.compareTo(MAX_VALUE) > 0) {
        throw new IllegalArgumentException("value too large");
      }
      return fromTwosComplement(bigDecimal, scale);
    }
  }

  private static boolean fitsInto64bit(BigDecimal bigDecimal, int scale) {
    int precision = bigDecimal.precision();
    if (scale >= 0) {
      return precision <= 18;
    } else {
      return (precision - scale) <= 18;
    }
  }

  private static BigDecimal96 fromLongValue(BigDecimal bigDecimal, int scale) {
    BigDecimal unscaled;
    if (scale > 0) {
      // allocates a new BigDecimal which on HotSpot is the same size as a BigInteger
      // if the BigDecimal is not compact (already has a BigInteger) then
      // bigDecimal.unscaledValue().toByteArray()
      // actually allocates less
      // but if the BigDecimal is compact (we assume it is) when this
      // saves a BigInteger and byte[] allocation
      unscaled = bigDecimal.movePointRight(scale);
    } else {
      // for positive and 0 scales there are no decimal places
      unscaled = bigDecimal;
    }
    // we do not support negative scales
    int highBits = getHighByte(Math.max(0, scale), COMPACT_ARRAY_LENGTH);
    long lowBits = unscaled.longValueExact();

    return new BigDecimal96(highBits, lowBits);
  }

  private static BigDecimal96 fromTwosComplement(BigDecimal bigDecimal, int scale) {
    BigInteger bigInteger = unscaledValue(bigDecimal, scale);
    // we only support positive scales
    // otherwise we would have to introduce a sign bit
    int correctedScale = Math.max(0, scale);
    byte[] twosComplement = bigInteger.toByteArray();
    if (twosComplement.length > 11) {
      throw new IllegalArgumentException("value too large");
    }

    int highBits = getHighByte(correctedScale, twosComplement.length);
    // the first 3 bytes go into the low bits of the first 32bit
    for (int i = 0; i < 3; i++) {
      if (((2 - i) + 8) < twosComplement.length) {
        int unsinedValue = 0xFF & twosComplement[(twosComplement.length - 8 - 3) + i];
        highBits |= unsinedValue << (8 * (2 - i));
      }
    }

    // the last 8 bytes go into the last 64bit
    long lowBits = 0;
    for (int i = 0; i < 8; i++) {
      long unsinedValue = 0xFF & twosComplement[i + (twosComplement.length - 8)];
      lowBits |= unsinedValue << (56 - (i * 8));
    }
    return new BigDecimal96(highBits, lowBits);
  }

  private static final BigInteger unscaledValue(BigDecimal bigDecimal, int scale) {
    if (scale >= 0) {
      return bigDecimal.unscaledValue();
    } else {
      return bigDecimal.setScale(0).unscaledValue();
    }
  }

  public BigDecimal96 negate() {
    // we could also name the method negated() but BigDecimal calls the method negate()
    if (this.isCompact()) {
      if (this.lowBits == 0L) {
        return this;
      }
      if (this.lowBits == Long.MIN_VALUE) {
        return BigDecimal96.valueOf(BigDecimal.valueOf(Long.MIN_VALUE).negate());
      }
      return new BigDecimal96(this.highBits, -this.lowBits);
    }
    return BigDecimal96.valueOf(this.toBigDecimal().negate());
  }

  public BigDecimal96 add(BigDecimal96 augend) {
    if (!this.isCompact() || !augend.isCompact()) {
      // we assume this happens only very rarely if at all
      return this.addUsingBigDecimalMath(augend);
    }
    int ourScale = this.getScale();
    int theirScale = augend.getScale();
    int operationScale = Math.max(ourScale, theirScale);
    long a;
    long b;
    try {
      if (ourScale < operationScale) {
        a = pow10(this.lowBits, operationScale - ourScale);
      } else {
        a = this.lowBits;
      }
      if (theirScale < operationScale) {
        b = pow10(augend.lowBits, operationScale - theirScale);
      } else {
        b = augend.lowBits;
      }
      long result = Math.addExact(a, b);
      return new BigDecimal96(getHighByte(operationScale, COMPACT_ARRAY_LENGTH), result);
    } catch (ArithmeticException e) {
      // overflow we assume this happens only very rarely if at all
      return this.addUsingBigDecimalMath(augend);
    }
  }

  private static long pow10(long l, int exponent) {
    long result = l;
    for (int i = 0; i < exponent; i++) {
      result = Math.multiplyExact(result, 10L);
    }
    return result;
  }

  private BigDecimal96 addUsingBigDecimalMath(BigDecimal96 augend) {
    return BigDecimal96.valueOf(this.toBigDecimal().add(augend.toBigDecimal()));
  }

  public BigDecimal96 subtract(BigDecimal96 subtrahend) {
    if (!this.isCompact() || !subtrahend.isCompact()) {
      // we assume this happens only very rarely if at all
      return this.subtractUsingBigDecimalMath(subtrahend);
    }
    int ourScale = this.getScale();
    int theirScale = subtrahend.getScale();
    int operationScale = Math.max(ourScale, theirScale);
    long a;
    long b;
    try {
      if (ourScale < operationScale) {
        a = pow10(this.lowBits, operationScale - ourScale);
      } else {
        a = this.lowBits;
      }
      if (theirScale < operationScale) {
        b = pow10(subtrahend.lowBits, operationScale - theirScale);
      } else {
        b = subtrahend.lowBits;
      }
      long result = Math.subtractExact(a, b);
      return new BigDecimal96(getHighByte(operationScale, COMPACT_ARRAY_LENGTH), result);
    } catch (ArithmeticException e) {
      // overflow we assume this happens only very rarely if at all
      return this.subtractUsingBigDecimalMath(subtrahend);
    }
  }

  private BigDecimal96 subtractUsingBigDecimalMath(BigDecimal96 subtrahend) {
    return BigDecimal96.valueOf(this.toBigDecimal().subtract(subtrahend.toBigDecimal()));
  }

  public BigDecimal96 withScale(int newScale) {
    if ((newScale < 0) || (newScale > MAX_SCALE)) {
      throw new IllegalArgumentException("invalid scale");
    }
    int currentScale = this.getScale();
    if (currentScale == newScale) {
      return this;
    }
  }

  public BigDecimal toBigDecimal() {
    if (this.isCompact()) {
      return this.toBigDecimalFromLong();
    } else {
      return this.toBigDecimalFromTwosComplement();
    }
  }

  private BigDecimal toBigDecimalFromLong() {
    int scale = this.getScale();
    return BigDecimal.valueOf(this.lowBits, scale);
  }

  private BigDecimal toBigDecimalFromTwosComplement() {
    int arrayLength = this.getArrayLength();
    byte[] twosComplement = new byte[arrayLength];

    // the 3 low bytes in the first 32 bits
    for (int i = 0; i < 3; i++) {
      if (((2 - i) + 8) < arrayLength) {
        int unsinedValue = (this.highBits >>> (16 - (i * 8))) & 0xFF;
        twosComplement[(twosComplement.length - 8 - 3) + i] = (byte) unsinedValue;
      }
    }

    // all 8 bytes in the last 64 bits
    for (int i = 0; i < 8; i++) {
      long unsinedValue = (this.lowBits >>> (56 - (i * 8))) & 0xFF;
      twosComplement[i + (arrayLength - 8)] = (byte) unsinedValue;
    }
    int scale = this.getScale();
    return new BigDecimal(new BigInteger(twosComplement), scale);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof BigDecimal96)) {
      return false;
    }
    BigDecimal96 other = (BigDecimal96) obj;
    return (this.highBits == other.highBits)
            && (this.lowBits == other.lowBits);
  }

  @Override
  public int hashCode() {
    return this.highBits ^ Long.hashCode(this.lowBits);
  }

  @Override
  public String toString() {
    return this.toBigDecimal().toString();
  }

  private Object writeReplace() {
    return new Ser96(this.highBits, this.lowBits);
  }

  /**
   * Serialization proxy for {@link BigDecimal96}.
   */
  static final class Ser96 implements Externalizable {

    private int first;
    private long second;

    /**
     * Public default constructor for serialization.
     */
    public Ser96() {
      this.first = 0;
      this.second = 0L;
    }

    Ser96(int first, long second) {
      this.first = first;
      this.second = second;
    }

    private Object readResolve() {
      return new BigDecimal96(this.first, this.second);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
      out.writeInt(this.first);
      out.writeLong(this.second);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
      this.first = in.readInt();
      this.second = in.readLong();
    }

  }

}
