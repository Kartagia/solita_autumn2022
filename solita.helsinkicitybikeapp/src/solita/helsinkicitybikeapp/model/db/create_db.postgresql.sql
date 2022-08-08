-- -*- mode: sql; sql-product: postres; sqlind-minor-mode; -*- 
/* Dropping all stuff the new tables require. 
*/
DROP VIEW IF EXISTS "invalid_journeys", "completed_journeys", "departures", "arrivals";
DROP TABLE IF EXISTS stations, station_names, "operators" CASCADE; 

/* The function returning the next identifier for a new entry on operators.  
*/
CREATE OR REPLACE FUNCTION get_next_opid() RETURNS INT AS $$
DECLARE
	result INT; 
BEGIN
	LOCK TABLE operators IN SHARE ROW EXCLUSIVE MODE; 
	SELECT MAX(opid) INTO result FROM operators;
	IF NOT FOUND OR result IS NULL THEN
	       -- DEBUG
		RAISE NOTICE 'Adding first opid';
		RETURN 1; 
	ELSE 
		RETURN result+1;
	END IF; 
END; 
$$ LANGUAGE PLPGSQL ; 

/* Operators. 
* This table is minimalistic at the moment as operator has no additional data except default 
* language of the operator. 
*/
CREATE TABLE IF NOT EXISTS operators(
    opid smallint PRIMARY KEY DEFAULT get_next_opid(), 
    operator_name varchar(80) NOT NULL UNIQUE, 
    lang varchar(80) DEFAULT 'fi'
);


-- The method performing necessary operations after the operators has been altered.
-- This method ensures that the operation_names table contains the primary name of the 
-- operator. 
CREATE OR REPLACE FUNCTION operators_altered_trigger() RETURNS TRIGGER AS $$
BEGIN
  LOCK TABLE localized_operator_names IN SHARE ROW EXCLUSIVE MODE; 
  IF (TG_OP = 'INSERT' OR TG_OP = 'UPDATE') THEN
    -- Inserting or Adding requires that the new primary operator name exists in the localized_operator_names. 
    IF (NOT EXISTS (SELECT operator_id FROM localized_operator_names WHERE operator_id = NEW.opid AND localized_operator_names.lang = NEW.lang)) THEN 
      -- Creating new default value entry. 
      INSERT INTO localized_operator_names (operator_id, lang, name) VALUES (NEW.opid, NEW.lang, NEW.name);
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
$$ LANGUAGE plpgsql; 



-- Adding the trigger updating the localized_operator_names table accordingly after the operators table has been updated.
-- - This trigger is called before the operator counts are updated. 
CREATE OR REPLACE TRIGGER operators_altered_trigger AFTER INSERT OR UPDATE OR DELETE ON operators
  FOR EACH ROW EXECUTE FUNCTION operators_altered_trigger(); 

/* The names of the operators cascaded. 
*/
CREATE TABLE IF NOT EXISTS localized_operator_names(
  operator_id SMALLINT NOT NULL,
  name varchar(80), 
  lang varchar(80) NOT NULL,
  PRIMARY KEY (operator_id, lang),
  FOREIGN KEY (operator_id) REFERENCES operators (opid) ON DELETE CASCADE ON UPDATE CASCADE); 


/* Create view of operator names. 
 */
CREATE OR REPLACE VIEW names_of_operators AS 
  SELECT operator_id, localized_operator_name as operator_name, localized_localized_operator_names.lang, 
	 localized_operator_names.lang = operators.lang as default_lang
    FROM localized_operator_names 
	 JOIN operators ON opid = operator_id; 

-- The function to deal with updates on the view names_of_operators updates.  
CREATE OR REPLACE FUNCTION alter_names_of_operators_view() RETURNS TRIGGER AS $$
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
$$ LANGUAGE plpgsql;

CREATE TRIGGER names_of_operators_alteration
INSTEAD OF INSERT OR UPDATE OR DELETE ON names_of_operators
 FOR EACH ROW EXECUTE FUNCTION alter_names_of_operators_view();

-- To acquire default next station id. 
CREATE OR REPLACE FUNCTION get_next_station_id() RETURNS smallint AS $$
DECLARE
	result smallint; 
BEGIN
	LOCK TABLE operators IN SHARE MODE; 
	SELECT MAX(sid) INTO result FROM operators;
	IF NOT FOUND OR result IS NULL THEN 
		RETURN 1; 
	ELSE 
		RETURN result+1;
	END IF; 
END; 
$$ LANGUAGE PLPGSQL ; 


/* The stations table containing station info. 
*/
CREATE TABLE stations (
	id smallserial PRIMARY KEY,
	sid smallint NOT NULL UNIQUE DEFAULT get_next_station_id(),
    operator_id smallint NOT NULL,
    lang varchar(80) DEFAULT 'fi',
	capacity smallint default 0,
    x decimal(9,6), 
    y decimal(9,6),
    FOREIGN KEY (operator_id) REFERENCES operators(opid) ON DELETE SET NULL ON UPDATE CASCADE
);



