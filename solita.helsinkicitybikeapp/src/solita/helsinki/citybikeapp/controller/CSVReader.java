package solita.helsinki.citybikeapp.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solita.helsinkicitybikeapp.model.CSVDocument;
import solita.helsinkicitybikeapp.model.CSVException;
import solita.helsinkicitybikeapp.model.SimpleCSVDocument;

/**
 * Class reading CSV files.
 * 
 * @author Antti Kautiainen
 *
 */
public class CSVReader {

	private Scanner lineScanner;

	/**
	 * The header names of the CVS file fields.
	 */
	private List<String> headerNames = null;

	/**
	 * The stream from which the data is read.
	 */
	private InputStream in;

	/**
	 * Does the reader require header from CSV files.
	 * 
	 * By default the reader does not require header row. 
	 */
	private boolean requiresHeader = false; 
	
	/**
	 * Does the reader require header from CSV files. 
	 * @return True, if and only if the first row of the CSV file is assumed
	 *  to contain header. 
	 */
	public boolean requiresHeader() {
		return this.requiresHeader; 
	}
	
	/**
	 * Creates a new CSV reader which may have header row. 
	 * 
	 * If the reader does not require header row, no header row is 
	 * read. 
	 * 
	 * @param requireHeader Does the reader require header row. 
	 */
	public CSVReader(boolean requireHeader) {
		this.requiresHeader = requireHeader; 
	}
	
	/**
	 * Creates a new CSV reader using given handler and header pattern. 
	 * @param handler The handler performing the handling of the CSV rows. 
	 * @param headerPattern The pattern handling the header row. If this
	 *  value is undefined (<code>null</code>), the reader would not require
	 *  header row.
	 */
	public CSVReader(CSVHandler handler, Pattern headerPattern) {
		this(headerPattern != null); 
		this.handler = handler;
		if (headerPattern != null) {
			this.headerPattern = headerPattern; 
		}
	}
	
	
	
	/**
	 * Creates reader for the given URL.
	 * 
	 * Opens the given file for CSV reading, and reads the header row.
	 * 
	 * @param url The URL containing the CSV data.
	 * @throws NullPointerException The given url is undefined.
	 * @throws IOException          the URL could not be read.
	 */
	public CSVReader(URL url) throws IOException {
		this();
		this.open(url);
		;
	}

	/**
	 * Creates a new CSV reader without file to read.
	 */
	public CSVReader() {
		lineScanner = null;
		this.in = null;
	}

	/**
	 * Interface for handling the addition of CSV rows.
	 * 
	 * @author Antti Kautiainen
	 *
	 */
	public static interface CSVHandler {
		/**
		 * Handles a data row.
		 * 
		 * The handler can be assured that each handled row has equal number of fields.
		 * 
		 * @param rowFields The list of header fields.
		 * @throws CSVException TODO
		 */
		public void handleRow(java.util.List<String> rowFields) throws CSVException;

		/**
		 * Handles header row. Call of this handled indicates that a new CSV file is
		 * handled.
		 * 
		 * @param headerFields The fields of the data row. An undefined value indicates
		 *                     that the CSV file did not have header row.
		 * @throws CSVException TODO
		 */
		public void handleHeaders(java.util.List<String> headerFields) throws CSVException;

		/**
		 * Handles the errors during parsing.
		 * 
		 * @param <E>       The handled exception type.
		 * @param exception The handled exception.
		 * @throws E The possibly thrown exception.
		 */
		default <E extends Exception> void handleError(E exception) throws E {
			throw exception;
		}
	}

	/**
	 * Simple handler which handles rows, but does not store anything.
	 * 
	 * @author Antti Kautiainen
	 *
	 */
	public static class TesterHandler implements CSVHandler {

		/**
		 * The predicate testing the header.
		 */
		private final Predicate<? super List<String>> headerTester;

		/**
		 * The predicate testing the data row.
		 */
		private final Predicate<? super List<String>> rowTester;

		/**
		 * Creates a new tester handler testing each row without storing it.
		 * 
		 * @param headerTester The predicate testing the header rows.
		 * @param rowTester    The predicate testing the data rows.
		 */
		public TesterHandler(Predicate<List<String>> headerTester, Predicate<List<String>> rowTester) {
			this.headerTester = headerTester;
			this.rowTester = rowTester;
		}

