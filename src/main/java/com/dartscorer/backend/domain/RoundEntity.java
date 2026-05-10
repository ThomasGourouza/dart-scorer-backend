package com.dartscorer.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "rounds")
public class RoundEntity {

  @Id
  private UUID id;

  @Column(name = "game_id", nullable = false)
  private UUID gameId;

  @Column(name = "player_id", nullable = false)
  private UUID playerId;

  @Column(name = "round_number", nullable = false)
  private int roundNumber;

  @Column(name = "score_before", nullable = false)
  private int scoreBefore;

  @Column(name = "score_after", nullable = false)
  private int scoreAfter;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getGameId() {
    return gameId;
  }

  public void setGameId(UUID gameId) {
    this.gameId = gameId;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public void setPlayerId(UUID playerId) {
    this.playerId = playerId;
  }

  public int getRoundNumber() {
    return roundNumber;
  }

  public void setRoundNumber(int roundNumber) {
    this.roundNumber = roundNumber;
  }

  public int getScoreBefore() {
    return scoreBefore;
  }

  public void setScoreBefore(int scoreBefore) {
    this.scoreBefore = scoreBefore;
  }

  public int getScoreAfter() {
    return scoreAfter;
  }

  public void setScoreAfter(int scoreAfter) {
    this.scoreAfter = scoreAfter;
  }
}
