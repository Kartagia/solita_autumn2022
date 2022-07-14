package solita.helsinkicitybikeapp.model;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * The configuration of the Journeys application.
 * 
 * @author Antti Kautiainen
 *
 */
public class Config extends Properties {

	/**
	 * The default properties generated from system properties.
	 * 
	 */
	protected static final Properties DEFAULT_PROPERTIES;

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
	public static final String DEFAULT_DATABASE_PROPERTY_NAME = "journeys.db.name";

	/**
	 * The property name of the property containing the database host.
	 */
	public static final String DEFAULT_DATABASE_HOST_PROPERTY_NAME = "journeys.db.host";

	/**
	 * The property name of the property containing the database connection port.
	 */
	public static final String DEFAULT_DATABASE_PORT_PORPERTY_NAME = "journeys.db.port";

	/**
	 * The property name of the property containing the database user name.
	 */
	public static final String DEFAULT_DATABASE_USER_PORPERTY_NAME = "journeys.db.user";

	/**
	 * The property name of the property containing the database user password..
	 */
	public static final String DEFAULT_DATABASE_USER_SECRET_PORPERTY_NAME = "journeys.db.user.secret";

	/**
	 * The default name of the configuration file.
	 */
	public static final String DEFAULT_PROPERTY_XML_FILENAME = "journeys_config.xml";

	/**
	 * Property path class allows creating property paths.
	 * 
	 * By default the property names must be defined strings.
	 * 
	 * @author Antti Kautiainen
	 *
	 */
	public static class PropertyPath {

		/**
		 * The parent property path.
		 */
		private PropertyPath parent = null;

		/**
		 * The base property name.
		 */
		private String basePropertyName;

		/**
		 * The delimiter between base property name and content property names.
		 */
		private String delimiter = ".";

		/**
		 * Creates a new property path with given base property.
		 * 
		 * @param baseProperty
		 * @throws IllegalArgumentException The base property was invalid.
		 */
		public PropertyPath(String baseProperty) throws java.lang.IllegalArgumentException {
			this(baseProperty, null);
		}

		/**
		 * Creates a new property path with a parent path and a base property name.
		 * 
		 * @param parent           The parent path. Undefined value indicates there is
		 *                         no parent.
		 * @param basePropertyName The property name of the child path name.
		 */
		public PropertyPath(PropertyPath parent, String basePropertyName) {
			this(basePropertyName, parent == null ? null : parent.getDelimiter());
			this.parent = parent;
		}

		/**
		 * The delimiter of the path.
		 * 
		 * @return The delimiter of the path.
		 */
		public String getDelimiter() {
			return this.delimiter;
		}

		/**
		 * Creates a new Property Path with given base property, delimiter, and default
		 * properties.
		 * 
		 * @param baseProperty The base property name.
		 * @param delimiter    The delimiter between base property and the content
		 *                     property name.
		 * @throws IllegalArgumentException Any parameter was invalid.
		 */
		public PropertyPath(String baseProperty, String delimiter) throws java.lang.IllegalArgumentException {
			this.basePropertyName = baseProperty;
			if (delimiter != null) {
				this.delimiter = delimiter;
			}
		}

		public String getBasePropertyName() {
			return this.basePropertyName;
		}

		/**
		 * The path to the base property path name.
		 * 
		 * @return The string containing the property path.
		 */
		public String getBasePathName() {
			return parent == null ? this.getBasePropertyName() : parent.getPropertyName(this.getBasePropertyName());
		}

		/**
		 * The base property path.
		 * 
		 * @return The path of the parent property. If this is undefined,
		 */
		public PropertyPath getBasePath() {
			return parent;
		}

		/**
		 * The path created by combining this path with given path.
		 * 
		 * @param contentProperty The content property name.
		 * @return The new property path for the given content.
		 * @throws IllegalArgumentException
		 */
		public PropertyPath getPath(String contentProperty) throws IllegalArgumentException {
			if (this.validContentPropertyName(contentProperty)) {
				return (contentProperty == null ? this : new PropertyPath(this, contentProperty));
			} else {
				throw new IllegalArgumentException("Invalid content property name");
			}
		}

