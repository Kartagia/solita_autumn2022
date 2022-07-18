package solita.helsinkicitybikeapp.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import i18n.Logging;

/**
 * Class representing journeys of the City Bike trips.
 * 
 * @author Antti Kautiainen
 *
 */
public abstract class Journeys implements Logging.MessageLogging {

	/**
	 * The format of the journey date formats.
	 * 
	 * @author Antti Kautiainen
	 *
	 */
	public class JourneyDateFormat extends SimpleDateFormat {

		/**
		 * 
		 * The version of the journey date format.
		 */
		private static final long serialVersionUID = -919981653024281134L;

		/**
		 * Creates a new journey date format.
		 * 
		 */
		public JourneyDateFormat() {
			super("yyyy-MM-ddTHH:mm:ss");
		}

	}

	/** The positive integer format.
	 * 
	 * @author kautsu
	 *
	 */
	public class PositiveIntegerFormat extends NumberFormat {

		/**
		 * The positive integer format. 
		 */
		public PositiveIntegerFormat() {
			super(); 
		}
		
		@Override
		public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Number parse(String source, ParsePosition parsePosition) {
			int index = parsePosition.getIndex(), end = index;
			for (int len = source.length(); end < len && Character.isDigit(source.charAt(end)); index++) {

			}
			if (end == index) {
				// We do not have valid integer.
				parsePosition.setErrorIndex(end);
				return null;
			} else {
				parsePosition.setIndex(end);
				return Integer.parseInt(source.substring(index, end));
			}
		}

		@Override
		public boolean isParseIntegerOnly() {
			return true;
		}

	}

	/**
	 * Class representing a single journey of the City Bike trips.
	 * 
	 * @author Antti Kautiainen.
	 *
	 */
	public class Journey {

		/**
		 * The property names of the journeys.
		 * 
		 * @return The list of properties of the journeys.
		 */
		public static List<String> getPropertyNames() {
			return Arrays.asList("id", "start.time", "end.time", "start.location.id", "end.location.id",
					"start.location.name", "end.location.name", "distance", "duraction");
		}

		/**
		 * The property names of the date valued properties.
		 * 
		 * @return The list of properties with date value.
		 */
		public static List<String> getDateProperties() {
			return Arrays.asList("start.time", "end.time");
		}

		/**
		 * The property names of the integer valued properties.
		 * 
		 * @return The list of properties with integer value.
		 */
		public static List<String> getIntegerProperties() {
			return Arrays.asList("id", "start.location.id", "end.location.id", "distance", "duration");
		}

		/**
		 * The property names of the string valued properties.
		 * 
		 * @return The list of properties with date value.
		 */
		public static List<String> getStringProperties() {
			return Arrays.asList("start.location.id", "end.location.id");
		}

		/**
		 * The collection of the property values.
		 */
		private java.util.Map<String, Object> properties = new java.util.TreeMap<>();

		/**
		 * The property value map.
		 * 
		 * @return The map from property names to property values.
		 */
		protected java.util.Map<String, Object> properties() {
			return this.properties;
		}

		/**
		 * Create a new journey with all fields set undefined.
		 */
		public Journey() {

		}

		/**
		 * Is the current journey valid.
		 * 
		 * @return True, if and only if the current journey is valid.
		 */
		public boolean isValid() {
			return getPropertyNames().stream()
					.allMatch((String property) -> (validProperty(property, getProperty(property))));
		}

		/**
		 * Get the value of property.
		 * 
		 * @param property The property whose value is queried.
		 * @return Undefined value, if the given property has no value. Otherwise the
		 *         property value.
		 */
		public Object getProperty(String property) {
			return properties().get(property);
		}

		/**
		 * Set the value of property.
		 * 
		 * @param property The property, whose value is changed.
		 * @param value    The value of the property.
		 * @return The previous value of the property.
		 * @throws IllegalArgumentException Either the property or property value was
		 *                                  invalid.
		 * @throws ClassCastException       The value was of invalid type.
		 */
		public Object setProperty(String property, Object value) throws IllegalArgumentException, ClassCastException {
			if (validProperty(property, value)) {
				return properties().put(property, value);
			} else {
				throw new IllegalArgumentException("Invalid property value");
			}
		}

		/**
		 * The formatter formatting the given property.
		 * 
		 * @param property The property.
		 * 
		 * @return The formatter formatting the property value, if any exits.
		 */
		public Format propertyFormatter(String property) {
			if (this.isDateProperty(property)) {
				return new JourneyDateFormat(); 
			} else if (this.isIntegerProperty(property)) {
				return new PositiveIntegerFormat(); 
			} else if (this.isIntegerProperty(property)) {
				/**
				 * The format parsing strings.
				 */
				return new Format() {

					@Override
					public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
						if (obj == null) {
							return toAppendTo;
						} else {
							return toAppendTo.append(obj.toString());
						}
					}

					@Override
					public Object parseObject(String source, ParsePosition pos) {
						if (source == null) {
							if (pos.getIndex() >= 0) {
								pos.setErrorIndex(pos.getIndex());
								return null;
							} else {
								return "";
							}
						} else if (pos.getIndex() == source.length()) {
							// The position is at the end of string.
							return "";
						} else {
							// The content of the string from the position to the end of string.
							int index = pos.getIndex();
							pos.setIndex(source.length());
							return source.substring(index);
						}
					}

				};
			} else {
				// Unknown type - no format is used.
				return null;
			}
		}

