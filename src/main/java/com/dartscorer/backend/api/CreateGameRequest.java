package com.dartscorer.backend.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

public record CreateGameRequest(
    @NotNull OffsetDateTime gameStartedAt,
    @NotEmpty @Size(min = 2, max = 10) List<@Valid PlayerPayload> players,
    @NotEmpty List<@Valid RoundPayload> rounds) {

  public record PlayerPayload(
      @NotNull @Positive Integer playerIndex,
      @NotBlank @Size(max = 48) String playerName,
      @NotBlank @Size(max = 32) String color,
      Boolean winner) {
  }

  public record RoundPayload(
      @NotNull @Positive Integer playerIndex,
      @NotNull @Positive Integer roundNumber,
      @NotNull Integer scoreBefore,
      @NotNull Integer scoreAfter,
      @JsonProperty("throws") @NotEmpty @Size(max = 3) List<@Valid ThrowPayload> throwsList) {
  }

  public record ThrowPayload(
      @NotNull OffsetDateTime dateTime,
      @NotNull @Positive Integer throwNumber,
      @NotNull Integer base,
      @NotNull Integer multiplier,
      @NotNull Integer delta,
      @NotNull Integer scoreBefore,
      @NotNull Integer scoreAfter) {
  }
}
