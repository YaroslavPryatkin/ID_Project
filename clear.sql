DROP TABLE IF EXISTS "players_titles"      CASCADE;
DROP TABLE IF EXISTS "rating_history"      CASCADE;
DROP TABLE IF EXISTS "live_rating"         CASCADE;
DROP TABLE IF EXISTS "games"               CASCADE;
DROP TABLE IF EXISTS "tournaments"          CASCADE;
DROP TABLE IF EXISTS "time_controls"       CASCADE;
DROP TABLE IF EXISTS "chess_type"          CASCADE;
DROP TABLE IF EXISTS "titles"              CASCADE;
DROP TABLE IF EXISTS "arbiters"            CASCADE;
DROP TABLE IF EXISTS "players"             CASCADE;
DROP TABLE IF EXISTS "person_contact_data" CASCADE;
DROP TABLE IF EXISTS "persons"             CASCADE;
DROP TABLE IF EXISTS "countries"           CASCADE;
DROP TABLE IF EXISTS "clubs"               CASCADE;
DROP TABLE IF EXISTS "club_memberships"    CASCADE;
DROP TABLE IF EXISTS "club_contact_data"   CASCADE;

DROP TYPE  IF EXISTS GENDER    CASCADE;
DROP TYPE  IF EXISTS CONTINENT CASCADE;

DROP FUNCTION IF EXISTS FIDE_scoring_probability;

DROP FUNCTION IF EXISTS FIDE_rating_change;

DROP FUNCTION IF EXISTS update_ratings_after_game_insert;
DROP TRIGGER IF EXISTS update_ratings_after_game_insert ON games;
