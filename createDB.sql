DROP TYPE IF EXISTS genders CASCADE;
CREATE TYPE genders AS ENUM ('Male', 'Female', 'other', 'not_mentioned');

DROP TYPE IF EXISTS continents CASCADE;
CREATE TYPE continents AS ENUM ('Asia', 'Europa', 'North_america', 'South_america', 'Africa', 'Oceania', 'other');

DROP TYPE IF EXISTS game_result CASCADE;
CREATE TYPE game_result AS ENUM ('white', 'black', 'draw');

DROP TYPE IF EXISTS game_end_reasons CASCADE;
CREATE TYPE game_end_reasons AS ENUM ('Checkmate', 'Resignation', 'Timeout', 'Forfeiture', 'Stalemate', 'Draw_by_Agreement', 'Insufficient_material', 'Threefold_repetition', '50_move_rule', '75_move_rule');

DROP TABLE IF EXISTS persons CASCADE;
CREATE TABLE persons (
  id serial PRIMARY KEY,
  date_of_birth date,
  gender genders NOT NULL DEFAULT 'not_mentioned',
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
  surname varchar(25) NOT NULL,
  country integer NOT NULL REFERENCES countries(id),
  club integer DEFAULT null REFERENCES Clubs(id)
);

DROP TABLE IF EXISTS Clubs CASCADE;
CREATE TABLE Clubs (
  id serial PRIMARY KEY,
  name varchar(50) NOT NULL UNIQUE,
  country integer DEFAULT null REFERENCES countries(id),
  description text
);

DROP TABLE IF EXISTS person_contact_data CASCADE;
CREATE TABLE person_contact_data (
  id serial PRIMARY KEY,
  ref integer NOT NULL REFERENCES persons(id),
  mail_address text NOT NULL,
  telephon text DEFAULT null,
  date_from date NOT NULL,
  date_to date DEFAULT null,
  CONSTRAINT datecheck CHECK ( date_from < date_to )
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
  qualification integer DEFAULT null REFERENCES qualifications(id),
  data_from date NOT NULL,
  date_to date DEFAULT null,
  CONSTRAINT datecheck CHECK ( date_from < date_to )
);

DROP TABLE IF EXISTS qualifications CASCADE;
CREATE TABLE qualifications (
  id serial PRIMARY KEY,
  name varchar(20) NOT NULL
);

DROP TABLE IF EXISTS tournaments CASCADE;
CREATE TABLE tournaments (
  id serial PRIMARY KEY,
  name varchar(50) NOT NULL,
  type integer NOT NULL REFERENCES chess_types(id),
  country integer NOT NULL REFERENCES countries(id),
  main_arbiter integer NOT NULL REFERENCES arbiters(id)
);

DROP TABLE IF EXISTS chess_types CASCADE;
CREATE TABLE chess_types (
  id serial PRIMARY KEY,
  name varchar(30) NOT NULL UNIQUE,
  time_control_minutes numeric(4),
  time_control_increment numeric(3),
  CONSTRAINT nullcheck CHECK ( (time_control_minutes is not null AND time_control_increment is not null) OR (time_control_minutes is null AND time_control_increment is null) )
);

DROP TABLE IF EXISTS games CASCADE;
CREATE TABLE games (
  id serial PRIMARY KEY,
  tournament integer NOT NULL REFERENCES tournaments(id),
  white integer NOT NULL REFERENCES players(id),
  black integer NOT NULL REFERENCES players(id),
  round_number numeric NOT NULL CHECK (round_number>0 )
);

DROP TABLE IF EXISTS game_moves CASCADE;
CREATE TABLE game_moves (
  id serial PRIMARY KEY,
  game integer NOT NULL REFERENCES games(id),
  piece integer NOT NULL REFERENCES pieces(id),
  place_from varchar(2) NOT NULL,
  place_to varchar(2) NOT NULL,
  captures integer DEFAULT null REFERENCES pieces(id),
  check boolean DEFAULT null
);

DROP TABLE IF EXISTS rating CASCADE;
CREATE TABLE rating (
  id serial PRIMARY KEY,
  player integer NOT NULL REFERENCES players(id),
  value numeric NOT NULL,
  type integer NOT NULL REFERENCES chess_types(id)
);

DROP TABLE IF EXISTS rating_history CASCADE;
CREATE TABLE rating_history (
  id serial PRIMARY KEY,
  player integer NOT NULL REFERENCES players(id),
  value numeric NOT NULL,
  type integer NOT NULL REFERENCES chess_types(id),
  date date NOT NULL
);

DROP TABLE IF EXISTS tournament_players CASCADE;
CREATE TABLE tournament_players (
  id serial PRIMARY KEY,
  tournament integer NOT NULL REFERENCES tournaments(id),
  player integer NOT NULL REFERENCES players(id)
);

DROP TABLE IF EXISTS titles CASCADE;
CREATE TABLE titles (
  id serial PRIMARY KEY,
  name varchar(50) NOT NULL,
  short_title varchar(3) DEFAULT null
);

DROP TABLE IF EXISTS pieces CASCADE;
CREATE TABLE pieces (
  id serial PRIMARY KEY,
  name varchar(6) NOT NULL UNIQUE,
  cost numeric(1)
);

DROP TABLE IF EXISTS piece_moves CASCADE;
CREATE TABLE piece_moves (
  id serial PRIMARY KEY,
  piece integer NOT NULL REFERENCES pieces(id),
  dx numeric(1) NOT NULL,
  dy numeric(1) NOT NULL,
  scalable boolean NOT NULL DEFAULT false
);

