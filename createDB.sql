DROP TYPE IF EXISTS genders CASCADE;
CREATE TYPE genders AS ENUM ('male', 'femail', 'else');

DROP TYPE IF EXISTS continents CASCADE;
CREATE TYPE continents AS ENUM ('asia', 'europa', 'north_america', 'south_america', 'africa', 'oceania');

DROP TYPE IF EXISTS game_result CASCADE;
CREATE TYPE game_result AS ENUM ('white', 'black', 'draw');

DROP TYPE IF EXISTS game_end_reasons CASCADE;
CREATE TYPE game_end_reasons AS ENUM ('something');

DROP TYPE IF EXISTS pieces CASCADE;
CREATE TYPE pieces AS ENUM ('something');

DROP TABLE IF EXISTS persons CASCADE;
CREATE TABLE persons (
  id serial PRIMARY KEY,
  date_of_birth date,
  gender genders NOT NULL,
  country integer NOT NULL REFERENCES countries(id),
  club integer REFERENCES Clubs(id),
  contact integer NOT NULL REFERENCES person_contact_data(id),
  description text
);

DROP TABLE IF EXISTS countries CASCADE;
CREATE TABLE countries (
  id serial PRIMARY KEY,
  name varchar(20) NOT NULL UNIQUE,
  continent continents NOT NULL
);

DROP TABLE IF EXISTS persons_non_constant CASCADE;
CREATE TABLE persons_non_constant (
  id serial PRIMARY KEY,
  ref integer NOT NULL REFERENCES persons(id),
  name varchar(20) NOT NULL,
  surname varchar(25) NOT NULL
);

DROP TABLE IF EXISTS Clubs CASCADE;
CREATE TABLE Clubs (
  id serial PRIMARY KEY,
  name varchar(30) NOT NULL UNIQUE,
  description text
);

DROP TABLE IF EXISTS person_contact_data CASCADE;
CREATE TABLE person_contact_data (
  id serial PRIMARY KEY,
  ref integer NOT NULL REFERENCES persons(id),
  mail_adress text NOT NULL,
  telefon text DEFAULT null,
  date_from date NOT NULL,
  date_to date DEFAULT null
);

DROP TABLE IF EXISTS players CASCADE;
CREATE TABLE players (
  id serial PRIMARY KEY,
  person integer NOT NULL REFERENCES persons(id)
);

DROP TABLE IF EXISTS arbiters CASCADE;
CREATE TABLE arbiters (
  id serial PRIMARY KEY,
  person integer NOT NULL REFERENCES persons(id)
);

DROP TABLE IF EXISTS arbiter_qualifications CASCADE;
CREATE TABLE arbiter_qualifications (
  id serial PRIMARY KEY,
  arbiter integer NOT NULL REFERENCES arbiters(id),
  qualification integer NOT NULL REFERENCES qualifications(id),
  data_from date NOT NULL,
  date_to date DEFAULT null
);

DROP TABLE IF EXISTS qualifications CASCADE;
CREATE TABLE qualifications (
  id serial PRIMARY KEY,
  name varchar(20) NOT NULL
);

DROP TABLE IF EXISTS touraments CASCADE;
CREATE TABLE touraments (
  id serial PRIMARY KEY,
  type integer NOT NULL REFERENCES chess_type(id),
  country integer NOT NULL REFERENCES countries(id),
  name varchar(30) NOT NULL,
  main_arbiter integer NOT NULL REFERENCES arbiters(id)
);

DROP TABLE IF EXISTS chess_type CASCADE;
CREATE TABLE chess_type (
  id serial PRIMARY KEY,
  name varchar(20) NOT NULL,
  time_control_from numeric,
  time_control_to numeric
);

DROP TABLE IF EXISTS games CASCADE;
CREATE TABLE games (
  id serial PRIMARY KEY,
  tournament integer NOT NULL REFERENCES touraments(id),
  white integer NOT NULL REFERENCES players(id),
  black integer REFERENCES players(id),
  result game_result,
  reason game_end_reasons,
  round_number numeric NOT NULL CHECK (round_number>0 ),
  CONSTRAINT nullcheck CHECK ( result is not null OR reason is null )
);

DROP TABLE IF EXISTS game_moves CASCADE;
CREATE TABLE game_moves (
  id serial PRIMARY KEY,
  game integer NOT NULL REFERENCES games(id),
  piece pieces NOT NULL
);

DROP TABLE IF EXISTS rating CASCADE;
CREATE TABLE rating (
  id serial PRIMARY KEY,
  player integer NOT NULL REFERENCES players(id),
  value numeric NOT NULL,
  type integer NOT NULL REFERENCES chess_type(id)
);

DROP TABLE IF EXISTS rating_history CASCADE;
CREATE TABLE rating_history (
  id serial PRIMARY KEY,
  player integer NOT NULL REFERENCES players(id),
  value numeric NOT NULL,
  type integer NOT NULL REFERENCES chess_type(id),
  date date NOT NULL
);

DROP TABLE IF EXISTS tournament_players CASCADE;
CREATE TABLE tournament_players (
  id serial PRIMARY KEY,
  tournament integer NOT NULL REFERENCES touraments(id),
  player integer NOT NULL REFERENCES players(id)
);

DROP TABLE IF EXISTS titles CASCADE;
CREATE TABLE titles (
  id serial PRIMARY KEY,
  name varchar(20) NOT NULL
);

DROP TABLE IF EXISTS clear_tournaments_table CASCADE;
CREATE TABLE clear_tournaments_table (
  id serial PRIMARY KEY,
  name text
);

DROP TABLE IF EXISTS tables_to_clear_all CASCADE;
CREATE TABLE tables_to_clear_all (
  id serial PRIMARY KEY,
  name text
);

INSERT INTO clear_tournaments_table (name) VALUES
  (touraments),
  (games),
  (game_moves),
  (rating),
  (rating_history),
  (tournament_players);

INSERT INTO clear_tournaments_table (name) VALUES
  (persons),
  (countries),
  (persons_non_constant),
  (Clubs),
  (person_contact_data),
  (players),
  (arbiters),
  (arbiter_qualifications),
  (qualifications),
  (touraments),
  (chess_type),
  (games),
  (game_moves),
  (rating),
  (rating_history),
  (tournament_players),
  (titles);

CREATE OR REPLACE FUNCTION clear_t ()
RETURNS void AS $$
DECLARE
  row varchar(30);
BEGIN
for row in select name from clear_tournaments_table LOOP
EXECUTE format ( 'TRUNCATE TABLE %I CASCADE', row );

END LOOP;
raise notice 'Cleared';
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION clear_all ()
RETURNS void AS $$
DECLARE
  row varchar(30);
BEGIN
for row in select name from tables_to_clear_all LOOP
EXECUTE format ( 'TRUNCATE TABLE %I CASCADE', row );

END LOOP;
raise notice 'Cleared';
END;
$$ LANGUAGE plpgsql;
