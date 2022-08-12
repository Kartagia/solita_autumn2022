package solita.helsinkicitybikeapp.model.db;

import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Scanner;

import solita.helsinki.citybikeapp.controller.JourneysLoader;

/**
 * Class handling Journeys Database.
 * 
 * The class is used to create the Journeys database.
 * 
 * @author Antti Kautiainen
 *
 */
public class JourneyDB implements i18n.Logging {

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
		String resourceName = "data/db/create_db.postgresql.sql";
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
	 * Initializes the database from the data entries on the jar file.
	 * 
	 * @throws SQLException The operation failed due SQL exception.
	 */
	public void initDatabaseFromJAR() throws SQLException {
		try {
			// Reading station information.
			InputStream in = getClass().getResourceAsStream(
					"/solita.helsinkicitybikeapp/data/Helsingin_ja_Espoon_kaupunkipy%C3%B6r%C3%A4asemat_avoin.csv");

			JourneysLoader loader;
			for (String fileName : Arrays.asList("2021-05.csv", "2021-06.csv", "2021-07.csv")) {
				in = getClass().getResourceAsStream("/solita.helsinkicitybikeapp/data" + fileName);
				loader = new JourneysLoader(in, this.getConnection());
				if (loader.readAll()) {
					// The loading of the journeys succeeded.

				} else {
					// The loading of the data failed.

				}
			}

			// Reading journey information.
		} catch (java.io.IOException ioe) {

		}
	}

}
