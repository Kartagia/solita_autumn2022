package solita.helsinkicitybikeapp.model;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
	 * CVS data row implementation allowing both building the data row one field at
	 * the time, and creation of data row from specified list of values.
	 * 
	 * When the data row is completed, the implementation should be finished with
	 * build()-method preventing adding new field values, but allowing setting the
	 * field values already defined.
	 * 
	 * @author Antti Kautiainen
	 *
	 */
	public static class CSVDataRowImpl extends java.util.AbstractList<String> implements CSVDataRow {

		/**
		 * Does the implementation deny the insertion of new fields.
		 * 
		 */
		private boolean finished = false;

		/**
		 * The list of field values.
		 */
		private final List<String> fields;

		/**
		 * The list of field names.
		 */
		private final List<String> fieldNames;

		/**
		 * Create a new empty data row.
		 * 
		 * @param fieldNameList The list of field names. This list should not be
		 *                      modified after call, as it is used as the list of field
		 *                      names.
		 * @throws IllegalArgumentException The field name list was invalid.
		 */
		public CSVDataRowImpl(List<String> fieldNameList) {
			fieldNames = fieldNameList == null ? null : fieldNameList;
			if (fieldNames == null) {
				fields = new java.util.ArrayList<>();
			} else {
				// Testing if the field name list contains any duplicates or undefined values.
				String element;
				for (int i = 0, end = fieldNames.size(); i < end; i++) {
					element = fieldNames.get(i);
					if (element == null) {
						throw new IllegalArgumentException("Field name list with undefined name");
					} else if (fieldNames.subList(0, i).contains(element)) {
						throw new IllegalArgumentException("Field name list with duplicate name");
					}
				}
				fields = new java.util.ArrayList<>(fieldNames.size());
			}

		}

		/**
		 * Creates a new field name list with given field name list and field list.
		 * 
		 * @param fieldNameList The field name list.
		 * @param fieldList     The list of field values. If undefined, an empty field
		 *                      list is created.
		 * @throws IllegalArgumentException The field list contains more elements than
		 *                                  the defined field name list.
		 */
		public CSVDataRowImpl(List<String> fieldNameList, List<String> fieldList) {
			this(fieldNameList);
			try {
				this.addAll(fieldList);
			} catch (IllegalStateException is) {
				throw new IllegalArgumentException("Field list with too many values");
			}
		}

		@Override
		public String get(int index) {
			if (fields == null)
				throw new IndexOutOfBoundsException();
			return fields.get(index);
		}

		@Override
		public int size() {
			return fields == null ? 0 : fields.size();
		}

		@Override
		public List<String> fieldNames() {
			return Collections.unmodifiableList(this.fieldNames);
		}

		/**
		 * Complete the building of the implementation locking adding and removal of the
		 * fields.
		 * 
		 * @return The completed CSV data row.
		 */
		public CSVDataRow build() {
			if (this.fieldNames != null && this.fields.size() != this.fieldNames.size()) {
				// Filling the unassigned fields with nulls.
				int i = this.fields.size(), len = this.fieldNames.size();
				for (; i < len; i++) {
					this.add(null);
				}
			}
			this.finished = true;
			return this;
		}

		/**
		 * Adds new field value.
		 * 
		 * @param value THe added field.
		 * @return true, if and only if the value was added to the values of fields.
		 * @throws UnsupportedOperationException The addition of the value is not
		 *                                       allowed due current state of the data
		 *                                       row. The data row already has value for
		 *                                       all fields.
		 */
		@Override
		public boolean add(String value) throws UnsupportedOperationException {
			if (this.finished || (this.fieldNames() != null && this.size() == this.fieldNames().size())) {
				throw new UnsupportedOperationException("Cannot add any more fields");
			} else {
				return this.fields.add(value);
			}
		}

		@Override
		public void add(int index, String value) throws IllegalStateException, UnsupportedOperationException {
			if (index == this.size()) {
				add(value);
			} else {
				throw new UnsupportedOperationException("Adding new fields is not permitted");
			}
		}

		@Override
		public String set(int index, String element) {
			List<String> fieldNames = this.fieldNames();
			if (index < 0)
				throw new IndexOutOfBoundsException();
			if (this.finished || index < this.fields.size()) {
				// It is just valid setting.
				return this.fields.set(index, element);
			} else if (fieldNames == null || (index < fieldNames.size())) {
				// Setting a new value to the given index.

				// The field list is expanded with undefined values in order
				// to allow adding the value of given index.

				// Inserting empty values between given index and the value.
				for (int i = 0; i < index; i++) {
					this.fields.add(null);
				}
				// Adding the set field, and returning the previous value of null.
				fields.add(element);
				return null;
			} else {
				// Invalid index.
				throw new IndexOutOfBoundsException();
			}
		}

	}

	/**
	 * Create a new CSV data row from field name list and field list.
	 * 
	 * If the field names is given, the defined field names list cannot contain
	 * undefined values nor duplicates, as each field name is defined. In this case
	 * missing fields are filled with undefined values as field values.
	 * 
	 * @param fieldNames The field names of the list. A defined field names list
	 *                   cannot contain undefined values or duplicates. If
	 *                   undefined, the created row will not support map interface,
	 *                   and the field names does not limit the number of possible
	 *                   data fields.
	 * @param fieldList  The field names of the created field list. This list cannot
	 *                   have any more elements than the defined field names list
	 *                   has. For undefined field names list the size of the field
	 *                   list is not constrained.
	 * @return The CSV data row with given field names, and field lists.
	 * @throws IllegalArgumentException Either the field names or field list was
	 *                                  invalid.
	 */
	static CSVDataRow fromList(List<String> fieldNames, List<String> fieldList) {
		return (new CSVDataRowImpl(fieldNames, fieldList)).build();
	}

	/**
	 * Create a new CSV data row from list of the row values.
	 * 
	 * @param fieldList The field list of the data row.
	 * @return The data row created from the given field list.
	 */
	static CSVDataRow fromList(List<String> fieldList) {
		return fromList(null, fieldList);
	}

	/**
	 * The cSV data row as map of field values.
	 * 
	 * The changes on the map reflects to the data row and vice versa.
	 * 
	 * @return The map view of the CSV data row. The changes on the map will reflect
	 *         changes on the data row. If the named interface is not supported, an undefined
	 *         value is returned, as there is no map representation. 
	 *         	
	 */
	default Map<String, String> asMap() {
		if (this.fieldNames() == null)
			return null;

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
	 * @return The value of the given field, or undefined if no value exists. (No
	 *         value exists, if field name interface is not supported).
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
	 * @throws IllegalArgumentException      The given value or key was invalid.
	 * @throws UnsupportedOperationException The CSV row does not support field
	 *                                       names.
	 */
	default String set(String key, String value) throws IllegalArgumentException, UnsupportedOperationException {
		List<String> fieldNames = this.fieldNames();
		if (fieldNames == null)
			throw new UnsupportedOperationException("Field names are not supported");
		int index = fieldNames().indexOf(key);
		if (index < 0) {
			throw new IllegalArgumentException("Invalid field name");
		}
		return CSVDataRow.this.set(index, value);
	}

	@Override
	public String set(int index, String value) throws IndexOutOfBoundsException, IllegalArgumentException;
}