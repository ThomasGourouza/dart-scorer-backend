package com.dartscorer.backend.api.stats;

import java.time.OffsetDateTime;
import java.util.List;

public final class StatsDtos {

  private StatsDtos() {}

  public record PlayerSummaryDto(
      String playerName,
      long gamesPlayed,
      long wins,
      double winRate,
      OffsetDateTime lastPlayedAt) {}

  public record PlayersListDto(List<PlayerSummaryDto> players) {}

  public record PlayerVariantBreakdownDto(int variant, long gamesPlayed, long wins) {}

  public record PlayerFavoriteSectorDto(int base, int multiplier, long hitCount) {}

  public record PlayerStatsDto(
      String playerName,
      long gamesPlayed,
      long wins,
      double winRate,
      long totalThrows,
      long totalRounds,
      long totalDelta,
      double averageDeltaPerThrow,
      double averageThrowsPerGame,
      double averageRoundsPerGame,
      long missCount,
      double missRate,
      long tonPlusCount,
      double avgFirstThreeRoundsPoints,
      Integer bestRoundPoints,
      Integer bestCheckout,
      PlayerFavoriteSectorDto favoriteSector,
      int highestSingleDartScore,
      List<PlayerVariantBreakdownDto> perVariant,
      OffsetDateTime firstPlayedAt,
      OffsetDateTime lastPlayedAt) {}
}
