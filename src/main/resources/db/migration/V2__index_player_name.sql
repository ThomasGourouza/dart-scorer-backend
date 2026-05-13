CREATE INDEX IF NOT EXISTS idx_players_player_name_lower
  ON players (LOWER(TRIM(player_name)));

CREATE INDEX IF NOT EXISTS idx_games_date_time
  ON games (date_time DESC);