		/**
		 * The parent of the current path.
		 * 
		 * @return The parent path of the current path.
		 */
		public PropertyPath getParentPath() {
			return this.parent;
		}

		/**
		 * How long the current path is.
		 * 
		 * @return The number of steps the path has to the root node.
		 */
		public int length() {
			return (parent == null ? 0 : parent.length() + 1);
		}

		/**
		 * The path entries of the path.
		 * 
		 * The entries includes the delimiters of the path between property names.
		 * 
		 * @return The list of path entries the path is formed from.
		 */
		public java.util.List<String> getEntries() {
			PropertyPath parent = this.getParentPath();
			java.util.List<String> result = (parent == null ? new java.util.ArrayList<>() : parent.getEntries());
			if (parent != null)
				result.add(parent.getDelimiter());
			result.add(this.getBasePropertyName());
			return result;
		}

		/**
		 * The shared path with current path.
		 * 
		 * @param path The path whose shared path with current is queried.
		 * @return The shared path, if any exists. Undefined value otherwise.
		 */
		public PropertyPath getSharedPath(PropertyPath path) {
			PropertyPath result = null;
			if (path != null) {
				PropertyPath myParent = this.parent, otherParent = path.parent;
				List<String> myEntries = this.getEntries();
				List<String> pathEntries = path.getEntries();
				boolean divergent = false;
				int myLen = myEntries.size(), pathLen = pathEntries.size(), len;

				// Setting the default result to the smaller of the two property paths.
				if (pathLen < myLen) {
					len = pathLen;
					result = path;
				} else {
					len = myLen;
					result = this;
				}
				for (int i = 0; (i < len); i++) {
					if (divergent) {
						// We are passing through sub-path tail which is not shared.
						// - Moving the result to its parent path.
						if (i % 2 == 1) {
							// The delimiter entry moves the result to parent.
							// (Every odd index is delimiter index, which moves the result to its parent)
							result = result.getParentPath();
						}
					} else if (myEntries.get(i) != pathEntries.get(i)) {
						// WE found divergent path - the current path is set as path, as the future
						// steps
						if (i == 0) {
							// The first step causes the difference - there is no shared path.
							// (The loop would give correct answer even without this change, but the
							// automatic
							// return optimizes the path).
							return null;
						} else {
							// move the result backwards to the parents to get the correct shared path.
							//
							// Using the loop to move result property path to its parent as many times
							// as the result has divergent path element.
							divergent = true;
						}
					}
				}
			}
			return result;
		}

		/**
		 * Test the delimiter.
		 * 
		 * @param delimiter The tested delimiter.
		 * @return True, if and only if the given delimiter value is valid delimiter
		 *         value.
		 */
		public boolean validDelimiter(String delimiter) {
			return true;
		}

		/**
		 * Test the base path.
		 * 
		 * @param basePath The tested base bath.
		 * @return True, if and only if the base path is valid.
		 */
		public boolean validBasePath(String basePath) {
			return basePath == null || !basePath.isEmpty();
		}

		/**
		 * Test the validity of property name.
		 * 
		 * @param propertyName The tested property name.
		 * @return True, if and only if the given property name is valid property name.
		 */
		public boolean validPropertyName(String propertyName) {
			return propertyName != null;
		}

		/**
		 * Test the validity of the content property name.
		 * 
		 * @param propertyName The tested property name.
		 * @return True, if and only if the given content property name is valid.
		 */
		public boolean validContentPropertyName(String propertyName) {
			return propertyName == null || validPropertyName(propertyName);
		}

