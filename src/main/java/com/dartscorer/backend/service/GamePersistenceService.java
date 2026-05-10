package com.dartscorer.backend.service;

import com.dartscorer.backend.api.CreateGameRequest;
import com.dartscorer.backend.domain.GameEntity;
import com.dartscorer.backend.domain.PlayerEntity;
import com.dartscorer.backend.domain.RoundEntity;
import com.dartscorer.backend.domain.ThrowEntity;
import com.dartscorer.backend.repository.GameRepository;
import com.dartscorer.backend.repository.PlayerRepository;
import com.dartscorer.backend.repository.RoundRepository;
import com.dartscorer.backend.repository.ThrowRepository;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GamePersistenceService {

  private static final Set<Integer> VALID_BASES = Set.of(
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 25, 50);

  private final GameRepository gameRepository;
  private final PlayerRepository playerRepository;
  private final RoundRepository roundRepository;
  private final ThrowRepository throwRepository;

  public GamePersistenceService(
      GameRepository gameRepository,
      PlayerRepository playerRepository,
      RoundRepository roundRepository,
      ThrowRepository throwRepository) {
    this.gameRepository = gameRepository;
    this.playerRepository = playerRepository;
    this.roundRepository = roundRepository;
    this.throwRepository = throwRepository;
  }

  @Transactional
  public UUID saveGame(CreateGameRequest request) {
    final UUID gameId = UUID.randomUUID();

    GameEntity game = new GameEntity();
    game.setId(gameId);
    game.setDateTime(request.gameStartedAt());
    gameRepository.save(game);

    Map<Integer, UUID> playerIdsByIndex = new HashMap<>();
    for (CreateGameRequest.PlayerPayload playerPayload : request.players()) {
      if (playerIdsByIndex.containsKey(playerPayload.playerIndex())) {
        throw new IllegalArgumentException("Duplicate playerIndex in payload.");
      }
      UUID playerId = UUID.randomUUID();
      playerIdsByIndex.put(playerPayload.playerIndex(), playerId);

      PlayerEntity playerEntity = new PlayerEntity();
      playerEntity.setId(playerId);
      playerEntity.setGameId(gameId);
      playerEntity.setPlayerIndex(playerPayload.playerIndex());
      playerEntity.setPlayerName(playerPayload.playerName().trim());
      playerEntity.setColor(playerPayload.color().trim());
      playerEntity.setWinner(playerPayload.winner());
      playerRepository.save(playerEntity);
    }

    for (CreateGameRequest.RoundPayload roundPayload : request.rounds()) {
      UUID playerId = playerIdsByIndex.get(roundPayload.playerIndex());
      if (playerId == null) {
        throw new IllegalArgumentException("Round references unknown playerIndex.");
      }

      UUID roundId = UUID.randomUUID();
      RoundEntity roundEntity = new RoundEntity();
      roundEntity.setId(roundId);
      roundEntity.setGameId(gameId);
      roundEntity.setPlayerId(playerId);
      roundEntity.setRoundNumber(roundPayload.roundNumber());
      roundEntity.setScoreBefore(roundPayload.scoreBefore());
      roundEntity.setScoreAfter(roundPayload.scoreAfter());
      roundRepository.save(roundEntity);

      Set<Integer> seenThrowNumbers = new HashSet<>();
      for (CreateGameRequest.ThrowPayload throwPayload : roundPayload.throwsList()) {
        validateThrow(throwPayload, seenThrowNumbers);

        ThrowEntity throwEntity = new ThrowEntity();
        throwEntity.setId(UUID.randomUUID());
        throwEntity.setRoundId(roundId);
        throwEntity.setDateTime(throwPayload.dateTime());
        throwEntity.setThrowNumber(throwPayload.throwNumber());
        throwEntity.setBase(throwPayload.base());
        throwEntity.setMultiplier(throwPayload.multiplier());
        throwEntity.setDelta(throwPayload.delta());
        throwEntity.setScoreBefore(throwPayload.scoreBefore());
        throwEntity.setScoreAfter(throwPayload.scoreAfter());
        throwRepository.save(throwEntity);
      }
    }

    return gameId;
  }

  private void validateThrow(CreateGameRequest.ThrowPayload throwPayload, Set<Integer> seenThrowNumbers) {
    if (throwPayload.throwNumber() < 1 || throwPayload.throwNumber() > 3) {
      throw new IllegalArgumentException("throwNumber must be between 1 and 3.");
    }
    if (!seenThrowNumbers.add(throwPayload.throwNumber())) {
      throw new IllegalArgumentException("throwNumber must be unique inside a round.");
    }
    if (!VALID_BASES.contains(throwPayload.base())) {
      throw new IllegalArgumentException("Invalid base value.");
    }
    if (throwPayload.multiplier() < 1 || throwPayload.multiplier() > 3) {
      throw new IllegalArgumentException("multiplier must be 1, 2, or 3.");
    }
    if ((throwPayload.base() == 0 || throwPayload.base() == 25 || throwPayload.base() == 50)
        && throwPayload.multiplier() != 1) {
      throw new IllegalArgumentException("multiplier must be 1 for base 0/25/50.");
    }
    int expectedDelta = throwPayload.base() == 0 ? 0 : throwPayload.base() * throwPayload.multiplier();
    if (throwPayload.delta() != expectedDelta) {
      throw new IllegalArgumentException("delta does not match base*multiplier.");
    }
  }
}
