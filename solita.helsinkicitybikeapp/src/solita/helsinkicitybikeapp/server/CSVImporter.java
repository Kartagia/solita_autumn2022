package solita.helsinkicitybikeapp.server;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

import solita.helsinki.citybikeapp.controller.JourneysLoader;
import solita.helsinkicitybikeapp.model.Config;

/**
 * The program class performing importing of the CSV data into the server. 
 * 
 * @author Antti Kautiainen
 *
 */
public class CSVImporter {

	/**
	 * The main program performing the importing operation. 
	 * 
	 * @param args
	 */
	/**
	 * The main program performs loading of the given database files into the database. 
	 * @param args Command line arguments.  
	 */
	public static void main(String[] args) {
		Logger logger = Logger.getLogger(JourneysLoader.class.getCanonicalName()); 

		Config config = new Config(); 
		Properties dbProperties = config.getDatabaseProperties(); 
		Connection db; 
		JourneysLoader loader; 
		try {
			db = getConnection(dbProperties.getProperty(config.DATABASE_PROTOCOL_PROPERTY_NAME), dbProperties); 
			logger.info("Connection established to database " + dbProperties.toString());
		} catch(java.sql.SQLException sqle) {
			// The connection failed.
			logger.severe("Could not connect to the database. Testing the integrity of the CSV files"); 
			db = null; 
		}
		
		File file; 
		for (String filename: args) {
			logger.info(String.format("Loading file \"%s\"",filename));
			file = new File(filename);
			try {
				loader = new JourneysLoader(file.toURI().toURL(), db);
			} catch (MalformedURLException e) {
				loader = null; 
				logger.severe("The file name was malformed: " + e.getMessage());
			} catch (IOException e) {
				loader = null; 
				logger.severe("The reading failed: " + e.getMessage());
			} catch (SQLException e) {
				loader = null; 
				logger.severe("The establishing of hte conneciton failed: " + e.getMessage());
			} 
			
			if (loader != null) {
				
			}
		}
	}

	/**
	 * Does the database use URL or just the database name. 
	 * @param protocol The database type. 
	 * @return True, if and only if the database connects with URL. 
	 */
	protected static boolean doesDbUseURL(String protocol) {
		switch (protocol) {
		case "derby": 
			return false;
		default:
			return true; 
		}
	}

	/**
	 * Tries to create database connection. 
	 * @param protocol The database protocol used for connection. 
	 * @return The established database connection. 
	 * @throws SQLException The establishing of the connection failed. 
	 */
	public static java.sql.Connection getConnection(String protocol, Properties connectionProperties) throws SQLException {
		String db = connectionProperties.getProperty("db"); 
		if (db == null) db = connectionProperties.getProperty("database");
		if (doesDbUseURL(protocol)) {
			String host = connectionProperties.getProperty("host");
			if (host == null) host = connectionProperties.getProperty("hostname");
			String port = connectionProperties.getProperty("port");
			return DriverManager.getConnection("jdbc:" + protocol + "://" + host + ":" + port + "/" + db, connectionProperties);
		} else { 
			return DriverManager.getConnection("jdbc:" + protocol + ":" + db, connectionProperties);
		}
	}

}
