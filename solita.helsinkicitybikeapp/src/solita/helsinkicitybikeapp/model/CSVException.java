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
	 * The exception caused by an attempt to give second header for same CSV
	 * document.
	 * 
	 * The default message is the {@link #DUPLICATE_HEADER_MESSAGE}.
	 * 
	 * @author Antti Kautiainen
	 *
	 */
	public static class DuplicateHeaderException extends CSVException.InvalidRowException {
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
		public DuplicateHeaderException(List<String> row) {
			this(row, DUPLICATE_HEADER_MESSAGE);
		}

		/**
		 * Create a new duplicate header exception caused with a message caused by given
		 * row.
		 * 
		 * @param row     The row causing the problem.
		 * @param message THe message of the exception.
		 */
		public DuplicateHeaderException(List<String> row, String message) {
			super(RowType.HEADER, message, row, null);
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
		 * Create a new empty data row exception for row of a specified type with given
		 * message and cuase.
		 * 
		 * @param type    The type of the row causing the problem.
		 * @param message The message of the row.
		 * @param cause   The cause of the row.
		 */
		public EmptyRowException(CSVException.RowType type, String message, Throwable cause) {
			super(type, message, null, cause);
		}

		/**
		 * Create a new empty data row exception with given row cuasing the probelm
		 * using defualt message.
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
		public EmptyRowException(CSVException.RowType type, List<String> row) {
			this(type, EMPTY_ROW_MESSAGE, row, row == null ? new NullPointerException(NULL_ROW_MESSAGE)
					: new IllegalArgumentException(ILLEGAL_ROW_MESSAGE));
		}

		public EmptyRowException(CSVException.RowType type, String message, List<String> row, Throwable cause) {
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
		public InvalidRowException(CSVException.RowType type, String message, List<String> row, Throwable cause) {
			super(message, cause);
			this.rowType = type;
			this.row = row;
		}

		public InvalidRowException(CSVException.RowType type, String message, List<String> row) {
			this(type, message, row, null);
		}

		/**
		 * The row type of the invalid row.
		 */
		public final CSVException.RowType rowType;

		public final List<String> row;

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