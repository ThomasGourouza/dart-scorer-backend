package com.dartscorer.backend.api;

import com.dartscorer.backend.service.GamePersistenceService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/games")
public class GameController {

  private final GamePersistenceService gamePersistenceService;

  public GameController(GamePersistenceService gamePersistenceService) {
    this.gamePersistenceService = gamePersistenceService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CreateGameResponse createGame(@Valid @RequestBody CreateGameRequest request) {
    try {
      UUID gameId = gamePersistenceService.saveGame(request);
      return new CreateGameResponse(gameId);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
    }
  }
}
