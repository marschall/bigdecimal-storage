package com.github.marschall.bigdecimalstorage;

import java.math.BigDecimal;

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

  public static BigDecimal128 valueOf(BigDecimal bigDecimal) {
    if (bigDecimal == null) {
      return null;
    }
    if (bigDecimal.scale() > MAX_SCALE) {
      throw new IllegalArgumentException("maximum scale allowed is: " + MAX_SCALE);
    }
    return new BigDecimal128(0L, 0L);
  }

  public BigDecimal toBigDecimal() {
    return BigDecimal.ZERO;
  }

}