		/**
		 * The property name for the property with given base property, delimiter, and
		 * property name.
		 * 
		 * If both the base property name, and the property name are undefined, an empty
		 * string is returned.
		 * 
		 * If the base property name is undefined, the property name is returned.
		 * 
		 * If the property name is undefined, the base property name is returned.
		 * 
		 * Otherwise both property name and base property name are defined, and the
		 * compound property name is created by combining the base property name with
		 * delimiter followed by the property name.
		 * 
		 * @param baseProperty The base property name. If this is null, the prefix is
		 *                     empty string.
		 * @param delimiter    The delimiter separating the prefix and the property
		 *                     name. Defaults to dot ('.').
		 * @param propertyName The property name. Defaults to no property name.
		 * @return The property name with given base property name, delimiter, and
		 *         property name.
		 */
		public static String getPropertyName(String baseProperty, String delimiter, String propertyName) {
			if (propertyName == null) {
				return (baseProperty == null ? "" : baseProperty);
			} else {
				return (baseProperty == null ? "" : (baseProperty + (delimiter == null ? "." : delimiter)))
						+ propertyName;
			}
		}

		/**
		 * The property name of the given content property name.
		 * 
		 * If the content property name is undefined (<code>null</code>), the base
		 * property name is returned.
		 * 
		 * @param contentPropertyName The queried property name.
		 * @return If the given content property name is undefined, the base property
		 *         name. Otherwise the full property name of the given content property
		 *         name.
		 * @throws IllegalArgumentException The given property name is invalid.
		 */
		public String getPropertyName(String contentPropertyName) throws java.lang.IllegalArgumentException {
			if (this.validContentPropertyName(contentPropertyName)) {
				return getPropertyName(this.basePropertyName, this.delimiter, contentPropertyName);
			} else {
				throw new IllegalArgumentException("Invalid content property name!");
			}
		}

		/**
		 * The property names of property content names.
		 * 
		 * @param propertyNames The property content names.
		 * @return the list containing the absolute property names of the given property
		 *         names.
		 * @throws NullPointerExcpetion The given property names was undefined.
		 */
		public java.util.Set<String> getPropertyNames(java.util.Collection<String> propertyNames) {
			java.util.TreeSet<String> result = new java.util.TreeSet<>();
			propertyNames.parallelStream().forEach((String key) -> {
				result.add(this.getPropertyName(key));
			});
			return result;
		}

		/**
		 * The string representation of the path is the combination of path entries.
		 * 
		 * @return {@inheritDoc}
		 */
		@Override
		public String toString() {
			return String.join("", this.getEntries());
		}
	}

	/**
	 * Property category implements sub property mapping.
	 * 
	 * @author kautsu
	 *
	 */
	public static class PropertyCategory extends Properties {

		/**
		 * The path of the category root.
		 */
		private final PropertyPath path;

		/**
		 * The content properties of the property path.
		 */
		private final Properties contentProperties;

		/**
		 * Creates a new property path.
		 * 
		 * @param path                     The path of the property category root.
		 * @param defaultContentProperties The default content properties.
		 */
		public PropertyCategory(PropertyPath path, Properties defaultContentProperties) {
			super();
			this.path = path;
			this.contentProperties = new Properties(defaultContentProperties);
		}

		/**
		 * The path of the property category.
		 * 
		 * @return The path of the property category. If undefined, the class acts like
		 *         a properties wrapper.
		 */
		public PropertyPath getPath() {
			return this.path;
		}

		/**
		 * Test the base path.
		 * 
		 * @param basePath The tested base bath.
		 * @return True, if and only if the base path is valid.
		 */
		public boolean validBasePath(String basePath) {
			PropertyPath path = this.getPath();
			return path == null || path.validBasePath(basePath);
		}

		/**
		 * Test validity of any property value.
		 * 
		 * @param propertyName The tested property name.
		 * @return True, if and only if the given property name is valid property name.
		 */
		public static boolean isValidPropertyName(String propertyName) {
			return propertyName != null;
		}

		/**
		 * Test the validity of property name.
		 * 
		 * @param propertyName The tested property name.
		 * @return True, if and only if the given property name is valid property name.
		 */
		public boolean validPropertyName(String propertyName) {
			if (!isValidPropertyName(propertyName)) {
				return false;
			}
			return propertyName.startsWith(this.getPrefix());
		}

