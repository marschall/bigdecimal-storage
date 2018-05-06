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

  // TODO ONE and ZERO constants

  private static final long serialVersionUID = 2L;

  private static final int MAX_SCALE = 6;

  private static final BigDecimal LONG_MAX_VALUE = BigDecimal.valueOf(Long.MAX_VALUE);

  private static final BigDecimal LONG_MIN_VALUE = BigDecimal.valueOf(Long.MIN_VALUE);

  //  private static final BigDecimal MAX_VALUE = 6;

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
    if (bigDecimal.precision() <= 18) {
      // we assume that in this case the BigDecimal is compact
      return fromLongValue(bigDecimal, scale);
    } else {
      return fromTwosComplement(bigDecimal, scale);
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
    int highBits = getHighByte(Math.max(0, scale), 8);
    long lowBits = unscaled.longValueExact();

    return new BigDecimal96(highBits, lowBits);
  }

  private static BigDecimal96 fromTwosComplement(BigDecimal bigDecimal, int scale) {
    BigInteger bigInteger = unscaledValue(bigDecimal, scale);
    // we only support positive scales
    // otherwise we would have to introduce a sign bit
    int correctedScale = Math.max(0, scale);
    byte[] twosComplement = bigInteger.toByteArray();

    int highBits = getHighByte(correctedScale, twosComplement.length);
    // the first 3 bytes go into the low bits of the first 32bit
    for (int i = 0; i < 3; i++) {
      if ((2 - i + 8) < twosComplement.length) {
        int unsinedValue = 0xFF & twosComplement[twosComplement.length - 8 - 3 + i];
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

  public BigDecimal toBigDecimal() {
    int arrayLength = this.getArrayLength();
    if (arrayLength == 8) {
      return this.toBigDecimalFromLong();
    } else {
      return this.toBigDecimalFromTwosComplement(arrayLength);
    }
  }

  private BigDecimal toBigDecimalFromLong() {
    int scale = this.getScale();
    return BigDecimal.valueOf(this.lowBits, scale);
  }

  private BigDecimal toBigDecimalFromTwosComplement(int arrayLength) {
    byte[] twosComplement = new byte[arrayLength];

    // the 3 low bytes in the first 32 bits
    for (int i = 0; i < 3; i++) {
      if ((2 - i + 8) < arrayLength) {
        int unsinedValue = (this.highBits >>> (16 - (i * 8))) & 0xFF;
        twosComplement[twosComplement.length - 8 - 3 + i] = (byte) unsinedValue;
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