DROP TABLE IF EXISTS players_titles CASCADE;
CREATE TABLE players_titles (
  id serial PRIMARY KEY,
  player integer NOT NULL REFERENCES players(id),
  title integer DEFAULT null REFERENCES titles(id),
  data_from date NOT NULL,
  date_to date DEFAULT null,
  CONSTRAINT datecheck CHECK ( date_from < date_to )
);

DROP TABLE IF EXISTS game_endings CASCADE;
CREATE TABLE game_endings (
  id serial PRIMARY KEY,
  game integer NOT NULL UNIQUE REFERENCES games(id),
  result game_result NOT NULL,
  reason game_end_reasons NOT NULL,
  final_move_string text NOT NULL
);

DROP TABLE IF EXISTS clear_tournaments_table CASCADE;
CREATE TABLE clear_tournaments_table (
  id serial PRIMARY KEY,
  name text
);

DROP TABLE IF EXISTS clear_non_static CASCADE;
CREATE TABLE clear_non_static (
  id serial PRIMARY KEY,
  name text
);

INSERT INTO countries (name, continent) VALUES
  ('Poland', 'Europa'),
  ('USA', 'North_america'),
  ('Germany', 'Europa'),
  ('UK', 'Europa'),
  ('Australia', 'Oceania'),
  ('Ukraine', 'Europa'),
  ('Fr*nce', 'Europa'),
  ('Spain', 'Europa'),
  ('Canada', 'North_america'),
  ('Brazil', 'South_america'),
  ('Egypt', 'Africa'),
  ('Belarus', 'Europa'),
  ('China', 'Asia'),
  ('Japan', 'Asia'),
  ('other', 'other');

INSERT INTO qualifications (name) VALUES
  ('FIDE_arbiter');

INSERT INTO chess_types (name, time_control_minutes, time_control_increment) VALUES
  ('Bullet_10', 1, 0),
  ('Bullet_20', 2, 0),
  ('Bullet_11', 1, 1),
  ('Bullet_21', 2, 1),
  ('Blitz_30', 3, 0),
  ('Blitz_32', 3, 2),
  ('Blitz_50', 5, 0),
  ('Blitz_52', 5, 2),
  ('Rapid_10', 10, 0),
  ('Rapid_15', 10, 5),
  ('Rapid_60', 15, 10),
  ('Standard_60', 60, 0),
  ('Standard_100', 100, 0);

INSERT INTO pieces (name, cost) VALUES
  ('King', null),
  ('Queen', 9),
  ('Rook', 5),
  ('Bishop', 3),
  ('Knight', 3),
  ('Pawn', 1);

INSERT INTO piece_moves (piece, dx, dy) VALUES
  (1, 1, 0),
  (1, 1, 1),
  (1, 1, -1),
  (1, 0, 1),
  (1, 0, -1),
  (1, -1, 0),
  (1, -1, 1),
  (1, -1, -1),
  (5, 2, 1),
  (5, 2, -1),
  (5, -2, 1),
  (5, -2, -1),
  (5, 1, 2),
  (5, -1, 2),
  (5, 1, -2),
  (5, -1, -2),
  (6, 0, 1),
  (6, 1, 1),
  (6, -1, 1),
  (6, 0, 2);

INSERT INTO piece_moves (piece, dx, dy, scalable) VALUES
  (2, 1, 0, true),
  (2, 1, 1, true),
  (2, 1, -1, true),
  (2, 0, 1, true),
  (2, 0, -1, true),
  (2, -1, 0, true),
  (2, -1, 1, true),
  (2, -1, -1, true),
  (3, 1, 0, true),
  (3, -1, 0, true),
  (3, 0, 1, true),
  (3, 0, -1, true),
  (4, 1, 1, true),
  (4, 1, -1, true),
  (4, -1, -1, true),
  (4, -1, 1, true);

INSERT INTO titles (name, short_title) VALUES
  ('Grandmaster', 'GM'),
  ('International_master', 'IM'),
  ('FIDE_Master', 'FM'),
  ('FIDE_Candidate_master', 'CM'),
  ('Woman_Grandmaster', 'WGM'),
  ('Woman_International_master', 'WIM'),
  ('Woman_FIDE_Master', 'WFM'),
  ('Woman_Candidate_master', 'WCM');

INSERT INTO clear_tournaments_table (name) VALUES
  ('tournaments'),
  ('games'),
  ('game_moves'),
  ('rating'),
  ('rating_history'),
  ('tournament_players');

INSERT INTO clear_non_static (name) VALUES
  ('persons'),
  ('persons_non_constant'),
  ('Clubs'),
  ('person_contact_data'),
  ('players'),
  ('arbiters'),
  ('arbiter_qualifications'),
  ('tournaments'),
  ('games'),
  ('game_moves'),
  ('rating'),
  ('rating_history'),
  ('tournament_players'),
  ('players_titles');

CREATE OR REPLACE FUNCTION clear_tournaments ()
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

CREATE OR REPLACE FUNCTION clear_non_static ()
RETURNS void AS $$
DECLARE
  row varchar(30);
BEGIN
for row in select name from clear_non_static LOOP
EXECUTE format ( 'TRUNCATE TABLE %I CASCADE', row );

END LOOP;
raise notice 'Cleared';
END;
$$ LANGUAGE plpgsql;