		/**
		 * Test the validity of the content property name.
		 * 
		 * @param propertyName The tested property name.
		 * @return True, if and only if the given content property name is valid.
		 */
		public boolean validContentPropertyName(String propertyName) {
			PropertyPath path = this.getPath();
			if (path == null) {
				return this.isValidPropertyName(propertyName);
			} else {
				try {
					return path.validContentPropertyName(propertyName)
							&& this.isValidPropertyName(this.getContentPropertyName(propertyName));
				} catch (java.util.NoSuchElementException ex) {
					return false;
				}
			}
		}

		/**
		 * Get the category prefix of the category.
		 * 
		 * @return String containing hte category prefix string.
		 */
		public String getPrefix() {
			PropertyPath path = this.getPath();
			return path == null ? "" : path.toString() + path.getDelimiter();
		}

		/**
		 * Get the content property name of the given property name.
		 * 
		 * @param propertyName The property name whose content property name is queried.
		 * @return Undefined value (<code>null</code>), if the given property name is
		 *         not valid property name for this category property names.
		 * @throws java.util.NoSuchElementException The given property name was not a
		 *                                          valid property name for this
		 *                                          category.
		 */
		public String getContentPropertyName(String propertyName) throws java.util.NoSuchElementException {
			if (propertyName != null) {
				// The property name exists, thus we have something to do.
				PropertyPath path = this.getPath();
				if (path != null) {
					String pathString = path.toString(), delim = path.getDelimiter();
					if (pathString.equals(propertyName)) {
						// The path is valid reference to the category name, and thus the content
						// property is undefined.
						return null;
					} else if (propertyName.startsWith(pathString + delim)) {
						// The path is valid reference to the property name
						// - getting the remaining property name.
						return propertyName.substring(pathString.length() + delim.length());
					}
				} else {
					// There is no parent.
					return propertyName;
				}
			}

			// There was an error
			throw new java.util.NoSuchElementException("Invalid property name");
		}

		@Override
		public synchronized Set<String> stringPropertyNames() {
			return path == null ? new TreeSet<String>()
					: path.getPropertyNames(this.contentProperties.stringPropertyNames());
		}

		@Override
		public synchronized String getProperty(String propertyName) {
			try {
				// Getting the property value.
				return this.contentProperties.getProperty(getContentPropertyName(propertyName));
			} catch (java.util.NoSuchElementException ex) {
				// The property was invalid.
				return null;
			}
		}

		/**
		 * Test the validity of the given property value.
		 * 
		 * No valid value exists for invalid property names.
		 * 
		 * @param propertyName The property name.
		 * @param value        The value candidate for the given property.
		 * @return True, if and only if the value is a valid value for the given
		 *         property name.
		 */
		public boolean validPropertyValue(String propertyName, String value) {
			return this.validPropertyName(propertyName);
		}

		/**
		 * Test the validity of a content property value.
		 * 
		 * No valid value exists for invalid content property names.
		 * 
		 * @param contentPropertyName The content property name.
		 * @param value               The content property value candidate.
		 * @return True, if and only if the value is a valid value for the given content
		 *         property.
		 */
		public boolean validContentPropertyValue(String contentPropertyName, String value) {
			return this.validPropertyName(contentPropertyName);
		}

		@Override
		public synchronized Object setProperty(String propertyName, String value) {
			if (this.validPropertyName(propertyName)) {
				try {
					String contentPropertyName = this.getContentPropertyName(propertyName);
					if (validPropertyValue(contentPropertyName, value)) {
						return this.contentProperties.setProperty(contentPropertyName, value);
					} else {
						throw new IllegalArgumentException("Invalid property value");
					}
				} catch (java.util.NoSuchElementException ex) {
					// The property was invalid.
					throw new IllegalArgumentException("Invalid pproperty name");
				}
			} else {
				return null;
			}
		}

		/**
		 * Get the content properties of the category.
		 * 
		 * @return The content properties of the category.
		 */
		public synchronized Properties getContentProperties() {
			return this.contentProperties;
		}

