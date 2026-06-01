--this was completely LLM-generated. Final project version will have human-written data

-- ============================================================================
-- sample_data.sql  —  Demo data for the International Chess Federation database
-- ----------------------------------------------------------------------------
-- Load order:
--     1. create.sql          (tables, types, rating functions + trigger)
--     2. triggers_baza.sql   (self-play guard, contact history, delete guard)
--     3. views_baza.sql      (reporting views)
--     4. sample_data.sql     (this file)
--
-- Re-runnable: it TRUNCATEs every table and restarts the identity counters,
-- so you can reload it as many times as you like during the presentation.
--
-- What it demonstrates:
--   * A realistic snapshot of the schema (countries, people, players, clubs,
--     arbiters, titles, tournaments, time controls).
--   * Three rating policies on chess_type: 'fide_standard', 'flat', 'unrated'.
--   * Seeded baseline ratings (rating_history + live_rating).
--   * Real games whose results AUTOMATICALLY recompute live ratings through the
--     update_ratings_after_new_game_result trigger (Elo / FIDE formula).
-- ============================================================================

BEGIN;

TRUNCATE
    games, players_titles, rating_history, live_rating,
    tournaments, time_controls, chess_type, titles,
    club_contact_data, club_memberships, clubs,
    person_contact_data, arbiters, players, persons, countries
    RESTART IDENTITY CASCADE;

-- ----------------------------------------------------------------------------
-- Countries
-- ----------------------------------------------------------------------------
INSERT INTO countries (name, continent) VALUES
                                            ('Norway',        'Europe'),         -- 1
                                            ('United States', 'North America'),  -- 2
                                            ('India',         'Asia'),           -- 3
                                            ('China',         'Asia'),           -- 4
                                            ('Russia',        'Europe'),         -- 5
                                            ('Poland',        'Europe');         -- 6

-- ----------------------------------------------------------------------------
-- People  (8 future players + 2 arbiters)
-- ----------------------------------------------------------------------------
INSERT INTO persons (first_name, last_name, date_of_birth, gender, country_id) VALUES
                                                                                   ('Magnus',       'Carlsen',          '1990-11-30', 'Male', 1),  -- 1
                                                                                   ('Hikaru',       'Nakamura',         '1987-12-09', 'Male', 2),  -- 2
                                                                                   ('Fabiano',      'Caruana',          '1992-07-30', 'Male', 2),  -- 3
                                                                                   ('Gukesh',       'Dommaraju',        '2006-05-29', 'Male', 3),  -- 4
                                                                                   ('Liren',        'Ding',             '1992-10-24', 'Male', 4),  -- 5
                                                                                   ('Jan-Krzysztof','Duda',             '1998-04-26', 'Male', 6),  -- 6
                                                                                   ('Yi',           'Wei',              '1999-06-02', 'Male', 4),  -- 7
                                                                                   ('Rameshbabu',   'Praggnanandhaa',   '2005-08-10', 'Male', 3),  -- 8
                                                                                   ('Anna',         'Schmidt',          '1985-03-14', 'Female', 6),-- 9  (arbiter)
                                                                                   ('Pavel',        'Nowak',            '1979-09-02', 'Male', 6);  -- 10 (arbiter)

-- ----------------------------------------------------------------------------
-- Players  (person_id 1..8  ->  player_id 1..8)
-- ----------------------------------------------------------------------------
INSERT INTO players (person_id) VALUES (1),(2),(3),(4),(5),(6),(7),(8);

-- ----------------------------------------------------------------------------
-- Arbiters (person_id 9,10  ->  arbiter_id 1,2)
-- ----------------------------------------------------------------------------
INSERT INTO arbiters (person_id) VALUES (9),(10);

-- ----------------------------------------------------------------------------
-- Clubs + memberships
-- ----------------------------------------------------------------------------
INSERT INTO clubs (country_id, name) VALUES
                                         (1, 'Oslo Chess Club'),            -- 1
                                         (2, 'Saint Louis Chess Club'),     -- 2
                                         (3, 'Chennai Knights');            -- 3