		@Override
		public void handleRow(List<String> rowFields) throws CSVException {
			if (rowTester != null && !rowTester.test(rowFields)) {
				throw new CSVException.InvalidRowException(CSVException.RowType.DATA, "Invalid data row", rowFields);
			}
		}

		@Override
		public void handleHeaders(List<String> headerFields) throws CSVException {
			if (headerTester != null && !headerTester.test(headerFields)) {
				throw new CSVException.InvalidRowException(CSVException.RowType.HEADER, "Invalid header", headerFields);
			}
		}

	}

	/**
	 * Opens the given URL for reading CSV, and reads the header row.
	 * 
	 * @param url The URL containing the CSV data.
	 * @throws NullPointerException The given url is undefined.
	 * @throws IOException          the URL could not be read.
	 */
	public void open(URL url) throws IOException {
		this.in = url.openStream();
		setSource(this.in);
	}

	/**
	 * Opens the given file for reading CSV content.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public void open(File file) throws IOException, IllegalArgumentException {
		if (file.isFile() || file.canRead()) {
			this.in = new FileInputStream(file);
			setSource(in);
		} else {
			throw new IllegalArgumentException("Not a readable normal file");
		}
	}

	/**
	 * Uses the given input stream as source of the CSV content.
	 * 
	 * This operation resets the CSV reader.
	 * 
	 * @param input The input stream used to read content.
	 */
	public void setSource(InputStream input) {
		lineScanner = (new Scanner(this.in)).useDelimiter(CSV_DELIMITER_PATTERN);
		this.lineNumber = 0;
		this.lineColumn = 0;
	}

	/**
	 * The handler handling the rows.
	 */
	private CSVHandler handler;

	/**
	 * Set the handler of rows.
	 * 
	 * @param handler The handler of rows.
	 */
	public void setHandler(CSVHandler handler) {
		this.handler = handler;
	}

	/**
	 * Get the current CSV handler.
	 * 
	 * @return The handler handling the full CSV rows.
	 */
	public CSVHandler getHandler() {
		return this.handler;
	}

	/////////////////////////////////////////////////////////////////
	//
	// The error handling subsection
	//
	/////////////////////////////////////////////////////////////////

	/**
	 * The line number of the current position.
	 */
	private int lineNumber = 0;

	/**
	 * The line column of the current position.
	 */
	private int lineColumn = 0;

	/**
	 * Format method to allow localization of formating.
	 * 
	 * @param pattern The pattern of formatting.
	 * @param values  The values of the patterns.
	 * @return The formatted string.
	 */
	public String format(String pattern, Object... values) {
		return String.format(pattern, values);
	}