		@Override
		public void store(Writer writer, String comments) throws IOException {
			// TODO Auto-generated method stub
			super.store(writer, comments);
		}

		@Override
		public Enumeration<Object> keys() {
			// TODO Auto-generated method stub
			return super.keys();
		}

		@Override
		public Object get(Object key) {
			if (key instanceof String && this.validPropertyName((String)key)) {
				return this.getProperty((String)key); 
			} else {
				return null; 
			}
		}

		@Override
		public Set<Object> keySet() {
			// TODO Auto-generated method stub
			return new AbstractSet<Object>() {

				
				
				@Override
				public Iterator<Object> iterator() {
					return new Iterator() {
						private Iterator iter = PropertyCategory.this.stringPropertyNames().iterator();
						
						@Override
						public boolean hasNext() {
							return iter.hasNext(); 
						}
						
						@Override
						public Object next() {
							return (Object)iter.next(); 
						}
						
						@Override
						public void remove() {
							iter.remove(); 
						}
					};
				}

				@Override
				public int size() {
					return PropertyCategory.this.size(); 
				}
				
			};
		}

		@Override
		public void store(OutputStream out, String comments) throws IOException {
			// TODO Auto-generated method stub
			super.store(out, comments);
		}

		@Override
		public void storeToXML(OutputStream os, String comment) throws IOException {
			// TODO Auto-generated method stub
			super.storeToXML(os, comment);
		}

		@Override
		public void storeToXML(OutputStream os, String comment, String encoding) throws IOException {
			// TODO Auto-generated method stub
			super.storeToXML(os, comment, encoding);
		}

		@Override
		public void storeToXML(OutputStream os, String comment, Charset charset) throws IOException {
			// TODO Auto-generated method stub
			super.storeToXML(os, comment, charset);
		}

		@Override
		public String getProperty(String key, String defaultValue) {
			if (this.validPropertyName(key)) {
				String value = getProperty(key);
				return (value == null ? defaultValue : value);
			} else {
				return null;
			}
		}

		@Override
		public Enumeration<?> propertyNames() {
			return new Enumeration<String>() {
				java.util.Iterator<String> iterator = PropertyCategory.this.stringPropertyNames().iterator();

				@Override
				public boolean hasMoreElements() {
					return iterator.hasNext();
				}

				@Override
				public String nextElement() {
					return iterator.next();
				}

			};
		}

		@Override
		public int size() {
			return this.contentProperties.size();
		}

		@Override
		public Enumeration<Object> elements() {

			return new Enumeration<Object>() {
				java.util.Iterator<Object> iterator = PropertyCategory.this.contentProperties.values().iterator();

				@Override
				public boolean hasMoreElements() {
					return iterator.hasNext();
				}

				@Override
				public Object nextElement() {
					return iterator.next();
				}

			};
		}

		@Override
		public boolean contains(Object value) {
			return (value instanceof String) && this.validPropertyName((String) value)
					&& this.contentProperties.contains(value);
		}

		@Override
		public boolean containsValue(Object value) {
			return this.contentProperties.containsValue(value);
		}

		@Override
		public boolean containsKey(Object key) {
			return (key instanceof String) && this.validPropertyName((String) key)
					&& this.contentProperties.contains(this.getContentPropertyName((String)key));
		}

		@Override
		public synchronized Object put(Object key, Object value) {
			return this.setProperty((String) key, (String) value);
		}

		@Override
		public synchronized Object remove(Object key) {
			if (key instanceof String && this.validPropertyName((String) key)) {
				// Trying to remove the content property name.
				return this.contentProperties.remove(this.getContentPropertyName((String) key));
			} else {
				// Nothing to remove.
				return null;
			}
		}

		@Override
		public synchronized void clear() {
			this.contentProperties.clear();
		}

		@Override
		public synchronized String toString() {
			// TODO Auto-generated method stub
			return super.toString();
		}

