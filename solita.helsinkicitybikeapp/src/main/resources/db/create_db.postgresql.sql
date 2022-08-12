-- -*- mode: sql; sql-product: postres; sqlind-minor-mode; -*-
/* Dropping all stuff the new tables require. 
 */

DROP TABLE IF EXISTS stations, station_names, "operators" CASCADE;


/* The function returning the next identifier for a new entry on operators.  
 */
CREATE OR REPLACE FUNCTION get_next_opid ()
    RETURNS int
    AS $$
DECLARE
    result int;
BEGIN
    LOCK TABLE operators IN SHARE ROW EXCLUSIVE MODE;
    SELECT
        MAX(opid) INTO result
    FROM
        operators;
    IF NOT FOUND OR result IS NULL THEN
        -- DEBUG
        RAISE NOTICE 'Adding first opid';
        RETURN 1;
    ELSE
        RETURN result + 1;
    END IF;
END;
$$
LANGUAGE PLPGSQL;


/* Operators. 
 * This table is minimalistic at the moment as operator has no additional data except default 
 * language of the operator. 
 */
CREATE TABLE IF NOT EXISTS operators (
    opid smallint PRIMARY KEY DEFAULT get_next_opid (),
    operator_name varchar(80) NOT NULL UNIQUE,
    lang varchar(80) DEFAULT 'fi'
);

-- The method performing necessary operations after the operators has been altered.
-- This method ensures that the operation_names table contains the primary name of the
-- operator.
CREATE OR REPLACE FUNCTION operators_altered_trigger ()
    RETURNS TRIGGER
    AS $$
BEGIN
    LOCK TABLE localized_operator_names IN SHARE ROW EXCLUSIVE MODE;
    IF (TG_OP = 'INSERT' OR TG_OP = 'UPDATE') THEN
        -- Inserting or Adding requires that the new primary operator name exists in the localized_operator_names.
        IF (NOT EXISTS (
            SELECT
                operator_id
            FROM
                localized_operator_names
            WHERE
                operator_id = NEW.opid AND localized_operator_names.lang = NEW.lang)) THEN
            -- Creating new default value entry.
            INSERT INTO localized_operator_names (operator_id, lang, name)
                VALUES (NEW.opid, NEW.lang, NEW.name);
            IF NOT FOUND THEN
                -- Could not create new primary name for the operator
                RAISE EXCEPTION 'Could not add the new name %1 with id %2 into table localized_operator_names', NEW.operator_name, NEW.name_id;
            END IF;
        END IF;
    ELSIF (TG_OP = 'DELETE') THEN
        -- Nothing to do as foreign key constraint on the operator_name table removes all references to the current operator,
        -- and separate trigger deals with name id update.
    END IF;
    IF (TG_OP = 'DELETE') THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$
LANGUAGE plpgsql;

-- Adding the trigger updating the localized_operator_names table accordingly after the operators table has been updated.
-- - This trigger is called before the operator counts are updated.
CREATE OR REPLACE TRIGGER operators_altered_trigger AFTER INSERT
    OR UPDATE
    OR DELETE ON operators FOR EACH ROW EXECUTE FUNCTION operators_altered_trigger ();


/* The names of the operators cascaded. 
 */
CREATE TABLE IF NOT EXISTS localized_operator_names (
    operator_id smallint NOT NULL,
    name varchar(80),
    lang varchar(80) NOT NULL,
    PRIMARY KEY (operator_id, lang),
    FOREIGN KEY (operator_id) REFERENCES operators (opid) ON DELETE CASCADE ON UPDATE CASCADE
);


/* Create view of operator names. 
 */
CREATE OR REPLACE VIEW names_of_operators AS
SELECT
    operator_id,
    localized_operator_names.name AS operator_name,
    localized_operator_names.lang AS lang,
    localized_operator_names.lang = operators.lang AS default_lang
FROM
    localized_operator_names
    JOIN operators ON opid = operator_id;

