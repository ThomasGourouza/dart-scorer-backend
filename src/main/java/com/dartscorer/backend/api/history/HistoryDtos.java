package com.dartscorer.backend.api.history;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class HistoryDtos {

  private HistoryDtos() {}

  public record GameListPlayerDto(
      int playerIndex,
      String playerName,
      String color,
      Boolean winner) {}

  public record GameListItemDto(
      UUID gameId,
      OffsetDateTime dateTime,
      int playersCount,
      int roundsCount,
      int throwsCount,
      Long durationSeconds,
      GameListPlayerDto winner,
      List<GameListPlayerDto> players) {}

  public record GameListPageDto(
      List<GameListItemDto> items,
      long total,
      int page,
      int pageSize) {}

  public record GameThrowDto(
      OffsetDateTime dateTime,
      int throwNumber,
      int base,
      int multiplier,
      int delta,
      int scoreBefore,
      int scoreAfter) {}

  public record GameRoundDto(
      int roundNumber,
      int playerIndex,
      String playerName,
      String color,
      int scoreBefore,
      int scoreAfter,
      @JsonProperty("throws") List<GameThrowDto> throwsList) {}

  public record GameDetailDto(
      UUID gameId,
      OffsetDateTime dateTime,
      Long durationSeconds,
      List<GameListPlayerDto> players,
      List<GameRoundDto> rounds) {}
}
