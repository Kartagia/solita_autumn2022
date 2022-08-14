package solita.helsinkicitybikeapp.model.db;

import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.logging.Logger;

import i18n.Logging.LocalizedMessageLogging;
import solita.helsinki.citybikeapp.controller.CSVReader;
import solita.helsinki.citybikeapp.controller.JourneysLoader;
import solita.helsinkicitybikeapp.model.CSVException;
import solita.helsinkicitybikeapp.model.CSVException.RowType;

/**
 * Class handling Journeys Database.
 * 
 * The class is used to create the Journeys database.
 * 
 * @author Antti Kautiainen
 *
 */
public class JourneyDB implements i18n.Logging.LocalizedMessageLogging {

	/**
	 * The SQL connection used for the database.
	 */
	private final java.sql.Connection connection;

	/**
	 * Creating new database connection to Journeys system.
	 * 
	 * @param connection The database connection.
	 */
	public JourneyDB(java.sql.Connection connection) {
		this.connection = connection;
	}

	/**
	 * The current database connection.
	 * 
	 * @return The database connection for journeys storing database.
	 */
	public java.sql.Connection getConnection() {
		return this.connection;
	}

	/**
	 * Construct station related tables.
	 * 
	 * @return Did the creation of station tables succeed or not.
	 * @throws SQLException The construction failed due station SQL exception.
	 */
	public boolean createStationTables() throws SQLException {
		java.sql.Connection db = this.getConnection();
		String resourceName = "db/create_db.postgresql.sql";
		InputStream in = getClass().getResourceAsStream(resourceName);
		if (in == null) {
			this.severe("Missing resource: Database initialization script {0} does not exist", resourceName);
			return false;
		} else {
			Scanner scanner = new Scanner(in);
			scanner.useDelimiter("\\Z");
			String query = scanner.next();
			scanner.close();

			try (Statement stmt = db.createStatement()) {
				stmt.execute(query);
				return true;
			} catch (SQLException se) {
				throw se;
			}
		}
	}

	/**
	 * Creates basic database for the database.
	 * 
	 * @param db The database connection of the altered database.
	 * @return True, if and only if the creation of the database was successful.
	 * @throws SQLException The creation failed due SQL exception.
	 */
	public boolean createDatabase() throws SQLException {
		java.sql.Connection db = this.getConnection();
		db.beginRequest();
		createStationTables();
		createJourneysTables();
		db.endRequest();
		db.commit();
		return true;
	}

	/**
	 * Creates Journeys tables.
	 */
	public void createJourneysTables() {

	}
	
	/**
	 * The station file reader reading the station CSV file. 
	 * @author Antti Kautiainen
	 *
	 */
	public class StationFileReader extends CSVReader implements i18n.Logging.LocalizedMessageLogging {
		
		private long recordNum = 0; 
		
		protected long getRecordNumber() {
			return recordNum; 
		}
		
		private String sourceName = null; 
		
		public String getSourceName() {
			return sourceName == null?format("NULL"):format("\"{0}\"", sourceName);
		}

		@Override
		public String info(String format, Object... formatArgs) {
			String result = format("Record: {0} of Source {1}: {2}", 
					this.getRecordNumber(), this.getSourceName(), format(format, formatArgs));
			Logger log = this.getLogger();
			if (log != null) {
				this.getLogger().info(result);
			}
			return result; 
		}

		@Override
		public String severe(String format, Object... formatArgs) {
			String result = format("Record: {0} of Source {1}: {2}", 
					this.getRecordNumber(), this.getSourceName(), format(format, formatArgs));
			Logger log = this.getLogger();
			if (log != null) {
				this.getLogger().severe(result);
			}
			return result; 
		}

		/**
		 * Tester testing station file header.
		 * @author Antti Kautiainen
		 *
		 */
		protected class HeaderTester implements Predicate<List<? extends CharSequence>> {

			public boolean test(List<? extends CharSequence> row) {
				if (row == null || row.isEmpty()) {
					throw new CSVException.EmptyRowException(RowType.HEADER, "Missing header.", null);
				} else {
					TreeSet<String> fieldNames = new TreeSet<>(row.stream().map(
							(CharSequence val)->(val instanceof String?(String)val:val.toString())).toList());
					if (fieldNames.size() != row.size()) {
						throw new CSVException.DuplicateHeaderException("Duplicate field names", row);
					}
				}
				// The test passed.
				return true;
			}
		}

		/**
		 * Tester testing station file data.
		 * @author Antti Kautiainen
		 *
		 */
		protected class DataTester implements Predicate<List<? extends CharSequence>> {

			@Override
			public boolean test(List<? extends CharSequence> row) {
				if (row.isEmpty()) {
					throw new CSVException.EmptyRowException(RowType.DATA, "Missing data row", null);
				} else {
					TreeSet<String> fieldNames = new TreeSet<>(row.stream().map(
							(CharSequence val)->(val instanceof String?(String)val:val.toString())).toList());
					if (fieldNames.size() != row.size()) {
						throw new CSVException.DuplicateHeaderException("Duplicate field names", row);
					}
				}
				// The test passed.
				return true;
			}
		}

		
		/**
		 * The station handler testing the validity of the read rows. 
		 * @author kautsu
		 *
		 */
		protected class StationHandler extends CSVReader.TesterHandler {

			public StationHandler() {
				this(StationFileReader.this.new HeaderTester(), StationFileReader.this.new DataTester()); 
			}
			
			protected StationHandler(
					Predicate<java.util.List<? extends CharSequence>> headerTester, 
					Predicate<java.util.List<? extends CharSequence>> rowTester) {
				super(headerTester, rowTester);
			}

		}

		private boolean lenient = true;

		public StationFileReader() {

		}

		public StationFileReader(boolean lenient) {
			super();
			this.setHandler(this.new StationHandler());
		}

		/** Creates a new station file reader from given source. 
		 * 
		 * @param source The source from which the station data is read. 
		 * @throws java.io.IOException The reading failed due IO exception. 
		 */
		public StationFileReader(InputStream source) throws java.io.IOException {
			this();
			this.open(source); 
		}

	}

	/**
	 * Initializes the database from the data entries on the jar file.
	 * 
	 * @throws SQLException The operation failed due SQL exception.
	 */
	public void initDatabaseFromJAR() throws SQLException {
		try {
			// Reading station information.
			InputStream in = getClass()
					.getResourceAsStream("Helsingin_ja_Espoon_kaupunkipy%C3%B6r%C3%A4asemat_avoin.csv");
			if (in == null) {
				// Station info did not exi
				info("No station info found");
				return;
			} else {
				CSVReader stationReader = this.new StationFileReader();
				stationReader.open(in);

				// Loading journeys after stations has been initialized.
				JourneysLoader loader;
				for (String fileName : Arrays.asList("2021-05.csv", "2021-06.csv", "2021-07.csv")) {
					in = getClass().getResourceAsStream("/solita.helsinkicitybikeapp/data" + fileName);
					if (in == null) {
						info("Journey data {0} not available in jar", fileName);
					} else {
						if (in != null) {
							// Loading the csvs.
							loader = new JourneysLoader(in, this.getConnection());
							if (loader.readAll()) {
								// The loading of the journeys succeeded.
								info("Journey data {0} read from jar", fileName);
							} else {
								// The loading of the data failed.
								info("Journey data {0} in jar contained error", fileName);
							}
						}
					}
				}
			}

			// Reading journey information.
		} catch (java.io.IOException ioe) {

		}
	}

}
