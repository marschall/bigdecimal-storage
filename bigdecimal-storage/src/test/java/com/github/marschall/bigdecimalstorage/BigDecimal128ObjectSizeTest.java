package com.github.marschall.bigdecimalstorage;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openjdk.jol.info.GraphLayout;


class BigDecimal128ObjectSizeTest {

  static Stream<BigDecimal> bigDecimals() {
    return Stream.of(
            new BigDecimal("1020.50"),
            new BigDecimal("0.25")
            );
  }

  @ParameterizedTest
  @MethodSource("bigDecimals")
  void size(BigDecimal bigDecimal) {
    BigDecimal128 bigDecimal128 = BigDecimal128.valueOf(bigDecimal);

    GraphLayout graphLayout123 = GraphLayout.parseInstance(bigDecimal128);

    GraphLayout graphLayout = GraphLayout.parseInstance(bigDecimal);
    assertThat(graphLayout123.totalSize()).isLessThan(graphLayout.totalSize());

    graphLayout = GraphLayout.parseInstance(bigDecimal.unscaledValue());
    assertThat(graphLayout123.totalSize()).isLessThan(graphLayout.totalSize());
  }

}
