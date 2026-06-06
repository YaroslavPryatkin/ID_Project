-- ==========================================================================
-- SCREEN 1: SYSTEM SETUP
-- ==========================================================================

CREATE OR REPLACE FUNCTION system_setup_get_countries() 
RETURNS TABLE ("ID" INTEGER, "Country Name" VARCHAR, "Continent" TEXT, "Status" TEXT) AS $$
BEGIN 
    RETURN QUERY SELECT c.country_id, c.name, c.continent::TEXT, c.is_active::TEXT FROM countries c ORDER BY c.name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION system_setup_get_chess_types() 
RETURNS TABLE ("ID" INTEGER, "Name" VARCHAR, "Min Time" INTERVAL, "Max Time" INTERVAL, "Rating Policy" TEXT, "K-Factor" INTEGER) AS $$
BEGIN 
    RETURN QUERY SELECT ct.chess_type_id, ct.name, ct.total_time_from, ct.total_time_to, ct.rating_policy::TEXT, ct.k_factor FROM chess_type ct ORDER BY ct.name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION system_setup_get_time_controls() 
RETURNS TABLE ("ID" INTEGER, "Name" VARCHAR, "Starting Time" INTERVAL, "Increment" INTERVAL) AS $$
BEGIN 
    RETURN QUERY SELECT tc.time_control_id, tc.name, tc.starting_time, tc.increment FROM time_controls tc ORDER BY tc.name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION system_setup_get_titles() 
RETURNS TABLE ("Short Name" CHAR(4), "Full Name" VARCHAR) AS $$
BEGIN 
    RETURN QUERY SELECT t.short_name, t.full_name FROM titles t ORDER BY t.full_name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION system_setup_noout_add_country(p_name TEXT, p_continent TEXT) RETURNS VOID AS $$
DECLARE v_continent CONTINENT;
BEGIN
    BEGIN
        v_continent := p_continent::CONTINENT;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid continent "%. Valid values: Asia, Africa, North America, South America, Antarctica, Europe, Australia.', p_continent;
    END;
    INSERT INTO countries (name, continent) VALUES (TRIM(p_name), v_continent);