		@Override
		public Collection<Object> values() {
			return new AbstractCollection<Object>() {

				@Override
				public Iterator<Object> iterator() {
					return PropertyCategory.this.contentProperties.values().iterator();
				}

				@Override
				public int size() {
					return PropertyCategory.this.contentProperties.size();
				}

			};
		}

		@Override
		public Set<java.util.Map.Entry<Object, Object>> entrySet() {
			return new Set<java.util.Map.Entry<Object, Object>>() {

				private Set<java.util.Map.Entry<Object, Object>> entrySet = PropertyCategory.this.contentProperties
						.entrySet();

				/**
				 * Get set entry from content entry.
				 * 
				 * @param entry The content entry.
				 * @return The map entry
				 */
				protected java.util.Map.Entry<Object, Object> convertEntry(java.util.Map.Entry<Object, Object> entry) {
					return new AbstractMap.SimpleEntry(PropertyCategory.this.getPropertyName((String) entry.getKey()),
							entry.getValue()) {

						@Override
						public Object getValue() {
							return entry.getValue();
						}

						@Override
						public Object setValue(Object value) {
							return entry.setValue(value);
						}

					};
				}

				/**
				 * Generating content entry from from set entry.
				 * 
				 * @param entry The entry set entry converted into content entry.
				 * @return The content entry of the given external entry, if any exists.
				 */
				protected java.util.Map.Entry<Object, Object> contentEntry(java.util.Map.Entry<Object, Object> entry) {
					try {
						return new AbstractMap.SimpleEntry(
								PropertyCategory.this.getContentPropertyName((String) entry.getKey()),
								entry.getValue());
					} catch (Exception e) {
						throw new IllegalArgumentException("Invalid entry");
					}
				}

				@Override
				public Iterator<java.util.Map.Entry<Object, Object>> iterator() {
					return new Iterator<java.util.Map.Entry<Object, Object>>() {

						private Iterator<java.util.Map.Entry<Object, Object>> iter = entrySet.iterator();

						@Override
						public boolean hasNext() {
							return iter.hasNext();
						}

						@Override
						public java.util.Map.Entry<Object, Object> next() {
							return convertEntry(iter.next());
						}

						@Override
						public void remove() {
							iter.remove(); 
						}

						
					};
				}

				@Override
				public int size() {
					return entrySet.size();
				}

				@Override
				public boolean isEmpty() {
					return entrySet.isEmpty();
				}

				@Override
				public boolean contains(Object o) {
					try {
						return o instanceof java.util.Map.Entry
								&& entrySet.contains(contentEntry((java.util.Map.Entry<Object, Object>) o));
					} catch (Exception e) {
						return false;
					}
				}

				@Override
				public Object[] toArray() {
					return entrySet.toArray();
				}

				@Override
				public <T> T[] toArray(T[] a) {
					return entrySet.toArray(a);
				}

				@Override
				public boolean add(java.util.Map.Entry<Object, Object> e) {
					java.util.Map.Entry<Object, Object> contentEntry;
					try {
						contentEntry = contentEntry(e); 
					} catch(Exception ex) {
						throw new IllegalArgumentException("Invlaid property name"); 
					}
					if (PropertyCategory.this.validPropertyValue((String) contentEntry.getKey(), (String) contentEntry.getValue())) {
						return entrySet.add(contentEntry);
					} else {
						throw new IllegalArgumentException("Invalid property value");
					}
				}

				@Override
				public boolean remove(Object o) {
					java.util.Map.Entry<Object, Object> contentEntry;
					try {
						contentEntry = contentEntry((java.util.Map.Entry<Object, Object>)o); 
					} catch(Exception ex) {
						return false; 
					}
					return entrySet.remove(contentEntry);
				}

				@Override
				public boolean containsAll(Collection<?> c) {
					return entrySet.containsAll(c);
				}

				@Override
				public boolean addAll(Collection<? extends java.util.Map.Entry<Object, Object>> c) {
					boolean result = false;
					for (java.util.Map.Entry<Object, Object> entry : c) {
						result |= this.add(entry);
					}
					return result;
				}

				@Override
				public boolean retainAll(Collection<?> c) {
					boolean result = false;
					Iterator<java.util.Map.Entry<Object, Object>> iter = this.iterator();
					java.util.Map.Entry<Object, Object> entry;
					while (iter.hasNext()) {
						if (!c.contains(iter.next())) {
							iter.remove();
							result = true;
						}
					}
					return result;
				}

				@Override
				public boolean removeAll(Collection<?> c) {
					boolean result = false;
					Iterator<java.util.Map.Entry<Object, Object>> iter = this.iterator();
					java.util.Map.Entry<Object, Object> entry;
					while (iter.hasNext()) {
						if (c.contains(iter.next())) {
							iter.remove();
							result = true;
						}
					}
					return result;
				}

				@Override
				public void clear() {
					entrySet.clear();
				}

			};
		}

