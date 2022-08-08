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
import java.util.function.Predicate;

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

	/**
	 * The positive integer format.
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
		public List<String> getPropertyNames() {
			return Journeys.this.getPropertyNames();
		}

		/**
		 * The property names of the date valued properties.
		 * 
		 * @return The list of properties with date value.
		 */
		public List<String> getDateProperties() {
			return Journeys.this.getJourneyDateProperties();
		}

		/**
		 * The property names of the integer valued properties.
		 * 
		 * @return The list of properties with integer value.
		 */
		public List<String> getIntegerProperties() {
			return Journeys.this.getJourneyIntegerProperties();
		}

		/**
		 * The property names of the string valued properties.
		 * 
		 * @return The list of properties with date value.
		 */
		public List<String> getStringProperties() {
			return Journeys.this.getJourneyStringProperties();
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
		 * 
		 * @param property the property of the tested type.
		 * @param value    The tested value.
		 * @return true, if and only if the given property value is of valid type.
		 */
		public boolean validPropertyType(String property, Object value) {
			return (isDateProperty(property) || isIntegerProperty(property) || isStringProperty(property));
		}

		/**
		 * Do the property has date value.
		 * 
		 * @param property The tested property.
		 * @return True, if and only if the given property has date value.
		 */
		public boolean isDateProperty(String property) {
			return property != null && this.getDateProperties().contains(property);
		}

		/**
		 * Do the property has integer value.
		 * 
		 * @param property The tested property.
		 * @return True, if and only if the given property has integer value.
		 */
		public boolean isIntegerProperty(String property) {
			return property != null && this.getIntegerProperties().contains(property);
		}

		/**
		 * Do the property has String value.
		 * 
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
			switch (property) {
			case "duration":
			case "distance":
				if ((Integer) value < 0) {
					// Negative duration or distance.
					return false;
				}
			case "end.date":
				if (value == null)
					return true;
				Date startDate = (Date) this.getProperty("start.date");
				try {
					if (startDate == null || startDate.compareTo((Date) value) > 0) {
						// End requires start time exists, and no time travel happens.
						return false;
					}
				} catch (ClassCastException cce) {
					return false;
				}
				break;
			}
			return true;
		}
	}

	/**
	 * The property names of the journeys.
	 * 
	 * @return The list of properties of the journeys collection.
	 */
	public List<String> getPropertyNames() {
		return Arrays.asList("journeys.count");
	}

	/**
	 * The integer valued property names of journeys.
	 * 
	 * @return The list of properties of the journeys collection with integer value.
	 */
	public List<String> getIntegerPropertyNames() {
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
		return Arrays.asList("id", "start.time", "end.time", "start.location.id", "end.location.id",
				"start.location.name", "end.location.name", "distance", "duraction");
	}

	/**
	 * The property names of the date valued properties.
	 * 
	 * @return The list of properties with date value.
	 */
	public List<String> getJourneyDateProperties() {
		return Arrays.asList("start.time", "end.time");
	}

	/**
	 * The property names of the integer valued properties.
	 * 
	 * @return The list of properties with integer value.
	 */
	public List<String> getJourneyIntegerProperties() {
		return Arrays.asList("id", "start.location.id", "end.location.id", "distance", "duration");
	}

	/**
	 * The property names of the string valued properties.
	 * 
	 * @return The list of properties with date value.
	 */
	public List<String> getJourneyStringProperties() {
		return Arrays.asList("start.location.id", "end.location.id");
	}

	/**
	 * Do the property has date value.
	 * 
	 * @param property The tested property.
	 * @return True, if and only if the given property has date value.
	 */
	public boolean isDateProperty(String property) {
		return property != null && this.getJourneyDateProperties().contains(property);
	}

	/**
	 * Do the property has integer value.
	 * 
	 * @param property The tested property.
	 * @return True, if and only if the given property has integer value.
	 */
	public boolean isIntegerProperty(String property) {
		return property != null && this.getJourneyIntegerProperties().contains(property)
				|| this.getIntegerPropertyNames().contains(property);
	}

	/**
	 * Do the property has String value.
	 * 
	 * @param property The tested property.
	 * @return True, if and only if the given property has String value.
	 */
	public boolean isStringProperty(String property) {
		return property != null && this.getJourneyStringProperties().contains(property);
	}

	/**
	 * Add a new journey to the journeys.
	 * 
	 * @param journey The added journey.
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


	/**
	 * Generic property search condition. 
	 * @author Antti Kautiainen
	 *
	 * @param <TYPE> The type of the tested value. 
	 */
	public static abstract class PropertySearchCondition<TYPE> implements Predicate<TYPE> {
		/**
		 * The tested property name. 
		 */
		private final String propertyName;

		/**
		 * The list of conditions the tested object has to pass. If this list is empty, 
		 * any value passes.  
		 */
		private final java.util.List<Predicate<Object>> predicates = new java.util.ArrayList<>();

		/**
		 * Create a search condition for given property name.
		 * 
		 * @param propertyName   The property name.
		 * @param valuePredicate The value predicate.
		 */
		public PropertySearchCondition(String propertyName, Predicate<Object> valuePredicate) {
			this.propertyName = propertyName;
			if (valuePredicate != null) {
				predicates.add(valuePredicate);
			}
		}

		/**
		 * Create a search condition for given property name.
		 * 
		 * @param propertyName   The property name.
		 * @param valuePredicate The value predicate.
		 */
		public PropertySearchCondition(String propertyName,
				java.util.Collection<? extends Predicate<Object>> valuePredicates) {
			this.propertyName = propertyName;
			if (valuePredicates != null) {
				// Adding all non-null values to the predicates.
				predicates.addAll(valuePredicates.stream().filter((Predicate<Object> pred) -> (pred != null)).toList());
			}
		}

		/**
		 * The property name of the tested property. 
		 * @return The tested property name. 
		 */
		public String getPropertyName() {
			return this.propertyName; 
		}
		
		/**
		 * The predicates used to test the property value. 
		 * @return An unmodifiable list of predicates. 
		 */
		public java.util.List<Predicate<Object>> getPredicates() {
			return java.util.Collections.unmodifiableList(this.predicates); 
		}
		
		/**
		 * Acquiring the property value of the object. 
		 * @param object the object. 
		 * @return The property value of the given object.
		 */
		public abstract Object getPropertyValue(TYPE object);

		/**
		 * Test the journey fitting the current predicate.
		 */
		public boolean test(TYPE object) {
			Object propertyValue = getPropertyValue(object); 
			return getPredicates().stream()
					.allMatch((Predicate<Object> tester) -> (tester.test(propertyValue)));
		}

	}
	
	
	/**
	 * Journey property search condition. 
	 * @author Antti Kautiainen
	 *
	 */
	public class JourneyPropertySearchCondition extends PropertySearchCondition<Journeys.Journey> {

		/**
		 * Creates a new property search condition for journey.
		 * @param propertyName
		 * @param predicate
		 */
		public JourneyPropertySearchCondition(String propertyName, Predicate<Object> predicate) {
			super(propertyName, predicate); 
		}
		
		@Override
		public Object getPropertyValue(Journey object) {
			return (object == null?null:object.getProperty(this.getPropertyName()));
		}
		
	}

	/**
	 * The property captions.
	 */
	private java.util.Map<String, String> propertyCaptions = new java.util.TreeMap<>();

	/**
	 * The property captions mapping.
	 * 
	 * @return The map storing property captions.
	 */
	protected java.util.Map<String, String> getPropertyCaptions() {
		return this.propertyCaptions;
	}

	/**
	 * Set property caption.
	 * 
	 * @param propertyName The property name.
	 * @param caption      The caption. If this value is null, the value is unset.
	 * @throws IllegalArgumentException The given caption was already reserved by
	 *                                  another field.
	 */
	public void setPropertyCaption(String propertyName, String caption)
			throws NullPointerException, IllegalArgumentException {
		if (getPropertyNames().contains(propertyName)) {
			this.propertyCaptions.put(propertyName, caption);
		} else {
			throw new IllegalArgumentException(format("Unknown property name"));
		}
	}

	/**
	 * The caption of the given property.
	 * 
	 * @param propertyName The property name.
	 * @return The caption for the property, if such caption exists. If it does not
	 *         an undefined (<code>Null</code>) value is returned.
	 */
	public String getPropertyCaption(String propertyName) {
		return propertyCaptions.get(propertyName);
	}
}
