package com.dartscorer.backend.service;

import com.dartscorer.backend.api.history.HistoryDtos.GameDetailDto;
import com.dartscorer.backend.api.history.HistoryDtos.GameListItemDto;
import com.dartscorer.backend.api.history.HistoryDtos.GameListPageDto;
import com.dartscorer.backend.api.history.HistoryDtos.GameListPlayerDto;
import com.dartscorer.backend.api.history.HistoryDtos.GameRoundDto;
import com.dartscorer.backend.api.history.HistoryDtos.GameThrowDto;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoryQueryService {

  private final JdbcTemplate jdbc;

  public HistoryQueryService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional(readOnly = true)
  public GameListPageDto listGames(
      int page,
      int pageSize,
      String sort,
      String dir,
      String fromIsoDate,
      String toIsoDate,
      String winnerLike,
      String playerLike,
      Integer minDurationMinutes,
      Integer maxDurationMinutes,
      Integer minRounds,
      Integer maxRounds) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.max(1, Math.min(pageSize, 100));
    int offset = safePage * safeSize;

    String sortColumn = resolveSortColumn(sort);
    String sortDir = resolveSortDir(dir);

    OffsetDateTime fromDateTime = parseStartOfDay(fromIsoDate);
    OffsetDateTime toDateTime = parseEndOfDay(toIsoDate);
    Long minDurationSeconds = minutesToSeconds(minDurationMinutes);
    Long maxDurationSeconds = minutesToSeconds(maxDurationMinutes);
    String winnerPattern = toLikePattern(winnerLike);
    String playerPattern = toLikePattern(playerLike);

    StringBuilder where = new StringBuilder();
    List<Object> whereParams = new ArrayList<>();
    appendFilters(
        where,
        whereParams,
        fromDateTime,
        toDateTime,
        winnerPattern,
        playerPattern,
        minDurationSeconds,
        maxDurationSeconds,
        minRounds,
        maxRounds);

    String aggCte = AGG_CTE_SQL;
    String whereClause = where.length() == 0 ? "" : " WHERE " + where;

    String countSql = aggCte + " SELECT COUNT(*) FROM agg" + whereClause;
    Long totalBoxed = jdbc.queryForObject(countSql, Long.class, whereParams.toArray());
    long total = totalBoxed == null ? 0L : totalBoxed;
    if (total == 0L) {
      return new GameListPageDto(List.of(), 0L, safePage, safeSize);
    }

    String idsSql =
        aggCte
            + " SELECT id FROM agg"
            + whereClause
            + " ORDER BY "
            + sortColumn
            + " "
            + sortDir
            + ", id"
            + " LIMIT ? OFFSET ?";
    List<Object> idsParams = new ArrayList<>(whereParams);
    idsParams.add(safeSize);
    idsParams.add(offset);
    List<UUID> gameIds =
        jdbc.query(idsSql, (rs, i) -> (UUID) rs.getObject(1), idsParams.toArray());

    if (gameIds.isEmpty()) {
      return new GameListPageDto(List.of(), total, safePage, safeSize);
    }

    Map<UUID, GameListAggregate> aggregates = loadAggregates(gameIds);
    Map<UUID, List<GameListPlayerDto>> playersByGame = loadPlayers(gameIds);

    List<GameListItemDto> items = new ArrayList<>(gameIds.size());
    for (UUID gameId : gameIds) {
      GameListAggregate agg = aggregates.get(gameId);
      if (agg == null) continue;
      List<GameListPlayerDto> players =
          playersByGame.getOrDefault(gameId, List.of());
      GameListPlayerDto winner =
          players.stream().filter(p -> Boolean.TRUE.equals(p.winner())).findFirst().orElse(null);
      items.add(
          new GameListItemDto(
              gameId,
              agg.dateTime,
              players.size(),
              agg.roundsCount,
              agg.throwsCount,
              agg.durationSeconds,
              winner,
              players));
    }

    return new GameListPageDto(items, total, safePage, safeSize);
  }

  @Transactional(readOnly = true)
  public Optional<GameDetailDto> getGame(UUID gameId) {
    Map<UUID, GameListAggregate> aggregates = loadAggregates(List.of(gameId));
    GameListAggregate agg = aggregates.get(gameId);
    if (agg == null) return Optional.empty();

    Map<UUID, List<GameListPlayerDto>> playersByGame = loadPlayers(List.of(gameId));
    List<GameListPlayerDto> players = playersByGame.getOrDefault(gameId, List.of());
    Map<Integer, GameListPlayerDto> playersByIndex = new HashMap<>();
    for (GameListPlayerDto p : players) {
      playersByIndex.put(p.playerIndex(), p);
    }

    List<RoundRow> roundRows =
        jdbc.query(
            "SELECT r.id, r.round_number, p.player_index, p.player_name, p.color,"
                + " r.score_before, r.score_after"
                + " FROM rounds r"
                + " JOIN players p ON p.id = r.player_id"
                + " WHERE r.game_id = ?"
                + " ORDER BY r.round_number, p.player_index",
            (rs, i) ->
                new RoundRow(
                    (UUID) rs.getObject(1),
                    rs.getInt(2),
                    rs.getInt(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getInt(6),
                    rs.getInt(7)),
            gameId);

    Map<UUID, List<GameThrowDto>> throwsByRound = new HashMap<>();
    if (!roundRows.isEmpty()) {
      List<UUID> roundIds = roundRows.stream().map(RoundRow::id).toList();
      String placeholders = inPlaceholders(roundIds.size());
      Object[] params = roundIds.toArray();
      jdbc.query(
          "SELECT round_id, date_time, throw_number, base, multiplier, delta,"
              + " score_before, score_after"
              + " FROM throws"
              + " WHERE round_id IN ("
              + placeholders
              + ")"
              + " ORDER BY throw_number",
          params,
          rs -> {
            UUID rId = (UUID) rs.getObject("round_id");
            Timestamp ts = rs.getTimestamp("date_time");
            OffsetDateTime dt =
                ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
            GameThrowDto throwDto =
                new GameThrowDto(
                    dt,
                    rs.getInt("throw_number"),
                    rs.getInt("base"),
                    rs.getInt("multiplier"),
                    rs.getInt("delta"),
                    rs.getInt("score_before"),
                    rs.getInt("score_after"));
            throwsByRound.computeIfAbsent(rId, k -> new ArrayList<>()).add(throwDto);
          });
    }

    List<GameRoundDto> roundsOut = new ArrayList<>(roundRows.size());
    for (RoundRow row : roundRows) {
      List<GameThrowDto> rowThrows = throwsByRound.getOrDefault(row.id, List.of());
      roundsOut.add(
          new GameRoundDto(
              row.roundNumber,
              row.playerIndex,
              row.playerName,
              row.color,
              row.scoreBefore,
              row.scoreAfter,
              rowThrows));
    }

    return Optional.of(
        new GameDetailDto(
            gameId, agg.dateTime, agg.durationSeconds, players, roundsOut));
  }

  private Map<UUID, GameListAggregate> loadAggregates(List<UUID> gameIds) {
    if (gameIds.isEmpty()) return Map.of();
    String placeholders = inPlaceholders(gameIds.size());
    Object[] params = gameIds.toArray();
    String sql =
        "SELECT g.id, g.date_time,"
            + " COALESCE(r.rounds_count, 0) AS rounds_count,"
            + " COALESCE(t.throws_count, 0) AS throws_count,"
            + " t.min_thrown_at, t.max_thrown_at"
            + " FROM games g"
            + " LEFT JOIN ("
            + " SELECT game_id, COUNT(*) AS rounds_count FROM rounds GROUP BY game_id"
            + ") r ON r.game_id = g.id"
            + " LEFT JOIN ("
            + " SELECT r2.game_id, COUNT(t.*) AS throws_count,"
            + " MIN(t.date_time) AS min_thrown_at,"
            + " MAX(t.date_time) AS max_thrown_at"
            + " FROM throws t JOIN rounds r2 ON r2.id = t.round_id GROUP BY r2.game_id"
            + ") t ON t.game_id = g.id"
            + " WHERE g.id IN ("
            + placeholders
            + ")";

    Map<UUID, GameListAggregate> out = new HashMap<>();
    jdbc.query(
        sql,
        params,
        rs -> {
          UUID id = (UUID) rs.getObject("id");
          Timestamp dt = rs.getTimestamp("date_time");
          OffsetDateTime when = dt.toInstant().atOffset(ZoneOffset.UTC);
          int roundsCount = rs.getInt("rounds_count");
          int throwsCount = rs.getInt("throws_count");
          Timestamp minTs = rs.getTimestamp("min_thrown_at");
          Timestamp maxTs = rs.getTimestamp("max_thrown_at");
          Long durationSeconds = null;
          if (minTs != null && maxTs != null) {
            durationSeconds =
                Math.max(
                    0L,
                    (maxTs.toInstant().toEpochMilli() - minTs.toInstant().toEpochMilli()) / 1000L);
          }
          out.put(id, new GameListAggregate(when, roundsCount, throwsCount, durationSeconds));
        });
    return out;
  }

  private Map<UUID, List<GameListPlayerDto>> loadPlayers(List<UUID> gameIds) {
    if (gameIds.isEmpty()) return Map.of();
    String placeholders = inPlaceholders(gameIds.size());
    Object[] params = gameIds.toArray();
    Map<UUID, List<GameListPlayerDto>> out = new LinkedHashMap<>();
    jdbc.query(
        "SELECT game_id, player_index, player_name, color, winner"
            + " FROM players"
            + " WHERE game_id IN ("
            + placeholders
            + ")"
            + " ORDER BY player_index",
        params,
        rs -> {
          UUID gid = (UUID) rs.getObject("game_id");
          GameListPlayerDto dto =
              new GameListPlayerDto(
                  rs.getInt("player_index"),
                  rs.getString("player_name"),
                  rs.getString("color"),
                  rs.getObject("winner") == null ? null : rs.getBoolean("winner"));
          out.computeIfAbsent(gid, k -> new ArrayList<>()).add(dto);
        });
    for (List<GameListPlayerDto> players : out.values()) {
      players.sort(Comparator.comparingInt(GameListPlayerDto::playerIndex));
    }
    return out;
  }

  private static String inPlaceholders(int count) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < count; i++) {
      if (i > 0) sb.append(',');
      sb.append('?');
    }
    return sb.toString();
  }

  // Per-game aggregate CTE: every column the list endpoint sorts or filters on
  // is materialized here so we can WHERE / ORDER BY / paginate uniformly.
  private static final String AGG_CTE_SQL =
      "WITH agg AS ("
          + " SELECT g.id, g.date_time,"
          + " COALESCE(r.rounds_count, 0) AS rounds_count,"
          + " COALESCE(t.throws_count, 0) AS throws_count,"
          + " COALESCE("
          + " GREATEST(0, EXTRACT(EPOCH FROM (t.max_thrown_at - t.min_thrown_at)))::BIGINT,"
          + " 0"
          + " ) AS duration_seconds,"
          + " COALESCE(pl.players_count, 0) AS players_count,"
          + " w.winner_name"
          + " FROM games g"
          + " LEFT JOIN ("
          + " SELECT game_id, COUNT(*) AS rounds_count FROM rounds GROUP BY game_id"
          + " ) r ON r.game_id = g.id"
          + " LEFT JOIN ("
          + " SELECT r2.game_id,"
          + " COUNT(t.*) AS throws_count,"
          + " MIN(t.date_time) AS min_thrown_at,"
          + " MAX(t.date_time) AS max_thrown_at"
          + " FROM throws t JOIN rounds r2 ON r2.id = t.round_id"
          + " GROUP BY r2.game_id"
          + " ) t ON t.game_id = g.id"
          + " LEFT JOIN ("
          + " SELECT game_id, COUNT(*) AS players_count FROM players GROUP BY game_id"
          + " ) pl ON pl.game_id = g.id"
          + " LEFT JOIN ("
          + " SELECT game_id, MIN(TRIM(player_name)) AS winner_name"
          + " FROM players WHERE winner = TRUE GROUP BY game_id"
          + " ) w ON w.game_id = g.id"
          + " )";

  // Whitelist mapping sort key -> SQL fragment. Prevents SQL injection on `sort`.
  private static final Map<String, String> SORT_COLUMNS =
      Map.of(
          "date", "date_time",
          "duration", "duration_seconds",
          "rounds", "rounds_count",
          "throws", "throws_count",
          "players", "players_count",
          "winner", "LOWER(COALESCE(winner_name, ''))");

  private static String resolveSortColumn(String sort) {
    if (sort == null) return SORT_COLUMNS.get("date");
    String key = sort.trim().toLowerCase();
    return SORT_COLUMNS.getOrDefault(key, SORT_COLUMNS.get("date"));
  }

  private static String resolveSortDir(String dir) {
    if (dir != null && "asc".equalsIgnoreCase(dir.trim())) return "ASC";
    return "DESC";
  }

  private static OffsetDateTime parseStartOfDay(String iso) {
    if (iso == null || iso.isBlank()) return null;
    try {
      return LocalDate.parse(iso).atStartOfDay().atOffset(ZoneOffset.UTC);
    } catch (DateTimeParseException ex) {
      return null;
    }
  }

  private static OffsetDateTime parseEndOfDay(String iso) {
    if (iso == null || iso.isBlank()) return null;
    try {
      return LocalDate.parse(iso).plusDays(1).atStartOfDay().minusNanos(1).atOffset(ZoneOffset.UTC);
    } catch (DateTimeParseException ex) {
      return null;
    }
  }

  private static Long minutesToSeconds(Integer minutes) {
    if (minutes == null) return null;
    if (minutes < 0) return 0L;
    return minutes.longValue() * 60L;
  }

  private static String toLikePattern(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    if (trimmed.isEmpty()) return null;
    return "%" + trimmed.toLowerCase() + "%";
  }

  private static void appendFilters(
      StringBuilder where,
      List<Object> params,
      OffsetDateTime fromDateTime,
      OffsetDateTime toDateTime,
      String winnerPattern,
      String playerPattern,
      Long minDurationSeconds,
      Long maxDurationSeconds,
      Integer minRounds,
      Integer maxRounds) {
    if (fromDateTime != null) {
      appendAnd(where, "agg.date_time >= ?");
      params.add(Timestamp.from(fromDateTime.toInstant()));
    }
    if (toDateTime != null) {
      appendAnd(where, "agg.date_time <= ?");
      params.add(Timestamp.from(toDateTime.toInstant()));
    }
    if (winnerPattern != null) {
      appendAnd(where, "LOWER(TRIM(COALESCE(agg.winner_name, ''))) LIKE ?");
      params.add(winnerPattern);
    }
    if (playerPattern != null) {
      appendAnd(
          where,
          "EXISTS (SELECT 1 FROM players p WHERE p.game_id = agg.id"
              + " AND LOWER(TRIM(p.player_name)) LIKE ?)");
      params.add(playerPattern);
    }
    if (minDurationSeconds != null) {
      appendAnd(where, "agg.duration_seconds >= ?");
      params.add(minDurationSeconds);
    }
    if (maxDurationSeconds != null) {
      appendAnd(where, "agg.duration_seconds <= ?");
      params.add(maxDurationSeconds);
    }
    if (minRounds != null) {
      appendAnd(where, "agg.rounds_count >= ?");
      params.add(minRounds);
    }
    if (maxRounds != null) {
      appendAnd(where, "agg.rounds_count <= ?");
      params.add(maxRounds);
    }
  }

  private static void appendAnd(StringBuilder where, String clause) {
    if (where.length() > 0) where.append(" AND ");
    where.append(clause);
  }

  private record GameListAggregate(
      OffsetDateTime dateTime, int roundsCount, int throwsCount, Long durationSeconds) {}

  private record RoundRow(
      UUID id,
      int roundNumber,
      int playerIndex,
      String playerName,
      String color,
      int scoreBefore,
      int scoreAfter) {}
}
