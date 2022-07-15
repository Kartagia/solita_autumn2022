package solita.helsinki.citybikeapp.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.util.logging.Logger;

import solita.helsinkicitybikeapp.model.Config;
import solita.helsinkicitybikeapp.model.Journeys;

/**
 * The class loading journeys. 
 * 
 * The Journeys loader uses handler interface to the reading of the CSV file in order
 * to allow insertion of the CSV data into the database instead of storing it into memory. 
 * 
 * @author Antti Kautiainen
 *
 */
public class JourneysLoader {

	private Journeys data; 
	
	private CSVReader reader; 
	
	/**
	 * Create Journeys CSV file loader from given source file. 
	 * @param source THe source URL. 
	 * @param db The database connection to store the journeys. 
	 * @throws IOException The opening of the source content failed. 
	 * @throws java.sql.SQLException The database connection failed. 
	 */
	public JourneysLoader(URL source, java.sql.Connection db) throws IOException, java.sql.SQLException {
		reader = new CSVReader(source); 
		data = db == null?new Journeys():new Journeys(db); 
	}

	/**
	 * Create Journeys CSV file loader from given source file. 
	 * @param source THe source file. 
	 * @param db The database connection to store the journeys. 
	 * @throws IOException The opening of the source content failed. 
	 * @throws java.sql.SQLException The database connection failed. 
	 */
	public JourneysLoader(File source, java.sql.Connection db) throws IOException, java.sql.SQLException {
		reader = new CSVReader();
		reader.open(source);
		data = db == null?new Journeys():new Journeys(db); 
	}
}
