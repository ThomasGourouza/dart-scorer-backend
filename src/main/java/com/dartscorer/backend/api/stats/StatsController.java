package com.dartscorer.backend.api.stats;

import com.dartscorer.backend.api.stats.StatsDtos.PlayerStatsDto;
import com.dartscorer.backend.api.stats.StatsDtos.PlayersListDto;
import com.dartscorer.backend.service.StatsQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

  private final StatsQueryService statsQueryService;

  public StatsController(StatsQueryService statsQueryService) {
    this.statsQueryService = statsQueryService;
  }

  @GetMapping("/players")
  public PlayersListDto listPlayers() {
    return statsQueryService.listPlayers();
  }

  @GetMapping("/players/{playerName}")
  public PlayerStatsDto getPlayerStats(
      @PathVariable("playerName") String playerName,
      @RequestParam(value = "from", required = false) String from,
      @RequestParam(value = "to", required = false) String to,
      @RequestParam(value = "variant", required = false) Integer variant) {
    return statsQueryService.getPlayerStats(playerName, from, to, variant);
  }
}
