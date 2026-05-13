package com.dartscorer.backend.service;

/** Pure helpers used by the stats query layer, exposed for unit testing. */
public final class StatsCalculations {

  private StatsCalculations() {}

  public static double rate(long numerator, long denominator) {
    if (denominator <= 0) return 0.0;
    return (double) numerator / (double) denominator;
  }

  public static double average(long total, long count) {
    if (count <= 0) return 0.0;
    return (double) total / (double) count;
  }
}
