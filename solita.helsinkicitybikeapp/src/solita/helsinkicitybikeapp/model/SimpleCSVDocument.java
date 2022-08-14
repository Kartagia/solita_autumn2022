package solita.helsinkicitybikeapp.model;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import solita.helsinki.citybikeapp.controller.CSVReader;
import solita.helsinki.citybikeapp.controller.CSVReader.ParseResult;

/**
 * The simple CSV document.
 * 
 * @author Antti Kautiainen
 *
 */
public class SimpleCSVDocument implements CSVDocument {

	/**
	 * Does the document require header.
	 */
	private boolean requireHeader;

	/**
	 * Creates a simple CSV document.
	 * 
	 * @param requireHeader Does the document require header.
	 */
	public SimpleCSVDocument(boolean requireHeader) {
		this.requireHeader = requireHeader;
	}

	/**
	 * Creates a simple CSV document, which does not require header.
	 */
	public SimpleCSVDocument() {
		this(false);
	}

	/**
	 * The data row of a simple document.
	 * 
	 * @author Antti Kautiainen
	 *
	 */
	public class DataRow extends AbstractList<String> implements CSVDataRow {

		private List<String> values = null;

		/**
		 * Creates a new data row form given record.
		 * 
		 * @param record The record as list of field values.
		 * @throws CSVException The given row was ivnalid.
		 */
		public DataRow(List<? extends CharSequence> record) throws CSVException {
			addAll(record);
		}

		/**
		 * Add field values.
		 * 
		 * @param record    The record as list of field values.
		 * @param parseData Are we handling parsed data with escape sequences still
		 *                  quoted.
		 * @throws CSVException The addition of the value causes CSV exception. 
		 */
		protected void addAll(List<? extends CharSequence> record, boolean parseData) throws CSVException {
			if (values != null)
				throw new IllegalStateException();
			List<String> values = new ArrayList<>(record == null ? 0 : record.size());
			if (record != null) {
				// Adding field values.
				for (CharSequence fieldValue : record) {
					if (fieldValue == null) {
						values.add(null);
					} else if (parseData) {
						// The parse data means escape sequence has
						String value = fieldValue.toString();
						if (value.startsWith("\"") && value.endsWith("\"")) {
							// We do have escaped sequence.
							values.add(unescape(value));
						} else {
							// We have just a string.
							values.add(value);
						}
					} else {
						values.add(fieldValue.toString());
					}
				}
			}
			if (validDataRow(values)) {
				this.values = values;
			} else {
				throw new CSVException.InvalidRowException(CSVException.RowType.DATA, "Invalid data row", values);
			}

		}

		protected void addAll(List<? extends CharSequence> record) throws CSVException {
			if (values != null)
				throw new IllegalStateException();
			List<String> values = new ArrayList<>(record == null ? 0 : record.size());
			if (record != null) {
				// Adding field values.
				String value;
				for (CharSequence fieldValue : record) {
					if (fieldValue == null) {
						values.add(null);
					} else if (fieldValue instanceof ParseResult) {
						// The parse data means escape sequence has
						value = fieldValue.toString();
						if (value.startsWith("\"") && value.endsWith("\"")) {
							// We do have escaped sequence.
							values.add(unescape(value));
						} else {
							// We have just a string.
							values.add(value);
						}
					} else {
						values.add(fieldValue.toString());
					}
				}
			}
			if (validDataRow(values)) {
				this.values = values;
			} else {
				throw new CSVException.InvalidRowException(CSVException.RowType.DATA, "Invalid data row", values);
			}

		}

		/**
		 * Creates a new data row from given field values.
		 * 
		 * @param record    The CSV record in list format.
		 * @param parseData Does the given record contain parse data.
		 * @throws CSVException The record was invalid.
		 */
		public DataRow(List<? extends CharSequence> record, boolean parseData) throws CSVException {
			values = new ArrayList<>(record == null ? 0 : record.size());
			if (record != null) {
				// Adding field values.
				for (CharSequence fieldValue : record) {
					if (fieldValue == null) {
						values.add(null);
					} else if (parseData) {
						// The parse data means escape sequence has
						String value = fieldValue.toString();
						if (value.startsWith("\"") && value.endsWith("\"")) {
							// We do have escaped sequence.
							values.add(value.substring(1, value.length() - 1));
						} else {
							// We have just a string.
							values.add(value);
						}
					} else {
						values.add(fieldValue.toString());
					}
				}
			}
			if (!validDataRow(values)) {
				throw new CSVException.InvalidRowException(CSVException.RowType.DATA, "Invalid data row", values);
			}
		}

