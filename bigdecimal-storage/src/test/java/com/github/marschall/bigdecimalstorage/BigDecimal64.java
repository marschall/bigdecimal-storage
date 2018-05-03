package com.github.marschall.bigdecimalstorage;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A 64bit decimal value with a fixed scale of 6.
 */
public final class BigDecimal64 {

  private static final int SCALE = 6;

  private static final BigDecimal MAX_VALUE = new BigDecimal("999999999999.999999");

  private static final BigDecimal MIN_VALUE = new BigDecimal("-999999999999.999999");

  public static Long toLong(BigDecimal bigDecimal) {
    if (bigDecimal == null) {
      return null;
    }
    if (bigDecimal.compareTo(MAX_VALUE) > 0) {
      throw new IllegalArgumentException("value too large");
    }
    if (bigDecimal.compareTo(MIN_VALUE) < 0) {
      throw new IllegalArgumentException("value too small");
    }
    int scale = bigDecimal.scale();
    if (scale > SCALE) {
      throw new IllegalArgumentException("scale too large");
    }
    byte[] twosComplement = bigDecimal.movePointRight(SCALE).toBigInteger().toByteArray();
    long value = 0L;
    for (int i = 0; i < twosComplement.length; i++) {
      int shift = 56 - (i * 8) - (64 - (twosComplement.length * 8));
      value |= ((long) (twosComplement[i] & 0xFF)) << shift;
    }
    return value;
  }

  public static BigDecimal toBigDecimal(Long l) {
    if (l == null) {
      return null;
    }
    long value = l;
    if (value == 0L) {
      return BigDecimal.ZERO;
    }
    byte[] twosComplement = new byte[getArraySize(value)];
    for (int i = 0; i < twosComplement.length; i++) {
      int shift = 56 - (i * 8) - (64 - (twosComplement.length * 8));
      twosComplement[i] = (byte) ((value >>> shift) & 0xFF);
    }

    return new BigDecimal(new BigInteger(twosComplement), SCALE);
  }

  private static int getArraySize(long value) {
    int numberOfLeadingZeros = Long.numberOfLeadingZeros(value);
    int bitsSet = 64 - numberOfLeadingZeros;
    int remainder = bitsSet % 8;
    if (remainder == 0) {
      return bitsSet / 8;
    }
    return ((bitsSet + 8) - remainder) / 8;
  }

}
