CREATE TYPE GENDER AS ENUM ('Male', 'Female', 'Other');
CREATE TYPE CONTINENT AS ENUM ('Asia', 'Africa', 'North America', 'South America', 'Antarctica', 'Europe', 'Australia');

CREATE TABLE "persons"(
    "person_id" SERIAL PRIMARY KEY,
    "first_name" VARCHAR(128) NOT NULL,
    "last_name" VARCHAR(128) NOT NULL,
    "date_of_birth" DATE NOT NULL,
    "gender" GENDER NOT NULL,
    "country_id" INTEGER NOT NULL
);

CREATE TABLE "countries"(
    "country_id" SERIAL PRIMARY KEY,
    "name" VARCHAR(128) NOT NULL,
    "continent" CONTINENT NOT NULL
);

CREATE TABLE "person_contact_data"(
    "person_id" INTEGER NOT NULL,
    "mail_address" TEXT NOT NULL,
    "phone_number" TEXT NOT NULL,
    "timestamp_from" DATE NOT NULL,
    "timestamp_to" DATE,
    PRIMARY KEY("person_id", "timestamp_from")
);

CREATE TABLE "players"(
    "player_id" SERIAL PRIMARY KEY,
    "person_id" INTEGER NOT NULL
);

CREATE TABLE "arbiters"(
    "arbiter_id" SERIAL PRIMARY KEY,
    "person_id" INTEGER NOT NULL
);

CREATE TABLE "tournaments"(
    "tournament_id" SERIAL PRIMARY KEY,
    "chess_type_id" INTEGER NOT NULL,
    "address" TEXT NOT NULL,
    "country_id" INTEGER NOT NULL,
    "name" VARCHAR(64) NOT NULL,
    "main_arbiter" INTEGER NOT NULL,
    "time_control_id" INTEGER NOT NULL
);

CREATE TABLE "chess_type"(
    "chess_type_id" SERIAL PRIMARY KEY,
    "name" VARCHAR(64) NOT NULL,
    "total_time_from" INTERVAL,
    "total_time_to" INTERVAL
);

CREATE TABLE "games"(
    "game_id" SERIAL PRIMARY KEY,
    "tournament_id" INTEGER NOT NULL,
    "white_player_id" INTEGER NOT NULL,
    "black_player_id" INTEGER NOT NULL,
    "result" NUMERIC(2, 1), 
    "round_number" INTEGER NOT NULL,
    "pgn" TEXT NULL
    CHECK ("result" IS NULL or 
    "result" = 1.0
    or
    "result" = 0.5
    or
    "result" = 0.0)
);

CREATE TABLE "live_rating"(
    "player_id" INTEGER,
    "chess_type_id" INTEGER,
    "value" NUMERIC(9, 2) NOT NULL,
    PRIMARY KEY("player_id", "chess_type_id")
);

CREATE TABLE "rating_history"(
    "player_id" INTEGER NOT NULL,
    "value" INTEGER NOT NULL,
    "chess_type_id" INTEGER NOT NULL,
    "date_from" DATE NOT NULL,
    "date_to" DATE,
    PRIMARY KEY("player_id", "chess_type_id", "date_from")
);

CREATE TABLE "time_controls"(
    "time_control_id" SERIAL PRIMARY KEY,
    "starting_time" INTERVAL NOT NULL,
    "increment" INTERVAL NOT NULL DEFAULT interval '0 seconds',
    UNIQUE("starting_time", "increment")
);