-- The function to deal with updates on the view names_of_operators updates.
CREATE OR REPLACE FUNCTION alter_names_of_operators_view ()
    RETURNS TRIGGER
    AS $$
  DECLARE
    qresult RECORD; 
BEGIN
  LOCK TABLE operators, localized_operator_names IN SHARE ROW EXCLUSIVE MODE;
  
  IF (TG_OP = 'DELETE') THEN
    IF (OLD.default_lang) THEN 
      -- Operator cannot be performed for the primary language. 
      RAISE EXCEPTION 'Cannot delete primary language of %', OLD.operator_id; 
    ELSE 
      -- Deleting the row on localized_operator_names
			DELETE FROM localized_operator_names WHERE operator_id = OLD.operator_id AND lang = OLD.lang;
      IF NOT FOUND THEN RETURN NULL; END IF;
    END IF;
    -- If we get here, operation is okay. Returning the old record. 
    RETURN OLD; 
  ELSIF (TG_OP = 'UPDATE') THEN
    -- Testing if we have something to do 
    if (NEW.operator_id IS NULL) THEN
      -- TODO: seek proper error code to represent FOREIGN KEY constraint failure. 
      RAISE EXCEPTION 'Cannot set operator_id to undefiend value';
    ELSIF (OLD.operator_id <> NEW.operator_id) THEN
      RAISE EXCEPTION 'Cannot alter the operator_id';
    END IF; 
    
    if (OLD.default_lang AND NOT NEW.default_lang) then
      -- Deselecting operation language is not accepted
      RAISE EXCEPTION 'Cannot disable default language'; 
    end if;
    
    -- Performing the update. 
    UPDATE localized_operator_names SET (operator_id, lang, name_id) = (NEW.operator_id, NEW.lang, 
									get_nameid(NEW.operator_name)); 
    IF NOT FOUND THEN RETURN NULL; END IF; 
    
    -- The update succeeded. Updating the operators table. 
    IF (NEW.default_language) THEN
      -- Updating the default language. 
      UPDATE operators SET operator_name=NEW.operator_name, lang=NEW.lang, name_id=get_nameid(NEW.operator_name) WHERE opid=OLD.operator_id; 
    END IF; 
    
    -- Updating the name counts and returning the result.
    RETURN NEW; 
  ELSIF (TG_OP = 'INSERT') THEN
    -- Testing if the operator identifier exists. 
    IF (NEW.operator_id IS NULL) THEN
      -- Adding new operator
      BEGIN 
	LOCK TABLE operators IN SHARE ROW EXCLUSIVE MODE;
	NEW.operator_id = get_next_opid(); 
	RAISE NOTICE 'Creating operator %', NEW.operator_id; 
	INSERT INTO operators (opid, operator_name, lang) VALUES (NEW.operator_id, NEW.operator_name, NEW.lang); 
	IF NOT FOUND THEN 
	  RAISE EXCEPTION 'Could not create new operator';
	END IF;
      END;
    ELSE 
      -- Performing the insertion
      INSERT INTO localized_operator_names (operator_id, lang, name_id) 
      VALUES (NEW.operator_id, NEW.lang, get_name_id(NEW.operator_name, TRUE)); 
      IF NOT FOUND THEN 
	RETURN NULL; 
      END IF;

      
      -- Updating the values of other tables. 
      IF (NEW.default_language) THEN
	-- Updating the default language and company name of the operators. 
	UPDATE operators SET operator_name=NEW.operator_name, lang=NEW.lang WHERE opid=NEW.operator_id; 
      END IF; 
    END IF; 
    

    -- Increasing the name count and returning the result.
    RETURN NEW; 
  END IF;
END;
$$
LANGUAGE plpgsql;

-- Registering the trigger handling the alteration of naems of operators view.
CREATE TRIGGER names_of_operators_alteration
    INSTEAD OF INSERT OR UPDATE OR DELETE ON names_of_operators
    FOR EACH ROW
    EXECUTE FUNCTION alter_names_of_operators_view ();

-- To acquire default next station id.
CREATE OR REPLACE FUNCTION get_next_station_id ()
    RETURNS smallint
    AS $$
