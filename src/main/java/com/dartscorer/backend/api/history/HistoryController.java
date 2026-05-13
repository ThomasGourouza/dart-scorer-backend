package com.dartscorer.backend.api.history;

import com.dartscorer.backend.api.history.HistoryDtos.GameDetailDto;
import com.dartscorer.backend.api.history.HistoryDtos.GameListPageDto;
import com.dartscorer.backend.service.HistoryQueryService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

  private final HistoryQueryService historyQueryService;

  public HistoryController(HistoryQueryService historyQueryService) {
    this.historyQueryService = historyQueryService;
  }

  @GetMapping("/games")
  public GameListPageDto listGames(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
    return historyQueryService.listGames(page, pageSize);
  }

  @GetMapping("/games/{gameId}")
  public GameDetailDto getGame(@PathVariable("gameId") UUID gameId) {
    return historyQueryService
        .getGame(gameId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found."));
  }
}
