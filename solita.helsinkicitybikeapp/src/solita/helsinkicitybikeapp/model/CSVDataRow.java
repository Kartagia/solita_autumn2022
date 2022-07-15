package solita.helsinkicitybikeapp.model;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;

/**
 * CSV Data Row implements both list and map interfaces.
 * 
 * The mapping is from field names to the field values. CSV Data Row without
 * field names will use default names of indexes.
 * 
 * The list is list of field values.
 * 
 * @author Antti Kautiainen
 *
 */
public interface CSVDataRow extends List<String> {

	/**
	 * The cSV data row as map of field values.
	 * 
	 * The changes on the map reflects to the data row and vice versa. 
	 * 
	 * @return The map view of the CSV data row. The changes on the map will reflect
	 *         changes on the data row.
	 */
	default Map<String, String> asMap() {
		return new AbstractMap<String, String>() {

			@Override
			public Set<Entry<String, String>> entrySet() {
				return new java.util.AbstractSet<Map.Entry<String, String>>() {

					@Override
					public Iterator<Entry<String, String>> iterator() {
						return new Iterator<Entry<String, String>>() {
							private int index = 0;

							@Override
							public boolean hasNext() {
								return index < CSVDataRow.this.size();
							}

							@Override
							public Map.Entry<String, String> next() {
								if (hasNext()) {
									final int entryIndex = index++;
									return new Map.Entry<String, String>() {

										/**
										 * The key of the entry is always the string representation of the index.
										 */
										private final String key = Integer.toString(entryIndex);

										@Override
										public String getKey() {
											return key;
										}

										@Override
										public String getValue() {
											return get(entryIndex);
										}

										@Override
										public String setValue(String value) {
											String result = get(entryIndex);
											set(entryIndex, value);
											return result;
										}

									};
								} else {
									throw new NoSuchElementException("No more elements");
								}
							}
						};
					}

					@Override
					public int size() {
						return CSVDataRow.this.size();
					}

				};
			}

			@Override
			public String get(Object key) {
				return (key instanceof String) ? get((String) key) : null;
			}

			@Override
			public String put(String key, String value) {
				return set(key, value);
			}

		};
	}

	@Override
	public int size();

	@Override
	public String get(int index) throws IndexOutOfBoundsException;

	/**
	 * The field names.
	 * 
	 * @return The list of field names.
	 */
	default List<String> fieldNames() {
		return Collections.emptyList();
	}

	/**
	 * The value of the given field.
	 * 
	 * @param key The field name.
	 * @return The value of the given field, or undefined if no value exists.
	 */
	default String get(String key) {
		try {
			return CSVDataRow.this.get(fieldNames().indexOf(key));
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Set the value of a field.
	 * 
	 * @param key The field name.
	 * @return The value of the given field, or undefined if no value exists.
	 * @throws IllegalArgumentException The given value or key was invalid.
	 */
	default String set(String key, String value) throws IllegalArgumentException {
		int index = fieldNames().indexOf(key);
		if (index < 0) {
			throw new IllegalArgumentException("Invalid field name");
		}
		return CSVDataRow.this.set(index, value);
	}

	@Override
	public String set(int index, String value) throws IndexOutOfBoundsException, IllegalArgumentException;
}