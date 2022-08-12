package solita.helsinkicitybikeapp.model.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import solita.helsinkicitybikeapp.model.Journeys;
import solita.helsinkicitybikeapp.model.Journeys.Journey;

/**
 * The database using Journeys.
 * 
 * The current view of the Journeys is given by methods {@linkplain #getJourneysViewName()} to allow
 * implementation of various ordering views to get the journeys. 
 * 
 * 
 * 
 * @author Antti Kautiainen
 *
 */
public class DatabaseJourneys extends Journeys {

	/**
	 * Database implementation of journeys.
	 * 
	 * The journeys does not update database until committed. 
	 * 
	 * @author Antti Kautaiinen
	 *
	 */
	public class DBJourney extends Journeys.Journey {

		public DBJourney() {
			super();
		}

		public DBJourney(ResultSet dbRow) throws IllegalArgumentException, SQLException {
			this();
			initFromResultSet(dbRow);
		}

		public boolean initFromResultSet(ResultSet dbRow) throws IllegalArgumentException, SQLException {
			for (String property : DatabaseJourneys.this.getJourneyIntegerProperties()) {
				this.setProperty(property, dbRow.getInt(getDBFieldName(property)));
			}
			setAltered(false); 
			return true;
		}

		/**
		 * The database field name of the given property.
		 * 
		 * @param propertyName The property name.
		 * @return The database field name, if any exists.
		 */
		public String getDBFieldName(String propertyName) {
			return DatabaseJourneys.this.getFieldName(propertyName);
		}
		
		/**
		 * Has the content of the journey been updated since last commit, or fetching 
		 * values for database. 
		 */
		private boolean isAltered = false; 
		
		/**
		 * Has the journey been changed from the database state. 
		 * @return True, only if the journey state has been altered since last udpate. 
		 */
		public synchronized boolean isAltered() {
			return isAltered; 
		}
		
		/**
		 * Setting the altered state of the journey. 
		 * @param altered The new altered state. 
		 */
		protected synchronized void setAltered(boolean altered) {
			this.isAltered = altered; 
		}
		
