package solita.helsinkicitybikeapp.model;

import java.util.List;

/**
 * The CSV document containing data read from the document.
 * 
 * @author Antti Kautiainen
 *
 */
public interface CSVDocument {
	
	/**
	 * Does the document require header row. 
	 * @return True, if and only if the CSV document requires header row. 
	 */
	default boolean requireHeaderRow() {
		return false; 
	}
	
	/**
	 * Sets the fields. 
	 * @param headerFields The header fields. 
	 * @return True, if and only if setting the header fields succeeded. 
	 * @throws IllegalStateException The state of the documents prevents 
	 *  setting of the header rows. 
	 * @throws CSVException The setting of the field caused CSV exception. 
	 */
	default boolean setFields(List<String> headerFields) throws IllegalStateException, CSVException {
		if (this.getHeaderFields() != null) {
			throw new IllegalStateException("Cannot assign header fields twice"); 
		} else if (this.size() > 0) {
			throw new IllegalStateException("Cannot assign header fields after first row"); 				
		} else {
			// The setting is not done. 
			return false; 
		}
	}
	
	/**
	 * The number of fields each data row has. 
	 * @return If the required field count is known, returns that number. Otherwise returns
	 *  undefined value. 
	 */
	default Integer getFieldCount() {
		List<String> headerFields = this.getHeaderFields();
		if (headerFields != null) {
			return headerFields.size(); 
		} else if (size() > 0) {
			// Getting the size of the first row.
			return getDataRow(0).size(); 
		} else {
			// Size is not determined. 
			return null; 
		}
	}
	
	/** Add a new row to the document.
	 * 
	 * @param fieldData The field data. 
	 * @return True, if and only if the row was added. 
	 * @throws CSVException The addition of the data row caused CSV exception. 
	 */
	default boolean addDataRow(List<String> fieldData) throws CSVException {
		if (fieldData == null) {
			throw new IllegalArgumentException("Undefined field data row"); 
		} else {
			Integer fieldCount = this.getFieldCount(); 
			if (fieldCount != null && fieldData.size() != fieldCount) {
				throw new IllegalArgumentException("Invalid nubmer of fields");
			}
			return false; 
		}
	}
	
	/**
	 * Remove an existing data row. 
	 * @param index The index of the removed data row. 
	 * @return True, if and only if a data row was removed. 
	 */
	default boolean removeDataRow(int index) {
		return false; 
	}

	/** Adds new row to the document.
	 * 
	 * @param fieldData The field data. 
	 * @param parsedRow Does the given field data contain parsed data. If this is true, the 
	 *  escaped rows will be be unescaped. 
	 * @return True, if and only if the row was added. 
	 * @throws CSVException The addition of the data row caused CSV exception. 
	 */
	default boolean addDataRow(List<? extends CharSequence> fieldData, boolean parsedRow) throws CSVException {
		if (fieldData == null) {
			throw new IllegalArgumentException("Undefined field data row"); 
		} else {
			Integer fieldCount = this.getFieldCount(); 
			if (fieldCount != null && fieldData.size() != fieldCount) {
				throw new IllegalArgumentException("Invalid nubmer of fields");
			}
			return false; 
		}
	}

	
	/**
	 * The header fields of the CSV document. 
	 * @return The header fields, if any exists. 
	 */
	List<String> getHeaderFields(); 
	
	/**
	 * The data row of the given index. 
	 * @param index The index. 
	 * @return The data row of the given index. 
	 */
	CSVDataRow getDataRow(int index); 
	
	/** The size of the data set. 
	 * 
	 * @return The number of data rows the CSV document has. 
	 */
	int size(); 
}