CREATE TABLE "titles"(
    "short_name" CHAR(4) PRIMARY KEY,
    "full_name" VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE "players_titles"(
    "player_id" INTEGER NOT NULL,
    "title_short_name" CHAR(4) NOT NULL,
    "date_achieved" DATE NOT NULL,
    "norm1_tournament_id" INTEGER,
    "norm2_tournament_id" INTEGER,
    "norm3_tournament_id" INTEGER,
    "rating_norm_date" DATE,
    PRIMARY KEY("player_id", "title_short_name")
);

CREATE TABLE "club_memberships"(
    "player_id" INTEGER NOT NULL,
    "club_id" INTEGER NOT NULL,
    PRIMARY KEY ("player_id", "club_id")
);

CREATE TABLE "clubs"(
    "club_id" SERIAL PRIMARY KEY,
    "country_id" INTEGER,
    "name" VARCHAR(128)
);

CREATE TABLE "club_contact_data"(
    "club_id" INTEGER NOT NULL,
    "mail_address" VARCHAR(254) NOT NULL,
    "website" VARCHAR(256),
    "timestamp_from" DATE NOT NULL,
    "timestamp_to" DATE,
    PRIMARY KEY("club_id", "timestamp_from")
);

ALTER TABLE
    "person_contact_data" ADD CONSTRAINT "person_contact_data_person_id_foreign" FOREIGN KEY("person_id") REFERENCES "persons"("person_id");
ALTER TABLE
    "club_contact_data" ADD CONSTRAINT "club_contact_data_club_id_foreign" FOREIGN KEY("club_id") REFERENCES "clubs"("club_id");
ALTER TABLE
    "tournaments" ADD CONSTRAINT "tournaments_chess_type_id_foreign" FOREIGN KEY("chess_type_id") REFERENCES "chess_type"("chess_type_id");
ALTER TABLE
    "players_titles" ADD CONSTRAINT "players_titles_player_id_foreign" FOREIGN KEY("player_id") REFERENCES "players"("player_id");
ALTER TABLE
    "club_memberships" ADD CONSTRAINT "club_memberships_player_id_foreign" FOREIGN KEY("player_id") REFERENCES "players"("player_id");
ALTER TABLE
    "players" ADD CONSTRAINT "players_person_id_foreign" FOREIGN KEY("person_id") REFERENCES "persons"("person_id");
ALTER TABLE
    "tournaments" ADD CONSTRAINT "tournaments_main_arbiter_foreign" FOREIGN KEY("main_arbiter") REFERENCES "arbiters"("arbiter_id");
ALTER TABLE
    "tournaments" ADD CONSTRAINT "tournaments_time_control_id_foreign" FOREIGN KEY("time_control_id") REFERENCES "time_controls"("time_control_id");
ALTER TABLE
    "persons" ADD CONSTRAINT "persons_country_id_foreign" FOREIGN KEY("country_id") REFERENCES "countries"("country_id");
ALTER TABLE
    "players_titles" ADD CONSTRAINT "players_titles_norm1_tournament_id_foreign" FOREIGN KEY("norm1_tournament_id") REFERENCES "tournaments"("tournament_id");
ALTER TABLE
    "players_titles" ADD CONSTRAINT "players_titles_norm2_tournament_id_foreign" FOREIGN KEY("norm2_tournament_id") REFERENCES "tournaments"("tournament_id");
ALTER TABLE
    "players_titles" ADD CONSTRAINT "players_titles_norm3_tournament_id_foreign" FOREIGN KEY("norm3_tournament_id") REFERENCES "tournaments"("tournament_id");
ALTER TABLE
    "live_rating" ADD CONSTRAINT "live_rating_player_id_foreign" FOREIGN KEY("player_id") REFERENCES "players"("player_id");
ALTER TABLE
    "live_rating" ADD CONSTRAINT "live_rating_chess_type_id_foreign" FOREIGN KEY("chess_type_id") REFERENCES "chess_type"("chess_type_id");
ALTER TABLE
    "rating_history" ADD CONSTRAINT "rating_history_player_id_foreign" FOREIGN KEY("player_id") REFERENCES "players"("player_id");
ALTER TABLE
    "rating_history" ADD CONSTRAINT "rating_history_chess_type_id_foreign" FOREIGN KEY("chess_type_id") REFERENCES "chess_type"("chess_type_id");
ALTER TABLE
    "games" ADD CONSTRAINT "games_black_player_id_foreign" FOREIGN KEY("black_player_id") REFERENCES "players"("player_id");
ALTER TABLE
    "games" ADD CONSTRAINT "games_white_player_id_foreign" FOREIGN KEY("white_player_id") REFERENCES "players"("player_id");
ALTER TABLE
    "games" ADD CONSTRAINT "games_tournament_id_foreign" FOREIGN KEY("tournament_id") REFERENCES "tournaments"("tournament_id");
ALTER TABLE
    "players_titles" ADD CONSTRAINT "players_titles_title_short_name_foreign" FOREIGN KEY("title_short_name") REFERENCES "titles"("short_name");
ALTER TABLE
    "tournaments" ADD CONSTRAINT "tournaments_country_id_foreign" FOREIGN KEY("country_id") REFERENCES "countries"("country_id");
ALTER TABLE
    "arbiters" ADD CONSTRAINT "arbiters_person_id_foreign" FOREIGN KEY("person_id") REFERENCES "persons"("person_id");

CREATE VIEW "persons_readable" AS ( 
  SELECT
  "first_name" AS "First name",
  "last_name" AS "Last name",
  "date_of_birth" AS "Date of birth",
  "gender" AS "Gender",
  "name" AS "Country"
  FROM persons JOIN countries USING (country_id)
);

CREATE FUNCTION FIDE_scoring_probability (first_rating INTEGER, second_rating INTEGER)
RETURNS NUMERIC(3, 2)AS $$
DECLARE
rating_diff INTEGER;
BEGIN
  IF first_rating < second_rating THEN 
    RETURN 1-FIDE_scoring_probability(second_rating, first_rating);
  END IF;

  rating_diff = LEAST(first_rating - second_rating, 400); --400-point rule.
  --TODO change later to only be counted once/tournament
  RETURN CASE
  WHEN rating_diff >= 0 AND rating_diff <= 3 THEN 0.50
  WHEN rating_diff >= 4 AND rating_diff <= 10 THEN 0.51
  WHEN rating_diff >= 11 AND rating_diff <=  17 THEN 0.52
  WHEN rating_diff >= 18 AND rating_diff <=  25 THEN 0.53
  WHEN rating_diff >= 26 AND rating_diff <=  32 THEN 0.54
  WHEN rating_diff >= 33 AND rating_diff <=  39 THEN 0.55
  WHEN rating_diff >= 40 AND rating_diff <=  46 THEN 0.56
  WHEN rating_diff >= 47 AND rating_diff <=  53 THEN 0.57
  WHEN rating_diff >= 54 AND rating_diff <=  61 THEN 0.58
  WHEN rating_diff >= 62 AND rating_diff <=  68 THEN 0.59
  WHEN rating_diff >= 69 AND rating_diff <=  76 THEN 0.60
  WHEN rating_diff >= 77 AND rating_diff <=  83 THEN 0.61
  WHEN rating_diff >= 84 AND rating_diff <=  91 THEN 0.62
  WHEN rating_diff >= 92 AND rating_diff <=  98 THEN 0.63
  WHEN rating_diff >= 99 AND rating_diff <=  106 THEN 0.64
  WHEN rating_diff >= 107 AND rating_diff <= 113 THEN 0.65
  WHEN rating_diff >= 114 AND rating_diff <= 121 THEN 0.66
  WHEN rating_diff >= 122 AND rating_diff <= 129 THEN 0.67
  WHEN rating_diff >= 130 AND rating_diff <= 137 THEN 0.68
  WHEN rating_diff >= 138 AND rating_diff <= 145 THEN 0.69
  WHEN rating_diff >= 146 AND rating_diff <= 153 THEN 0.70
  WHEN rating_diff >= 154 AND rating_diff <= 162 THEN 0.71
  WHEN rating_diff >= 163 AND rating_diff <= 170 THEN 0.72
  WHEN rating_diff >= 171 AND rating_diff <= 179 THEN 0.73
  WHEN rating_diff >= 180 AND rating_diff <= 188 THEN 0.74
  WHEN rating_diff >= 189 AND rating_diff <= 197 THEN 0.75
  WHEN rating_diff >= 198 AND rating_diff <= 206 THEN 0.76
  WHEN rating_diff >= 207 AND rating_diff <= 215 THEN 0.77
  WHEN rating_diff >= 216 AND rating_diff <= 225 THEN 0.78
  WHEN rating_diff >= 226 AND rating_diff <= 235 THEN 0.79
  WHEN rating_diff >= 236 AND rating_diff <= 245 THEN 0.80
  WHEN rating_diff >= 246 AND rating_diff <= 256 THEN 0.81
  WHEN rating_diff >= 257 AND rating_diff <= 267 THEN 0.82
  WHEN rating_diff >= 268 AND rating_diff <= 278 THEN 0.83
  WHEN rating_diff >= 279 AND rating_diff <= 290 THEN 0.84
  WHEN rating_diff >= 291 AND rating_diff <= 302 THEN 0.85
  WHEN rating_diff >= 303 AND rating_diff <= 315 THEN 0.86
  WHEN rating_diff >= 316 AND rating_diff <= 328 THEN 0.87
  WHEN rating_diff >= 329 AND rating_diff <= 344 THEN 0.88
  WHEN rating_diff >= 345 AND rating_diff <= 357 THEN 0.89
  WHEN rating_diff >= 358 AND rating_diff <= 374 THEN 0.90
  WHEN rating_diff >= 375 AND rating_diff <= 391 THEN 0.91
  WHEN rating_diff >= 392 AND rating_diff <= 411 THEN 0.92
  WHEN rating_diff >= 412 AND rating_diff <= 432 THEN 0.93
  WHEN rating_diff >= 433 AND rating_diff <= 456 THEN 0.94
  WHEN rating_diff >= 457 AND rating_diff <= 484 THEN 0.95
  WHEN rating_diff >= 485 AND rating_diff <= 517 THEN 0.96
  WHEN rating_diff >= 518 AND rating_diff <= 559 THEN 0.97
  WHEN rating_diff >= 560 AND rating_diff <= 619 THEN 0.98
  WHEN rating_diff >= 620 AND rating_diff <= 735 THEN 0.99
  ELSE 1.00
  END;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION FIDE_rating_change(player_rating INTEGER, opponent_rating INTEGER, K_factor INTEGER, score NUMERIC(2, 1))
RETURNS NUMERIC (5, 2) AS
$$
BEGIN
  RETURN (score-FIDE_scoring_probability(player_rating, opponent_rating))*K_factor;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_K_factor(player_id INTEGER, chess_type_id INTEGER) RETURNS INTEGER
AS
$$
BEGIN
  RETURN 20;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION update_ratings_after_new_game_result() RETURNS TRIGGER AS $update_ratings_after_new_game_result$
DECLARE
  white_old_rating INTEGER;
  black_old_rating INTEGER;
  game_chess_type_id INTEGER;
BEGIN
  IF NEW.result IS NULL THEN
    RETURN NEW;
  END IF;
  IF TG_OP = 'UPDATE' AND OLD.result IS NOT DISTINCT FROM NEW.result THEN
    RETURN NEW;
  END IF;
  --TODO add changing the result
  game_chess_type_id = (
    SELECT t.chess_type_id 
    FROM games g JOIN tournaments t USING(tournament_id) 
    WHERE g.game_id = NEW.game_id);

  white_old_rating = (
    SELECT value 
    FROM rating_history rh 
    WHERE 
    rh.player_id = NEW.white_player_id
    AND rh.chess_type_id = game_chess_type_id
    AND rh.date_to IS NULL
  );

  black_old_rating = (
    SELECT value 
    FROM rating_history rh 
    WHERE 
    rh.player_id = NEW.black_player_id
    AND rh.chess_type_id = game_chess_type_id
    AND rh.date_to IS NULL
  );

  UPDATE live_rating 
  SET value = value + FIDE_rating_change(
    white_old_rating,
    black_old_rating,
    get_K_factor(NEW.white_player_id, game_chess_type_id),
    NEW.result
  )
  WHERE player_id = NEW.white_player_id AND chess_type_id = game_chess_type_id;

  UPDATE live_rating 
  SET value = value + FIDE_rating_change(
    black_old_rating,
    white_old_rating,
    get_K_factor(NEW.white_player_id, game_chess_type_id),
    1-NEW.result
  )
  WHERE player_id = NEW.black_player_id AND chess_type_id = game_chess_type_id;

  RETURN NEW;
END;
$update_ratings_after_new_game_result$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER update_ratings_after_new_game_result AFTER 
INSERT OR UPDATE ON games FOR EACH ROW
EXECUTE PROCEDURE update_ratings_after_new_game_result();
