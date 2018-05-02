package com.github.marschall.bigdecimalstorage;

import java.math.BigDecimal;

/**
 * A 64bit decimal value with a fixed scale of 6.
 */
public final class BigDecimal64 {

  public static Long toLong(BigDecimal bigDecimal) {
    if (bigDecimal == null) {
      return null;
    }
    return 0L;
  }

  public static BigDecimal toBigDecimal(Long l) {
    if (l == null) {
      return null;
    }
    return BigDecimal.ZERO;
  }

}
