package com.github.marschall.bigdecimalstorage.benchmark;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import com.github.marschall.bigdecimalstorage.BigDecimal96;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class BigDecimal96Benchmark {


  private BigDecimal bigDecimal;
  private BigDecimal96 bigDecimal96;

  @Setup
  public void setup() {
    this.bigDecimal = new BigDecimal("1234567890123.123456");
    this.bigDecimal96 = BigDecimal96.valueOf(this.bigDecimal);
  }

  @Benchmark
  public BigDecimal toBigDecimal() {
    return this.bigDecimal96.toBigDecimal();
  }

  @Benchmark
  public BigDecimal96 valueOf() {
    return BigDecimal96.valueOf(this.bigDecimal);
  }


}
