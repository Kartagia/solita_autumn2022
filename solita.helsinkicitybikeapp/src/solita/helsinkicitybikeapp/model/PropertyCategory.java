package solita.helsinkicitybikeapp.model;

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
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

/**
 * Property category implements sub property mapping.
 * 
 * @author kautsu
 *
 */
public class PropertyCategory extends Properties {

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