		/**
		 * Tests the validity of the property.
		 * 
		 * @param property The property, whose validity is tested.
		 * @return True, if and only if the property is valid property.
		 */
		public boolean validProperty(String property) {
			return getPropertyNames().contains(property);
		}

		/** 
		 * Test the validity of the property value type. 
		 * @param property the property of the tested type. 
		 * @param value The tested value.
		 * @return true, if and only if the given property value is of valid type. 
		 */
		public boolean validPropertyType(String property, Object value) {
			return (isDateProperty(property) || isIntegerProperty(property) || isStringProperty(property));
		}
		
		/**
		 * Do the property has date value.  
		 * @param property The tested property.
		 * @return True, if and only if the given property has date value. 
		 */
		public boolean isDateProperty(String property) {
			return property != null && this.getDateProperties().contains(property); 
		}
		/**
		 * Do the property has integer value.  
		 * @param property The tested property.
		 * @return True, if and only if the given property has integer value. 
		 */
		public boolean isIntegerProperty(String property) {
			return property != null && this.getIntegerProperties().contains(property); 
		}
		/**
		 * Do the property has String value.  
		 * @param property The tested property.
		 * @return True, if and only if the given property has String value. 
		 */
		public boolean isStringProperty(String property) {
			return property != null && this.getStringProperties().contains(property); 
		}

		/**
		 * Tests the validity of the property value.
		 * 
		 * Invalid properties does not have valid values.
		 * 
		 * @param property The property, whose value is tested.
		 * @param value    The tested value of the property.
		 * @return True, if and only if the value is valid value for the property.
		 */
		public boolean validProperty(String property, Object value) {
			if (!(validProperty(property) && validPropertyType(property, value))) {
				return false;
			}
			return true; 
		}
	}

	/**
	 * The database using Journeys.
	 * 
	 * @author Antti Kautiainen
	 *
	 */
	public static class DatabaseJourneys extends Journeys {
		/**
		 * The connection to the Journeys database.
		 */
		private java.sql.Connection journeysDB = null;

		/**
		 * Creates a new collection of journeys from database connection.
		 * 
		 * @param db The database connection used to get the journey data.
		 */
		public DatabaseJourneys(java.sql.Connection db) {
			this.journeysDB = db;
		}

		@Override
		public boolean addJourney(Journey journey) throws IllegalArgumentException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Journey getJourney(int index) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	/**
	 * The CSV Document implementation of the journeys.
	 * 
	 * @author Antti Kautiainen
	 *
	 */
	public static class CSVJourneys extends Journeys implements Logging.MessageLogging {
		/**
		 * The CSV document storing the journeys.
		 */
		private CSVDocument journeysCSV = null;

		public CSVJourneys() throws CSVException {
			journeysCSV = new SimpleCSVDocument();
			initFields();
		}

		public void initFields() throws IllegalStateException, CSVException {
			journeysCSV.setFields(getJourneyPropertyNames());
		}

		@Override
		public boolean addJourney(Journey journey) throws IllegalArgumentException {
			if (journey == null) {
				// Undefined journey cannot be added to journeys.
				return false;
			}
			try {
				return journeysCSV.addDataRow(journey == null ? (List<String>) null
						: (getJourneyPropertyNames().stream().map((String property) -> {
							return String.valueOf(journey.getProperty(property));
						})).toList());
			} catch (CSVException e) {
				// This happen if the row is null
				severe("CSV Error {0} which should never happen", e);
				throw new IllegalArgumentException("Invalid journey", e);
			}
		}

		@Override
		public Journey getJourney(int index) {
			CSVDataRow data = journeysCSV.getDataRow(index);
			if (data == null) {
				return null;
			} else {
				Journey result = new Journey();

				return result;
			}
		}

	}

	/**
	 * The property names of the journeys.
	 * 
	 * @return The list of properties of the journeys.
	 */
	public List<String> getPropertyNames() {
		return Arrays.asList("journeys.count");
	}
	
	/**
	 * Creates a new empty journeys structure.
	 */
	public Journeys() {
	}

	/**
	 * The property names of the journeys.
	 * 
	 * @return The list of properties of the contained journeys.
	 */
	public List<String> getJourneyPropertyNames() {
		return Journey.getPropertyNames();
	}

	/**
	 * Add a new journey to the journeys.
	 * 
	 * @param journey The addded journey.
	 * @return True, if and only if the journey was added.
	 * @throws IllegalArgumentException The journey was invalid for this journeys
	 *                                  collection.
	 */
	public abstract boolean addJourney(Journey journey) throws IllegalArgumentException;

	/**
	 * Get the journey of the given index.
	 * 
	 * @param index The index of the journey.
	 * @return Undefined value, if the given index does not have journey. Otherwise
	 *         the journey of the given index.
	 */
	public abstract Journey getJourney(int index);
}
