-- =========================================================================
-- SCREEN: system_setup
-- =========================================================================

-- READ
CREATE OR REPLACE FUNCTION system_setup_get_countries() 
RETURNS TABLE (country_id INTEGER, name VARCHAR, continent CONTINENT, is_active ACTIVE_STATUS) AS $$
BEGIN RETURN QUERY SELECT c.country_id, c.name, c.continent, c.is_active FROM countries c ORDER BY c.name; END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION system_setup_get_chess_types() 
RETURNS TABLE (id INTEGER, name VARCHAR, rating_policy RATING_POLICY, k_factor INTEGER) AS $$
BEGIN RETURN QUERY SELECT ct.chess_type_id, ct.name, ct.rating_policy, ct.k_factor FROM chess_type ct ORDER BY ct.name; END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION system_setup_get_time_controls() 
RETURNS TABLE (id INTEGER, starting_time INTERVAL, increment INTERVAL) AS $$
BEGIN RETURN QUERY SELECT tc.time_control_id, tc.starting_time, tc.increment FROM time_controls tc; END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION system_setup_get_titles() 
RETURNS TABLE (short_name CHAR(4), full_name VARCHAR) AS $$
BEGIN RETURN QUERY SELECT t.short_name, t.full_name FROM titles t; END;
$$ LANGUAGE plpgsql;

-- CREATE / UPDATE
CREATE OR REPLACE FUNCTION system_setup_noout_add_country(p_name TEXT, p_continent CONTINENT) RETURNS VOID AS $$
BEGIN
    INSERT INTO countries (name, continent) VALUES (TRIM(p_name), p_continent);
EXCEPTION WHEN unique_violation THEN RAISE EXCEPTION 'The country "%" is already registered.', p_name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION system_setup_noout_toggle_country_status(p_name TEXT, p_status ACTIVE_STATUS) RETURNS VOID AS $$
BEGIN
    UPDATE countries SET is_active = p_status WHERE name = p_name;
    IF NOT FOUND THEN RAISE EXCEPTION 'Country "%" not found.', p_name; END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION system_setup_noout_add_chess_type(p_name TEXT, p_total_time_from TEXT, p_total_time_to TEXT, p_rating_policy RATING_POLICY, p_k_factor INTEGER) RETURNS VOID AS $$
BEGIN
    INSERT INTO chess_type (name, total_time_from, total_time_to, rating_policy, k_factor)
    VALUES (TRIM(p_name), CAST(p_total_time_from AS INTERVAL), CAST(p_total_time_to AS INTERVAL), p_rating_policy, p_k_factor);
EXCEPTION WHEN unique_violation THEN RAISE EXCEPTION 'Chess type "%" already exists.', p_name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION system_setup_noout_add_time_control(p_starting_time TEXT, p_increment TEXT) RETURNS VOID AS $$
BEGIN
    INSERT INTO time_controls (starting_time, increment) VALUES (CAST(p_starting_time AS INTERVAL), CAST(p_increment AS INTERVAL));
EXCEPTION WHEN unique_violation THEN RAISE EXCEPTION 'This time control already exists.';
END;
$$ LANGUAGE plpgsql;

-- DELETE
CREATE OR REPLACE FUNCTION system_setup_noout_delete_country(p_name TEXT) RETURNS VOID AS $$
BEGIN
    DELETE FROM countries WHERE name = p_name;
    IF NOT FOUND THEN RAISE EXCEPTION 'Country "%" not found.', p_name; END IF;
EXCEPTION WHEN foreign_key_violation THEN RAISE EXCEPTION 'Cannot delete country "%" as it is linked to persons or tournaments.', p_name;
END;
$$ LANGUAGE plpgsql;


-- =========================================================================
-- SCREEN: person_management
-- =========================================================================

-- READ
CREATE OR REPLACE FUNCTION person_management_get_persons() 
RETURNS TABLE (id INTEGER, first_name VARCHAR, last_name VARCHAR, dob DATE, gender GENDER, country VARCHAR) AS $$
BEGIN
    RETURN QUERY SELECT p.person_id, p.first_name, p.last_name, p.date_of_birth, p.gender, c.name::VARCHAR 
    FROM persons p JOIN countries c USING (country_id) ORDER BY p.last_name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION person_management_get_players() 
RETURNS TABLE (player_id INTEGER, person_id INTEGER, first_name VARCHAR, last_name VARCHAR) AS $$
BEGIN
    RETURN QUERY SELECT pl.player_id, p.person_id, p.first_name, p.last_name 
    FROM players pl JOIN persons p USING (person_id);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION person_management_get_arbiters() 
