package com.github.marschall.bigdecimalstorage.benchmark;

import static org.openjdk.jmh.results.format.ResultFormatType.TEXT;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public final class Main {

  public static void main(String[] args) throws RunnerException {
    Options options = new OptionsBuilder()
            .include("com.github.marschall.bigdecimalstorage.benchmark.*")
            .warmupIterations(5)
            .measurementIterations(5)
            .forks(3)
            .resultFormat(TEXT)
            .output(args[0])
            .build();
    new Runner(options).run();
  }

}
