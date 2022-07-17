package solita.helsinkicitybikeapp.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import i18n.Logging;

/**
 * The configuration of the Journeys application.
 * 
 * @author Antti Kautiainen
 *
 */
public class Config extends Properties implements Logging.LocalizedLogging {

	/**
	 * The properties generated from system properties.
	 * 
	 * These properties override default properties. 
	 * 
	 */
	protected static final Properties SYSTEM_PROPERTIES;

	/**
	 * The property key of the configuration directory.
	 */
	public static final String BASE_DIRECTORY_PROPERTY_NAME = "journeys.basedir";

	/**
	 * The property key of the configuration base directory list.
	 */
	public static final String BASE_DIRECTORY_LIST_PROPERTY_NAME = "journeys.basedirs";

	/**
	 * The property name of the XML file containing the properties.
	 */
	public static final String XML_PROPERTY_FILE_PROPERTY_NAME = "journeys.config.xml";

	/**
	 * The property name of the property containing the database name.
	 */
	public static final String DATABASE_PROPERTY_NAME = "journeys.db.name";

	/**
	 * The property name of the property containing the database protocol
	 */
	public static final String DATABASE_PROTOCOL_PROPERTY_NAME = "journeys.db.protocol";

	/**
	 * The property name of the property containing the database host.
	 */
	public static final String DATABASE_HOST_PROPERTY_NAME = "journeys.db.host";

	/**
	 * The property name of the property containing the database connection port.
	 */
	public static final String DATABASE_PORT_PORPERTY_NAME = "journeys.db.port";

	/**
	 * The property name of the property containing the database user name.
	 */
	public static final String DATABASE_USER_PROPERTY_NAME = "journeys.db.user";

	/**
	 * The property name of the property containing the database user password..
	 */
	public static final String DATABASE_USER_SECRET_PROPERTY_NAME = "journeys.db.user.secret";

	/**
	 * The default name of the configuration file.
	 */
	public static final String PROPERTY_XML_FILENAME = "journeys_config.xml";

	/**
	 * Creates a new default configuration.
	 */
	public Config() {
		super();
	}

	/**
	 * Creates a new default configuration with given values.
	 * 
	 * @param defaultValues The default values.
	 */
	public Config(Properties defaultValues) {
		super(defaultValues);
	}

	/**
	 * Creates a new default configuration from given default values, and
	 * configuration read from the given file.
	 * 
	 * @param configFile    The configuration file.
	 * @param defaultValues The default properties.
	 * @throws IOException                      The loading of the configuration
	 *                                          failed due Input/Output error.
	 * @throws InvalidPropertiesFormatException The given xml file is not valid
	 *                                          Properties file.
	 */
	public Config(File configFile, Properties defaultValues) throws InvalidPropertiesFormatException, IOException {
		super(defaultValues);
		if (configFile.canRead() && configFile.isFile()) {
			if (configFile.toPath().getFileName().endsWith(".xml")) {
				// Reading from xml file.
				this.loadFromXML(new FileInputStream(configFile));
			} else {
				// It is text file
				this.load(new FileInputStream(configFile));
			}
		}
	}

	/**
	 * Generates database properties from the property set.
	 * 
	 * @return The properties related to the database connection.
	 */
	public Properties getDatabaseProperties() {
		Properties result = new Properties();
		String property = this.getProperty(this.DATABASE_PROTOCOL_PROPERTY_NAME);
		if (property != null) {
			// WE do have database system.
			if (property.equals("postgresql") || property.equals("psql")) {
				// We have "postgresql" database using either "psql" or "postgresql".
				result.setProperty("dbms", "postgresql");
			} else {
				// Unknown database.
				result.setProperty("dmms", property);
			}

			result.setProperty("db", getProperty(this.DATABASE_PROPERTY_NAME));
			result.setProperty("host", getProperty(this.DATABASE_HOST_PROPERTY_NAME));
			result.setProperty("port", getProperty(this.DATABASE_PORT_PORPERTY_NAME));
			result.setProperty("user", getProperty(this.DATABASE_USER_PROPERTY_NAME));
			result.setProperty("password", getProperty(this.DATABASE_USER_SECRET_PROPERTY_NAME));

		} else {
			// We do not have database system.
		}
		return result;
	}

	/** The logger used for the static methods of this class. 
	 * 
	 */
	static final Logging LOGGER = Logging.LocalizedLogging.createLocalizedLogging(new Logging.MessageLogging() {
	});

	/**
	 * Static initializer creating the default properties from system properties.
	 */
	static {
		SYSTEM_PROPERTIES = new Properties();

		String propertyFileName = System.getProperty(XML_PROPERTY_FILE_PROPERTY_NAME, PROPERTY_XML_FILENAME);

		// Creating the file to determine whether the base directory is tested for existing configuration file or not. 
		File propertyFile = new File(propertyFileName);

		// Setting the base directory.
		File[] fileList = null;
		boolean found = false;
		String directoryName = null;
		File dir;

		// Seeking default base directory - the default base directory has to have the
		// configuration file.
		// TODO: Add handling of the base directories property name to at list of base
		// directories.
		List<String> baseDirectoryPropertyList = Arrays.asList(BASE_DIRECTORY_PROPERTY_NAME);
		for (String propertyName : baseDirectoryPropertyList) {
			directoryName = System.getProperty(propertyName);
			if (directoryName != null) {
				dir = new java.io.File(directoryName);
				if (dir.exists() && dir.isDirectory()) {
					if (!propertyFile.isAbsolute() && (new File(dir, propertyFileName)).canRead()) {
						SYSTEM_PROPERTIES.setProperty(BASE_DIRECTORY_PROPERTY_NAME, directoryName);
						found = true;
						LOGGER.info("Base directory \"{0}\" set from system properties", directoryName);
						break;
					} else {
						LOGGER.info("Base directory candidate \"{0}\" did not contain configuration file \"{1}\"", directoryName,
								propertyFileName);
					}
				} else if (!dir.exists()) {
					LOGGER.info("Base directory candidate \"{0}\" did not exist", directoryName);
				} else if (dir.isDirectory()) {
					LOGGER.info("Base directory candidate \"{0}\" is not a directory", directoryName);
				} else {
					LOGGER.info("Base directory candidate \"{0}\" coould not be read", directoryName);
				}
			}
		}

	}

}
