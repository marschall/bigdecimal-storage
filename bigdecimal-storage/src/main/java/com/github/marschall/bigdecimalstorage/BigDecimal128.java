package com.github.marschall.bigdecimalstorage;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A 128 bit integer can hold a scale of up to 8 (4 bits) and a
 * 120 bit mantissa.
 */
public final class BigDecimal128 implements Serializable {

  // TODO ONE and ZERO constants

  private static final long serialVersionUID = 1L;

  private static final int MAX_SCALE = 6;

  private static final BigDecimal LONG_MAX_VALUE = BigDecimal.valueOf(Long.MAX_VALUE);

  private static final BigDecimal LONG_MIN_VALUE = BigDecimal.valueOf(Long.MIN_VALUE);

  //  private static final BigDecimal MAX_VALUE = 6;

  // 4 scale bits
  // 4 length bits
  // the first 56 bits
  private final long highBits;
  // the last 64 bits
  private final long lowBits;

  private BigDecimal128(long highBits, long lowBits) {
    this.highBits = highBits;
    this.lowBits = lowBits;
  }

  private int getScale() {
    return (int) ((this.highBits >>> 60) & 0b1111);
  }

  private int getArrayLength() {
    return (int) ((this.highBits >>> 56) & 0b1111);
  }

  private static long getHighByte(int scale, int arrayLength) {
    return (((long) Math.abs(scale)) << 60) | (((long) arrayLength) << 56);
  }

  public static BigDecimal128 valueOf(BigDecimal bigDecimal) {
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

  private static BigDecimal128 fromLongValue(BigDecimal bigDecimal, int scale) {
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
    long highBits = getHighByte(Math.max(0, scale), 8);
    long lowBits = unscaled.longValueExact();

    return new BigDecimal128(highBits, lowBits);
  }

  private static BigDecimal128 fromTwosComplement(BigDecimal bigDecimal, int scale) {
    BigInteger bigInteger = unscaledValue(bigDecimal, scale);
    // we only support positive scales
    // otherwise we would have to introduce a sign bit
    int correctedScale = Math.max(0, scale);
    byte[] twosComplement = bigInteger.toByteArray();

    long highBits = getHighByte(correctedScale, twosComplement.length);
    // the first 7 bytes go into the low bits of the first 64bit
    for (int i = 0; i < 7; i++) {
      if (((6 - i) + 8) < twosComplement.length) {
        long unsinedValue = 0xFF & twosComplement[(twosComplement.length - 8 - 7) + i];
        highBits |= unsinedValue << (8 * (6 - i));
      }
    }

    // the last 8 bytes go into the last 64bit
    long lowBits = 0;
    for (int i = 0; i < 8; i++) {
      long unsinedValue = 0xFF & twosComplement[i + (twosComplement.length - 8)];
      lowBits |= unsinedValue << (56 - (i * 8));
    }
    return new BigDecimal128(highBits, lowBits);
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

    // the 7 low bytes in the first 64 bits
    for (int i = 0; i < 7; i++) {
      if (((6 - i) + 8) < arrayLength) {
        long unsinedValue = (this.highBits >>> (48 - (i * 8))) & 0xFF;
        twosComplement[(twosComplement.length - 8 - 7) + i] = (byte) unsinedValue;
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
    if (!(obj instanceof BigDecimal128)) {
      return false;
    }
    BigDecimal128 other = (BigDecimal128) obj;
    return (this.highBits == other.highBits)
            && (this.lowBits == other.lowBits);
  }

  @Override
  public int hashCode() {
    return Long.hashCode(this.highBits) ^ Long.hashCode(this.lowBits);
  }

  @Override
  public String toString() {
    return this.toBigDecimal().toString();
  }

  private Object writeReplace() {
    return new Ser128(this.highBits, this.lowBits);
  }

  /**
   * Serialization proxy for {@link BigDecimal128}.
   */
  static final class Ser128 implements Externalizable {

    private long first;
    private long second;

    /**
     * Public default constructor for serialization.
     */
    public Ser128() {
      this.first = 0L;
      this.second = 0L;
    }

    Ser128(long first, long second) {
      this.first = first;
      this.second = second;
    }

    private Object readResolve() {
      return new BigDecimal128(this.first, this.second);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
      out.writeLong(this.first);
      out.writeLong(this.second);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
      this.first = in.readLong();
      this.second = in.readLong();
    }

  }

}
