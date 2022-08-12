package solita.helsinki.citybikeapp.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import i18n.Logging;
import solita.helsinki.citybikeapp.controller.CSVReader.CSVHandler;
import solita.helsinkicitybikeapp.model.CSVException;
import solita.helsinkicitybikeapp.model.CSVException.RowType;
import solita.helsinkicitybikeapp.model.CSVJourneys;
import solita.helsinkicitybikeapp.model.Config;
import solita.helsinkicitybikeapp.model.Journeys;
import solita.helsinkicitybikeapp.model.Journeys.Journey;
import solita.helsinkicitybikeapp.model.db.DatabaseJourneys;

/**
 * The class loading journeys. 
 * 
 * The Journeys loader uses handler interface to the reading of the CSV file in order
 * to allow insertion of the CSV data into the database instead of storing it into memory. 
 * 
 * @author Antti Kautiainen
 *
 */
public class JourneysLoader implements i18n.Logging.LocalizedMessageLogging {

	
	
	/**
	 * The pattern matching for the CSV header of the Journeys CSV files. 
	 * 
	 */
	public static final Pattern CSV_HEADER_PATTERN = Pattern
			.compile("^(?:\\u000d\\u00bb\\u00bf)?" + CSVReader.CSV_SIMPLE_DATA_ROW.toString());

	
	/**
	 * The storage of the journey data. 
	 */
	private Journeys data; 
	
	/**
	 * Get the current journeys structure. 
	 * @return The journeys structure of the current parse. 
	 */
	protected Journeys getJourneys() {
		return this.data; 
	}
	
	/**
	 * The reader reading the CVS file. 
	 */
	private CSVReader reader = new CSVReader(getCSVHandler(), CSV_HEADER_PATTERN);
	
	/**
	 * The handler handling the CVS rows. 
	 */
	private CSVHandler handler = new CSVHandler() {

		private List<String> propertyCaptions = null; 
		
		@Override
		public void handleRow(List<String> rowFields) throws CSVException {
			// Creating the journey to add.
			Journeys.Journey entry = (JourneysLoader.this.getJourneys()).new Journey();
			int index = 0; 
			for (String property: JourneysLoader.this.getJourneys().getPropertyNames()) {
				try {
					// Assigning the property value
					entry.setProperty(property, entry.propertyFormatter(property).parseObject(rowFields.get(index))); 
				} catch(IllegalArgumentException | java.text.ParseException pe) {
					// The value was invalid. 
					throw new CSVException.InvalidRowException(RowType.DATA, 
							format("Invalid field value at index {0}", index), rowFields); 
				}
				index++; 
			}
			JourneysLoader.this.getJourneys().addJourney(entry); 
		}

		@Override
		public void handleHeaders(List<String> headerFields) throws CSVException {
			if (propertyCaptions != null) {
				throw new CSVException.DuplicateHeaderException(headerFields);
			}
			Journeys data = getJourneys(); 
			if (data.getJourneyPropertyNames().size() != headerFields.size()) {
				throw new CSVException.InvalidRowException(CSVException.RowType.HEADER, "Invalid field count", headerFields);
			} 
			// TODO: Add journey caption support to the Journeys
			propertyCaptions = headerFields; 
			List<String> propertyNames = data.getJourneyPropertyNames(); 
			for (int i=0, len=propertyNames.size(); i < len; i++) {
				data.setPropertyCaption(propertyNames.get(i), headerFields.get(i)); 
			}
		}
		
	}; 
	
	/**
	 * The handler handling the CSV reading. 
	 * @return
	 */
	protected CSVHandler getCSVHandler() {
		return handler; 
	}
	
	/**
	 * Create Journeys CSV file loader from given source file. 
	 * @param source THe source stream of CSV. 
	 * @param db The database connection to store the journeys. 
	 * @throws IOException The opening of the source content failed. 
	 * @throws java.sql.SQLException The database connection failed. 
	 */
	public JourneysLoader(java.io.InputStream source, java.sql.Connection db) throws IOException, java.sql.SQLException {
		reader = new CSVReader(getCSVHandler(), CSV_HEADER_PATTERN);
		reader.open(source);
		data = db == null?new CSVJourneys():new DatabaseJourneys(db); 
	}
	
	/**
	 * Create Journeys CSV file loader from given source file. 
	 * @param source THe source URL. 
	 * @param db The database connection to store the journeys. 
	 * @throws IOException The opening of the source content failed. 
	 * @throws java.sql.SQLException The database connection failed. 
	 */
	public JourneysLoader(URL source, java.sql.Connection db) throws IOException, java.sql.SQLException {
		reader = new CSVReader(getCSVHandler(), CSV_HEADER_PATTERN);
		reader.open(source);
		data = db == null?new CSVJourneys():new DatabaseJourneys(db); 
	}

	/**
	 * Create Journeys CSV file loader from given source file. 
	 * @param source THe source file. 
	 * @param db The database connection to store the journeys. 
	 * @throws IOException The opening of the source content failed. 
	 * @throws java.sql.SQLException The database connection failed. 
	 */
	public JourneysLoader(File source, java.sql.Connection db) throws IOException, java.sql.SQLException {
		reader = new CSVReader(getCSVHandler(), CSV_HEADER_PATTERN);
		reader.open(source);
		data = db == null?new CSVJourneys():new DatabaseJourneys(db); 
	}
	
	/**
	 * Reads all journeys from the journey reader. 
	 * @return True, if and only if the reading succeeded. 
	 */
	public boolean readAll() {
		try {
			// Reading all rows of the read file. 
			return reader.readAll();
		} catch (CSVException | IOException | ParseException e) {
			// The reading failed. 
			return false; 
		} 
	}
}
