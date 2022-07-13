/* Creating tables, if they do not exist */

/* Stations is created first as the journeys has foreign key link to the stations. 
*/

CREATE TABLE stations IF NOT EXISTS 
    sid smallint DEFAULT MAX(sid)+1 COMMENT 'Identifier number of the stations',
    name varchar(80) COMMENT 'Name of the station',
    PRIMARY KEY sid,
    UNIQUE name;

/* Stations is created first as the journeys has foreign key link to the stations. 
*/

CREATE TABLE IF NOT EXISTS journeys
    jid integer PRIMARY KEY,
    departure_time TIMESTAMP NOT NULL,
    return_time TIMESTAMP, 
    departure_station_id SMALLINT NOT NULL,
    return_station_id SMALLINT, 
    distance INTEGER COMMENT 'Distance of the use during lease in meters', 
    duration INTEGER COMMENT 'Duration of the lease in seconds'
    PRIMARY KEY (jid),
    FOREIGN KEY (departure_station_id) ON UPDATE CASCADE
    FOREIGN KEY (return_station_id) ON UPDATE CASCADE;

/* Creating the views. */

DROP VIEW IF EXISTS CompleteJourneys; 
DROP VIEW IF EXISTS Arrivals; 
DROP VIEW IF EXISTS Departures; 

/*
Departures could contain the expired time, but I decided not to implement it. 
*/

CREATE VIEW Departures AS 
SELECT journeys.jid,
    journeys.departure_time,
    journeys.departure_station_id,
    stations.name AS departure_station_name
FROM journeys
JOIN stations ON journeys.departure_station_id = stations.sid;

/*
The distance travelled during lease, and the duration of the lease belongs
to the arrivals view allowing future redefinition of the table structure according
to the normal forms storing all arrival information into arrival table linked to the
journeys. 
*/

CREATE VIEW Arrivals AS 
SELECT journeys.jid,
    journeys.return_time,
    journeys.return_station_id,
    stations.name AS return_station_name,
    journeys.duration,
    journeys.distance
FROM journeys
JOIN stations ON journeys.return_station_id = stations.sid;


/*
The complete journeys view contains all journeys with both arrival and return time. 
*/
CREATE VIEW CompleteJourneys AS
SELECT d.jid,
    d.departure_time,
    r.return_time,
    d.departure_station_id,
    d.departure_station_name,
    r.return_station_id,
    r.return_station_name,
    r.duration,
    r.distance
FROM "Departures" d
JOIN "Arrivals" r ON d.jid = r.jid;

/*
The view listing complete journeys with errors. 
*/
CREATE VIEW InvalidJourneys AS
SELECT d.jid, 
    d.departure_time, 
    r.return_time, 
    d.departure_station_name, 
    r.return_station_id,
    r.return_station_name,
    r.duration,
    r.distance,
    case 
    when return_time is null then 'Unknown return time'
    when departure_time > return_time Then 'Time travel'
    when duration < 0 then 'Negative duration'
    when distance < 0 then 'Negative distance'
    else 'Conflicting duration'
FROM "Departures" d
JOIN "Arrivals" r ON d.jid = r.jid
WHERE 
    return_time IS NULL OR d.departure_time > r.return_time OR r.duration < 0 OR r.distance < 0
    OR duration <> extract('epoch' from (return_time - departure_time));