EXCEPTION 
    WHEN unique_violation THEN RAISE EXCEPTION 'Country "%" already exists.', p_name;
    WHEN foreign_key_violation THEN RAISE EXCEPTION 'Database constraint violation.';
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION system_setup_noout_toggle_country_status(p_country_name TEXT, p_status TEXT) RETURNS VOID AS $$
DECLARE v_status ACTIVE_STATUS;
DECLARE v_country_id INTEGER;
BEGIN
    BEGIN
        v_status := p_status::ACTIVE_STATUS;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid status "%. Valid values: Active, Inactive.', p_status;
    END;
    
    SELECT country_id INTO v_country_id FROM countries WHERE name = TRIM(p_country_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Country "%" does not exist.', p_country_name; END IF;
    
    UPDATE countries SET is_active = v_status WHERE country_id = v_country_id;
EXCEPTION 
    WHEN OTHERS THEN RAISE EXCEPTION 'Error toggling country status: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION system_setup_noout_add_chess_type(p_name TEXT, p_min_time TEXT, p_max_time TEXT, p_rating_policy TEXT, p_k_factor TEXT) RETURNS VOID AS $$
DECLARE 
    v_min_interval INTERVAL;
    v_max_interval INTERVAL;
    v_rating_policy RATING_POLICY;
    v_k_factor INTEGER;
BEGIN
    BEGIN
        v_min_interval := p_min_time::INTERVAL;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid minimum time format "%". Use format like "00:15:00" (HH:MM:SS).', p_min_time;
    END;
    
    BEGIN
        v_max_interval := p_max_time::INTERVAL;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid maximum time format "%". Use format like "01:00:00" (HH:MM:SS).', p_max_time;
    END;
    
    BEGIN
        v_rating_policy := p_rating_policy::RATING_POLICY;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid rating policy "%. Valid values: unrated, flat, fide_standard.', p_rating_policy;
    END;
    
    IF p_k_factor IS NOT NULL AND p_k_factor != '' THEN
        BEGIN
            v_k_factor := p_k_factor::INTEGER;
        EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
            RAISE EXCEPTION 'Invalid K-factor "%". Must be a positive integer.', p_k_factor;
        END;
        
        IF v_k_factor <= 0 THEN
            RAISE EXCEPTION 'K-factor must be a positive integer, got %.', v_k_factor;
        END IF;
    ELSE
        v_k_factor := NULL;
    END IF;
    
    IF v_rating_policy = 'flat' AND v_k_factor IS NULL THEN
        RAISE EXCEPTION 'K-factor is required for "flat" rating policy.';
    END IF;
    
    INSERT INTO chess_type (name, total_time_from, total_time_to, rating_policy, k_factor) 
    VALUES (TRIM(p_name), v_min_interval, v_max_interval, v_rating_policy, v_k_factor);
EXCEPTION 
    WHEN unique_violation THEN RAISE EXCEPTION 'Chess type "%" already exists.', p_name;
    WHEN OTHERS THEN RAISE EXCEPTION 'Error creating chess type: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION system_setup_noout_add_time_control(p_name TEXT, p_starting_time TEXT, p_increment TEXT) RETURNS VOID AS $$
DECLARE 
    v_starting_interval INTERVAL;
    v_increment_interval INTERVAL;
BEGIN
    BEGIN
        v_starting_interval := p_starting_time::INTERVAL;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid starting time format "%". Use format like "00:15:00" (HH:MM:SS).', p_starting_time;
    END;
    
    IF v_starting_interval <= '0 seconds'::INTERVAL THEN
        RAISE EXCEPTION 'Starting time must be positive.';
    END IF;
    
    BEGIN
        v_increment_interval := p_increment::INTERVAL;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid increment format "%". Use format like "00:05:00" (HH:MM:SS).', p_increment;
    END;
    
    INSERT INTO time_controls (name, starting_time, increment) 
    VALUES (TRIM(p_name), v_starting_interval, v_increment_interval);
EXCEPTION 
    WHEN unique_violation THEN RAISE EXCEPTION 'Time control "%" or this combination already exists.', p_name;
    WHEN OTHERS THEN RAISE EXCEPTION 'Error creating time control: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION system_setup_noout_delete_country(p_country_name TEXT) RETURNS VOID AS $$
DECLARE v_country_id INTEGER;
BEGIN
    SELECT country_id INTO v_country_id FROM countries WHERE name = TRIM(p_country_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Country "%" does not exist.', p_country_name; END IF;
    
    IF EXISTS (SELECT 1 FROM persons WHERE country_id = v_country_id) THEN
        RAISE EXCEPTION 'Cannot delete country "%" - there are persons from this country.', p_country_name;
    END IF;
    
    IF EXISTS (SELECT 1 FROM clubs WHERE country_id = v_country_id) THEN
        RAISE EXCEPTION 'Cannot delete country "%" - there are clubs from this country.', p_country_name;
    END IF;
    
    IF EXISTS (SELECT 1 FROM tournaments WHERE country_id = v_country_id) THEN
        RAISE EXCEPTION 'Cannot delete country "%" - there are tournaments in this country.', p_country_name;
    END IF;
    
    DELETE FROM countries WHERE country_id = v_country_id;
EXCEPTION 
    WHEN OTHERS THEN RAISE EXCEPTION 'Error deleting country: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

-- ==========================================================================
-- SCREEN 2: PEOPLE MANAGEMENT
-- ==========================================================================

CREATE OR REPLACE FUNCTION person_management_get_persons() 
RETURNS TABLE ("ID" INTEGER, "First Name" VARCHAR(128), "Last Name" VARCHAR(128), "Date of Birth" DATE, "Gender" TEXT, "Country" VARCHAR) AS $$
BEGIN 
    RETURN QUERY SELECT p.person_id, p.first_name, p.last_name, p.date_of_birth, p.gender::TEXT, c.name 
    FROM persons p JOIN countries c USING (country_id) ORDER BY p.last_name, p.first_name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION person_management_get_contact_data() 
RETURNS TABLE ("Person ID" INTEGER, "First Name" VARCHAR(128), "Last Name" VARCHAR(128), "Email" VARCHAR, "Phone" VARCHAR) AS $$
BEGIN
    RETURN QUERY SELECT p.person_id, p.first_name , p.last_name, pcd.mail_address, pcd.phone_number
    FROM persons p JOIN person_contact_data pcd ON (p.person_id = pcd.person_id) WHERE pcd.timestamp_to IS NULL
    ORDER BY p.last_name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION person_management_get_person_details(p_person_name TEXT) 
RETURNS TABLE ("ID" INTEGER, "First Name" VARCHAR(128), "Last Name" VARCHAR(128), "Date of Birth" DATE, "Gender" TEXT, "Country" VARCHAR) AS $$
DECLARE v_person_id INTEGER;
BEGIN
    SELECT person_id INTO v_person_id FROM persons_concatenated WHERE name = TRIM(p_person_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Person "%" not found.', p_person_name; END IF;
    
    RETURN QUERY SELECT p.person_id, p.first_name, p.last_name, p.date_of_birth, p.gender::TEXT, c.name 
    FROM persons p JOIN countries c USING (country_id) WHERE p.person_id = v_person_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION person_management_noout_add_person(p_first_name TEXT, p_last_name TEXT, p_date_of_birth TEXT, p_gender TEXT, p_country_name TEXT) RETURNS VOID AS $$
DECLARE 
    v_gender GENDER;
    v_country_id INTEGER;
    v_dob DATE;
BEGIN
    BEGIN
        v_dob := p_date_of_birth::DATE;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid date format "%". Use format YYYY-MM-DD.', p_date_of_birth;
    END;
    
    BEGIN
        v_gender := p_gender::GENDER;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid gender "%. Valid values: Male, Female, Other.', p_gender;
    END;
    
    SELECT country_id INTO v_country_id FROM countries WHERE name = TRIM(p_country_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Country "%" does not exist.', p_country_name; END IF;
    
    INSERT INTO persons (first_name, last_name, date_of_birth, gender, country_id) 
    VALUES (TRIM(p_first_name), TRIM(p_last_name), v_dob, v_gender, v_country_id);
EXCEPTION 
    WHEN OTHERS THEN RAISE EXCEPTION 'Error adding person: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION person_management_noout_update_person(p_person_name TEXT, p_first_name TEXT, p_last_name TEXT, p_country_name TEXT) RETURNS VOID AS $$
DECLARE v_person_id INTEGER;
DECLARE v_country_id INTEGER;
BEGIN
    SELECT person_id INTO v_person_id FROM persons_concatenated WHERE name = TRIM(p_person_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Person "%" not found.', p_person_name; END IF;
    
    SELECT country_id INTO v_country_id FROM countries WHERE name = TRIM(p_country_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Country "%" does not exist.', p_country_name; END IF;
    
    UPDATE persons SET first_name = TRIM(p_first_name), last_name = TRIM(p_last_name), country_id = v_country_id 
    WHERE person_id = v_person_id;
EXCEPTION 
    WHEN OTHERS THEN RAISE EXCEPTION 'Error updating person: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION person_management_noout_add_contact_data(p_person_name TEXT, p_email TEXT, p_phone TEXT, p_valid_from_date TEXT) RETURNS VOID AS $$
DECLARE 
    v_person_id INTEGER;
    v_dob DATE;
BEGIN
    SELECT person_id INTO v_person_id FROM persons_concatenated WHERE name = TRIM(p_person_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Person "%" not found.', p_person_name; END IF;
    
    BEGIN
        v_dob := p_valid_from_date::DATE;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid date format "%". Use format YYYY-MM-DD.', p_valid_from_date;
    END;
    
    INSERT INTO person_contact_data (person_id, mail_address, phone_number, timestamp_from) 
    VALUES (v_person_id, TRIM(p_email), TRIM(p_phone), v_dob);
EXCEPTION 
    WHEN OTHERS THEN RAISE EXCEPTION 'Error adding contact data: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION person_management_noout_delete_person(p_person_name TEXT) RETURNS VOID AS $$
DECLARE v_person_id INTEGER;
BEGIN
    SELECT person_id INTO v_person_id FROM persons_concatenated WHERE name = TRIM(p_person_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Person "%" not found.', p_person_name; END IF;
    
    IF EXISTS (SELECT 1 FROM players WHERE person_id = v_person_id) THEN
        RAISE EXCEPTION 'Cannot delete person "%" - they are registered as a player.', p_person_name;
    END IF;
    
    IF EXISTS (SELECT 1 FROM arbiters WHERE person_id = v_person_id) THEN
        RAISE EXCEPTION 'Cannot delete person "%" - they are registered as an arbiter.', p_person_name;
    END IF;
    
    DELETE FROM person_contact_data WHERE person_id = v_person_id;
    DELETE FROM persons WHERE person_id = v_person_id;
EXCEPTION 
    WHEN OTHERS THEN RAISE EXCEPTION 'Error deleting person: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

-- ==========================================================================
-- SCREEN 3: ROLES MANAGEMENT
-- ==========================================================================

CREATE OR REPLACE FUNCTION roles_management_get_players() 
RETURNS TABLE ("ID" INTEGER, "First Name" VARCHAR(128), "Last Name" VARCHAR(128), "Date of Birth" DATE, "Gender" TEXT, "Country" VARCHAR) AS $$
BEGIN 
    RETURN QUERY SELECT p.person_id, p.first_name, p.last_name, p.date_of_birth, p.gender::TEXT, c.name AS "Country"
    FROM persons p
    JOIN countries c USING (country_id)
    WHERE EXISTS (SELECT 1 FROM players WHERE person_id = p.person_id)
    ORDER BY p.last_name, p.first_name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION roles_management_get_arbiters() 
RETURNS TABLE ("ID" INTEGER, "First Name" VARCHAR(128), "Last Name" VARCHAR(128), "Date of Birth" DATE, "Gender" TEXT, "Country" VARCHAR) AS $$
BEGIN 
    RETURN QUERY SELECT p.person_id, p.first_name, p.last_name, p.date_of_birth, p.gender::TEXT, c.name AS "Country"
    FROM persons p
    JOIN countries c USING (country_id)
    WHERE EXISTS (SELECT 1 FROM arbiters WHERE person_id = p.person_id)
    ORDER BY p.last_name, p.first_name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION roles_management_noout_promote_to_player(p_person_name TEXT) RETURNS VOID AS $$
DECLARE v_person_id INTEGER;
BEGIN
    SELECT person_id INTO v_person_id FROM people_not_players_concatenated WHERE name = TRIM(p_person_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Person "%" not found or is already a player.', p_person_name; END IF;
    
    INSERT INTO players (person_id) VALUES (v_person_id);
EXCEPTION 
    WHEN unique_violation THEN RAISE EXCEPTION 'Person "%" is already a player.', p_person_name;
    WHEN OTHERS THEN RAISE EXCEPTION 'Error promoting to player: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION roles_management_noout_promote_to_arbiter(p_person_name TEXT) RETURNS VOID AS $$
DECLARE v_person_id INTEGER;
BEGIN
    SELECT person_id INTO v_person_id FROM people_not_arbiters_concatenated WHERE name = TRIM(p_person_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Person "%" not found or is already an arbiter.', p_person_name; END IF;
    
    INSERT INTO arbiters (person_id) VALUES (v_person_id);
EXCEPTION 
    WHEN unique_violation THEN RAISE EXCEPTION 'Person "%" is already an arbiter.', p_person_name;
    WHEN OTHERS THEN RAISE EXCEPTION 'Error promoting to arbiter: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION roles_management_noout_demote_player(p_player_name TEXT) RETURNS VOID AS $$
DECLARE v_person_id INTEGER;
DECLARE v_player_id INTEGER;
BEGIN
    SELECT person_id INTO v_person_id FROM players_concatenated WHERE name = TRIM(p_player_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Player "%" not found.', p_player_name; END IF;
    
    SELECT player_id INTO v_player_id FROM players WHERE person_id = v_person_id;
    
    IF EXISTS (SELECT 1 FROM club_memberships WHERE player_id = v_player_id) THEN
        RAISE EXCEPTION 'Cannot remove player "%" - they are member of a club.', p_player_name;
    END IF;
    
    IF EXISTS (SELECT 1 FROM games WHERE white_player_id = v_player_id OR black_player_id = v_player_id) THEN
        RAISE EXCEPTION 'Cannot remove player "%" - they have participated in games.', p_player_name;
    END IF;
    
    IF EXISTS (SELECT 1 FROM players_titles WHERE player_id = v_player_id) THEN
        RAISE EXCEPTION 'Cannot remove player "%" - they have titles assigned.', p_player_name;
    END IF;
    
    IF EXISTS (SELECT 1 FROM live_rating WHERE player_id = v_player_id) THEN
        RAISE EXCEPTION 'Cannot remove player "%" - they have ratings.', p_player_name;
    END IF;
    
    DELETE FROM players WHERE player_id = v_player_id;
EXCEPTION 
    WHEN OTHERS THEN RAISE EXCEPTION 'Error removing player: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION roles_management_noout_demote_arbiter(p_arbiter_name TEXT) RETURNS VOID AS $$
DECLARE v_person_id INTEGER;
DECLARE v_arbiter_id INTEGER;
BEGIN
    SELECT person_id INTO v_person_id FROM arbiters_concatenated WHERE name = TRIM(p_arbiter_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Arbiter "%" not found.', p_arbiter_name; END IF;
    
    SELECT arbiter_id INTO v_arbiter_id FROM arbiters WHERE person_id = v_person_id;
    
    IF EXISTS (SELECT 1 FROM tournaments WHERE main_arbiter = v_arbiter_id) THEN
        RAISE EXCEPTION 'Cannot remove arbiter "%" - they are main arbiter for tournaments.', p_arbiter_name;
    END IF;
    
    DELETE FROM arbiters WHERE arbiter_id = v_arbiter_id;
EXCEPTION 
    WHEN OTHERS THEN RAISE EXCEPTION 'Error removing arbiter: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

-- ==========================================================================
-- SCREEN 4: CLUBS & TEAMS
-- ==========================================================================

CREATE OR REPLACE FUNCTION club_management_get_clubs() 
RETURNS TABLE ("ID" INTEGER, "Name" VARCHAR, "Country" VARCHAR) AS $$
BEGIN 
    RETURN QUERY SELECT c.club_id, c.name, co.name FROM clubs c JOIN countries co ON (c.country_id = co.country_id) ORDER BY c.name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION club_management_get_club_members(p_club_name TEXT) 
RETURNS TABLE ("ID" INTEGER, "First Name" VARCHAR(128), "Last Name" VARCHAR(128), "Date of Birth" DATE, "Gender" TEXT, "Country" VARCHAR) AS $$
DECLARE v_club_id INTEGER;
BEGIN
    SELECT club_id INTO v_club_id FROM clubs WHERE name = TRIM(p_club_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Club "%" not found.', p_club_name; END IF;
    
    RETURN QUERY SELECT p.person_id, p.first_name, p.last_name, p.date_of_birth, p.gender::TEXT, c.name
    FROM club_memberships cm
    JOIN players pl ON (cm.player_id = pl.player_id)
    JOIN persons p ON (pl.person_id = p.person_id)
    JOIN countries c ON (p.country_id = c.country_id)
    WHERE cm.club_id = v_club_id
    ORDER BY p.last_name, p.first_name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION club_management_get_club_contacts() 
RETURNS TABLE ("Club Name" VARCHAR, "Email" VARCHAR, "Website" VARCHAR, "Valid From" DATE) AS $$
BEGIN 
    RETURN QUERY SELECT c.name, ccd.mail_address, ccd.website, ccd.timestamp_from
    FROM club_contact_data ccd JOIN clubs c ON (ccd.club_id = c.club_id)
    WHERE ccd.timestamp_to IS NULL
    ORDER BY c.name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION club_management_noout_add_club(p_name TEXT, p_country_name TEXT) RETURNS VOID AS $$
DECLARE v_country_id INTEGER;
BEGIN
    SELECT country_id INTO v_country_id FROM countries WHERE name = TRIM(p_country_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Country "%" does not exist.', p_country_name; END IF;
    INSERT INTO clubs (name, country_id) VALUES (TRIM(p_name), v_country_id);
EXCEPTION 
    WHEN unique_violation THEN RAISE EXCEPTION 'Club "%" already exists.', p_name;
    WHEN OTHERS THEN RAISE EXCEPTION 'Error adding club: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION club_management_noout_add_club_contact(p_club_name TEXT, p_email TEXT, p_website TEXT, p_valid_from_date TEXT) RETURNS VOID AS $$
DECLARE 
    v_club_id INTEGER;
    v_dob DATE;
BEGIN
    SELECT club_id INTO v_club_id FROM clubs WHERE name = TRIM(p_club_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Club "%" does not exist.', p_club_name; END IF;
    
    BEGIN
        v_dob := p_valid_from_date::DATE;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid date format "%". Use format YYYY-MM-DD.', p_valid_from_date;
    END;
    
    INSERT INTO club_contact_data (club_id, mail_address, website, timestamp_from) 
    VALUES (v_club_id, TRIM(p_email), TRIM(p_website), v_dob);
EXCEPTION 
    WHEN OTHERS THEN RAISE EXCEPTION 'Error adding club contact: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION club_management_noout_add_membership(p_player_name TEXT, p_club_name TEXT) RETURNS VOID AS $$
DECLARE 
    v_person_id INTEGER;
    v_player_id INTEGER;
    v_club_id INTEGER;
BEGIN
    SELECT person_id INTO v_person_id FROM players_concatenated WHERE name = TRIM(p_player_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Player "%" not found.', p_player_name; END IF;
    
    SELECT player_id INTO v_player_id FROM players WHERE person_id = v_person_id;
    
    SELECT club_id INTO v_club_id FROM clubs WHERE name = TRIM(p_club_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Club "%" does not exist.', p_club_name; END IF;
    
    INSERT INTO club_memberships (player_id, club_id) VALUES (v_player_id, v_club_id);
EXCEPTION 
    WHEN unique_violation THEN RAISE EXCEPTION 'Player "%" is already member of club "%".', p_player_name, p_club_name;
    WHEN OTHERS THEN RAISE EXCEPTION 'Error adding membership: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION club_management_noout_remove_membership(p_player_name TEXT, p_club_name TEXT) RETURNS VOID AS $$
DECLARE 
    v_person_id INTEGER;
    v_player_id INTEGER;
    v_club_id INTEGER;
BEGIN
    SELECT person_id INTO v_person_id FROM players_concatenated WHERE name = TRIM(p_player_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Player "%" not found.', p_player_name; END IF;
    
    SELECT player_id INTO v_player_id FROM players WHERE person_id = v_person_id;
    
    SELECT club_id INTO v_club_id FROM clubs WHERE name = TRIM(p_club_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Club "%" does not exist.', p_club_name; END IF;
    
    DELETE FROM club_memberships WHERE player_id = v_player_id AND club_id = v_club_id;
    IF NOT FOUND THEN RAISE EXCEPTION 'Player "%" is not a member of club "%".', p_player_name, p_club_name; END IF;
EXCEPTION 
    WHEN OTHERS THEN RAISE EXCEPTION 'Error removing membership: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION club_management_noout_delete_club(p_club_name TEXT) RETURNS VOID AS $$
DECLARE v_club_id INTEGER;
BEGIN
    SELECT club_id INTO v_club_id FROM clubs WHERE name = TRIM(p_club_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Club "%" does not exist.', p_club_name; END IF;
    
    IF EXISTS (SELECT 1 FROM club_memberships WHERE club_id = v_club_id) THEN
        RAISE EXCEPTION 'Cannot delete club "%" - there are players in the club.', p_club_name;
    END IF;
    
    DELETE FROM club_contact_data WHERE club_id = v_club_id;
    DELETE FROM clubs WHERE club_id = v_club_id;
EXCEPTION 
    WHEN OTHERS THEN RAISE EXCEPTION 'Error deleting club: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

-- ==========================================================================
-- SCREEN 5: TOURNAMENTS
-- ==========================================================================

CREATE OR REPLACE FUNCTION tournament_management_get_tournaments() 
RETURNS TABLE ("ID" INTEGER, "Name" VARCHAR, "City" VARCHAR, "Address" VARCHAR, "Country" VARCHAR, "Format" VARCHAR, "Start Date" DATE, "End Date" DATE) AS $$
BEGIN 
    RETURN QUERY SELECT t.tournament_id, t.name, t.city, t.street_address, c.name, ct.name, t.date_from, t.date_to
    FROM tournaments t 
    JOIN countries c ON (t.country_id = c.country_id)
    JOIN chess_type ct ON (t.chess_type_id = ct.chess_type_id)
    ORDER BY t.date_from DESC;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tournament_management_get_participants(p_tournament_name TEXT) 
RETURNS TABLE ("Player ID" INTEGER, "First Name" VARCHAR(128), "Last Name" VARCHAR(128)) AS $$
DECLARE v_tournament_id INTEGER;
BEGIN
    SELECT tournament_id INTO v_tournament_id FROM tournaments WHERE name = TRIM(p_tournament_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Tournament "%" not found.', p_tournament_name; END IF;
    
    RETURN QUERY SELECT DISTINCT p.person_id, p.first_name, p.last_name
    FROM tournaments t 
    JOIN games g ON (g.tournament_id = t.tournament_id)
    JOIN players pl ON (pl.player_id = g.white_player_id OR pl.player_id = g.black_player_id)
    JOIN persons p ON (pl.person_id = p.person_id)
    WHERE t.tournament_id = v_tournament_id
    ORDER BY p.last_name, p.first_name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tournament_management_noout_create_tournament(
    p_tournament_name TEXT, 
    p_format TEXT, 
    p_city TEXT, 
    p_address TEXT, 
    p_country_name TEXT, 
    p_main_arbiter_name TEXT, 
    p_time_control_name TEXT, 
    p_start_date TEXT, 
    p_end_date TEXT
) RETURNS VOID AS $$
DECLARE 
    v_chess_type_id INTEGER;
    v_country_id INTEGER;
    v_arbiter_person_id INTEGER;
    v_arbiter_id INTEGER;
    v_time_control_id INTEGER;
    v_start_date DATE;
    v_end_date DATE;
BEGIN
    SELECT chess_type_id INTO v_chess_type_id FROM chess_type WHERE name = TRIM(p_format);
    IF NOT FOUND THEN RAISE EXCEPTION 'Chess format "%" not found.', p_format; END IF;
    
    SELECT country_id INTO v_country_id FROM countries WHERE name = TRIM(p_country_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Country "%" does not exist.', p_country_name; END IF;
    
    SELECT person_id INTO v_arbiter_person_id FROM arbiters_concatenated WHERE name = TRIM(p_main_arbiter_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Arbiter "%" not found.', p_main_arbiter_name; END IF;
    
    SELECT arbiter_id INTO v_arbiter_id FROM arbiters WHERE person_id = v_arbiter_person_id;
    
    SELECT time_control_id INTO v_time_control_id FROM time_controls WHERE name = TRIM(p_time_control_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Time control "%" not found.', p_time_control_name; END IF;
    
    BEGIN
        v_start_date := p_start_date::DATE;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid start date format "%". Use format YYYY-MM-DD.', p_start_date;
    END;
    
    BEGIN
        v_end_date := p_end_date::DATE;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid end date format "%". Use format YYYY-MM-DD.', p_end_date;
    END;
    
    IF v_start_date >= v_end_date THEN
        RAISE EXCEPTION 'Start date must be before end date.';
    END IF;
    
    INSERT INTO tournaments (name, chess_type_id, city, street_address, country_id, main_arbiter, time_control_id, date_from, date_to)
    VALUES (TRIM(p_tournament_name), v_chess_type_id, TRIM(p_city), TRIM(p_address), v_country_id, v_arbiter_id, v_time_control_id, v_start_date, v_end_date);
EXCEPTION 
    WHEN unique_violation THEN RAISE EXCEPTION 'Tournament "%" already exists.', p_tournament_name;
    WHEN OTHERS THEN RAISE EXCEPTION 'Error creating tournament: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tournament_management_noout_update_dates(p_tournament_name TEXT, p_new_start_date TEXT, p_new_end_date TEXT) RETURNS VOID AS $$
DECLARE 
    v_tournament_id INTEGER;
    v_start_date DATE;
    v_end_date DATE;
BEGIN
    SELECT tournament_id INTO v_tournament_id FROM tournaments WHERE name = TRIM(p_tournament_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Tournament "%" not found.', p_tournament_name; END IF;
    
    BEGIN
        v_start_date := p_new_start_date::DATE;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid start date format "%". Use format YYYY-MM-DD.', p_new_start_date;
    END;
    
    BEGIN
        v_end_date := p_new_end_date::DATE;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid end date format "%". Use format YYYY-MM-DD.', p_new_end_date;
    END;
    
    IF v_start_date >= v_end_date THEN
        RAISE EXCEPTION 'Start date must be before end date.';
    END IF;
    
    UPDATE tournaments SET date_from = v_start_date, date_to = v_end_date WHERE tournament_id = v_tournament_id;
EXCEPTION 
    WHEN OTHERS THEN RAISE EXCEPTION 'Error updating tournament dates: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tournament_management_noout_delete_tournament(p_tournament_name TEXT) RETURNS VOID AS $$
DECLARE v_tournament_id INTEGER;
BEGIN
    SELECT tournament_id INTO v_tournament_id FROM tournaments WHERE name = TRIM(p_tournament_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Tournament "%" not found.', p_tournament_name; END IF;
    
    IF EXISTS (SELECT 1 FROM games WHERE tournament_id = v_tournament_id) THEN
        RAISE EXCEPTION 'Cannot delete tournament "%" - there are games recorded.', p_tournament_name;
    END IF;
    
    DELETE FROM tournaments WHERE tournament_id = v_tournament_id;
EXCEPTION 
    WHEN OTHERS THEN RAISE EXCEPTION 'Error deleting tournament: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

-- ==========================================================================
-- SCREEN 6: MATCHES & GAMES
-- ==========================================================================

CREATE OR REPLACE FUNCTION match_recording_get_games(p_tournament_name TEXT) 
RETURNS TABLE ("Round" INTEGER, "White Player" VARCHAR, "Black Player" VARCHAR, "Result" text) AS $$
BEGIN 
    RETURN QUERY SELECT round_number, white_player, black_player, result::text
    FROM tournament_results
    WHERE name = TRIM(p_tournament_name)
    ORDER BY g.round_number, g.game_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION match_recording_noout_record_game(
    p_tournament_name TEXT, 
    p_white_player_name TEXT, 
    p_black_player_name TEXT, 
    p_result TEXT, 
    p_date TEXT, 
    p_round TEXT, 
    p_pgn_data TEXT
) RETURNS VOID AS $$
DECLARE 
    v_tournament_id INTEGER;
    v_white_person_id INTEGER;
    v_white_player_id INTEGER;
    v_black_person_id INTEGER;
    v_black_player_id INTEGER;
    v_result MATCH_RESULT;
    v_date DATE;
    v_round INTEGER;
BEGIN
    SELECT tournament_id INTO v_tournament_id FROM tournaments WHERE name = TRIM(p_tournament_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Tournament "%" not found.', p_tournament_name; END IF;
    
    SELECT person_id INTO v_white_person_id FROM players_concatenated WHERE name = TRIM(p_white_player_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'White player "%" not found.', p_white_player_name; END IF;
    
    SELECT player_id INTO v_white_player_id FROM players WHERE person_id = v_white_person_id;
    
    SELECT person_id INTO v_black_person_id FROM players_concatenated WHERE name = TRIM(p_black_player_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Black player "%" not found.', p_black_player_name; END IF;
    
    SELECT player_id INTO v_black_player_id FROM players WHERE person_id = v_black_person_id;
    
    IF v_white_player_id = v_black_player_id THEN
        RAISE EXCEPTION 'A player cannot play against themselves.';
    END IF;
    
    BEGIN
        v_result := p_result::MATCH_RESULT;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid result "%. Valid values: White Wins, Draw, Black Wins, Unplayed.', p_result;
    END;
    
    BEGIN
        v_date := p_date::DATE;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid date format "%". Use format YYYY-MM-DD.', p_date;
    END;
    
    BEGIN
        v_round := p_round::INTEGER;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid round number "%". Must be a positive integer.', p_round;
    END;
    
    IF v_round <= 0 THEN
        RAISE EXCEPTION 'Round number must be positive.';
    END IF;
    
    INSERT INTO games (tournament_id, white_player_id, black_player_id, result, date, round_number, pgn)
    VALUES (v_tournament_id, v_white_player_id, v_black_player_id, v_result, v_date, v_round, NULLIF(TRIM(p_pgn_data), ''));
EXCEPTION 
    WHEN OTHERS THEN RAISE EXCEPTION 'Error recording game: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION match_recording_noout_update_game(p_game_id TEXT, p_result TEXT, p_pgn_data TEXT) RETURNS VOID AS $$
DECLARE 
    v_game_id INTEGER;
    v_result MATCH_RESULT;
BEGIN
    BEGIN
        v_game_id := p_game_id::INTEGER;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid game ID "%". Must be a positive integer.', p_game_id;
    END;
    
    IF NOT EXISTS (SELECT 1 FROM games WHERE game_id = v_game_id) THEN
        RAISE EXCEPTION 'Game ID % not found.', v_game_id;
    END IF;
    
    BEGIN
        v_result := p_result::MATCH_RESULT;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid result "%. Valid values: White Wins, Draw, Black Wins, Unplayed.', p_result;
    END;
    
    UPDATE games SET result = v_result, pgn = NULLIF(TRIM(p_pgn_data), '') WHERE game_id = v_game_id;
EXCEPTION 
    WHEN OTHERS THEN RAISE EXCEPTION 'Error updating game: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION match_recording_noout_delete_game(p_game_id TEXT) RETURNS VOID AS $$
DECLARE v_game_id INTEGER;
BEGIN
    BEGIN
        v_game_id := p_game_id::INTEGER;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid game ID "%". Must be a positive integer.', p_game_id;
    END;
    
    IF NOT EXISTS (SELECT 1 FROM games WHERE game_id = v_game_id) THEN
        RAISE EXCEPTION 'Game ID % not found.', v_game_id;
    END IF;
    
    DELETE FROM games WHERE game_id = v_game_id;
EXCEPTION 
    WHEN OTHERS THEN RAISE EXCEPTION 'Error deleting game: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

-- ==========================================================================
-- SCREEN 7: RATINGS & TITLES
-- ==========================================================================

CREATE OR REPLACE FUNCTION ratings_and_titles_get_live_ratings() 
RETURNS TABLE ("Player ID" INTEGER, "First Name" VARCHAR(128), "Last Name" VARCHAR(128), "Format" VARCHAR, "Rating Value" NUMERIC) AS $$
BEGIN 
    RETURN QUERY SELECT p.person_id, p.first_name, p.last_name, ct.name, lr.value
    FROM live_rating lr
    JOIN players pl ON (lr.player_id = pl.player_id)
    JOIN persons p ON (pl.person_id = p.person_id)
    JOIN chess_type ct ON (lr.chess_type_id = ct.chess_type_id)
    ORDER BY p.last_name, p.first_name, ct.name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ratings_and_titles_get_rating_history(p_player_name TEXT) 
RETURNS TABLE ("Format" VARCHAR, "Rating Value" INTEGER, "Valid From" DATE, "Valid To" DATE) AS $$
DECLARE v_person_id INTEGER;
DECLARE v_player_id INTEGER;
BEGIN
    SELECT person_id INTO v_person_id FROM players_concatenated WHERE name = TRIM(p_player_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Player "%" not found.', p_player_name; END IF;
    
    SELECT player_id INTO v_player_id FROM players WHERE person_id = v_person_id;
    IF NOT FOUND THEN RAISE EXCEPTION 'Person ID % is not a player.', v_person_id; END IF;
    
    RETURN QUERY SELECT ct.name::VARCHAR, rh.value, rh.date_from, rh.date_to 
    FROM rating_history rh JOIN chess_type ct USING (chess_type_id) WHERE rh.player_id = v_player_id ORDER BY rh.date_from DESC;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ratings_and_titles_get_titled_players() 
RETURNS TABLE ("Player ID" INTEGER, "First Name" VARCHAR(128), "Last Name" VARCHAR(128), "Title" VARCHAR, "Date Achieved" DATE) AS $$
BEGIN 
    RETURN QUERY SELECT p.person_id, p.first_name, p.last_name, t.full_name, pt.date_achieved
    FROM players_titles pt 
    JOIN players p ON (p.player_id = pt.player_id)
    JOIN persons per ON (per.person_id = p.person_id)
    JOIN titles t ON (t.short_name = pt.title_short_name)
    ORDER BY per.last_name, per.first_name, t.full_name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ratings_and_titles_noout_award_title(p_player_name TEXT, p_title_full_name TEXT, p_date_achieved TEXT) RETURNS VOID AS $$
DECLARE 
    v_person_id INTEGER;
    v_player_id INTEGER;
    v_title_short_name CHAR(4);
    v_date_achieved DATE;
BEGIN
    SELECT person_id INTO v_person_id FROM players_concatenated WHERE name = TRIM(p_player_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Player "%" not found.', p_player_name; END IF;
    
    SELECT player_id INTO v_player_id FROM players WHERE person_id = v_person_id;
    IF NOT FOUND THEN RAISE EXCEPTION 'Person ID % is not a player.', v_person_id; END IF;
    
    SELECT short_name INTO v_title_short_name FROM titles WHERE full_name = TRIM(p_title_full_name);
    IF NOT FOUND THEN RAISE EXCEPTION 'Title "%" not found.', p_title_full_name; END IF;
    
    BEGIN
        v_date_achieved := p_date_achieved::DATE;
    EXCEPTION WHEN INVALID_TEXT_REPRESENTATION THEN
        RAISE EXCEPTION 'Invalid date format "%". Use format YYYY-MM-DD.', p_date_achieved;
    END;
    
    INSERT INTO players_titles (player_id, title_short_name, date_achieved)
    VALUES (v_player_id, v_title_short_name, v_date_achieved);
EXCEPTION 
    WHEN unique_violation THEN RAISE EXCEPTION 'Player "%" already has title "%".', p_player_name, p_title_full_name;
    WHEN OTHERS THEN RAISE EXCEPTION 'Error awarding title: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;