		/**
		 * Commits the changes to the database. 
		 * @return Whether the database was updated or not. 
		 */
		public synchronized boolean commit() {
			if (isAltered()) {
				setAltered(false);
				return true; 
			} else {
				return false; 
			}
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
				String locationName, idProperty, nameProperty;
				Integer locationId;
				for (String locationProperty : Arrays.asList("start", "end")) {
					idProperty = locationProperty + ".location.id"; 
					nameProperty = locationProperty + ".location.name"; 
					locationId = (Integer) journey.getProperty(idProperty);
					locationName = (String) journey.getProperty(nameProperty);
					if (!checkStationName(getLanguage(), locationId, locationName)) {
						// The station is erroneous.
						throw new IllegalArgumentException(
								this.severe("The {0]={1} with {2} {3} does not exist", 
										idProperty, locationId, 
										nameProperty, locationName));
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
					this.info("Journey {0} added to journeys with id {1}", journey.toString(),
							journey.getProperty(ID_PROPERTY));
					journey.setProperty(ID_PROPERTY, id);
					return true;
				} else {
					// THe operation failed.
					this.severe("Could not add journey {0}", journey.toString());
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
					this.addJourney(this.new DBJourney(resultSet));
				} else {
					// No such journey exists.
					return null;
				}
			} catch (SQLException e) {
				this.severe("Getting a journey at row {0} failed due SQL Exception {1}", index, e.getMessage());
				return null; 
			} catch (Exception e) {
				this.severe("Getting a journey at row {0} failed due {1} {2}", index, e.getClass().getName(),
						e.getMessage());
				return null; 
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
		if (index >= 0) {
			java.sql.Connection db = getConnection();
			if (db != null) {
				try {
					PreparedStatement pstmt = db.prepareStatement(
							"SELECT row_id, jid FROM journeys_view ORDER BY departure_time DESC WHERE row_id=?");
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

	/**
	 * The names of the journey database query fields.
	 * 
	 * @return The list of the database fields returned by the
	 */
	public List<String> getJourneyDBFieldNames() {
		return Arrays.asList("jid", "departure_station_id", "arrival_station_id", "duration", "distance",
				"depature_time", "arrival_time", "depature_station_name", "arrival_station_name");
	}

	/**
	 * The query returning single journey with given index.
	 * 
	 * The query will have single parameter, the fetched journey index.
	 * 
	 * @return The prepared SQL Query string for fetching single journey by index.
	 */
	protected String composeFetchJourneyQuery() {
		return composeFetchJourneysQuery(1, false);
	}

	/**
	 * The query returning multiple journeys.
	 * 
	 * The range query will pair indexes into pairs assuming the indexes are start
	 * and end indexes of ranges. If odd number of indexes is given, the last index
	 * is treated as range containing only that index.
	 * 
	 * The default operator is OR allowing any ranges.
	 * 
	 * @param numberOfIndexes The number of indexes the query allows as parameters.
	 * @param rangeQuery      Do we create range query pairing two consequent
	 *                        indexes into start and end indexes of the range.
	 * @return The prepared SQL Query string for fetching multiple journeys.
	 */
	protected String composeFetchJourneysQuery(int numberOfIndexes, boolean rangeQuery) {
		return composeFetchJourneysQuery(numberOfIndexes, rangeQuery, "OR");
	}

	
	/**
	 * The view used to fetch journeys. 
	 * @return The current view used to fetch journeys. 
	 */
	protected String getJourneysViewName() {
		return "journeys_view"; 
	}
	
	/**
	 * The query returning multiple journeys.
	 * 
	 * The range query will pair indexes into pairs assuming the indexes are start
	 * and end indexes of ranges. If odd number of indexes is given, the last index
	 * is treated as range containing only that index.
	 * 
	 * The row condition is paired with given operator. 
	 * 
	 * @param numberOfIndexes The number of indexes the query allows as parameters.
	 * @param rangeQuery      Do we create range query pairing two consequent
	 *                        indexes into start and end indexes of the range.
	 * @param operator	The operator combining query conditions. 
	 * @return The prepared SQL Query string for fetching multiple journeys. 
	 */
	protected String composeFetchJourneysQuery(int numberOfIndexes, boolean rangeQuery, String operator) {
		StringBuilder result = new StringBuilder("SELECT * FROM ");
		result.append(this.getJourneysViewName()); 
		if (numberOfIndexes > 0) {
			result.append("WHERE ");
			String operatorString = " " + operator + " ";
			int i = 0;
			if (rangeQuery) {
				// Composing range query.
				while (i + 1 < numberOfIndexes) {
					if (i > 0) {
						result.append(operatorString);
					}
					result.append("(row_id >= ? and row_id < ?)");
					i += 2;
				}
			}
			// Composing single row query.
			for (; i < numberOfIndexes; i++) {
				if (i > 0)
					result.append(operatorString);
				result.append("row_id=?");
			}

		}

		return result.toString();
	}

	private String fetchJourneysSQLQuery = "SELECT * FROM journeys_view ORDER BY departure_time DESC WHERE row_id>=? AND row_id <?";

	protected String getFetchJourneysSQLQuery() {
		return this.fetchJourneysSQLQuery;
	}

	/**
	 * Fetching continuous range of journeys.
	 * 
	 * @param startIndex The first fetched journey.
	 * @param endIndex   The first index not belonging to the returned journeys.
	 * @param endIndex   The last fetched journey.
	 * @return The list containing all journeys within given bounds.
	 */
	public List<Journeys.Journey> getJourneys(int startIndex, int endIndex) {
		if (startIndex >= 0) {
			java.sql.Connection db = getConnection();
			if (db != null) {
				try {
					PreparedStatement pstmt = db.prepareStatement(getFetchJourneysSQLQuery());
					pstmt.setInt(1, startIndex + 1);
					pstmt.setInt(2, endIndex + 1);
					ResultSet resultSet = pstmt.executeQuery();
					List<Journeys.Journey> result = new java.util.ArrayList<>();
					Journeys.Journey journey;
					while (resultSet.next()) {
						result.add(this.new DBJourney(resultSet));
					}
					return result;
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

	/**
	 * Handling SQL exception.
	 * 
	 * @param exception	The handled exception.
	 * @throws E Throws the exception unless it is handled. 
	 */
	public void handleException(SQLException exception) throws SQLException {
		throw exception; 
	}

	/**
	 * Handling exception.
	 * 
	 * @param <E>       The type of the exception.
	 * @param exception	The handled exception.
	 * @throws E Throws the exception unless it is handled. 
	 */
	public <E extends Throwable> void handleException(E exception) throws E {
		throw exception; 
	}

}