INSERT INTO club_memberships (player_id, club_id) VALUES
                                                      (1, 1),
                                                      (2, 2),
                                                      (3, 2),
                                                      (4, 3),
                                                      (8, 3);

INSERT INTO club_contact_data (club_id, mail_address, website, timestamp_from) VALUES
                                                                                   (1, 'contact@oslochess.no',   'https://oslochess.no',   '2015-01-01'),
                                                                                   (2, 'info@saintlouischess.us','https://saintlouischessclub.org', '2008-06-01');

-- ----------------------------------------------------------------------------
-- Contact data
-- The trigger_close_previous_contact trigger auto-closes the previous open
-- record when a newer one is inserted: notice Magnus' first row gets a
-- timestamp_to filled in automatically by the second INSERT.
-- ----------------------------------------------------------------------------
INSERT INTO person_contact_data (person_id, mail_address, phone_number, timestamp_from) VALUES
    (1, 'old.magnus@example.com', '+4711111111', '2018-01-01');
INSERT INTO person_contact_data (person_id, mail_address, phone_number, timestamp_from) VALUES
    (1, 'magnus@example.com',     '+4722222222', '2023-01-01');  -- closes the row above
INSERT INTO person_contact_data (person_id, mail_address, phone_number, timestamp_from) VALUES
                                                                                            (2, 'hikaru@example.com',     '+12025550143', '2021-05-10'),
                                                                                            (3, 'fabiano@example.com',    '+12025550178', '2020-02-20');

-- ----------------------------------------------------------------------------
-- Titles
-- ----------------------------------------------------------------------------
INSERT INTO titles (short_name, full_name) VALUES
                                               ('GM',  'Grandmaster'),
                                               ('IM',  'International Master'),
                                               ('FM',  'FIDE Master'),
                                               ('WGM', 'Woman Grandmaster');

INSERT INTO players_titles (player_id, title_short_name, date_achieved) VALUES
                                                                            (1, 'GM', '2004-04-26'),
                                                                            (2, 'GM', '2003-01-01'),
                                                                            (3, 'GM', '2007-07-01'),
                                                                            (4, 'GM', '2019-03-15'),
                                                                            (5, 'GM', '2009-06-01'),
                                                                            (6, 'GM', '2013-01-01'),
                                                                            (7, 'GM', '2013-03-01'),
                                                                            (8, 'GM', '2018-06-23');

-- ----------------------------------------------------------------------------
-- Time controls
-- ----------------------------------------------------------------------------
INSERT INTO time_controls (starting_time, increment) VALUES
                                                         ('90 minutes', '30 seconds'),   -- 1  classical
                                                         ('15 minutes', '10 seconds'),   -- 2  rapid
                                                         ('3 minutes',  '2 seconds');    -- 3  blitz

-- ----------------------------------------------------------------------------
-- Chess types  (one per rating policy, to show how K-factor differs)
--   fide_standard -> K computed from age / peak rating  (10 / 20 / 40)
--   flat          -> K is a fixed number (k_factor column)
--   unrated       -> K = 0, ratings never move
-- ----------------------------------------------------------------------------
INSERT INTO chess_type (name, total_time_from, total_time_to, rating_policy, k_factor) VALUES
                                                                                           ('Classical', '120 minutes', '360 minutes', 'fide_standard', NULL),  -- 1
                                                                                           ('Rapid',     '20 minutes',  '60 minutes',  'flat',          20),    -- 2
                                                                                           ('Blitz',     '6 minutes',   '15 minutes',  'unrated',       NULL);  -- 3

-- ----------------------------------------------------------------------------
-- Tournaments
-- ----------------------------------------------------------------------------
INSERT INTO tournaments
(chess_type_id, city, street_address, country_id, name, main_arbiter, time_control_id, date_from, date_to) VALUES
                                                                                                               (1, 'Oslo',      'Karl Johans gate 1', 1, 'Demo Masters 2026', 1, 1, '2026-01-05', '2026-01-12'),  -- 1 classical
                                                                                                               (2, 'St. Louis', '4657 Maryland Ave',  2, 'Rapid Challenge 2026', 1, 2, '2026-02-01', '2026-02-03'); -- 2 rapid