RETURNS TABLE (arbiter_id INTEGER, person_id INTEGER, first_name VARCHAR, last_name VARCHAR) AS $$
BEGIN
    RETURN QUERY SELECT a.arbiter_id, p.person_id, p.first_name, p.last_name 
    FROM arbiters a JOIN persons p USING (person_id);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION person_management_get_contact_data() 
RETURNS TABLE (person_id INTEGER, full_name TEXT, email VARCHAR, phone VARCHAR) AS $$
BEGIN
    RETURN QUERY SELECT p.person_id, (p.first_name || ' ' || p.last_name)::TEXT, pcd.mail_address, pcd.phone_number
    FROM persons p JOIN person_contact_data pcd ON (p.person_id = pcd.person_id) WHERE pcd.timestamp_to IS NULL;
END;
$$ LANGUAGE plpgsql;

-- CREATE / UPDATE
CREATE OR REPLACE FUNCTION person_management_noout_add_person(p_first_name TEXT, p_last_name TEXT, p_dob DATE, p_gender GENDER, p_country_name TEXT) RETURNS VOID AS $$
DECLARE v_country_id INTEGER;
BEGIN
    SELECT country_id INTO v_country_id FROM countries WHERE name = p_country_name;
    IF NOT FOUND THEN RAISE EXCEPTION 'Country "%" does not exist.', p_country_name; END IF;
    INSERT INTO persons (first_name, last_name, date_of_birth, gender, country_id)
    VALUES (TRIM(p_first_name), TRIM(p_last_name), p_dob, p_gender, v_country_id);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION person_management_noout_update_person(p_person_id INTEGER, p_first_name TEXT, p_last_name TEXT, p_country_name TEXT) RETURNS VOID AS $$
DECLARE v_country_id INTEGER;
BEGIN
    SELECT country_id INTO v_country_id FROM countries WHERE name = p_country_name;
    IF NOT FOUND THEN RAISE EXCEPTION 'Country "%" does not exist.', p_country_name; END IF;
    UPDATE persons SET first_name = TRIM(p_first_name), last_name = TRIM(p_last_name), country_id = v_country_id WHERE person_id = p_person_id;
    IF NOT FOUND THEN RAISE EXCEPTION 'Person ID % not found.', p_person_id; END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION person_management_noout_add_contact_data(p_person_id INTEGER, p_mail_address TEXT, p_phone_number TEXT, p_date_from DATE) RETURNS VOID AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM persons WHERE person_id = p_person_id) THEN RAISE EXCEPTION 'Person ID % not found.', p_person_id; END IF;
    INSERT INTO person_contact_data (person_id, mail_address, phone_number, timestamp_from)
    VALUES (p_person_id, TRIM(p_mail_address), TRIM(p_phone_number), p_date_from);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION person_management_noout_promote_to_player(p_person_id INTEGER) RETURNS VOID AS $$
BEGIN
    INSERT INTO players (person_id) VALUES (p_person_id);
EXCEPTION WHEN unique_violation THEN RAISE EXCEPTION 'Person % is already a player.', p_person_id;
          WHEN foreign_key_violation THEN RAISE EXCEPTION 'Person ID % does not exist.', p_person_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION person_management_noout_promote_to_arbiter(p_person_id INTEGER) RETURNS VOID AS $$
BEGIN
    INSERT INTO arbiters (person_id) VALUES (p_person_id);
EXCEPTION WHEN unique_violation THEN RAISE EXCEPTION 'Person % is already an arbiter.', p_person_id;
          WHEN foreign_key_violation THEN RAISE EXCEPTION 'Person ID % does not exist.', p_person_id;
END;
$$ LANGUAGE plpgsql;

-- DELETE
CREATE OR REPLACE FUNCTION person_management_noout_delete_person(p_person_id INTEGER) RETURNS VOID AS $$
BEGIN
    DELETE FROM persons WHERE person_id = p_person_id;
    IF NOT FOUND THEN RAISE EXCEPTION 'Person ID % not found.', p_person_id; END IF;
EXCEPTION WHEN foreign_key_violation THEN RAISE EXCEPTION 'Cannot delete Person ID % due to active tournament, player or contact links.', p_person_id;
END;
$$ LANGUAGE plpgsql;


-- =========================================================================
-- SCREEN: club_management
-- =========================================================================