		/**
		 * Creates a new data row with all fields set to undefined.
		 * 
		 * @param fieldCount The field count of the created field.
		 */
		public DataRow(int fieldCount) {
			values = new ArrayList<>(fieldCount);
			for (int i = 0; i < fieldCount; i++) {
				values.add(null);
			}
		}

		@Override
		public String get(int index) throws IndexOutOfBoundsException {
			return values.get(index);
		}

		@Override
		public int size() {
			return values.size();
		}
	}

	/**
	 * Test validity of a data row.
	 * 
	 * @param dataRowValues The tested data row.
	 * @return True, if and only if the given value is valid.
	 */
	public boolean validDataRow(List<String> dataRowValues) {
		if (dataRowValues == null)
			return false;
		Integer requiredFieldCount = this.getFieldCount();
		return (requiredFieldCount == null || requiredFieldCount == dataRowValues.size());
	}

	/**
	 * The header field names.
	 * 
	 */
	private java.util.ArrayList<String> headers = new ArrayList<>();

	/**
	 * The data rows.
	 */
	private java.util.ArrayList<CSVDataRow> dataRows = new ArrayList<>();

	@Override
	public List<String> getHeaderFields() {
		return this.headers;
	}

	/**
	 * The field count.
	 */
	private Integer fieldCount = null;

	@Override
	public CSVDataRow getDataRow(int index) {
		return dataRows.get(index);
	}

	@Override
	public int size() {
		return dataRows.size();
	}

	@Override
	public boolean requireHeaderRow() {
		return this.requireHeader;
	}

	@Override
	public boolean setFields(List<? extends CharSequence> headerFields) throws IllegalStateException, CSVException {
		if (!CSVDocument.super.setFields(headerFields)) {
			// The header was not set by the parent, but it did not cause exception.
			this.headers.addAll(headerFields.stream().map((CharSequence val)->(val instanceof String?(String)val:val.toString())).toList());
			this.fieldCount = headerFields.size();
		}
		return true;
	}

	@Override
	public Integer getFieldCount() {
		return fieldCount;
	}

	@Override
	public boolean addDataRow(List<? extends CharSequence> fieldData) throws CSVException {
		try {
			if (this.dataRows.add(this.new DataRow(fieldData))) {
				if (getFieldCount() == null) {
					// Setting field count from first data row.
					this.fieldCount = fieldData.size();
				}
				return true;
			} else {
				return false;
			}
		} catch (CSVException e) {
			throw new IllegalArgumentException("Invalid data row", e);
		}
	}

	/**
	 * remove data row from CSV document.
	 * 
	 * @param index The index of the removed row.
	 * @return True, if and only if the row was removed.
	 */
	public boolean removeDataRow(int index) {
		if (index > 0 && index < dataRows.size()) {
			this.dataRows.remove(index);
			return true;
		} else {
			return false;
		}
	}

	public String unescape(CharSequence data) {
		return data == null ? null : unescape(data.toString());
	}

	/**
	 * Get the original string from escaped sequence.
	 * 
	 * @param data The escaped data.
	 * @return The given data with escape changes reverted to original values.
	 */
	public String unescape(String data) {
		if (data == null)
			return null;
		if (data.startsWith("\"") && data.endsWith("\"")) {
			// We do have escape sequence.
			// - Removing escaping from the sequence (double double quotes are replaced with
			// single double quote)
			return data.substring(1, data.length() - 1).replaceAll("\"\"", "\"");
		} else {
			// WE do not have escaped sequence.
			return data;
		}
	}

	/**
	 * The pattern matching to any character causing the CSV to escape the sequence.
	 */
	public static final Pattern ESCAPEABLE_PATTERN = Pattern.compile("(?:^\\s|\\s$|[,\\\"]|\\p{Cntrl})",
			Pattern.UNICODE_CHARACTER_CLASS);

	/**
	 * Escape the field value, if necessary.
	 * 
	 * @param data The field value to escape.
	 * @return The given field escaped, if escaping is necessary.
	 */
	public String escape(String data) {
		if (data != null && ESCAPEABLE_PATTERN.matcher(data).matches()) {
			return "\"" + data.replaceAll("\"", "\"\"") + "\"";
		} else {
			// No need to escape.
			return data;
		}
	}

}