DECLARE
    result smallint;
BEGIN
    LOCK TABLE operators IN SHARE MODE;
    SELECT
        MAX(sid) INTO result
    FROM
        operators;
    IF NOT FOUND OR result IS NULL THEN
        RETURN 1;
    ELSE
        RETURN result + 1;
    END IF;
END;
$$
LANGUAGE PLPGSQL;


/* The stations table containing station info. 
 */
CREATE TABLE stations (
    id smallserial PRIMARY KEY,
    sid smallint NOT NULL UNIQUE DEFAULT get_next_station_id (),
    operator_id smallint NOT NULL,
    lang varchar(80) DEFAULT 'fi',
    capacity smallint DEFAULT 0,
    x decimal(9, 6),
    y decimal(9, 6),
    FOREIGN KEY (operator_id) REFERENCES operators (opid) ON DELETE SET NULL ON UPDATE CASCADE
);


/* The view of station names. 
 */
CREATE TABLE IF NOT EXISTS station_names (
    station_id smallint NOT NULL DEFAULT get_next_station_id (),
    lang varchar(20) NOT NULL DEFAULT 'fi',
    name varchar(80) NOT NULL,
    PRIMARY KEY (station_id, lang),
    FOREIGN KEY (station_id) REFERENCES stations (sid) ON DELETE CASCADE ON UPDATE CASCADE
);


/*
The view of the names of stations indicating also the default language. 
 */
CREATE OR REPLACE VIEW names_of_stations AS
SELECT
    station_id,
    station_names.lang AS lang,
    station_names.name AS station_name,
    station_names.lang = stations.lang AS default_lang
FROM
    station_names
    JOIN stations ON station_id = stations.sid;


/* The view station information hiding the automatically generated id from users. 
 */
CREATE OR REPLACE VIEW station_info AS
SELECT
    stations.id AS jid,
    sid,
	opid, 
    operator_name,
    capacity,
    x,
    y,
    stations.lang as lang,
    name AS station_name
FROM
    stations
    LEFT JOIN operators ON stations.operator_id = operators.opid
    LEFT JOIN station_names ON station_names.station_id = sid
        AND station_names.lang = stations.lang;

-- The function to deal with updats on the view names_of_operators updates.
CREATE OR REPLACE FUNCTION alter_station_info_view ()
    RETURNS TRIGGER
    AS $$
DECLARE
	qresult RECORD;
	operator_id smallint; 
	lang varchar(80); 
