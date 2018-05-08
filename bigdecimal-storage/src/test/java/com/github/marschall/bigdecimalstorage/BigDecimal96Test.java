package com.github.marschall.bigdecimalstorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

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
import org.openjdk.jol.info.ClassLayout;

class BigDecimal96Test {

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
      (byte) 0xFF
  };

  private static final byte[] MAX_NEGATIVE = new byte[] {
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
    BigDecimal96 bigDecimal96 = BigDecimal96.valueOf(bigDecimal);
    assertThat(bigDecimal).isEqualByComparingTo(bigDecimal96.toBigDecimal());
  }

  @ParameterizedTest
  @MethodSource("bigDecimals")
  void testEquals(BigDecimal bigDecimal) {
    BigDecimal96 bigDecimal96 = BigDecimal96.valueOf(bigDecimal);
    assertEquals(bigDecimal96, bigDecimal96);
    assertNotEquals(bigDecimal96, bigDecimal);
    assertEquals(bigDecimal96, BigDecimal96.valueOf(bigDecimal));
  }

  @Test
  void testEqualsSameSacle() {
    BigDecimal one = BigDecimal.ONE;
    BigDecimal two = BigDecimal.valueOf(2L);
    assertNotEquals(BigDecimal96.valueOf(one), BigDecimal96.valueOf(two));
  }

  @Test
  void testEqualsDifferentSacle() {
    BigDecimal scaleZero = new BigDecimal("2");
    BigDecimal scaleTwo = new BigDecimal("2.00");
    assertNotEquals(BigDecimal96.valueOf(scaleZero), BigDecimal96.valueOf(scaleTwo));
  }

  @ParameterizedTest
  @MethodSource("bigDecimals")
  void testHashCode(BigDecimal bigDecimal) {
    BigDecimal96 bigDecimal96 = BigDecimal96.valueOf(bigDecimal);
    assertEquals(bigDecimal96.hashCode(), bigDecimal96.hashCode());
    assertEquals(bigDecimal96.hashCode(), BigDecimal96.valueOf(bigDecimal).hashCode());
  }

  @ParameterizedTest
  @MethodSource("bigDecimals")
  void testToString(BigDecimal bigDecimal) {
    BigDecimal96 bigDecimal96 = BigDecimal96.valueOf(bigDecimal);
    assertEquals(bigDecimal96.toString(), bigDecimal.toPlainString());
  }

  @ParameterizedTest
  @MethodSource("bigDecimals")
  void cloning(BigDecimal bigDecimal) throws IOException, ClassNotFoundException {
    byte[] serializedForm;
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(128);
         ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(BigDecimal96.valueOf(bigDecimal));
      oos.flush();

      serializedForm = bos.toByteArray();
    }
    BigDecimal readBack;
    try (ByteArrayInputStream bis = new ByteArrayInputStream(serializedForm);
         ObjectInputStream ois = new ObjectInputStream(bis)) {
      readBack = ((BigDecimal96) ois.readObject()).toBigDecimal();
    }
    assertEquals(0, bigDecimal.compareTo(readBack));
  }

  @Test
  void nullConstructor() {
    assertNull(BigDecimal96.valueOf(null));
  }

  @ParameterizedTest
  @MethodSource("invalidBigDecimals")
  void illegalArguments(BigDecimal invalid) {
    assertThrows(IllegalArgumentException.class, () -> BigDecimal96.valueOf(invalid));
  }

  @ParameterizedTest
  @MethodSource("bigDecimals")
  void negate(BigDecimal bigDecimal) {
    assumeFalse(bigDecimal.unscaledValue().equals(BigDecimal96.MIN_VALUE.toBigInteger()));
    BigDecimal96 bigDecimal96 = BigDecimal96.valueOf(bigDecimal);
    BigDecimal96 bigDecimal96Negated = bigDecimal96.negate();

    assertEquals(bigDecimal96, bigDecimal96Negated.negate());
    assertEquals(0, bigDecimal96Negated.toBigDecimal().compareTo(bigDecimal.negate()));
  }

  @Test
  void negateExtremeValueOverflows() {
    BigDecimal96 bigDecimal96 = BigDecimal96.valueOf(BigDecimal96.MIN_VALUE);
    assertThrows(IllegalArgumentException.class, bigDecimal96::negate);
  }

  @Test
  void negateZero() {
    BigDecimal96 zero = BigDecimal96.valueOf(BigDecimal.ZERO);
    assertSame(zero, zero.negate());
  }

  @Test
  void addSubtractDifferentScale() {
    BigDecimal96 a = BigDecimal96.valueOf(new BigDecimal("0.1"));
    BigDecimal96 b = BigDecimal96.valueOf(new BigDecimal("0.01"));
    BigDecimal96 sum = BigDecimal96.valueOf(new BigDecimal("0.11"));

    assertEquals(sum, a.add(b));
    assertEquals(sum, b.add(a));

    assertEquals(b, sum.subtract(a));
    assertEquals(BigDecimal96.valueOf(new BigDecimal("0.10")), sum.subtract(b));
  }

  @Test
  void addSubtractNegative() {
    BigDecimal96 a = BigDecimal96.valueOf(new BigDecimal("1"));
    BigDecimal96 b = BigDecimal96.valueOf(new BigDecimal("-3"));
    BigDecimal96 sum = BigDecimal96.valueOf(new BigDecimal("-2"));

    assertEquals(sum, a.add(b));
    assertEquals(sum, b.add(a));

    assertEquals(b, sum.subtract(a));
    assertEquals(a, sum.subtract(b));
  }

  @Test
  void addSubtractOverflows() {
    BigDecimal96 a = BigDecimal96.valueOf(new BigDecimal("500000000000.000001"));
    BigDecimal96 sum = BigDecimal96.valueOf(new BigDecimal("1000000000000.000002"));

    assertEquals(sum, a.add(a));

    assertEquals(sum, a.subtract(a.negate()));
  }

  @Test
  void addSubtractPowOverflows() {
    BigDecimal96 a = BigDecimal96.valueOf(new BigDecimal("100000000000000000"));
    BigDecimal96 b = BigDecimal96.valueOf(new BigDecimal("0.000001"));
    BigDecimal96 sum = BigDecimal96.valueOf(new BigDecimal("100000000000000000.000001"));

    assertEquals(sum, a.add(b));
    assertEquals(sum, b.add(a));

    assertEquals(sum, a.subtract(b.negate()));
    assertEquals(sum, b.subtract(a.negate()));
  }

  @Test
  void addOneLarger() {
    BigDecimal96 a = BigDecimal96.valueOf(new BigDecimal("9999999999999999999"));
    BigDecimal96 b = BigDecimal96.valueOf(new BigDecimal("1"));
    BigDecimal96 sum = BigDecimal96.valueOf(new BigDecimal("10000000000000000000"));

    assertEquals(sum, a.add(b));
    assertEquals(sum, b.add(a));

    assertEquals(sum, a.subtract(b.negate()));
    assertEquals(sum, b.subtract(a.negate()));
  }

  @Test
  void addBothLarger() {
    BigDecimal96 a = BigDecimal96.valueOf(new BigDecimal("5000000000000000000"));
    BigDecimal96 sum = BigDecimal96.valueOf(new BigDecimal("10000000000000000000"));

    assertEquals(sum, a.add(a));

    assertEquals(sum, a.subtract(a.negate()));
  }

  @Test
  void objectSize() {
    ClassLayout classLayout96 = ClassLayout.parseClass(BigDecimal96.class);

    ClassLayout classLayout128 = ClassLayout.parseClass(BigDecimal128.class);

    assertThat(classLayout96.instanceSize()).isLessThan(classLayout128.instanceSize());
  }

}