-- READ
CREATE OR REPLACE FUNCTION club_management_get_clubs() 
RETURNS TABLE (club_id INTEGER, name VARCHAR, country VARCHAR) AS $$
BEGIN
    RETURN QUERY SELECT c.club_id, c.name, co.name::VARCHAR FROM clubs c LEFT JOIN countries co USING (country_id);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION club_management_get_club_members(p_club_name TEXT) 
RETURNS TABLE (player_id INTEGER, member_name TEXT) AS $$
DECLARE v_club_id INTEGER;
BEGIN
    SELECT club_id INTO v_club_id FROM clubs WHERE name = p_club_name;
    IF NOT FOUND THEN RAISE EXCEPTION 'Club "%" not found.', p_club_name; END IF;
    RETURN QUERY SELECT cm.player_id, (p.first_name || ' ' || p.last_name)::TEXT 
    FROM club_memberships cm JOIN players pl USING (player_id) JOIN persons p USING (person_id) WHERE cm.club_id = v_club_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION club_management_get_club_contacts() 
RETURNS TABLE (club_id INTEGER, club_name VARCHAR, email VARCHAR, website VARCHAR) AS $$
BEGIN
    RETURN QUERY SELECT c.club_id, c.name, ccd.mail_address, ccd.website 
    FROM clubs c JOIN club_contact_data ccd USING (club_id) WHERE ccd.timestamp_to IS NULL;
END;
$$ LANGUAGE plpgsql;

-- CREATE / UPDATE / DELETE
CREATE OR REPLACE FUNCTION club_management_noout_add_club(p_name TEXT, p_country_name TEXT) RETURNS VOID AS $$
DECLARE v_country_id INTEGER;
BEGIN
    SELECT country_id INTO v_country_id FROM countries WHERE name = p_country_name;
    IF NOT FOUND THEN RAISE EXCEPTION 'Country "%" does not exist.', p_country_name; END IF;
    INSERT INTO clubs (name, country_id) VALUES (TRIM(p_name), v_country_id);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION club_management_noout_add_club_contact(p_club_name TEXT, p_mail_address TEXT, p_website TEXT, p_date_from DATE) RETURNS VOID AS $$
DECLARE v_club_id INTEGER;
BEGIN
    SELECT club_id INTO v_club_id FROM clubs WHERE name = p_club_name;
    IF NOT FOUND THEN RAISE EXCEPTION 'Club "%" not found.', p_club_name; END IF;
    INSERT INTO club_contact_data (club_id, mail_address, website, timestamp_from)
    VALUES (v_club_id, TRIM(p_mail_address), TRIM(p_website), p_date_from);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION club_management_noout_add_membership(p_player_id INTEGER, p_club_name TEXT) RETURNS VOID AS $$
DECLARE v_club_id INTEGER;
BEGIN
    SELECT club_id INTO v_club_id FROM clubs WHERE name = p_club_name;
    IF NOT FOUND THEN RAISE EXCEPTION 'Club "%" not found.', p_club_name; END IF;
    INSERT INTO club_memberships (player_id, club_id) VALUES (p_player_id, v_club_id);
EXCEPTION WHEN unique_violation THEN RAISE EXCEPTION 'Player % is already a member of this club.', p_player_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION club_management_noout_remove_membership(p_player_id INTEGER, p_club_name TEXT) RETURNS VOID AS $$
DECLARE v_club_id INTEGER;
BEGIN
    SELECT club_id INTO v_club_id FROM clubs WHERE name = p_club_name;
    DELETE FROM club_memberships WHERE player_id = p_player_id AND club_id = v_club_id;
    IF NOT FOUND THEN RAISE EXCEPTION 'Membership not found.'; END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION club_management_noout_delete_club(p_name TEXT) RETURNS VOID AS $$
BEGIN
    DELETE FROM clubs WHERE name = p_name;
    IF NOT FOUND THEN RAISE EXCEPTION 'Club "%" not found.', p_name; END IF;
EXCEPTION WHEN foreign_key_violation THEN RAISE EXCEPTION 'Cannot delete club "%" due to active roster memberships or contacts.', p_name;
END;
$$ LANGUAGE plpgsql;


-- =========================================================================
-- SCREEN: tournament_management
-- =========================================================================

-- READ
CREATE OR REPLACE FUNCTION tournament_management_get_tournaments() 
RETURNS TABLE (id INTEGER, name VARCHAR, format VARCHAR, city VARCHAR, start_date DATE, end_date DATE) AS $$
BEGIN
    RETURN QUERY SELECT t.tournament_id, t.name, ct.name::VARCHAR, t.city, t.date_from, t.date_to 
    FROM tournaments t JOIN chess_type ct USING (chess_type_id) ORDER BY t.date_from DESC;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tournament_management_get_participants(p_tournament_name TEXT) 