BEGIN
	LOCK TABLE operators, stations IN SHARE ROW EXCLUSIVE MODE;
		
	IF (TG_OP = 'DELETE') THEN
		RAISE NOTICE 'Deleting row %s from stations', OLD.sid; 
		DELETE FROM stations WHERE stations.sid = OLD.sid; 
		IF NOT FOUND THEN 
			RAISE NOTICE USING MESSAGE = format('Deletion of row %I failed', OLD.sid); 
			RETURN NULL; 
		ELSE
		RAISE NOTICE 'Row %s deleted from stations', OLD.sid; 
			RETURN OLD; 
		END IF;
	ELSIF (TG_OP = 'UPDATE') THEN
		IF NEW.jid <> OLD.jid THEN 
			RAISE EXCEPTION 'The jid of the station info is read only';
		END IF; 
		-- Fetching the default language of the station. 
		BEGIN
		SELECT stations.lang INTO STRICT lang FROM stations WHERE stations.sid = OLD.sid; 
		EXCEPTION
			WHEN NO_DATA_FOUND THEN 
				RAISE EXCEPTION USING MESSAGE = 'Cannot update to non-existing station';
		END; 
		
		-- Fetching the operator identifier of the new operator. 
		BEGIN
		SELECT opid INTO STRICT operator_id FROM operators WHERE operators.operator_name = NEW.operator_name; 
		EXCEPTION
			WHEN NO_DATA_FOUND THEN 
				-- Adding new operator
				INSERT INTO operators (operator_name, lang) VALUES (NEW.operator_name, lang); 
				IF FOUND THEN 
					RAISE NOTICE 'Created new operator'; 
					SELECT opid INTO STRICT operator_id FROM operators WHERE operators.operator_name = NEW.operator_name; 
				ELSIF NEW.operator_name IS NULL THEN
					RAISE EXCEPTION USING MESSAGE = 'Could not add operator with undefined name';
				ELSE 
					RAISE EXCEPTION USING MESSAGE = 'Could not add new operator ' || quote_nullable(NEW.operator_name);				
				END IF; 
			WHEN TOO_MANY_ROWS THEN
				RAISE EXCEPTION USING MESSAGE = FORMAT('Operators have several companies with same name %L', (NEW.operator_name));
		END;
		RAISE NOTICE USING MESSAGE = FORMAT('Found operator identifier %L', operator_id); 
		
		-- Updating the stations table setting the operator of the station. 
		RAISE NOTICE 'Updating row %s on stations', NEW.sid; 
		UPDATE stations SET stations.sid = NEW.sid, stations.operator_id = operator_id, stations.lang=lang, 
		stations.capacity=NEW.capacity, stations.x=NEW.x, stations.y=New.y WHERE stations.sid = station_info.sid; 
		IF NOT FOUND THEN RETURN NULL; END IF;
		
		-- Updating the operators table. 
		RAISE NOTICE FORMAT('Updating row %s on operators', operator_id); 
		UPDATE operators SET operator_name=NEW.operator_name WHERE opid=operator_id; 
		IF NOT FOUND THEN 
			RAISE EXCEPTION FORMAT('Could not change the operator name of operator %L', NEW.operator_id); 
		END IF; 
		RAISE NOTICE FORMAT('Updated operator %s name to %s', NEW.operator_id, NEW.operator_name); 
		
		-- Checking if the new entry already exists. 
		IF (NEW.lang <> OLD.lang) THEN
			-- The language is altered.
			-- Updating the entry of the new language on the stations info. 
			UPDATE station_names SET station_name = NEW.station_name, station_names.lang = NEW.lang WHERE station_id = NEW.sid AND station_names.lang = NEW.lang; 
			IF NOT FOUND THEN 
				-- Inserting new entry for new langauge. 
				INSERT INTO station_names(operator_id, lang, name) VALUES (NEW.operator_id, NEW.lang, NEW.name); 
				IF NOT FOUND THEN 
					RAISE EXCEPTION USING MESSAGE=FORMAT('Could not change the station name of station %L', OLD.sid); 
				END IF; 
			ELSE 
				-- All is fine. 
			END IF; 
		ELSE 
			-- The language does not change, thus updating existing entry. 
			-- Trying to update existing station_names table entry. 
			RAISE NOTICE format('Updating row %, % on station names', operator_id, lang); 
			UPDATE station_names SET station_id=NEW.sid, station_name = NEW.station_name WHERE station_id = NEW.sid; 
			IF NOT FOUND THEN 
				-- Inserting new row to the station_names. 
				INSERT INTO station_names(operator_id, lang, name) VALUES (NEW.operator_id, NEW.lang, NEW.name); 
				IF NOT FOUND THEN 
					RAISE EXCEPTION 'Could not change the station name of station %', OLD.sid; 
				END IF; 
			END IF; 
		END IF; 
		
		-- Updating the name counts and returning the result.
		RETURN NEW; 
	ELSIF (TG_OP = 'INSERT') THEN
		-- Testing if the operator identifier exists. 
		IF NEW.jid IS NOT NULL THEN RAISE EXCEPTION 'Cannot specify jid on insertion'; END IF;
		BEGIN
		SELECT opid INTO STRICT operator_id FROM operators WHERE operators.operator_name = NEW.operator_name; 
		EXCEPTION
			WHEN NO_DATA_FOUND THEN 
				-- Adding new operator
				INSERT INTO operators (operator_name) VALUES (NEW.operator_name); 
				IF FOUND THEN 
					SELECT opid INTO STRICT operator_id FROM operators WHERE operators.operator_name = NEW.operator_name; 
				ELSE 
					RAISE EXCEPTION 'Could not add new operator';
				END IF; 
			WHEN TOO_MANY_ROWS THEN
				RAISE EXCEPTION 'Operators have several companies with same name %', NEW.operator_name;
		END;

		RAISE NOTICE 'Inserting row % on stations', NEW.sid; 
		INSERT INTO stations(sid, operator_id, capacity, x, y) VALUES (NEW.sid, operator_id, NEW.capacity, NEW.x, NEW.y);
		IF NOT FOUND THEN RAISE EXCEPTION 'Could not add new station'; END IF;
		SELECT stations.lang INTO STRICT lang FROM stations WHERE stations.sid=NEW.sid; 

		RAISE NOTICE 'Updating row % on station_names', (format('(%L,%L)', NEW.sid, lang)); 
		INSERT INTO station_names(station_id, lang, name) VALUES (NEW.sid, lang, NEW.station_name); 

		RAISE NOTICE 'Created station info %', format('%', NEW);

		-- Increasing the name count and returning the result.
		RETURN NEW; 
	END IF;
