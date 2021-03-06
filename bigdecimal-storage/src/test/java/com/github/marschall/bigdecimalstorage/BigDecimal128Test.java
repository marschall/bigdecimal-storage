package com.github.marschall.bigdecimalstorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class BigDecimal128Test {

  private static final byte[] MAX_POSITIVE = new byte[] {
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
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF
  };

  private static final byte[] MAX_NEGATIVE = new byte[] {
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
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
      };

  private static final byte[] TOO_LARGE_POSITIVE = new byte[] {
      (byte) 0x01,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
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
  };

  private static final byte[] TOO_LARGE_NEGATIVE = new byte[] {
      (byte) 0xFF,
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
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF
  };

  static Stream<BigDecimal> bigDecimals() {
    return Stream.of(
            BigDecimal.ZERO,
            BigDecimal.ONE,
            BigDecimal.ONE.negate(),
            new BigDecimal("1.1"),
            new BigDecimal("-1.1"),
            new BigDecimal("1234567890123.123456"),
            new BigDecimal("-1234567890123.123456"),
            new BigDecimal("1234567890123123456"),
            new BigDecimal("-1234567890123123456"),
            new BigDecimal("9999999999999.999999"),
            new BigDecimal("-9999999999999.999999"),
            new BigDecimal("99999999999999.99999"),
            new BigDecimal("-99999999999999.99999"),
            new BigDecimal("9999999999999999999"),
            new BigDecimal("-9999999999999999999"),
            new BigDecimal("1111111111111.111111"),
            new BigDecimal("-1111111111111.111111"),
            new BigDecimal("11111111111111.11111"),
            new BigDecimal("-11111111111111.11111"),
            new BigDecimal("1111111111111111111"),
            new BigDecimal("-1111111111111111111"),
            new BigDecimal("9999999999999.000009"),
            new BigDecimal("-9999999999999.000009"),
            new BigDecimal("9999999999999000009"),
            new BigDecimal("-9999999999999000009"),
            new BigDecimal("1000000000000.000001"),
            new BigDecimal("-1000000000000.000001"),
            new BigDecimal("1000000000000000001"),
            new BigDecimal("-1000000000000000001"),
            new BigDecimal(new BigInteger(MAX_POSITIVE)),
            new BigDecimal(new BigInteger(MAX_POSITIVE), 2),
            new BigDecimal(new BigInteger(MAX_POSITIVE), 6),
            new BigDecimal(new BigInteger(MAX_NEGATIVE)),
            new BigDecimal(new BigInteger(MAX_NEGATIVE), 2),
            new BigDecimal(new BigInteger(MAX_NEGATIVE), 6),
            new BigDecimal(Integer.MAX_VALUE),
            new BigDecimal(Integer.MIN_VALUE),
            new BigDecimal(Long.MAX_VALUE),
            new BigDecimal(Long.MIN_VALUE),
            new BigDecimal(BigInteger.ONE, -2),
            new BigDecimal(BigInteger.ONE, -2).negate(),
            new BigDecimal(BigInteger.ONE, -19),
            new BigDecimal(BigInteger.ONE, -19).negate()
            );
  }

  static Stream<BigDecimal> invalidBigDecimals() {
    return Stream.of(
            new BigDecimal("0.1234567"),
            new BigDecimal("-0.1234567"),
            new BigDecimal(new BigInteger(TOO_LARGE_POSITIVE)),
            new BigDecimal(new BigInteger(TOO_LARGE_POSITIVE), 2),
            new BigDecimal(new BigInteger(TOO_LARGE_POSITIVE), 6),
            new BigDecimal(new BigInteger(TOO_LARGE_NEGATIVE)),
            new BigDecimal(new BigInteger(TOO_LARGE_NEGATIVE), 2),
            new BigDecimal(new BigInteger(TOO_LARGE_NEGATIVE), 6)
            );
  }

  @ParameterizedTest
  @MethodSource("bigDecimals")
  void identity(BigDecimal bigDecimal) {
    BigDecimal128 bigDecimal128 = BigDecimal128.valueOf(bigDecimal);
    assertThat(bigDecimal).isEqualByComparingTo(bigDecimal128.toBigDecimal());
  }

  @Test
  void testEqualsSameSacle() {
    BigDecimal one = BigDecimal.ONE;
    BigDecimal two = BigDecimal.valueOf(2L);
    assertNotEquals(BigDecimal128.valueOf(one), BigDecimal128.valueOf(two));
  }

  @Test
  void testEqualsDifferentSacle() {
    BigDecimal scaleZero = new BigDecimal("2");
    BigDecimal scaleTwo = new BigDecimal("2.00");
    assertNotEquals(BigDecimal128.valueOf(scaleZero), BigDecimal128.valueOf(scaleTwo));
  }

  @ParameterizedTest
  @MethodSource("bigDecimals")
  void testHashCode(BigDecimal bigDecimal) {
    BigDecimal128 bigDecimal128 = BigDecimal128.valueOf(bigDecimal);
    assertEquals(bigDecimal128.hashCode(), bigDecimal128.hashCode());
    assertEquals(bigDecimal128.hashCode(), BigDecimal128.valueOf(bigDecimal).hashCode());
  }

  @ParameterizedTest
  @MethodSource("bigDecimals")
  void testToString(BigDecimal bigDecimal) {
    BigDecimal128 bigDecimal128 = BigDecimal128.valueOf(bigDecimal);
    assertEquals(bigDecimal128.toString(), bigDecimal.toPlainString());
  }

  @ParameterizedTest
  @MethodSource("bigDecimals")
  void cloning(BigDecimal bigDecimal) throws IOException, ClassNotFoundException {
    byte[] serializedForm;
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(128);
         ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(BigDecimal128.valueOf(bigDecimal));
      oos.flush();

      serializedForm = bos.toByteArray();
    }
    BigDecimal readBack;
    try (ByteArrayInputStream bis = new ByteArrayInputStream(serializedForm);
         ObjectInputStream ois = new ObjectInputStream(bis)) {
      readBack = ((BigDecimal128) ois.readObject()).toBigDecimal();
    }
    assertEquals(0, bigDecimal.compareTo(readBack));
  }

  @Test
  void nullConstructor() {
    assertNull(BigDecimal128.valueOf(null));
  }

  @ParameterizedTest
  @MethodSource("invalidBigDecimals")
  void illegalArguments(BigDecimal invalid) {
    assertThrows(IllegalArgumentException.class, () -> BigDecimal128.valueOf(invalid));
  }

}
