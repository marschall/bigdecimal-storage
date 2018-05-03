package com.github.marschall.bigdecimalstorage;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openjdk.jol.info.GraphLayout;


class BigDecimalObjectSizeTest {

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
    BigDecimal96 bigDecimal96 = BigDecimal96.valueOf(bigDecimal);

    GraphLayout graphLayout128 = GraphLayout.parseInstance(bigDecimal128);
    GraphLayout graphLayout96 = GraphLayout.parseInstance(bigDecimal96);

    GraphLayout graphLayout = GraphLayout.parseInstance(bigDecimal);
    assertThat(graphLayout128.totalSize()).isLessThan(graphLayout.totalSize());
    assertThat(graphLayout96.totalSize()).isLessThan(graphLayout128.totalSize());

    graphLayout = GraphLayout.parseInstance(bigDecimal.unscaledValue());
    assertThat(graphLayout128.totalSize()).isLessThan(graphLayout.totalSize());
  }

}