END;
$$
LANGUAGE plpgsql;

-- Adding trigger updating the name counts after changes on operator names table.
CREATE OR REPLACE TRIGGER update_station_info_view INSTEAD OF INSERT
    OR UPDATE
    OR DELETE ON station_info FOR EACH ROW EXECUTE FUNCTION alter_station_info_view ();

-- Adding the journeys table.
CREATE TABLE journeys (
    departure_time timestamp NOT NULL,
    arrival_time timestamp DEFAULT NULL,
    jid serial PRIMARY KEY NOT NULL,
    departure_station_id smallint NOT NULL,
    arrival_station_id smallint,
    duration smallint,
    distance smallint,
	departure_station_name VARCHAR, 
	arrival_station_name VARCHAR, 
    constraint valid_departure_station_id 
	FOREIGN KEY (departure_station_id) REFERENCES stations (sid) ON UPDATE CASCADE ON DELETE CASCADE,
    constraint valid_arrival_station_id 
	FOREIGN KEY (arrival_station_id) REFERENCES stations (sid) ON UPDATE CASCADE ON DELETE CASCADE
);

-- The function to deal with updats on the view names_of_operators updates.
CREATE OR REPLACE FUNCTION alter_journeys_trigger ()
    RETURNS TRIGGER
    AS $BODY$
DECLARE
	station_name VARCHAR; 
BEGIN
    IF T_OP = 'INSERT' OR T_OP = 'UPDATE' THEN
        -- Insertting a new record or altering an existing record.
        IF NEW.departure_station_id IS NOT NULL THEN
            SELECT
                name into station_name
            FROM
                station_names
            WHERE
                station_id = NEW.departure_station_id AND 
				name = NEW.departure_station_name;
			IF NOT FOUND THEN RETURN NULL; END IF; 
        END IF;
        IF NEW.arrival_station_id IS NOT NULL AND (NEW.arrival_station_name IS NULL OR 
        NOT EXISTS (SELECT name FROM station_names 
					WHERE station_id = NEW.arrival_station_id) AND name = NEW.station_name) THEN
            -- Aleration is not accepted.
            RETURN NULL;
        END IF;

        -- The alteration is accepted. 
    ELSE
        -- Deleting record - no constrait checks are performed as the fields are always correct
    END IF;
    -- Returning default result.
    IF T_OP = 'INSERT' OR T_OP = 'ALTER' THEN
        RETURN NEW;
    ELSE
        RETURN OLD;
    END IF;
END;
$BODY$
LANGUAGE plpgsql;

-- Adding the trigger updating the localized_operator_names table accordingly after the operators table has been updated.
-- - This trigger is called before the operator counts are updated.
CREATE OR REPLACE TRIGGER journeys_altering_trigger BEFORE INSERT
    OR UPDATE
    OR DELETE ON journeys FOR EACH ROW EXECUTE FUNCTION alter_journeys_trigger ();

