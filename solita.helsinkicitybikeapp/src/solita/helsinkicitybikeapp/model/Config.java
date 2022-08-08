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
 * The order of precedence of configuration property values from highest to
 * lowest:
 * <ol>
 * <li>The system properties property values</li>
 * <li>The configuration files from last to first.</li>
 * <li>The default values (if any)</li>
 * </ol>
 * 
 * For example if the system has 3 configuration files,
 * <code>journeys.properties.xml</code> having properties file property of
 * <code>dev.properties.xml</code>. The referred properties file has properties
 * file property of <code>db.properties.xml</code>, the properties of
 * <code>db.properties.xml</code> will be used instead of any properties given
 * in either <code>dev.properties.xml</code> or
 * <code>journeys.properties.xml</code>.
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
	public static final String PROPERTY_XML_FILENAME = "journeys.config.xml";

	/**
	 * Creates a new default configuration.
	 */
	public Config() {
		super();
	}

	/**
	 * Creates a new default configuration with given default values.
	 * 
	 * @param defaultValues The default values used for missing properties.
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
				result.setProperty("dbms", property);
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

	/**
	 * The logger used for the static methods of this class.
	 * 
	 */
	static final Logging LOGGER = Logging.LocalizedLogging.createLocalizedLogging(new Logging.MessageLogging() {
	});

	/**
	 * The default directory storing user properties.
	 */
	public static final String DEFAULT_CONFIG_DIRECTORY = ".journeys";

	/**
	 * The default configuration directory under user home directory.
	 * 
	 * @return The default configuration directory under user home.
	 */
	public static String getUserHomeConfigDirectory() {
		return System.getProperty("user.home") + System.getProperty("file.separator") + DEFAULT_CONFIG_DIRECTORY;
	}

	/**
	 * The base directory of the given file.
	 * 
	 * @param propertyFileName The name of the property file.
	 * @return If the given given file is relative, tries to seek the directory
	 *         containing the property file.
	 */
	public static String getBaseDirectory(String propertyFileName) throws java.io.IOException {
		File dir;
		File propertyFile = new File(propertyFileName);
		if (propertyFile.isAbsolute()) {
			// The file is absolute. The base directory is undefined.
			return null;
		}
		List<String> baseDirectoryPropertyList = Arrays.asList(System.getProperty(BASE_DIRECTORY_PROPERTY_NAME),
				System.getProperty("user.dir"), Config.getUserHomeConfigDirectory());
		for (String directoryName : baseDirectoryPropertyList) {
			if (directoryName != null) {
				dir = new java.io.File(directoryName);
				if (dir.exists() && dir.isDirectory()) {
					if ((new File(dir, propertyFileName)).canRead()) {
						return directoryName;
					} else {
						LOGGER.info("Base directory candidate \"{0}\" did not contain configuration file \"{1}\"",
								directoryName, propertyFileName);
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
		// None of the directories contained property file.
		return null;
	}

	/**
	 * The system default properties of the current configuration. 
	 * @return The system default properties of the current instance.
	 */
	public Properties getSystemDefaultProperties() {
		return Config.SYSTEM_PROPERTIES;
	}
	
	/**
	 * Initializes the system default properties. 
	 * @return The system default properties. 
	 */
	public static Properties initSystemDefaultProperties() {
		Properties SYSTEM_PROPERTIES = new Properties();

		String propertyFileName = System.getProperty(XML_PROPERTY_FILE_PROPERTY_NAME, PROPERTY_XML_FILENAME);

		// Creating the file to determine whether the base directory is tested for
		// existing configuration file or not.
		File propertyFile = new File(propertyFileName);

		// Setting the base directory.
		File[] fileList = null;
		boolean found = false;
		try {
			String directoryName = Config.getBaseDirectory(propertyFileName);
			if (directoryName != null) {
				// Setting the system properties of the base directory name to the found
				// directory name, and changing the property file to address the file in that
				// directory.
				SYSTEM_PROPERTIES.setProperty(BASE_DIRECTORY_PROPERTY_NAME, directoryName);
				LOGGER.info("Base directory \"{0}\"", directoryName);
				LOGGER.info("Configuration file \"{1}\" under Base directory \"{0}\"", directoryName, propertyFileName);
				propertyFile = new File(directoryName, propertyFileName);
			}

			if (!propertyFile.exists()) {
				// The base configuration file does not exist.
				LOGGER.severe("Configuration file \"{0}\" does not exist", propertyFile.getCanonicalPath());
			} else if (!propertyFile.canRead()) {
				// The base configuration file does not exist or it cannot be read.
				LOGGER.severe("Cannot read configuration file \"{0}\"", propertyFile.getCanonicalPath());
			} else {
				// All is fine.
			}
		} catch (java.io.IOException ioe) {
			// The reading of the base directory failed to exception
			LOGGER.severe("Could not read default configuration due exception %s", ioe.toString());
		}
		return SYSTEM_PROPERTIES; 
	}
	
	/**
	 * Static initializer creating the default properties from system properties.
	 */
	static {
		SYSTEM_PROPERTIES = Config.initSystemDefaultProperties();
	}

}
