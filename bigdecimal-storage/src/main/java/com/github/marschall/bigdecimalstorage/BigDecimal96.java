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

  private static final long serialVersionUID = 1L;

  private static final int MAX_SCALE = 6;

  //  private static final BigDecimal MAX_VALUE = 6;

  // 4 scale bits
  // 4 length bits
  // the first 56 bits
  private final long highBits;
  // the last 32 bits
  private final int lowBits;

  private BigDecimal96(long highBits, int lowBits) {
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

  public static BigDecimal96 valueOf(BigDecimal bigDecimal) {
    if (bigDecimal == null) {
      return null;
    }
    int scale = bigDecimal.scale();
    if (scale > MAX_SCALE) {
      throw new IllegalArgumentException("maximum scale allowed is: " + MAX_SCALE);
    }
    BigInteger bigInteger = unscaledValue(bigDecimal, scale);
    // we only support positive scales
    // otherwise we would have to introduce a sign bit
    int correctedScale = Math.max(scale, 0);
    byte[] twosComplement = bigInteger.toByteArray();
    long highBits = getHighByte(correctedScale, twosComplement.length);

    // the first 7 bytes go into the low bits of the first 64bit
    for (int i = 0; i < 7; i++) {
      if (i < twosComplement.length) {
        long unsinedValue = 0xFF & twosComplement[i];
        highBits |= unsinedValue << (48 - (i * 8));
      } else {
        break;
      }
    }

    // the last 4 bytes go into the low bits of the first 64bit
    int lowBits = 0;
    for (int i = 0; i < 4; i++) {
      if ((i + 7) < twosComplement.length) {
        int unsinedValue = 0xFF & twosComplement[i + 7];
        lowBits |= unsinedValue << (24 - (i * 8));
      } else {
        break;
      }
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
    int scale = this.getScale();
    int arrayLength = this.getArrayLength();
    byte[] twosComplement = new byte[arrayLength];

    // the 7 low bytes in the first 64 bits
    for (int i = 0; i < 7; i++) {
      if (i < twosComplement.length) {
        long unsinedValue = (this.highBits >>> (48 - (i * 8))) & 0xFF;
        twosComplement[i] = (byte) unsinedValue;
      }
    }

    // all 8 bytes in the last 32 bits
    for (int i = 0; i < 4; i++) {
      if ((i + 7) < twosComplement.length) {
        long unsinedValue = (this.lowBits >>> (56 - (i * 8))) & 0xFF;
        twosComplement[i + 7] = (byte) unsinedValue;
      }
    }
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
    return Long.hashCode(this.highBits) ^ Long.hashCode(this.lowBits);
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

    private long first;
    private int second;

    /**
     * Public default constructor for serialization.
     */
    public Ser96() {
      this.first = 0L;
      this.second = 0;
    }

    Ser96(long first, int second) {
      this.first = first;
      this.second = second;
    }

    private Object readResolve() {
      return new BigDecimal96(this.first, this.second);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
      out.writeLong(this.first);
      out.writeInt(this.second);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
      this.first = in.readLong();
      this.second = in.readInt();
    }

  }

}
