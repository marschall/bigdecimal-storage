package com.github.marschall.bigdecimalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class BigDecimal64Test {

  static Stream<BigDecimal> bigDecimals() {
    return Stream.of(
            BigDecimal.ZERO,
            BigDecimal.ONE,
            BigDecimal.ONE.negate(),
            new BigDecimal("1.1"),
            new BigDecimal("-1.1"),
            new BigDecimal("123456789012.123456"),
            new BigDecimal("-123456789012.123456"),
            new BigDecimal("999999999999.999999"),
            new BigDecimal("-999999999999.999999"),
            new BigDecimal("999999999999.9"),
            new BigDecimal("-999999999999.9"),
            new BigDecimal("111111111111.111111"),
            new BigDecimal("-111111111111.111111"),
            new BigDecimal("111111111111.1"),
            new BigDecimal("-111111111111.1"),
            new BigDecimal("999999999999.000009"),
            new BigDecimal("-999999999999.000009"),
            new BigDecimal("100000000000.000001"),
            new BigDecimal("-100000000000.000001"),
            new BigDecimal(BigInteger.ONE, -2),
            new BigDecimal(BigInteger.ONE, -2).negate()
            );
}

  @ParameterizedTest
  @MethodSource("bigDecimals")
  void identity(BigDecimal bigDecimal) {
    Long bigDecimal64 = BigDecimal64.toLong(bigDecimal);
    assertEquals(0, bigDecimal.compareTo(BigDecimal64.toBigDecimal(bigDecimal64)));
  }

  @Test
  void testNulls() {
    assertNull(BigDecimal64.toBigDecimal(null));
    assertNull(BigDecimal64.toLong(null));
  }

}
