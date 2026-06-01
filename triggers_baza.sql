----Player cannot play against himself
--
--CREATE OR REPLACE FUNCTION dont_self_play()
--RETURNS TRIGGER AS
--$$
--BEGIN
--   IF NEW.white_player_id = NEW.black_player_id THEN RAISE EXCEPTION 'A player cannot play against themselves.';
--   END IF;
--   RETURN NEW;
--END;
--$$
--LANGUAGE plpgsql;
--
--CREATE TRIGGER trigger_dont_self_play
--BEFORE INSERT OR UPDATE ON games
--FOR EACH ROW EXECUTE FUNCTION dont_self_play();

-- I see no reason for this not to be a check. After discussing I commented this out ~sara.



--Monitoring contact history: when a new concact data is added, we close previous record

CREATE OR REPLACE FUNCTION close_previous_contact()
RETURNS TRIGGER AS
$$
BEGIN 
   UPDATE person_contact_data SET timestamp_to = CURRENT_DATE
   WHERE person_id = NEW.person_id AND timestamp_to IS NULL;
   RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trigger_close_previous_contact 
BEFORE INSERT ON person_contact_data
FOR EACH ROW EXECUTE FUNCTION close_previous_contact();

--Block deleting finished games

CREATE RULE dont_delete_games AS
ON DELETE TO games DO INSTEAD NOTHING;

