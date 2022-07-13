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
	 * The default properties.
	 */
	public static final Properties DEFAULT_PROPERTIES;

	/**
	 * The property key of the configuration directory.
	 */
	public static final String PROPERTY_FILE_BASE_DIRECTORY_PROPERTY_NAME = "properties.journeys.basedir";

	/**
	 * The property key of the base property file name.
	 */
	public static final String PROPERTY_FILE_BASE_NAME_PROPERTY_NAME = "properties.journeys.file";

	/**
	 * The property name of the ISO-8859 encoded legacy property file.
	 */
	public static final String PROPERTY_FILE_PROPERTY_NAME = "properties.journeys.file.iso8859";

	/**
	 * The property name of the XML file containing the properties.
	 */
	public static final String XML_PROPERTY_FILE_PROPERTY_NAME = "properties.journeys.file.xml";

	public static final String DEFAULT_DATABASE_PROPERTY_NAME = "odbc.journeys.db.name"; 
	
	public static final String DEFAULT_DATABASE_PORT_PORPERTY_NAME = "odbc.journeys.db.port"; 

	public static final String DEFAULT_DATABASE_USER_PORPERTY_NAME = "odbc.journeys.db.user"; 
	
	static {
		DEFAULT_PROPERTIES = new Properties();
		
		// Setting the base directory. 
		for (String propertyName : new String[] { "user.dir" }) {
			String directoryName = null; 
			File dir; 
			try {
				directoryName = System.getProperty(propertyName);
				dir = new java.io.File(directoryName); 
				if (dir.exists() && dir.canRead() && dir.isDirectory()) {
					DEFAULT_PROPERTIES.setProperty(PROPERTY_FILE_BASE_DIRECTORY_PROPERTY_NAME, directoryName);
					break; // Exiting loop as the property was set. 
				}
			} catch (SecurityException se) {
				// The acquisition of the property failed. 
			}
		}
		
		// Setting the default files containing the properties. 
		DEFAULT_PROPERTIES.put(PROPERTY_FILE_BASE_NAME_PROPERTY_NAME, "journeys.properties");
		DEFAULT_PROPERTIES.put(PROPERTY_FILE_PROPERTY_NAME, "journeys.properties.cfg");
		DEFAULT_PROPERTIES.put(XML_PROPERTY_FILE_PROPERTY_NAME, "journeys.properties.xml");
	}

	/**
	 * The default properties of the journeys. 
	 */
	private Properties journeyProperties = new Properties(DEFAULT_PROPERTIES) {
		
		
		
		/**
		 * The default journeys properties.
		 */
		private static final long serialVersionUID = 1L;

	};

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

	}

}
