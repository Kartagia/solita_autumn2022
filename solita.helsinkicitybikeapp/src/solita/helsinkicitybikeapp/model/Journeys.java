package solita.helsinkicitybikeapp.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Class representing journeys of the City Bike trips.
 * 
 * @author Antti Kautiainen
 *
 */
public class Journeys {


	/**
	 * The connection to the Journeys database.
	 */
	private java.sql.Connection journeysDB;

	/**
	 * Creates a new empty journeys structure.
	 */
	public Journeys() {

	}

	/**
	 * Creates a new collection of journeys from CSV document.
	 * 
	 * @param source The source of journeys.
	 * @throws IOException The opening of the source failed due IO Exception.
	 */
	public Journeys(URL source) throws IOException {
		// Reading from URL source.
		InputStream input = source.openStream();
	}

	/**
	 * Creates a new collection of journeys from database connection.
	 * 
	 * @param db The database connection used to get the journey data.
	 */
	public Journeys(java.sql.Connection db) {
		journeysDB = db; 
	}

}
