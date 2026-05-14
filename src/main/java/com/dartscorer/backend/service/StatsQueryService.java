package com.dartscorer.backend.service;

import com.dartscorer.backend.api.stats.StatsDtos.PlayerCompareDto;
import com.dartscorer.backend.api.stats.StatsDtos.PlayerFavoriteSectorDto;
import com.dartscorer.backend.api.stats.StatsDtos.PlayerStatsDto;
import com.dartscorer.backend.api.stats.StatsDtos.PlayerSummaryDto;
import com.dartscorer.backend.api.stats.StatsDtos.PlayerVariantBreakdownDto;
import com.dartscorer.backend.api.stats.StatsDtos.PlayersCompareDto;
import com.dartscorer.backend.api.stats.StatsDtos.PlayersListDto;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatsQueryService {

  private final JdbcTemplate jdbc;

  public StatsQueryService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional(readOnly = true)
  public PlayersListDto listPlayers() {
    List<PlayerSummaryDto> players =
        jdbc.query(
            "SELECT MAX(TRIM(p.player_name)) AS display_name,"
                + " COUNT(DISTINCT p.game_id) AS games_played,"
                + " COUNT(*) FILTER (WHERE p.winner = TRUE) AS wins,"
                + " MAX(g.date_time) AS last_played"
                + " FROM players p"
                + " JOIN games g ON g.id = p.game_id"
                + " WHERE TRIM(p.player_name) <> ''"
                + " GROUP BY LOWER(TRIM(p.player_name))"
                + " ORDER BY games_played DESC, last_played DESC NULLS LAST",
            (rs, i) -> {
              String name = rs.getString("display_name");
              long games = rs.getLong("games_played");
              long wins = rs.getLong("wins");
              double rate = games == 0 ? 0.0 : (double) wins / (double) games;
              Timestamp ts = rs.getTimestamp("last_played");
              OffsetDateTime when = ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
              return new PlayerSummaryDto(name, games, wins, rate, when);
            });
    return new PlayersListDto(players);
  }

  @Transactional(readOnly = true)
  public PlayersCompareDto listComparePlayers() {
    String sql =
        "WITH base AS ("
            + " SELECT LOWER(TRIM(p.player_name)) AS norm_name,"
            + " p.id AS player_id, p.game_id, p.winner, p.player_name"
            + " FROM players p"
            + " WHERE TRIM(p.player_name) <> ''"
            + "),"
            + " agg AS ("
            + " SELECT norm_name,"
            + " MAX(TRIM(player_name)) AS display_name,"
            + " COUNT(DISTINCT game_id) AS games_played,"
            + " COUNT(*) FILTER (WHERE winner = TRUE) AS wins"
            + " FROM base GROUP BY norm_name"
            + " ),"
            + " throws_agg AS ("
            + " SELECT b.norm_name,"
            + " COALESCE(SUM(t.delta), 0) AS total_delta,"
            + " COUNT(t.id) AS total_throws"
            + " FROM base b"
            + " JOIN rounds r ON r.player_id = b.player_id"
            + " JOIN throws t ON t.round_id = r.id"
            + " GROUP BY b.norm_name"
            + " ),"
            + " checkout_agg AS ("
            + " SELECT b.norm_name, MAX(r.score_before) AS best_checkout"
            + " FROM base b JOIN rounds r ON r.player_id = b.player_id"
            + " WHERE r.score_after = 0"
            + " GROUP BY b.norm_name"
            + " ),"
            + " ton_agg AS ("
            + " SELECT b.norm_name, COUNT(*) AS ton_plus_round_count"
            + " FROM base b JOIN rounds r ON r.player_id = b.player_id"
            + " WHERE (r.score_before - r.score_after) >= 50"
            + " GROUP BY b.norm_name"
            + " ),"
            + " best_round_agg AS ("
            + " SELECT b.norm_name, MAX(r.score_before - r.score_after) AS best_round"
            + " FROM base b JOIN rounds r ON r.player_id = b.player_id"
            + " WHERE r.score_before >= r.score_after"
            + " GROUP BY b.norm_name"
            + " ),"
            + " first_player_agg AS ("
            + " SELECT b.norm_name,"
            + " COUNT(*) FILTER (WHERE p.player_index = 1) AS first_games,"
            + " COUNT(*) FILTER (WHERE p.player_index = 1 AND p.winner = TRUE) AS first_wins"
            + " FROM base b JOIN players p ON p.id = b.player_id"
            + " GROUP BY b.norm_name"
            + " ),"
            + " color_counts AS ("
            + " SELECT LOWER(TRIM(player_name)) AS norm_name, color, COUNT(*) AS hits"
            + " FROM players"
            + " WHERE TRIM(player_name) <> ''"
            + " GROUP BY norm_name, color"
            + " ),"
            + " color_ranked AS ("
            + " SELECT norm_name, color,"
            + " ROW_NUMBER() OVER (PARTITION BY norm_name ORDER BY hits DESC, color ASC) AS rn"
            + " FROM color_counts"
            + " )"
            + " SELECT a.display_name, a.games_played, a.wins,"
            + " COALESCE(ta.total_delta, 0)  AS total_delta,"
            + " COALESCE(ta.total_throws, 0) AS total_throws,"
            + " ca.best_checkout,"
            + " br.best_round,"
            + " COALESCE(fp.first_games, 0) AS first_games,"
            + " COALESCE(fp.first_wins, 0)  AS first_wins,"
            + " COALESCE(tg.ton_plus_round_count, 0) AS ton_plus_round_count,"
            + " cl.color AS dominant_color"
            + " FROM agg a"
            + " LEFT JOIN throws_agg       ta ON ta.norm_name = a.norm_name"
            + " LEFT JOIN checkout_agg     ca ON ca.norm_name = a.norm_name"
            + " LEFT JOIN ton_agg          tg ON tg.norm_name = a.norm_name"
            + " LEFT JOIN best_round_agg   br ON br.norm_name = a.norm_name"
            + " LEFT JOIN first_player_agg fp ON fp.norm_name = a.norm_name"
            + " LEFT JOIN color_ranked     cl ON cl.norm_name = a.norm_name AND cl.rn = 1"
            + " ORDER BY a.games_played DESC, a.display_name ASC";

    List<PlayerCompareDto> rows =
        jdbc.query(
            sql,
            (rs, i) -> {
              String name = rs.getString("display_name");
              long games = rs.getLong("games_played");
              long wins = rs.getLong("wins");
              long totalDelta = rs.getLong("total_delta");
              long totalThrows = rs.getLong("total_throws");
              Integer bestCheckout = (Integer) rs.getObject("best_checkout");
              Integer bestRound = (Integer) rs.getObject("best_round");
              long firstGames = rs.getLong("first_games");
              long firstWins = rs.getLong("first_wins");
              long tonPlus = rs.getLong("ton_plus_round_count");
              String color = rs.getString("dominant_color");
              double winRate = StatsCalculations.rate(wins, games);
              double avgDelta = StatsCalculations.average(totalDelta, totalThrows);
              double firstWinRate = StatsCalculations.rate(firstWins, firstGames);
              return new PlayerCompareDto(
                  name,
                  games,
                  wins,
                  winRate,
                  avgDelta,
                  bestCheckout,
                  bestRound,
                  firstWinRate,
                  tonPlus,
                  color);
            });
    return new PlayersCompareDto(rows);
  }

  @Transactional(readOnly = true)
  public PlayerStatsDto getPlayerStats(
      String rawPlayerName, String fromIsoDate, String toIsoDate, Integer variant) {
    String name = rawPlayerName == null ? "" : rawPlayerName.trim();
    if (name.isEmpty()) {
      return emptyStats("");
    }

    OffsetDateTime fromDateTime = parseStartOfDay(fromIsoDate);
    OffsetDateTime toDateTime = parseEndOfDay(toIsoDate);
    String displayName = resolveDisplayName(name);
    if (displayName == null) {
      return emptyStats(name);
    }

    String filterSql = buildFilterSql(fromDateTime, toDateTime, variant);
    List<Object> filterParams = filterParams(name, fromDateTime, toDateTime, variant);

    GameAggregates ga = loadGameAggregates(filterSql, filterParams);
    if (ga.gamesPlayed == 0) {
      return emptyStats(displayName);
    }

    ThrowAggregates ta = loadThrowAggregates(filterSql, filterParams);
    long tonPlusRounds = loadTonPlusRoundCount(filterSql, filterParams);
    FirstPlayerAggregates fpa = loadFirstPlayerAggregates(filterSql, filterParams);
    PlayerFavoriteSectorDto favorite = loadFavoriteSector(filterSql, filterParams);
    Integer bestRound = loadBestRound(filterSql, filterParams);
    Integer bestCheckout = loadBestCheckout(filterSql, filterParams);
    double avgFirstThree = loadAvgFirstThreeRoundsPoints(filterSql, filterParams);
    int highestSingle = loadHighestSingleDart(filterSql, filterParams);
    List<PlayerVariantBreakdownDto> perVariant = loadVariantBreakdown(filterSql, filterParams);

    double winRate = StatsCalculations.rate(ga.wins, ga.gamesPlayed);
    double firstPlayerWinRate =
        StatsCalculations.rate(fpa.firstPlayerWins, fpa.firstPlayerGamesPlayed);
    double avgDelta = StatsCalculations.average(ta.totalDelta, ta.totalThrows);
    double avgThrowsPerGame = StatsCalculations.average(ta.totalThrows, ga.gamesPlayed);
    double avgRoundsPerGame = StatsCalculations.average(ta.totalRounds, ga.gamesPlayed);
    double missRate = StatsCalculations.rate(ta.missCount, ta.totalThrows);

    return new PlayerStatsDto(
        displayName,
        ga.gamesPlayed,
        ga.wins,
        winRate,
        fpa.firstPlayerGamesPlayed,
        fpa.firstPlayerWins,
        firstPlayerWinRate,
        ta.totalThrows,
        ta.totalRounds,
        ta.totalDelta,
        avgDelta,
        avgThrowsPerGame,
        avgRoundsPerGame,
        ta.missCount,
        missRate,
        tonPlusRounds,
        avgFirstThree,
        bestRound,
        bestCheckout,
        favorite,
        highestSingle,
        perVariant,
        ga.firstPlayedAt,
        ga.lastPlayedAt);
  }

  private GameAggregates loadGameAggregates(String filterSql, List<Object> filterParams) {
    String sql =
        "WITH matching AS ("
            + " SELECT p.id AS player_id, p.game_id, g.date_time, p.winner"
            + " FROM players p"
            + " JOIN games g ON g.id = p.game_id"
            + " WHERE "
            + filterSql
            + ")"
            + " SELECT COUNT(DISTINCT game_id) AS games_played,"
            + " COUNT(*) FILTER (WHERE winner = TRUE) AS wins,"
            + " MIN(date_time) AS first_played,"
            + " MAX(date_time) AS last_played"
            + " FROM matching";

    return jdbc.queryForObject(
        sql,
        (rs, i) -> {
          long games = rs.getLong("games_played");
          long wins = rs.getLong("wins");
          Timestamp first = rs.getTimestamp("first_played");
          Timestamp last = rs.getTimestamp("last_played");
          OffsetDateTime firstWhen = first == null ? null : first.toInstant().atOffset(ZoneOffset.UTC);
          OffsetDateTime lastWhen = last == null ? null : last.toInstant().atOffset(ZoneOffset.UTC);
          return new GameAggregates(games, wins, firstWhen, lastWhen);
        },
        filterParams.toArray());
  }

  private ThrowAggregates loadThrowAggregates(String filterSql, List<Object> filterParams) {
    String sql =
        "WITH player_rows AS ("
            + " SELECT p.id AS player_id FROM players p"
            + " JOIN games g ON g.id = p.game_id"
            + " WHERE "
            + filterSql
            + "),"
            + " rounds_filtered AS ("
            + " SELECT r.id AS round_id FROM rounds r"
            + " JOIN player_rows pr ON pr.player_id = r.player_id"
            + "),"
            + " throws_filtered AS ("
            + " SELECT t.* FROM throws t JOIN rounds_filtered rf ON rf.round_id = t.round_id"
            + ")"
            + " SELECT (SELECT COUNT(*) FROM rounds_filtered) AS total_rounds,"
            + " (SELECT COUNT(*) FROM throws_filtered) AS total_throws,"
            + " (SELECT COALESCE(SUM(delta), 0) FROM throws_filtered) AS total_delta,"
            + " (SELECT COUNT(*) FROM throws_filtered WHERE base = 0) AS miss_count";

    return jdbc.queryForObject(
        sql,
        (rs, i) ->
            new ThrowAggregates(
                rs.getLong("total_rounds"),
                rs.getLong("total_throws"),
                rs.getLong("total_delta"),
                rs.getLong("miss_count")),
        filterParams.toArray());
  }

  private long loadTonPlusRoundCount(String filterSql, List<Object> filterParams) {
    String sql =
        "WITH player_rows AS ("
            + " SELECT p.id AS player_id FROM players p"
            + " JOIN games g ON g.id = p.game_id"
            + " WHERE "
            + filterSql
            + ")"
            + " SELECT COUNT(*) AS ton_plus_round_count"
            + " FROM rounds r"
            + " JOIN player_rows pr ON pr.player_id = r.player_id"
            + " WHERE (r.score_before - r.score_after) >= 50";
    Long val =
        jdbc.queryForObject(
            sql, (rs, i) -> rs.getLong("ton_plus_round_count"), filterParams.toArray());
    return val == null ? 0L : val;
  }

  private FirstPlayerAggregates loadFirstPlayerAggregates(
      String filterSql, List<Object> filterParams) {
    String sql =
        "WITH player_rows AS ("
            + " SELECT p.player_index, p.winner FROM players p"
            + " JOIN games g ON g.id = p.game_id"
            + " WHERE "
            + filterSql
            + ")"
            + " SELECT COUNT(*) FILTER (WHERE player_index = 1) AS first_games,"
            + " COUNT(*) FILTER (WHERE player_index = 1 AND winner = TRUE) AS first_wins"
            + " FROM player_rows";
    return jdbc.queryForObject(
        sql,
        (rs, i) -> new FirstPlayerAggregates(rs.getLong("first_games"), rs.getLong("first_wins")),
        filterParams.toArray());
  }

  private PlayerFavoriteSectorDto loadFavoriteSector(
      String filterSql, List<Object> filterParams) {
    String sql =
        "WITH player_rows AS ("
            + " SELECT p.id AS player_id FROM players p"
            + " JOIN games g ON g.id = p.game_id"
            + " WHERE "
            + filterSql
            + ")"
            + " SELECT t.base, t.multiplier, COUNT(*) AS hits"
            + " FROM throws t"
            + " JOIN rounds r ON r.id = t.round_id"
            + " JOIN player_rows pr ON pr.player_id = r.player_id"
            + " WHERE t.base <> 0"
            + " GROUP BY t.base, t.multiplier"
            + " ORDER BY hits DESC, t.base * t.multiplier DESC"
            + " LIMIT 1";

    List<PlayerFavoriteSectorDto> rows =
        jdbc.query(
            sql,
            (rs, i) ->
                new PlayerFavoriteSectorDto(
                    rs.getInt("base"), rs.getInt("multiplier"), rs.getLong("hits")),
            filterParams.toArray());
    return rows.isEmpty() ? null : rows.get(0);
  }

  private Integer loadBestRound(String filterSql, List<Object> filterParams) {
    String sql =
        "WITH player_rows AS ("
            + " SELECT p.id AS player_id FROM players p"
            + " JOIN games g ON g.id = p.game_id"
            + " WHERE "
            + filterSql
            + ")"
            + " SELECT MAX(score_before - score_after) AS best_round"
            + " FROM rounds r"
            + " JOIN player_rows pr ON pr.player_id = r.player_id"
            + " WHERE score_before >= score_after";
    Integer val =
        jdbc.queryForObject(sql, (rs, i) -> (Integer) rs.getObject("best_round"), filterParams.toArray());
    return val;
  }

  private Integer loadBestCheckout(String filterSql, List<Object> filterParams) {
    String sql =
        "WITH player_rows AS ("
            + " SELECT p.id AS player_id FROM players p"
            + " JOIN games g ON g.id = p.game_id"
            + " WHERE "
            + filterSql
            + ")"
            + " SELECT MAX(r.score_before) AS best_checkout"
            + " FROM rounds r"
            + " JOIN player_rows pr ON pr.player_id = r.player_id"
            + " WHERE r.score_after = 0";
    Integer val =
        jdbc.queryForObject(
            sql, (rs, i) -> (Integer) rs.getObject("best_checkout"), filterParams.toArray());
    return val;
  }

  private double loadAvgFirstThreeRoundsPoints(String filterSql, List<Object> filterParams) {
    String sql =
        "WITH player_rows AS ("
            + " SELECT p.id AS player_id, p.game_id FROM players p"
            + " JOIN games g ON g.id = p.game_id"
            + " WHERE "
            + filterSql
            + ")"
            + " SELECT AVG(r.score_before - r.score_after)::DOUBLE PRECISION AS avg_pts"
            + " FROM rounds r"
            + " JOIN player_rows pr ON pr.player_id = r.player_id"
            + " WHERE r.round_number <= 3";
    Double val =
        jdbc.queryForObject(sql, (rs, i) -> (Double) rs.getObject("avg_pts"), filterParams.toArray());
    return val == null ? 0.0 : val;
  }

  private int loadHighestSingleDart(String filterSql, List<Object> filterParams) {
    String sql =
        "WITH player_rows AS ("
            + " SELECT p.id AS player_id FROM players p"
            + " JOIN games g ON g.id = p.game_id"
            + " WHERE "
            + filterSql
            + ")"
            + " SELECT COALESCE(MAX(t.delta), 0) AS max_delta"
            + " FROM throws t"
            + " JOIN rounds r ON r.id = t.round_id"
            + " JOIN player_rows pr ON pr.player_id = r.player_id";
    Integer val =
        jdbc.queryForObject(
            sql, (rs, i) -> rs.getInt("max_delta"), filterParams.toArray());
    return val == null ? 0 : val;
  }

  private List<PlayerVariantBreakdownDto> loadVariantBreakdown(
      String filterSql, List<Object> filterParams) {
    String sql =
        "WITH player_rows AS ("
            + " SELECT p.id AS player_id, p.game_id, p.winner FROM players p"
            + " JOIN games g ON g.id = p.game_id"
            + " WHERE "
            + filterSql
            + "),"
            + " game_variants AS ("
            + " SELECT g.id AS game_id, MAX(r.score_before) AS variant"
            + " FROM games g"
            + " JOIN rounds r ON r.game_id = g.id"
            + " WHERE r.round_number = 1"
            + " GROUP BY g.id"
            + " )"
            + " SELECT gv.variant,"
            + " COUNT(DISTINCT pr.game_id) AS games_played,"
            + " COUNT(*) FILTER (WHERE pr.winner = TRUE) AS wins"
            + " FROM player_rows pr"
            + " JOIN game_variants gv ON gv.game_id = pr.game_id"
            + " GROUP BY gv.variant"
            + " ORDER BY gv.variant";
    return jdbc.query(
        sql,
        (rs, i) ->
            new PlayerVariantBreakdownDto(
                rs.getInt("variant"), rs.getLong("games_played"), rs.getLong("wins")),
        filterParams.toArray());
  }

  private String resolveDisplayName(String name) {
    List<String> names =
        jdbc.query(
            "SELECT MAX(TRIM(player_name)) AS display_name"
                + " FROM players"
                + " WHERE LOWER(TRIM(player_name)) = LOWER(TRIM(?))",
            (rs, i) -> rs.getString("display_name"),
            name);
    for (String value : names) {
      if (value != null && !value.isBlank()) return value;
    }
    return null;
  }

  private String buildFilterSql(
      OffsetDateTime fromDate, OffsetDateTime toDate, Integer variant) {
    StringBuilder sql = new StringBuilder();
    sql.append("LOWER(TRIM(p.player_name)) = LOWER(TRIM(?))");
    if (fromDate != null) {
      sql.append(" AND g.date_time >= ?");
    }
    if (toDate != null) {
      sql.append(" AND g.date_time <= ?");
    }
    if (variant != null) {
      sql.append(
          " AND p.game_id IN ("
              + " SELECT game_id FROM rounds WHERE round_number = 1 GROUP BY game_id"
              + " HAVING MAX(score_before) = ?"
              + " )");
    }
    return sql.toString();
  }

  private List<Object> filterParams(
      String name, OffsetDateTime fromDate, OffsetDateTime toDate, Integer variant) {
    List<Object> params = new ArrayList<>();
    params.add(name);
    if (fromDate != null) params.add(Timestamp.from(fromDate.toInstant()));
    if (toDate != null) params.add(Timestamp.from(toDate.toInstant()));
    if (variant != null) params.add(variant);
    return params;
  }

  private OffsetDateTime parseStartOfDay(String iso) {
    if (iso == null || iso.isBlank()) return null;
    try {
      LocalDate date = LocalDate.parse(iso);
      return date.atStartOfDay().atOffset(ZoneOffset.UTC);
    } catch (DateTimeParseException ex) {
      return null;
    }
  }

  private OffsetDateTime parseEndOfDay(String iso) {
    if (iso == null || iso.isBlank()) return null;
    try {
      LocalDate date = LocalDate.parse(iso);
      return date.plusDays(1).atStartOfDay().minusNanos(1).atOffset(ZoneOffset.UTC);
    } catch (DateTimeParseException ex) {
      return null;
    }
  }

  private PlayerStatsDto emptyStats(String displayName) {
    return new PlayerStatsDto(
        displayName,
        0L, 0L, 0.0,
        0L, 0L, 0.0,
        0L, 0L, 0L, 0.0, 0.0, 0.0,
        0L, 0.0,
        0L, 0.0,
        null, null, null,
        0,
        List.of(),
        null, null);
  }

  private record GameAggregates(
      long gamesPlayed,
      long wins,
      OffsetDateTime firstPlayedAt,
      OffsetDateTime lastPlayedAt) {}

  private record ThrowAggregates(
      long totalRounds, long totalThrows, long totalDelta, long missCount) {}

  private record FirstPlayerAggregates(long firstPlayerGamesPlayed, long firstPlayerWins) {}
}