	/**
	 * Handles exception.
	 * 
	 * @param <E>       The handled exception type.
	 * @param exception The handled exception.
	 * @throws E The exception is possibly thrown.
	 */
	public <E extends Exception> void handleError(E exception) throws E {
		if (this.handler != null) {
			this.handler.handleError(exception);
		} else {
			throw exception;
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Class Constants: Reading the CSV content
	//
	// The constants used to parse CSV data.
	//
	/////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * The pattern matching for the CSV header.
	 * 
	 */
	public static final Pattern CSV_HEADER_PATTERN = CSVReader.CSV_SIMPLE_DATA_ROW;

	/**
	 * Simple delimiter pattern separating data rows. The pattern has no capturing
	 * groups.
	 */
	public static final Pattern CSV_SIMPLE_ROW_DELIMITER_PATTERN = Pattern.compile("\\r\\n");

	/**
	 * The delimiter pattern separating fields from each other. The pattern has no
	 * capturing groups.
	 */
	public static final Pattern CSV_DELIMITER_PATTERN = Pattern.compile("(?:\\s*,\\s*)");

	/**
	 * The pattern string matching escaped field. The pattern has no capturing
	 * groups.
	 */
	public static final String ESCAPED_REGEX_STRING = "(?:\"(?:[^\"]|\"\")*\")";

	/**
	 * The pattern string matching normal field The pattern has no capturing groups.
	 */
	public static final String UNESCAPED_FIELD_REGEX_STRING = "(?:[^\",]*?)";

	/**
	 * The regular expression matching to field values. The pattern has no capturing
	 * groups.
	 */
	public static final Pattern FIELD_REGEX = Pattern.compile(
			"(?:" + String.join("|", (new String[] { ESCAPED_REGEX_STRING, UNESCAPED_FIELD_REGEX_STRING })) + ")");

	/**
	 * The regular expression matching to a simple row. 
	 * The pattern will have two matching groups: the first field and the last field, if there was more than one field. 
	 * (Simple row does not allow CRLF combination within quotes)
	 */
	public static final Pattern CSV_SIMPLE_DATA_ROW = Pattern.compile("^\\s*(" + FIELD_REGEX.toString() + ")(?:"
			+ CSV_DELIMITER_PATTERN.toString() + "("+ FIELD_REGEX.toString() + ")"+ ")*\\s*$");

	/**
	 * The regular expression matching the next field of the row.
	 * 
	 * The pattern ahs two capturing groups: The raw field value, and the delimiter following the field. 
	 */
	public static final Pattern CSV_NEXT_FIELD_REGEX = Pattern
			.compile("(" + FIELD_REGEX.toString() + ")" + "(" + CSV_DELIMITER_PATTERN.toString() + "|$" + ")");

	/////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Methods: Reading the CSV content
	//
	// The instance methods used to read CSV rows. These methods are used for actual
	// implementation to allow overriding the default patterns and handlers.
	//
	/////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Adding the field to the result.
	 * 
	 * @param result The list of fields.
	 * @param field  The name of the fields.
	 * @return True, if and only if the adding of a field succeeded.
	 */
	public boolean addField(List<String> result, String field) {
		return result.add(field);
	}

	/**
	 * The pattern of the header row. By default it is just a data row. 
	 */
	private Pattern headerPattern = CSVReader.CSV_HEADER_PATTERN; 

	
	/**
	 * The pattern matching header row. 
	 * 
	 * @return The pattern matching data row. The pattern will have two groups: 
	 * 	
	 */
	public Pattern headerRowPattern() {
		return headerPattern;  
	}
	
	/**
	 * The pattern matching simple data row. The simple data row does not allow line
	 * breaks within the quoted values.
	 * 
	 * @return The pattern matching data row. The pattern will have two groups:
	 *         first group, and last group, if it is not first group.
	 */
	public Pattern dataRowPattern() {
		return CSV_SIMPLE_DATA_ROW;
	}

	/**
	 * The pattern matching next CSV field.
	 * 
	 * @return The pattern matching next field regular expression pattern. The
	 *         pattern has two capturing groups: the field value, and the delimiter
	 *         following the field.
	 */
	public Pattern nextFieldPattern() {
		return CSV_NEXT_FIELD_REGEX;
	}

	/**
	 * The class representing parse result. 
	 * 
	 * @author Antti Kautiainen
	 *
	 */
	public static class ParseResult {
		/**
		 * Creates a new parse result.
		 * 
		 * @param startIndex The start index of the parse result.
		 * @param endIndex   The end index of the parse result.
		 */
		public ParseResult(int startIndex, int endIndex) throws IllegalArgumentException {
			if (startIndex < 0) {
				throw new IllegalArgumentException("Invalid start index");
			} else if (endIndex < startIndex) {
				throw new IllegalArgumentException("Invalid end index");
			} else {
				this.start = startIndex;
				this.end = endIndex;
			}
		}

		/**
		 * The start index of the result.
		 */
		public final int start;

		/**
		 * The end index of the result.
		 */
		public final int end;

		/**
		 * The end index of the parse.
		 * 
		 * @return The end index of parse.
		 */
		public int getStart() {
			return this.start;
		}

		/**
		 * The end index of the parse.
		 * 
		 * @return The first index not belonging to the parse result.
		 */
		public int getEnd() {
			return this.end;
		}

		/**
		 * Test emptiness of the result.
		 * 
		 * @return True, if and only if the parse result is empty.
		 */
		public boolean isEmpty() {
			return getStart() == getEnd();
		}

		/**
		 * Create a copy of another parse result.
		 * 
		 * @param other The parse result whose copy is created.
		 * @throws NullPointerException The given other is undefined.
		 */
		public ParseResult(ParseResult other) throws NullPointerException {
			this(other.start, other.end);
		}

		/**
		 * Creates parse result from the given row.
		 * 
		 * @param row The row whose parse result is generated.
		 * @return The subsequence of the given sequence containing the parse result.
		 * @throws IndexOutOfBoundsException The sequence was outside the given
		 *                                   character sequence.
		 */
		public CharSequence getParseResult(CharSequence row) throws IndexOutOfBoundsException {
			if (row == null) {
				return null;
			} else if (this.end > row.length()) {
				throw new IndexOutOfBoundsException("The row does not contain the parse result");
			} else {
				return row.subSequence(start, end);
			}
		}

		/**
		 * The parse result of the default row.
		 * 
		 * @return If a default row exists, the parse result of the default row.
		 *         Otherwise an undefined value is returned.
		 */
		public CharSequence getParseResult() {
			return getParseResult(null);
		}
	}

	/**
	 * String parse result is a parse result containing the actual parse content.
	 * 
	 * The instances of this kind of parse result can be used as strings.
	 * 
	 * @author Antti Kautiainen
	 *
	 */
	public static class StringParseResult extends ParseResult implements CharSequence {
		public StringParseResult(String row, ParseResult result) {
			super(result);
			if (row == null)
				throw new NullPointerException("Undefined row");
			if (this.start >= row.length()) {
				throw new IndexOutOfBoundsException("Start index out of bounds");
			}
			this.row = row;
		}

		/**
		 * The row the parse result refers to.
		 */
		private final String row;

		@Override
		public int length() {
			return end - start;
		}

		@Override
		public char charAt(int index) {
			int rowIndex = start + index;
			if (rowIndex < start || rowIndex >= end) {
				throw new IndexOutOfBoundsException("Invalid index");
			}
			return row.charAt(rowIndex);
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			int rowStart = this.start + start, rowEnd = this.start + end;
			if (rowStart < this.start || rowEnd > this.end || rowEnd < rowStart) {
				throw new IndexOutOfBoundsException("Invalid index");
			}
			return row.subSequence(rowStart, rowEnd);
		}

		@Override
		public String toString() {
			return row.substring(start, end);
		}

		/**
		 * The parse result of the default row.
		 * 
		 * @return The parse result of the default row.
		 */
		public CharSequence getParseResult() {
			return this;
		}
	}

	/**
	 * Reads all rows of the currently open CSV source. 
	 * @param handler The handler handling read rows. 
	 * @return True, if and only if the reading succeeded. 
	 * @throws CSVException The reading failed due CSV exception. 
	 * @throws IOException The reading failed due Input/Output error. 
	 * @throws ParseException The reading failed due invalid content. 
	 */
	public boolean readAll(CSVHandler handler) throws CSVException, IOException, ParseException {
		if (this.requiresHeader()) {
			List<String> headerRow = null;
			try {
				headerRow = this.readHeaderRow();
			} catch (IllegalStateException e) {
				handler.handleError(e);
			} catch(IOException ioe) {
				handler.handleError(ioe);
			} catch (ParseException pe) {
				handler.handleError(pe);
			}
			if (headerRow == null) {
				throw new CSVException.EmptyRowException(CSVException.RowType.HEADER, "Empty header row", null);
			}
		} 
		try {
		List<String> dataRow; 
		while ( (dataRow = this.readDataRow()) != null) {
			if (handler != null) {
				try {
					handler.handleRow(dataRow);
				} catch (IllegalStateException e) {
					handler.handleError(e);
				} catch(CSVException csve) {
					handler.handleError(csve);
				}
			}
		}
		
		} catch(CSVException e) {
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Reached end of file. 
		return true; 
	}
	
	/**
	 * Parses next document.
	 * 
	 * @return The resulting CVS document.
	 */
	public CSVDocument parse() throws CSVException, ParseException, IOException {
		CSVDocument doc = new SimpleCSVDocument();

		return parse(doc);
	}

	/**
	 * Reads the default source into given document.
	 * 
	 * @param doc The document into which the read CSV data is added.
	 * @return The result CSV document.
	 * @throws IOException    The parse failed due Input/Output error.
	 * @throws ParseException The parse failed due invalid content.
	 * @throws CSVException   The parse failed due CSV Exception.
	 */
	public CSVDocument parse(CSVDocument doc) throws IOException, ParseException, CSVException {
		if (doc.requireHeaderRow()) {
			List<String> header = this.readHeaderRow();
			doc.setFields(header);
		}
		List<String> dataRow;
		while ((dataRow = this.readDataRow()) != null) {
			// we have data row.
			// TODO: add information the data row is
			doc.addDataRow(dataRow);
		}

		return doc;
	}

	/**
	 * Reads next row from the internal scanner.
	 * 
	 * @return The list of fields of the next row. Undefined (<code>null</code>)
	 *         value, if the source has no more rows.
	 * @throws IOException              The reading failed due I/O exception.
	 * @throws java.text.ParseException The reading failed due invalid format.
	 */
	public List<CharSequence> readRow() throws IOException, java.text.ParseException {
		List<CharSequence> result = null;
		if (lineScanner.hasNext()) {
			// The source has more data rows. 
			if (lineScanner.hasNext(this.dataRowPattern())) {
				String token = lineScanner.nextLine();
				Matcher matcher = this.dataRowPattern().matcher(token);
				if (matcher.matches()) {
					// We do have row.
					Integer requiredFieldCount = this.getFieldCount();
					int fieldCount = 0;
					result = new ArrayList<>();
					int startIndex = 0;
					matcher = this.nextFieldPattern().matcher(token);
					String delimiter;
					while (matcher.matches()) {
						startIndex = matcher.end();
						// Handling the cases:
						fieldCount++;
						delimiter = matcher.group(3);
						if (requiredFieldCount != null && fieldCount == requiredFieldCount
								&& (delimiter != null && !delimiter.isEmpty())) {
							// We have a parse error - the delimiter indicates there is more rows to return.
							throw new ParseException("Too many fields on the row", matcher.start(3));
						} else if (delimiter == null || delimiter.isEmpty()) {
							// We have a parse error - the row contains too few fields.
							throw new ParseException("Too few fields on the row", matcher.end());
						} else {
							// Adding the group field to the result.
							result.add(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
						}
					}

					// Setting the previous data row, and returning the result.
					if (requiredFieldCount == null) {
						this.setFieldCount(result.size());
					}
					return result;
				} else {
					// We have error.
					throw new Error("Scanner has next failed!");
				}
			} else {
				// Invalid row.
				throw new ParseException("Invalid parse row", 0);
			}
		}
		return result;
	}

	/**
	 * Test validity of read row.
	 * 
	 * By default the row is valid, if it is defined and has at least one field
	 * value, and if the number of fields is set it has as many fields as the
	 * previous header or data row has.
	 * 
	 * @param row The list of fields of the tested row.
	 * @return True, if and only if the given row is valid.
	 */
	public boolean validRow(List<CharSequence> row) {
		return row != null && validFieldCount(row.size());
	}

	/**
	 * Test validity of the data row.
	 * 
	 * @param row THe tested data row.
	 * @return True, if and only if the given row is valid header row. The default
	 *         requires every field has different non-empty name.
	 */
	public boolean validDataRow(List<CharSequence> row) {
		// The value is only instance of the value, if its index is equal to its
		// last index. All tested values exists in the rows.
		//
		// The action can be performed parallel, as the row is not modified.
		return validRow(row);
	}

	/**
	 * Test validity of a data row.
	 * 
	 * @param row The tested row.
	 * @return True, if and only if the row is valid.
	 * @throws ParseException The exception caused by an invalid row.
	 */
	public boolean checkDataRow(List<CharSequence> row) throws ParseException {
		if (row == null) {
			throw new ParseException("Undefined row", 0);
		} else if (!validDataRow(row)) {
			// The row count cannot be undefined.
			int index = Math.min(this.getFieldCount(), row.size());
			CharSequence invalidElement = row.get(index);
			if (invalidElement instanceof ParseResult) {
				// We do have proper index of the error.
				throw new ParseException("Invalid number of fields on data row",
						((ParseResult) invalidElement).getStart());
			} else {
				// We do use the
				throw new ParseException("Invalid number of fields on data row", 0);
			}
		} else {
			// test passed.
			return true;
		}
	}

	/**
	 * Test validity of the header row.
	 * 
	 * @param row THe tested header row.
	 * @return True, if and only if the given row is valid header row. The default
	 *         requires every field has different non-empty name.
	 */
	public boolean validHeaderRow(List<CharSequence> row) {
		// The value is only instance of the value, if its index is equal to its
		// last index. All tested values exists in the rows.
		//
		// The action can be performed parallel, as the row is not modified.
		return row != null && row.parallelStream().allMatch((CharSequence value) -> (value != null
				&& (value.length() != 0) && row.indexOf(value) == row.lastIndexOf(value)));
	}

	/**
	 * Test validity of header row.
	 * 
	 * @param row The tested row.
	 * @return True, if and only if the header row is valid.
	 * @throws ParseException The exception caused by an invalid row.
	 */
	public boolean checkHeaderRow(List<CharSequence> row) throws ParseException {
		if (row == null) {
			throw new ParseException("Undefined row", 0);
		} else {
			CharSequence value;
			for (int i = 0, len = row.size(); i < len; i++) {
				value = row.get(i);
				if (value == null) {
					throw new ParseException("Undefined field name", i);
				} else if (value.isEmpty()) {
					throw new ParseException("Empty field name", i);
				} else if (row.indexOf(value) != row.lastIndexOf(value)) {
					// The value is invalid.
					// - the error index is the index of first duplicate instance after the first
					// instance triggering
					// this effect.
					throw new ParseException("Duplicate field at", row.subList(i, row.size()).indexOf(value));
				}
			}
			// test passed.
			return true;
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	//
	// The number of fields each row should have.
	//

	/**
	 * The required field count.
	 */
	private Integer fieldCount = null;

	/**
	 * The number of fields the CSV file has.
	 * 
	 * @return The number of fields each data row should have. IF this value is 0,
	 *         the number of columns is not determined yet.
	 */
	public Integer getFieldCount() {
		return fieldCount;
	}

	/**
	 * Setting the number of fields the CSV field has.
	 * 
	 * @param fieldCount The new field count
	 * @throws IllegalArgumentException The given field count is invalid.
	 * @throws IllegalStateException    The current state of the reader prevents the
	 *                                  alteration of the field count.
	 */
	public void setFieldCount(Integer fieldCount) throws IllegalArgumentException {
		if (fieldCount != null && fieldCount < 0)
			throw new IllegalArgumentException("Negative field count");
		if (this.fieldCount != null && lineScanner != null && lineScanner.hasNext()) {
			// The state of the parse does not allow setting field count twice.
			throw new IllegalStateException("Field count cannot be set twice during parse!");
		}
		this.fieldCount = fieldCount;
	}

	/**
	 * Test validity of a field count.
	 * 
	 * @param fieldCount The tested field count.
	 * @return True, if and only if the field count is valid.
	 */
	public final boolean validFieldCount(int fieldCount) {
		Integer count = this.getFieldCount();
		return fieldCount >= 0 && (count == null || count == fieldCount);
	}

	/**
	 * Reads header row. 
	 * 
	 * @return The header row fields. 
	 * @throws IOException           The reading of row failed due IO exception.
	 * @throws ParseException        The reading of the row failed due parse error.
	 * @throws IllegalStateException The state of the reader prevents reading header
	 *                               row.
	 */
	public List<String> readHeaderRow() throws IOException, ParseException, IllegalStateException {
		List<CharSequence> result = readRow();
		if (result == null) return null; // There is no header to read. 

		// Checking validity of the header row.
		if (validHeaderRow(result)) {
			// Converting the parse result to the string result.
			return result.stream().map((CharSequence element) -> (element.toString())).toList();
		} else {
			// Determining the error position.
			checkHeaderRow(result);

			// The check header row should always throw exception.
			throw new ParseException("Invalid header row", 0);
		}
	}

	/**
	 * Reads data row.
	 * 
	 * IF the CVS reader has read any header or data row, the reading of a header
	 * row always triggers exception.
	 * 
	 * @return The read data row. 
	 * @throws IOException           The reading of row failed due IO exception.
	 * @throws ParseException        The reading of the row failed due parse error.
	 * @throws IllegalStateException The state of the reader prevents reading header
	 *                               row.
	 */
	public List<String> readDataRow() throws IOException, ParseException {
		List<CharSequence> result = readRow();

		// Checking validity of the header row.
		if (validDataRow(result)) {
			// Converting the parse result to the string result.
			return result.stream().map((CharSequence element) -> (element.toString())).toList();
		} else {
			// Determining the error position.
			checkDataRow(result);

			// The check header row should always throw exception.
			throw new ParseException("Invalid data row", 0);
		}
	}

}
