package solita.helsinkicitybikeapp.model.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;

import solita.helsinkicitybikeapp.model.Journeys;
import solita.helsinkicitybikeapp.model.Journeys.Journey;

/**
 * The database using Journeys.
 * 
 * @author Antti Kautiainen
 *
 */
public class DatabaseJourneys extends Journeys {

	public class Journey extends Journeys.Journey {

		public Journey(ResultSet dbRow) throws SQLException {
			super();
		}
	}

	/**
	 * The connection to the Journeys database.
	 */
	private java.sql.Connection journeysDB = null;

	/**
	 * The connection to the database.
	 * 
	 * @return The database connection storing the journeys.
	 */
	protected java.sql.Connection getConnection() {
		return this.journeysDB;
	}

	/**
	 * Creates a new collection of journeys from database connection.
	 * 
	 * @param db The database connection used to get the journey data.
	 */
	public DatabaseJourneys(java.sql.Connection db) {
		this.journeysDB = db;
	}

	/**
	 * The language of the program.
	 * 
	 * @return The language of the program.
	 */
	public String getLanguage() {
		return "fi";
	}

	private static final java.util.Map<String, String> JOURNEY_DB_FIELD_NAMES;

	/**
	 * Static initializer.
	 */
	static {
		// INitializing the database fields of an individual journey.
		java.util.Map<String, String> fieldNames = new java.util.TreeMap<>();
		fieldNames.put(DURATION_PROPERTY, "duration");
		fieldNames.put(ID_PROPERTY, "jid");
		fieldNames.put(DISTANCE_PROPERTY, "distance");
		fieldNames.put(START_LOCATION_ID_PROPERTY, "departure_station_id");
		fieldNames.put(START_LOCATION_NAME_PROPERTY, "departure_station_name");
		fieldNames.put(START_TIME_PROPERTY, "departure_time");
		fieldNames.put(END_TIME_PROPERTY, "arrival_time");
		fieldNames.put(END_LOCATION_ID_PROPERTY, "arrival_station_id");
		fieldNames.put(END_LOCATION_NAME_PROPERTY, "arrival_station_name");
		JOURNEY_DB_FIELD_NAMES = Collections.unmodifiableMap(fieldNames);
	}

	/**
	 * The field names of the journey property names.
	 * 
	 * @return The mapping from journey property names to the database field names.
	 */
	public java.util.Map<String, String> fieldNames() {
		return JOURNEY_DB_FIELD_NAMES;
	}

	/**
	 * The field name of the given journey property name.
	 * 
	 * @param journeyPropertyName The journey property name.
	 * @return The field name of the journey property.
	 */
	public String getFieldName(String journeyPropertyName) {
		return this.fieldNames().get(journeyPropertyName);
	}

	/**
	 * The stored SQL query.
	 */
	private String insertJourneySQLQuery = null;

	/**
	 * The SQL query string for query inserting new journey.
	 * 
	 * @return
	 */
	protected String getInsertJourneySQLQueryString() {
		return this.insertJourneySQLQuery;
	}

	/**
	 * Composes the add journey SQL query.
	 * 
	 * @param journey The journey defining the journey.
	 * @return The string of SQL query for adding journey.
	 */
	protected String composeAddJourneySQLQuery() {
		return "INSERT INTO journeys(jid, departure_time, arrival_time"
				+ ", departure_station_id, arrival_station_id, duration, distance,"
				+ ", departure_station_name, arrival_station_name" + ") VALUES(?,?,?,?,?,?,?,?,?)" + " RETURNING jid";
	}

	/**
	 * Forgets the current journey SQL query. Next time the journey is added, a new
	 * SQL query is composed.
	 */
	public void forgetAddJourneySQLQuery() {
		this.insertJourneySQLQuery = null;
	}

