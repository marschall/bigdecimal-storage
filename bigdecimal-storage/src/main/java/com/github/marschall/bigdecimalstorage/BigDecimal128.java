package com.github.marschall.bigdecimalstorage;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class BigDecimal128 {

  private static final int MAX_SCALE = 6;

//  private static final BigDecimal MAX_VALUE = 6;

  // 4 scale bits
  // 4 length bits

  private final long highBits;
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
    BigInteger bigInteger = unscaledValue(bigDecimal, scale);
    // we only support positive scales
    int correctedScale = Math.max(scale, 0);
    byte[] twosComplement = bigInteger.toByteArray();
    long highBits = getHighByte(correctedScale, twosComplement.length);

    // the first 7 bytes go into the low bits of the first 64bit
    for (int i = 0; i < 7; i++) {
      if (i < twosComplement.length) {
        long unsinedValue = (long) (0xFF & twosComplement[i]);
        highBits |= unsinedValue << (48 - (i * 8));
      }
    }

    // the last 8 bytes go into the low bits of the first 64bit
    long lowBits = 0;
    for (int i = 0; i < 8; i++) {
      if (i + 7 < twosComplement.length) {
        long unsinedValue = (long) (0xFF & twosComplement[i + 7]);
        lowBits |= unsinedValue << (56 - (i * 8));
      }
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

    // all 8 bytes in the last 64 bits
    for (int i = 0; i < 8; i++) {
      if (i + 7 < twosComplement.length) {
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
    if (!(obj instanceof BigDecimal128)) {
      return false;
    }
    BigDecimal128 other = (BigDecimal128) obj;
    return this.highBits == other.highBits
            && this.lowBits == other.lowBits;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(this.highBits) ^ Long.hashCode(this.lowBits);
  }

  @Override
  public String toString() {
    return this.toBigDecimal().toString();
  }

}