-- ----------------------------------------------------------------------------
-- Baseline ratings.
-- The rating trigger reads the player's "published" rating from rating_history
-- (the row with date_to IS NULL) to compute the expected score, and accumulates
-- the change into live_rating. So we seed BOTH tables for every player who will
-- play, in every chess type they will play.
-- ----------------------------------------------------------------------------

-- Classical (chess_type 1)
INSERT INTO rating_history (player_id, value, chess_type_id, date_from) VALUES
                                                                            (1, 2839, 1, '2025-01-01'),
                                                                            (2, 2802, 1, '2025-01-01'),
                                                                            (3, 2805, 1, '2025-01-01'),
                                                                            (4, 2783, 1, '2025-01-01'),
                                                                            (5, 2728, 1, '2025-01-01'),
                                                                            (6, 2731, 1, '2025-01-01'),
                                                                            (7, 2755, 1, '2025-01-01'),
                                                                            (8, 2758, 1, '2025-01-01');

INSERT INTO live_rating (player_id, chess_type_id, value) VALUES
                                                              (1, 1, 2839),
                                                              (2, 1, 2802),
                                                              (3, 1, 2805),
                                                              (4, 1, 2783),
                                                              (5, 1, 2728),
                                                              (6, 1, 2731),
                                                              (7, 1, 2755),
                                                              (8, 1, 2758);

-- Rapid (chess_type 2)
INSERT INTO rating_history (player_id, value, chess_type_id, date_from) VALUES
                                                                            (1, 2830, 2, '2025-01-01'),
                                                                            (2, 2879, 2, '2025-01-01'),
                                                                            (3, 2773, 2, '2025-01-01'),
                                                                            (5, 2780, 2, '2025-01-01');

INSERT INTO live_rating (player_id, chess_type_id, value) VALUES
                                                              (1, 2, 2830),
                                                              (2, 2, 2879),
                                                              (3, 2, 2773),
                                                              (5, 2, 2780);

-- ----------------------------------------------------------------------------
-- Games.
-- Every row with a non-NULL result fires update_ratings_after_new_game_result,
-- which updates live_rating for both players. The last game is left unplayed
-- (result = NULL) to show up in the tournament_schedule view as a pending pairing.
-- ----------------------------------------------------------------------------

-- Classical "Demo Masters 2026" (tournament 1)  -- fide_standard, K = 10 here
-- Round 1
INSERT INTO games (tournament_id, white_player_id, black_player_id, result, date, round_number) VALUES
                                                                                                    (1, 1, 2, 0.5, '2026-01-05', 1),
                                                                                                    (1, 3, 4, 1.0, '2026-01-05', 1),
                                                                                                    (1, 5, 6, 0.0, '2026-01-05', 1),
                                                                                                    (1, 7, 8, 0.5, '2026-01-05', 1);
-- Round 2
INSERT INTO games (tournament_id, white_player_id, black_player_id, result, date, round_number) VALUES
                                                                                                    (1, 1, 3, 1.0, '2026-01-06', 2),
                                                                                                    (1, 2, 4, 0.5, '2026-01-06', 2),
                                                                                                    (1, 5, 7, 0.5, '2026-01-06', 2),
                                                                                                    (1, 6, 8, 0.0, '2026-01-06', 2);
-- Round 3  (one game not yet played)
INSERT INTO games (tournament_id, white_player_id, black_player_id, result, date, round_number) VALUES
    (1, 1, 4, NULL, '2026-01-07', 3);

-- Rapid "Rapid Challenge 2026" (tournament 2)  -- flat, K = 20
INSERT INTO games (tournament_id, white_player_id, black_player_id, result, date, round_number) VALUES
                                                                                                    (2, 2, 1, 1.0, '2026-02-01', 1),
                                                                                                    (2, 3, 5, 0.5, '2026-02-01', 1);

COMMIT;