	@Override
	public boolean addJourney(Journeys.Journey journey) throws IllegalArgumentException {
		java.sql.Connection db = getConnection();
		if (db != null) {
			if (insertJourneySQLQuery == null) {
				insertJourneySQLQuery = composeAddJourneySQLQuery();
			}
			try {
				db.beginRequest();
				db.createStatement().execute("LOCK VIEW journeys IN SHARE ROW EXCLUSIVE MODE");
				String locationName;
				Integer locationId;
				for (String locationProperty : Arrays.asList("start", "end")) {
					locationId = (Integer) journey.getProperty(locationProperty + ".location.id");
					locationName = (String) journey.getProperty(locationProperty + ".location.name");
					if (!checkStationName(getLanguage(), locationId, locationName)) {
						// The station is erroneous. 
						throw new IllegalArgumentException(
								this.severe("The location id={0}, name={1} does not exist", locationId, locationName));
					}
				}

				PreparedStatement pstmt = db.prepareStatement(insertJourneySQLQuery);
				int index = 1;
				for (String property : journey.getDateProperties()) {
					if (getFieldName(property) != null)
						pstmt.setTimestamp(index++,
								new Timestamp(((java.util.Date) journey.getProperty(property)).getTime()));

				}
				for (String property : journey.getIntegerProperties()) {
					if (getFieldName(property) != null)
						pstmt.setInt(index++, (Integer) journey.getProperty(property));
				}
				for (String property : journey.getStringProperties()) {
					if (getFieldName(property) != null)
						pstmt.setString(index++, (String) journey.getProperty(property));
				}
				pstmt.setString(index++, getLanguage());
				ResultSet resultSet = pstmt.executeQuery();
				if (resultSet.next()) {
					// The operation succeeded.
					Integer id = resultSet.getInt(1);
					this.info("Journey {0} added to journeys with id {1}", journey.toString(), journey.getProperty(ID_PROPERTY));
					journey.setProperty(ID_PROPERTY, id);
					db.endRequest();
					return true;
				} else {
					// THe operation failed.
					this.severe("Could not add journey {0}", journey.toString());
					db.endRequest();
					return false;
				}
			} catch (SQLException e) {
				this.severe("Adding a journey {0} failed due {1}", journey.toString(), e.getMessage());
			}
		}
		// Default result is false.
		return false;
	}

	/**
	 * Composes update query for updating given property names with prepared
	 * statement.
	 * 
	 * The properties which does not exist in the database are ignored.
	 * 
	 * @param propertyNames The updated property names.
	 * @return The SQL query for prepared statement updating the given fields in the
	 *         order they appear. Unknown properties are ignored.
	 */
	protected String composeUpdateJourneySQLQuery(java.util.List<String> propertyNames) {
		StringBuilder builder = new StringBuilder("UPDATE journeys SET ");
		String fieldName;
		boolean first = true;
		for (String property : propertyNames) {
			fieldName = this.getFieldName(property);
			if (fieldName != null) {
				if (first) {
					first = false;
				} else {
					builder.append(", ");
				}
				builder.append(fieldName + "=?");
			}
		}
		builder.append("WHERE jid=?");
		return builder.toString();
	}

	/**
	 * Checks validity of the station name.
	 * 
	 * @param language  The language of the station name.
	 * @param property  The station identifier number.
	 * @param property2 The expected station name.
	 * @return True, if and only if either the station id has given station name.
	 */
	private boolean checkStationName(String language, Object property, Object property2) {
		java.sql.Connection db = getConnection();
		if (db != null) {
			try {
				PreparedStatement stationNameQuery = db
						.prepareStatement("SELECT station_name FROM station_names WHERE lang=? AND station_id=?");
				stationNameQuery.setString(1, language);
				Integer stationId = (Integer) property;
				String stationName = (String) property2;
				stationNameQuery.setInt(2, stationId);

				ResultSet result = stationNameQuery.executeQuery();
				if (result != null && result.next()) {
					// We have a station.
					if ((stationName == null && result.getString(1) == null)
							|| (stationName != null && stationName.equals(result.getString(1)))) {
						return true;
					}
				}
			} catch (SQLException sqle) {
				// The station does not exist.
			}
		}
		return false;
	}

	@Override
	public Journey getJourney(int index) {
		java.sql.Connection db = getConnection();
		if (db != null) {
			try {
				PreparedStatement stmt = db
						.prepareStatement("SELECT jid, departure_station_id, arrival_station_id, duration, distance"
								+ ", departure_time, arrival_time " + ", departure_station, arrival_station, lang"
								+ " FROM journeys WHERE jid=?");
				stmt.setInt(1, getRowIdOfIndex(index));
				ResultSet resultSet = stmt.executeQuery();
				if (resultSet.next()) {
					// WE do have result.
					Journey result = this.new Journey(resultSet);
				} else {
					// No such journey exists.
					return null;
				}
			} catch (SQLException e) {
				this.severe("Getting a journey at row {0} failed due {1}", index, e.getMessage());
			}
		}
		// The default is null.
		return null;
	}

	/**
	 * The row index of the given index.
	 * 
	 * @param index The journey index.
	 * @return The row index of the database for given index.
	 */
	protected Integer getRowIdOfIndex(int index) {
		if (index < 0) {
			java.sql.Connection db = getConnection();
			if (db != null) {
				try {
					PreparedStatement pstmt = db.prepareStatement(
							"SELECT row() as row_id, jid FROM journeys ORDER BY departure_time DESC WHERE row_id=?");
					pstmt.setInt(1, index + 1);
					ResultSet result = pstmt.executeQuery();
					if (result.next()) {
						return result.getInt(2);
					} else {
						return null;
					}
				} catch (SQLException e) {
					// Exception prevented answering the result.
					this.severe("Fetching jid of row {0} failed due {1}", index, e.getMessage());
					return null; 
				}
			}
		}
		// The default is null.
		return null;
	}

	public Journey getJourneys(int startIndex, int endIndex) {
		
	}

}