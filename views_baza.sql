--Current contact data
CREATE VIEW current_contact_data AS 
SELECT p.person_id, p.first_name, p.last_name, pcd.mail_address, pcd.phone_number
FROM persons p JOIN person_contact_data pcd ON(p.person_id = pcd.person_id)
WHERE pcd.timestamp_to IS NULL;

--Tournament schedule
CREATE VIEW tournament_schedule AS
SELECT t.tournament_id, t.name AS tournament_name, g.round_number, pw.first_name || ' ' || pw.last_name AS white_player, pb.first_name || ' ' || pb.last_name AS black_player, g.result
FROM games g JOIN tournaments t ON (t.tournament_id = g.tournament_id)
JOIN players wp ON (wp.player_id = g.white_player_id)
JOIN persons pw ON (pw.person_id = wp.person_id)
JOIN players bp ON (bp.player_id = g.black_player_id)
JOIN persons pb ON (pb.person_id = bp.person_id);

--Players with titles
CREATE VIEW titled_players AS
SELECT p.player_id, per.first_name, per.last_name, pt.title_short_name, t.full_name, pt.date_achieved
FROM players_titles pt JOIN players p ON (p.player_id = pt.player_id)
JOIN persons per ON (per.person_id = p.person_id)
JOIN titles t ON (t.short_name = pt.title_short_name);

--View of tournament results
CREATE VIEW tournament_results AS
SELECT t.tournament_id, t.name AS tournament_name, g.round_number, pw.first_name || ' ' || pw.last_name AS white_player, pb.first_name || ' ' || pb.last_name AS black_player,
CASE WHEN g.result = 1 THEN '1-0'
     WHEN g.result = 0.5 THEN '1/2-1/2'
     WHEN g.result = 0 THEN '0-1'
     ELSE 'Not played'
END AS result
FROM games g JOIN tournaments t ON (t.tournament_id = g.tournament_id)
JOIN players wp ON (wp.player_id = g.white_player_id)
JOIN persons pw ON (pw.person_id = wp.person_id)
JOIN players bp ON (bp.player_id = g.black_player_id)
JOIN persons pb ON (pb.person_id = bp.person_id);

--Particippants of the tournament
CREATE VIEW tournament_participants AS
SELECT DISTINCT t.tournament_id, t.name, p.player_id, per.first_name, per.last_name
FROM tournaments t JOIN games g ON (g.tournament_id = t.tournament_id)
JOIN players p ON (p.player_id = g.white_player_id OR p.player_id = g.black_player_id)
JOIN persons per ON (per.person_id = p.person_id);

--Alocating players for round 1
CREATE VIEW round_1 AS WITH players_ordered AS (
   SELECT p.player_id, ROW_NUMBER() OVER (ORDER BY p.player_id) AS seed,
   COUNT(*) OVER () AS total_players FROM players p)
SELECT per1.first_name || ' ' || per1.last_name AS player_one, per2.first_name || ' ' || per2.last_name AS player_two
FROM players_ordered p1 JOIN players_ordered p2 ON (p1.seed + p2.seed = p1.total_players + 1)
JOIN players pl1 ON (pl1.player_id = p1.player_id)
JOIN persons per1 ON (per1.person_id = pl1.person_id)
JOIN players pl2 ON (pl2.player_id = p2.player_id)
JOIN persons per2 ON (per2.person_id = pl2.person_id)
WHERE p1.seed < p2.seed;