		/**
		 * The property name of the given content property name.
		 * 
		 * @param key The content property name.
		 * @return The property name for the given content property name. If the key is
		 *         undefined, the name of the category is returned.
		 */
		protected String getPropertyName(String key) {
			PropertyPath path = this.getPath();
			return (path == null ? key : path.getPropertyName(key));
		}

		@Override
		public Object getOrDefault(Object key, Object defaultValue) {
			Object result = this.get(key); 
			return (result == null?defaultValue:result); 
		}

		@Override
		public synchronized Object putIfAbsent(Object key, Object value) {
			Object result = this.get(key); 
			return (result == null?put(key,value):result); 
		}

		@Override
		public synchronized boolean remove(Object key, Object value) {
			Object curVal = get(key); 
			if (value == curVal) {
				this.remove(key);
				return true; 
			} else {
				return false; 
			}
		}

		@Override
		public synchronized boolean replace(Object key, Object oldValue, Object newValue) {
			if (remove(key, oldValue)) {
				put(key, newValue);
				return true; 
			} else {
				return false; 
			}
		}

		@Override
		public synchronized Object replace(Object key, Object value) {
			if (containsKey(key)) {
				return this.put(key,  value); 
			} else {
				return false; 
			}
		}

	}

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
	 */
	public Config(File configFile, Properties defaultValues) {
		super(defaultValues);
	}

	/**
	 * Static initializer creating the default properties from system properties.
	 */
	static {
		DEFAULT_PROPERTIES = new Properties();

		String propertyFileName = System.getProperty(XML_PROPERTY_FILE_PROPERTY_NAME, DEFAULT_PROPERTY_XML_FILENAME);

		File propertyFile = new File(propertyFileName);

		/**
		 * The filter accepting only existing readable configuration file.
		 * 
		 * If the given property file name is absolute path, the configuration file
		 * filter is undefined.
		 */
		java.io.FileFilter configFileFilter = (propertyFile.isAbsolute() ? null : new java.io.FileFilter() {

			@Override
			public boolean accept(File pathname) {
				// The configuration file has to be a readable file ending with the property
				// file name.
				return (pathname.isFile() && pathname.canRead() && pathname.toPath().endsWith(propertyFileName));
			}

		});

		// Setting the base directory.
		File[] fileList = null;
		boolean found = false;
		String directoryName = null;
		File dir;
		for (String propertyName : new String[] { BASE_DIRECTORY_PROPERTY_NAME, "user.dir", "user.home" }) {
			try {
				directoryName = System.getProperty(propertyName);
				if (directoryName != null) {
					dir = new java.io.File(directoryName);
					if (dir.exists() && dir.canRead() && dir.isDirectory()) {

						if (configFileFilter == null || (fileList = dir.listFiles(configFileFilter)).length >= 1) {
							// The configuration file exists.
							// The directory is valid starting directory.
							DEFAULT_PROPERTIES.setProperty(BASE_DIRECTORY_PROPERTY_NAME, directoryName);
							found = true;
							break; // Exiting loop as the property was set.
						}
					}
				}
			} catch (SecurityException se) {
				// The acquisition of the property failed.
				// - Trying the next option.
			}
		}
		if (!found) {
			// The setting of the base directory failed.

		}

	}

}