RETURNS TABLE (player_id INTEGER, first_name VARCHAR, last_name VARCHAR) AS $$
DECLARE v_tournament_id INTEGER;
BEGIN
    SELECT tournament_id INTO v_tournament_id FROM tournaments WHERE name = p_tournament_name;
    IF NOT FOUND THEN RAISE EXCEPTION 'Tournament "%" not found.', p_tournament_name; END IF;
    RETURN QUERY SELECT DISTINCT p.player_id, per.first_name, per.last_name
    FROM games g JOIN players p ON (p.player_id = g.white_player_id OR p.player_id = g.black_player_id)
    JOIN persons per ON (per.person_id = p.person_id) WHERE g.tournament_id = v_tournament_id;
END;
$$ LANGUAGE plpgsql;

-- CREATE / UPDATE / DELETE
CREATE OR REPLACE FUNCTION tournament_management_noout_create_tournament(
    p_name TEXT, p_chess_type_name TEXT, p_city TEXT, p_address TEXT, p_country_name TEXT, p_main_arbiter_id INTEGER, p_time_control_id INTEGER, p_date_from DATE, p_date_to DATE
) RETURNS VOID AS $$
DECLARE v_chess_type_id INTEGER; v_country_id INTEGER;
BEGIN
    IF p_date_to IS NOT NULL AND p_date_from >= p_date_to THEN RAISE EXCEPTION 'End date must be after start date.'; END IF;
    SELECT chess_type_id INTO v_chess_type_id FROM chess_type WHERE name = p_chess_type_name;
    IF NOT FOUND THEN RAISE EXCEPTION 'Chess type "%" not found.', p_chess_type_name; END IF;
    SELECT country_id INTO v_country_id FROM countries WHERE name = p_country_name;
    IF NOT FOUND THEN RAISE EXCEPTION 'Country "%" not found.', p_country_name; END IF;

    INSERT INTO tournaments (name, chess_type_id, city, street_address, country_id, main_arbiter, time_control_id, date_from, date_to) 
    VALUES (TRIM(p_name), v_chess_type_id, TRIM(p_city), TRIM(p_address), v_country_id, p_main_arbiter_id, p_time_control_id, p_date_from, p_date_to);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tournament_management_noout_update_dates(p_tournament_name TEXT, p_date_from DATE, p_date_to DATE) RETURNS VOID AS $$
BEGIN
    IF p_date_to IS NOT NULL AND p_date_from >= p_date_to THEN RAISE EXCEPTION 'End date must be after start date.'; END IF;
    UPDATE tournaments SET date_from = p_date_from, date_to = p_date_to WHERE name = p_tournament_name;
    IF NOT FOUND THEN RAISE EXCEPTION 'Tournament "%" not found.', p_tournament_name; END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tournament_management_noout_delete_tournament(p_name TEXT) RETURNS VOID AS $$
BEGIN
    DELETE FROM tournaments WHERE name = p_name;
    IF NOT FOUND THEN RAISE EXCEPTION 'Tournament "%" not found.', p_name; END IF;
EXCEPTION WHEN foreign_key_violation THEN RAISE EXCEPTION 'Cannot delete tournament "%" because games have already been recorded.', p_name;
END;
$$ LANGUAGE plpgsql;


-- =========================================================================
-- SCREEN: match_recording
-- =========================================================================

-- READ
CREATE OR REPLACE FUNCTION match_recording_get_games(p_tournament_name TEXT) 
RETURNS TABLE (game_id INTEGER, round INTEGER, white_player TEXT, black_player TEXT, result MATCH_RESULT, pgn TEXT) AS $$
DECLARE v_tournament_id INTEGER;
BEGIN
    SELECT tournament_id INTO v_tournament_id FROM tournaments WHERE name = p_tournament_name;
    IF NOT FOUND THEN RAISE EXCEPTION 'Tournament "%" not found.', p_tournament_name; END IF;

    RETURN QUERY 
    SELECT g.game_id, g.round_number, (pw.first_name || ' ' || pw.last_name)::TEXT, (pb.first_name || ' ' || pb.last_name)::TEXT, g.result, g.pgn
    FROM games g 
    JOIN players wp ON (wp.player_id = g.white_player_id) JOIN persons pw ON (pw.person_id = wp.person_id)
    JOIN players bp ON (bp.player_id = g.black_player_id) JOIN persons pb ON (pb.person_id = bp.person_id)
    WHERE g.tournament_id = v_tournament_id ORDER BY g.round_number, g.game_id;
