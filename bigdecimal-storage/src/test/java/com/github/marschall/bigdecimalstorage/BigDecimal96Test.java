package com.github.marschall.bigdecimalstorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
            new BigDecimal("9999999999999999999"),
            new BigDecimal("-9999999999999999999"),
            new BigDecimal("1111111111111.111111"),
            new BigDecimal("-1111111111111.111111"),
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
            new BigDecimal(BigInteger.ONE, -2),
            new BigDecimal(BigInteger.ONE, -2).negate()
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
    assertNull(BigDecimal128.valueOf(null));
  }

  @Test
  void objectSize() {
    ClassLayout classLayout96 = ClassLayout.parseClass(BigDecimal96.class);

    ClassLayout classLayout128 = ClassLayout.parseClass(BigDecimal128.class);

    assertThat(classLayout96.instanceSize()).isLessThan(classLayout128.instanceSize());
  }

}
