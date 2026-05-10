package com.dartscorer.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "players")
public class PlayerEntity {

  @Id
  private UUID id;

  @Column(name = "game_id", nullable = false)
  private UUID gameId;

  @Column(name = "player_index", nullable = false)
  private int playerIndex;

  @Column(name = "player_name", nullable = false)
  private String playerName;

  @Column(name = "color", nullable = false)
  private String color;

  @Column(name = "winner")
  private Boolean winner;

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

  public int getPlayerIndex() {
    return playerIndex;
  }

  public void setPlayerIndex(int playerIndex) {
    this.playerIndex = playerIndex;
  }

  public String getPlayerName() {
    return playerName;
  }

  public void setPlayerName(String playerName) {
    this.playerName = playerName;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public Boolean getWinner() {
    return winner;
  }

  public void setWinner(Boolean winner) {
    this.winner = winner;
  }
}
