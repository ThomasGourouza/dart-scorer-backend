package com.dartscorer.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "throws")
public class ThrowEntity {

  @Id
  private UUID id;

  @Column(name = "round_id", nullable = false)
  private UUID roundId;

  @Column(name = "date_time", nullable = false)
  private OffsetDateTime dateTime;

  @Column(name = "throw_number", nullable = false)
  private int throwNumber;

  @Column(name = "base", nullable = false)
  private int base;

  @Column(name = "multiplier", nullable = false)
  private int multiplier;

  @Column(name = "delta", nullable = false)
  private int delta;

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

  public UUID getRoundId() {
    return roundId;
  }

  public void setRoundId(UUID roundId) {
    this.roundId = roundId;
  }

  public OffsetDateTime getDateTime() {
    return dateTime;
  }

  public void setDateTime(OffsetDateTime dateTime) {
    this.dateTime = dateTime;
  }

  public int getThrowNumber() {
    return throwNumber;
  }

  public void setThrowNumber(int throwNumber) {
    this.throwNumber = throwNumber;
  }

  public int getBase() {
    return base;
  }

  public void setBase(int base) {
    this.base = base;
  }

  public int getMultiplier() {
    return multiplier;
  }

  public void setMultiplier(int multiplier) {
    this.multiplier = multiplier;
  }

  public int getDelta() {
    return delta;
  }

  public void setDelta(int delta) {
    this.delta = delta;
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
