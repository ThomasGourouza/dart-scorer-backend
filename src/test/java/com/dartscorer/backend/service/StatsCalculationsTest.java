package com.dartscorer.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StatsCalculationsTest {

  @Test
  void rateReturnsZeroWhenDenominatorIsZeroOrNegative() {
    assertEquals(0.0, StatsCalculations.rate(5, 0));
    assertEquals(0.0, StatsCalculations.rate(5, -1));
  }

  @Test
  void rateComputesDoubleDivision() {
    assertEquals(0.5, StatsCalculations.rate(1, 2));
    assertEquals(0.25, StatsCalculations.rate(1, 4));
  }

  @Test
  void averageReturnsZeroWhenCountIsZeroOrNegative() {
    assertEquals(0.0, StatsCalculations.average(100, 0));
    assertEquals(0.0, StatsCalculations.average(100, -3));
  }

  @Test
  void averageComputesDoubleAverage() {
    assertEquals(2.5, StatsCalculations.average(10, 4));
    assertEquals(33.0, StatsCalculations.average(99, 3));
  }
}
