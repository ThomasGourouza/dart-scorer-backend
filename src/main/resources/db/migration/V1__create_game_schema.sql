CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE games (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  date_time TIMESTAMPTZ NOT NULL
);

CREATE TABLE players (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  game_id UUID NOT NULL REFERENCES games(id) ON DELETE CASCADE,
  player_index INTEGER NOT NULL,
  player_name TEXT NOT NULL,
  color TEXT NOT NULL,
  winner BOOLEAN,
  CONSTRAINT uq_players_game_player_index UNIQUE (game_id, player_index)
);

CREATE TABLE rounds (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  game_id UUID NOT NULL REFERENCES games(id) ON DELETE CASCADE,
  player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
  round_number INTEGER NOT NULL CHECK (round_number > 0),
  score_before INTEGER NOT NULL,
  score_after INTEGER NOT NULL,
  CONSTRAINT uq_rounds_game_player_round UNIQUE (game_id, player_id, round_number)
);

CREATE TABLE throws (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  round_id UUID NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
  date_time TIMESTAMPTZ NOT NULL,
  throw_number INTEGER NOT NULL CHECK (throw_number BETWEEN 1 AND 3),
  base INTEGER NOT NULL CHECK (base IN (0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,25,50)),
  multiplier INTEGER NOT NULL CHECK (multiplier IN (1,2,3)),
  delta INTEGER NOT NULL,
  score_before INTEGER NOT NULL,
  score_after INTEGER NOT NULL,
  CONSTRAINT uq_throws_round_throw_number UNIQUE (round_id, throw_number)
);

CREATE INDEX idx_players_game_id ON players(game_id);
CREATE INDEX idx_rounds_game_id ON rounds(game_id);
CREATE INDEX idx_rounds_player_id ON rounds(player_id);
CREATE INDEX idx_throws_round_id ON throws(round_id);
CREATE INDEX idx_throws_date_time ON throws(date_time);
