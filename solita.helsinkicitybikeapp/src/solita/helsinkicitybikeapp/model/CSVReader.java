package solita.helsinkicitybikeapp.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private final InputStream in;

	/**
	 * Opens the given file for CSV reading, and reads the header row.
	 * 
	 * @param url The URL containing the CSV data.
	 * @throws NullPointerException The given url is undefined.
	 * @throws IOException          the URL could not be read.
	 */
	public CSVReader(URL url) throws IOException {
		this.in = url.openStream();
		lineScanner = (new Scanner(this.in)).useDelimiter(CSV_DELIMITER_PATTERN);
	}

	/**
	 * Adding the field to the result.
	 * 
	 * @param result The list of fields.
	 * @param field  The name of the fields.
	 * @return
	 */
	public boolean addField(List<String> result, String field) {
		return result.add(field);
	}

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
	 * The pattern matching for the CSV header.
	 * 
	 */
	public static final Pattern CSV_HEADER_PATTERN = Pattern
			.compile("^\\u000d" + "\\u00bb" + "\\u00bf" + CSVReader.CSV_SIMPLE_DATA_ROW.toString());

	/**
	 * Simple delimiter pattern separating data rows.
	 * The pattern has no capturing groups. 	  
	 */
	public static final Pattern CSV_SIMPLE_ROW_DELIMITER_PATTERN = Pattern.compile("\\r\\n");

	/**
	 * The delimiter pattern separating fields from each other.
	 * The pattern has no capturing groups. 
	 */
	public static final Pattern CSV_DELIMITER_PATTERN = Pattern.compile("(?:\\s*,\\s*)");

	/**
	 * The pattern string matching escaped field.
	 * The pattern has no capturing groups. 
	 */
	public static final String ESCAPED_REGEX_STRING = "(?:\"(?:[^\"]|\"\")*\")";

	/**
	 * The pattern string matching normal field
	 * The pattern has no capturing groups. 
	 */
	public static final String UNESCAPED_FIELD_REGEX_STRING = "(?:[^\",]*?)";

	/**
	 * The regular expression matching to field values.
	 * The pattern has no capturing groups.  
	 */
	public static final Pattern FIELD_REGEX = Pattern
			.compile("(?:" + String.join("|", (new String[] { ESCAPED_REGEX_STRING, UNESCAPED_FIELD_REGEX_STRING })) + ")");

	/**
	 * The regular expression matching to a simple row. (Simple row does not allow
	 * CRLF combination within quotes)
	 */
	public static final Pattern CSV_SIMPLE_DATA_ROW = Pattern.compile("^\\s*" + FIELD_REGEX.toString() + 
			"(?:" + CSV_DELIMITER_PATTERN.toString() + FIELD_REGEX.toString() + ")*\\s*$");

	/**
	 * The regular expression matching the next field of the row.
	 * The pattern has no capturing groups.  
	 */
	public static final Pattern CSV_NEXT_FIELD_REGEX = Pattern
			.compile("(" + FIELD_REGEX.toString() +")"+ "(" + CSV_DELIMITER_PATTERN.toString() + "|$" + ")");

	/** The pattern matching simple data row. 
	 * The simple data row does not allow line breaks within the quoted values. 
	 * @return The pattern matching data row. The pattern will have two groups: first group, and last group, if
	 *  it is not first group. 
	 */
	public Pattern dataRowPattern() {
		return CSV_SIMPLE_DATA_ROW;
	}

	/**
	 * The pattern matching next CSV field. 
	 * @return The pattern matching next field regular expression pattern. The pattern has two capturing groups: the field value, 
	 *  and the delimiter following the field. 
	 */
	public Pattern nextFieldPattern() {
		return CSV_NEXT_FIELD_REGEX;
	}

	/**
	 * Reads next row from the internal scanner. 
	 * 
	 * @return The list of fields of the next row. Undefined (<code>null</code>) value, if the source
	 *  has no more rows. 
	 * @throws IOException              The reading failed due I/O exception.
	 * @throws java.text.ParseException The reading failed due invalid format.
	 */
	public List<String> readRow() throws IOException, java.text.ParseException {
		List<String> result = null;
		if (lineScanner.hasNext(this.dataRowPattern())) {
			String token = lineScanner.nextLine();
			Matcher matcher = this.dataRowPattern().matcher(token);
			if (matcher.matches()) {
				// We do have row.
				int requiredFieldCount = this.fieldCount();
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
					if (requiredFieldCount > 0 && fieldCount == requiredFieldCount
							&& (delimiter != null && !delimiter.isEmpty())) {
						// We have parse error - the delimiter indicates there is more rows to return.
						throw new ParseException("Too many fields on the row", matcher.start(3));
					} else if (delimiter == null || delimiter.isEmpty()) {
						throw new ParseException("Too few fields on the row", matcher.end());
					} else {
						result.add(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
					}
				}
				
				// Setting the previous data row, and returning the result. 
				this.previousData = result;
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

	/**
	 * Test validity of the header row.
	 * 
	 * @param row THe tested header row.
	 * @return True, if and only if the given row is valid header row. The default
	 *         requires every field has different non-empty name.
	 */
	public boolean validHeaderRow(List<String> row) {
		// The value is only instance of the value, if its index is equal to its
		// last index. All tested values exists in the rows.
		//
		// The action can be performed parallel, as the row is not modified.
		return row != null && row.parallelStream().allMatch(
				(String value) -> (value != null && !value.isEmpty() && row.indexOf(value) == row.lastIndexOf(value)));
	}

	/**
	 * The field names of the current CVS.
	 * 
	 * The undefined value indicates that the CVS does not contain header row.
	 */
	private List<String> fieldNames = null;

	/**
	 * The previous data row. This is used in absense of field names to determine
	 * the required field count.
	 */
	private List<String> previousData = null;

	/**
	 * The number of fields the CSV file has.
	 * 
	 * @return The number of fields each data row should have. IF this value is 0, the number of columns
	 *  is not determined yet. 
	 */
	public int fieldCount() {
		return (fieldNames == null ? (previousData == null?0:previousData.size()) : fieldNames.size());
	}

	/**
	 * Test validity of read row.
	 * 
	 * By default the row is valid, if it is defined and has at least one field value, and if the number of fields is 
	 * set it has as many fields as the previous header or data row has. 
	 * 
	 * @param row The list of fields of the tested row. 
	 * @return True, if and only if the given row is valid.
	 */
	public boolean validRow(List<String> row) {
		// Performance note:
		// row.size() is only executed once - either with the zero required field count, or with non-zero required field count. 
		int requiredFieldCount = this.fieldCount(); 
		return row != null && ( (requiredFieldCount == 0 && row.size() > 0) || (requiredFieldCount > 0 && requiredFieldCount == row.size()) );
	}

	/**
	 * Reads header row, and initializes new CVS result.
	 * 
	 * IF the CVS reader has read any header or data row, the reading of a header row
	 * always triggers exception.
	 * 
	 * @return True, if and only if the given header row is valid header row.
	 * @throws IOException           The reading of row failed due IO exception.
	 * @throws ParseException        The reading of the row failed due parse error.
	 * @throws IllegalStateException The state of the reader prevents reading header
	 *                               row.
	 */
	public List<String> readHeaderRow() throws IOException, ParseException {
		if (fieldNames != null) {
			throw new IllegalStateException("Cannot read header row twice!");
		}
		if (previousData != null) {
			throw new IllegalStateException("Cannot read header row after data row!");
		}
		List<String> result = readRow();
		
		// Checking validity of the header row.
		if (validHeaderRow(result)) {
			return result;
		} else {
			// Determining the error position.
			int resultLen = result.size();
			if (fieldNames != null && resultLen != fieldNames.size()) {
				// The
			}
			// Some of the elements is invalid.
			for (int i = 0; i < resultLen; i++) {
				if (result.get(i) == null) {
					throw new ParseException("Invalid header", i);
				}
			}
			throw new ParseException("Invalid parse header", 0);
		}
	}

}