/* The view of station names. 
*/
CREATE TABLE IF NOT EXISTS station_names ( 
    station_id smallint NOT NULL DEFAULT get_next_station_id(), 
    lang varchar(20) NOT NULL DEFAULT 'fi', 
	name varchar(80) NOT NULL,
    PRIMARY KEY (station_id, lang), 
    FOREIGN KEY (station_id) REFERENCES stations (sid) ON DELETE CASCADE ON UPDATE CASCADE);


/* The view of station names by language. 
*/
CREATE OR REPLACE VIEW station_names_view AS 
SELECT stations.sid as station_id, station_names.lang, namelist.entry as station_name 
FROM station_names 
JOIN namelist ON station_names.name_id = namelist.nid
JOIN stations ON stations.id = station_names.station_id; 

/* The view station information hiding the automatically generated id from users. 
*/
CREATE OR REPLACE VIEW station_info AS 
SELECT stations.id as jid, sid, operator_name, capacity, x, y, station_names.lang, name as station_name from stations join operators ON stations.operator_id=operators.opid 
JOIN station_names ON station_names.station_id = sid AND station_names.lang = stations.lang;

-- The function to deal with updats on the view names_of_operators updates.  
CREATE OR REPLACE FUNCTION alter_station_info_view() RETURNS TRIGGER AS $$
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
			RAISE NOTICE 'Deletion of row % failed', OLD.sid; 
			RETURN NULL; 
		ELSE
		RAISE NOTICE 'Row %s deleted from stations', OLD.sid; 
			RETURN OLD; 
		END IF;
	ELSIF (TG_OP = 'UPDATE') THEN
		IF NEW.jid <> OLD.jid THEN 
			RAISE EXCEPTION 'The jid of the station info is read only';
		END IF; 

		-- Fetching the new operator_id for the statations and operators table. 
		BEGIN
		SELECT stations.lang INTO STRICT lang FROM stations WHERE stations.sid = OLD.sid; 
		EXCEPTION
			WHEN NO_DATA_FOUND THEN 
				RAISE EXCEPTION 'Cannot update to non-existing station';
		END; 

		BEGIN
		SELECT opid INTO STRICT operator_id FROM operators WHERE operators.operator_name = NEW.operator_name; 
		EXCEPTION
			WHEN NO_DATA_FOUND THEN 
				-- Adding new operator
				INSERT INTO operators (operator_name, lang) VALUES (NEW.operator_name, lang); 
				IF FOUND THEN 
					RAISE NOTICE 'Created new operator'; 
					SELECT opid INTO STRICT operator_id FROM operators WHERE operators.operator_name = NEW.operator_name; 
				ELSE 
					RAISE EXCEPTION 'Could not add new operator';
				END IF; 
			WHEN TOO_MANY_ROWS THEN
				RAISE EXCEPTION 'Operators have several companies with same name %', quote_nullable(NEW.operator_name);
		END;
		RAISE NOTICE 'Found operator identifier %', operator_id; 
		
		RAISE NOTICE 'Updating row %s on stations', NEW.sid; 
		UPDATE stations SET stations.sid = NEW.sid, stations.operator_id = operator_id, stations.lang=lang, 
		stations.capacity=NEW.capacity, stations.x=NEW.x, stations.y=New.y WHERE stations.sid = station_info.sid; 
		IF NOT FOUND THEN RETURN NULL; END IF;
		
		RAISE NOTICE 'Updating row %s on operators', operator_id; 
		UPDATE operators SET operator_name=NEW.operator_name WHERE opid=operator_id; 
		IF NOT FOUND THEN 
			RAISE EXCEPTION 'Could not change the operator name of operator %', NEW.operator_id; 
		END IF; 
		RAISE NOTICE 'Updated operator % name to %', NEW.operator_id, NEW.operator_name; 
		
		-- Trying to update existing 
		RAISE NOTICE 'Updating row %, % on station names', operator_id, lang; 
		UPDATE station_names SET station_id=NEW.sid, name_id = get_nameid(NEW.station_name) WHERE station_id = OLD.sid; 
		IF NOT FOUND THEN 
			-- Inserting new row to the station names. 
			INSERT INTO station_names(operator_id, lang, name) VALUES (NEW.operator_id, NEW.lang, NEW.name); 
			IF NOT FOUND THEN 
				RAISE EXCEPTION 'Could not change the station name of station %', OLD.sid; 
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
$$ LANGUAGE plpgsql;

-- Adding trigger updating the name counts after changes on operator names table. 
CREATE OR REPLACE TRIGGER update_station_info_view INSTEAD OF INSERT OR UPDATE OR DELETE ON station_info
	FOR EACH ROW EXECUTE FUNCTION alter_station_info_view(); 