END;
$$ LANGUAGE plpgsql;

-- CREATE / UPDATE / DELETE
CREATE OR REPLACE FUNCTION match_recording_noout_record_game(
    p_tournament_name TEXT, p_white_player_id INTEGER, p_black_player_id INTEGER, p_result MATCH_RESULT, p_date DATE, p_round_number INTEGER, p_pgn TEXT
) RETURNS VOID AS $$
DECLARE v_tournament_id INTEGER;
BEGIN
    IF p_white_player_id = p_black_player_id THEN RAISE EXCEPTION 'A player cannot play against themselves.'; END IF;
    SELECT tournament_id INTO v_tournament_id FROM tournaments WHERE name = p_tournament_name;
    IF NOT FOUND THEN RAISE EXCEPTION 'Tournament "%" not found.', p_tournament_name; END IF;

    INSERT INTO games (tournament_id, white_player_id, black_player_id, result, date, round_number, pgn)
    VALUES (v_tournament_id, p_white_player_id, p_black_player_id, p_result, p_date, p_round_number, TRIM(p_pgn));
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION match_recording_noout_update_game(p_game_id INTEGER, p_result MATCH_RESULT, p_pgn TEXT) RETURNS VOID AS $$
BEGIN
    UPDATE games SET result = p_result, pgn = TRIM(p_pgn) WHERE game_id = p_game_id;
    IF NOT FOUND THEN RAISE EXCEPTION 'Game ID % not found.', p_game_id; END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION match_recording_noout_delete_game(p_game_id INTEGER) RETURNS VOID AS $$
BEGIN
    DELETE FROM games WHERE game_id = p_game_id;
    IF NOT FOUND THEN RAISE EXCEPTION 'Game ID % not found.', p_game_id; END IF;
END;
$$ LANGUAGE plpgsql;


-- =========================================================================
-- SCREEN: ratings_and_titles
-- =========================================================================

-- READ
CREATE OR REPLACE FUNCTION ratings_and_titles_get_live_ratings() 
RETURNS TABLE (player_id INTEGER, name TEXT, chess_type VARCHAR, current_rating NUMERIC) AS $$
BEGIN
    RETURN QUERY SELECT lr.player_id, (p.first_name || ' ' || p.last_name)::TEXT, ct.name::VARCHAR, lr.value 
    FROM live_rating lr JOIN players pl USING (player_id) JOIN persons p USING (person_id) JOIN chess_type ct USING (chess_type_id)
    ORDER BY lr.value DESC;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ratings_and_titles_get_rating_history(p_player_id INTEGER) 
RETURNS TABLE (chess_type VARCHAR, rating_value INTEGER, valid_from DATE, valid_to DATE) AS $$
BEGIN
    RETURN QUERY SELECT ct.name::VARCHAR, rh.value, rh.date_from, rh.date_to 
    FROM rating_history rh JOIN chess_type ct USING (chess_type_id) WHERE rh.player_id = p_player_id ORDER BY rh.date_from DESC;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ratings_and_titles_get_titled_players() 
RETURNS TABLE (player_id INTEGER, name TEXT, title_name VARCHAR, date_achieved DATE) AS $$
BEGIN
    RETURN QUERY SELECT pt.player_id, (p.first_name || ' ' || p.last_name)::TEXT, t.full_name::VARCHAR, pt.date_achieved 
    FROM players_titles pt JOIN players pl USING (player_id) JOIN persons p USING (person_id) JOIN titles t ON (t.short_name = pt.title_short_name);
END;
$$ LANGUAGE plpgsql;

-- CREATE
CREATE OR REPLACE FUNCTION ratings_and_titles_noout_award_title(p_player_id INTEGER, p_title_name TEXT, p_date_achieved DATE) RETURNS VOID AS $$
DECLARE v_short_name CHAR(4);
BEGIN
    SELECT short_name INTO v_short_name FROM titles WHERE full_name = p_title_name;
    IF NOT FOUND THEN RAISE EXCEPTION 'Title "%" not found.', p_title_name; END IF;
    INSERT INTO players_titles (player_id, title_short_name, date_achieved) VALUES (p_player_id, v_short_name, p_date_achieved);
EXCEPTION WHEN unique_violation THEN RAISE EXCEPTION 'Player % already holds this title.', p_player_id;
END;
$$ LANGUAGE plpgsql;
