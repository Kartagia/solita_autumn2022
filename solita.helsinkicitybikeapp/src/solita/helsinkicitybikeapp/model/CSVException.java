package solita.helsinkicitybikeapp.model;

import java.util.List;

/**
 * The exception class handling CSV related exceptions.
 * 
 * @author Antti Kautiainen
 *
 */
public class CSVException extends RuntimeException {

	/**
	 * Row exception linked to the data rows. 
	 * 
	 * @author Antti Kautiainen 
	 *
	 */
	public static class DataRowException extends CSVException.InvalidRowException {
		/**
		 * Create a new data row exception. 
		 * @param message The message of the exception. 
		 * @param rowFields The invalid row fields. 
		 */
		public DataRowException(List<? extends CharSequence> rowFields, String message) {
			this(message, rowFields, null); 
		}
		
		/**
		 * Create a new data row exception with cause. 
		 * @param message The message of the exception. 
		 * @param rowFields The invalid row fields. 
		 * @param cause The cause of the exception. 
		 */
		public DataRowException(String message, List<? extends CharSequence> rowFields, Throwable cause) {
			super(RowType.HEADER, message, rowFields, cause);
		}
	}

	/**
	 * Row exception linked to the header rows. 
	 * 
	 * @author Antti Kautiainen 
	 *
	 */
	public static class HeaderException extends CSVException.InvalidRowException {
		/**
		 * Create a new header row exception. 
		 * @param message The message of the exception. 
		 * @param rowFields The invalid row fields. 
		 */
		public HeaderException(String message, List<? extends CharSequence> rowFields) {
			this(message, rowFields, null); 
		}
		
		/**
		 * Create a new header row exception with cause. 
		 * @param message The message of the exception. 
		 * @param rowFields The invalid row fields. 
		 * @param cause The cause of the exception. 
		 */
		public HeaderException(String message, List<? extends CharSequence> rowFields, Throwable cause) {
			super(RowType.HEADER, message, rowFields, cause);
		}
	}
	
	/**
	 * The exception caused by an attempt to give second header for same CSV
	 * document.
	 * 
	 * The default message is the {@link #DUPLICATE_HEADER_MESSAGE}.
	 * 
	 * @author Antti Kautiainen
	 *
	 */
	public static class DuplicateHeaderException extends CSVException.HeaderException {
		/**
		 * The message of the duplicate header.
		 */
		public static final String DUPLICATE_HEADER_MESSAGE = "Duplicate header";

		/**
		 * Create a new duplicate header exception caused by given row.
		 * 
		 * The default message is used as message.
		 * 
		 * @param row The row causing the problem.
		 */
		public DuplicateHeaderException(List<? extends CharSequence> row) {
			this(DUPLICATE_HEADER_MESSAGE, row);
		}

		/**
		 * Create a new duplicate header exception caused with a message caused by given
		 * row.
		 * @param message THe message of the exception.
		 * @param row     The row causing the problem.
		 */
		public DuplicateHeaderException(String message, List<? extends CharSequence> row) {
			super(message, row);
		}
	}

	/**
	 * The message of an undefined row.
	 */
	public static final String NULL_ROW_MESSAGE = "Undefined row";

	/**
	 * The message of an invalid row.
	 */
	public static final String ILLEGAL_ROW_MESSAGE = "Invalid row";

	/**
	 * The exception indicating that the data row was empty.
	 * 
	 * The default message is the {@link #EMPTY_ROW_MESSAGE}.
	 * 
	 * @author Antti Kautiainen
	 *
	 */
	public static class EmptyRowException extends CSVException.InvalidRowException {
		/**
		 * The message indicating the row is empty.
		 */
		public static final String EMPTY_ROW_MESSAGE = "Empty Data row";

		/**
		 * Create an empty data row exception without cause with default message.
		 * 
		 */
		public EmptyRowException() {
			this(null);
		}

		/**
		 * Create an empty data row exception with default message and given cause.
		 * 
		 * @param cause The cause of the exception.
		 */
		public EmptyRowException(Throwable cause) {
			this(RowType.DATA, EMPTY_ROW_MESSAGE, cause);
		}

		/**
		 * Create a new empty data row exception with given message and cause. 
		 * 
		 * @param type    The type of the row causing the problem.
		 * @param message The message of the row.
		 * @param cause   The cause of the row.
		 */
		public EmptyRowException(CSVException.RowType type, String message, Throwable cause) {
			super(type, message, null, cause);
		}

		/**
		 * Create a new empty data row exception with default message. 
		 * 
		 * The value of the row determines the cause:
		 * <dl>
		 * <dt>NullPointerException</dt>
		 * <dd>The given row was undefined.</dd>
		 * <dt>IllegalArgumentException</dt>
		 * <dd>The given row was existing row.</dd>
		 * </dl>
		 * 
		 * The exception lets user to create empty row exception with non-empty row.
		 * 
		 * @param type The type of the row.
		 * @param row  The row causing the exception.
		 */
		public EmptyRowException(CSVException.RowType type, List<? extends CharSequence> row) {
			this(type, EMPTY_ROW_MESSAGE, row, row == null ? new NullPointerException(NULL_ROW_MESSAGE)
					: new IllegalArgumentException(ILLEGAL_ROW_MESSAGE));
		}

		/**
		 * Create a new empty row exception with specified message and cause. 
		 * @param type The type of the erroneous row. 
		 * @param message The message of the exception. 
		 * @param row The invalid row causing the exception. 
		 * @param cause The cause of the exception. 
		 */
		public EmptyRowException(CSVException.RowType type, String message, List<? extends CharSequence> row, Throwable cause) {
			super(type, message, row, cause);
		}
	}

	/**
	 * The enumeration for row type.
	 * 
	 * @author Antti Kautiainen
	 *
	 */
	public static enum RowType {
		HEADER, DATA;
	}

	/**
	 * The exception indicating that the data row is invalid.
	 * 
	 * @author Antti Kautiainen
	 *
	 */
	public static class InvalidRowException extends CSVException {
		/**
		 * Creates a new invalid row exception with given message, and cause. 
		 * @param type The type of the erroneous row. 
		 * @param message The message of the exception. 
		 * @param row The invalid row causing the exception. 
		 * @param cause The cause of the exception. 
		 */
		public InvalidRowException(CSVException.RowType type, String message, List<? extends CharSequence> row, Throwable cause) {
			super(message, cause);
			this.rowType = type;
			this.row = row;
		}

		/**
		 * Create a new row exception with given message and cause. 
		 * 
		 * @param type    The type of the row causing the problem.
		 * @param message The message of the row.
		 * @param cause   The cause of the row.
		 */
		public InvalidRowException(CSVException.RowType type, String message, List<? extends CharSequence> row) {
			this(type, message, row, null);
		}

		/**
		 * The row type of the invalid row.
		 */
		public final CSVException.RowType rowType;

		/**
		 * The row causing the problem. Changes on this row will change the invalid row. 
		 */
		public final List<? extends CharSequence> row;

	}

	/**
	 * Creates a new CSV exception.
	 * 
	 * @param message The message of the exception.
	 * @param cause   The cause of the exception.
	 */
	public CSVException(String message, Throwable cause) {
		super(message, cause);
	}

}