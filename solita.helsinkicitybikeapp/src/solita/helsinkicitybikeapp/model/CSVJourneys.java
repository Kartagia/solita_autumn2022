package solita.helsinkicitybikeapp.model;

import java.util.List;

import i18n.Logging;
import solita.helsinkicitybikeapp.model.Journeys.Journey;

/**
 * The CSV Document implementation of the journeys.
 * 
 * @author Antti Kautiainen
 *
 */
public class CSVJourneys extends Journeys implements Logging.MessageLogging {
	/**
	 * The CSV document storing the journeys.
	 */
	private CSVDocument journeysCSV = null;

	public CSVJourneys() throws CSVException {
		journeysCSV = new SimpleCSVDocument();
		initFields();
	}

	public void initFields() throws IllegalStateException, CSVException {
		journeysCSV.setFields(getJourneyPropertyNames());
	}

	@Override
	public boolean addJourney(Journey journey) throws IllegalArgumentException {
		if (journey == null) {
			// Undefined journey cannot be added to journeys.
			return false;
		}
		try {
			return journeysCSV.addDataRow(journey == null ? (List<String>) null
					: (getJourneyPropertyNames().stream().map((String property) -> {
						return String.valueOf(journey.getProperty(property));
					})).toList());
		} catch (CSVException e) {
			// This happen if the row is null
			severe("CSV Error {0} which should never happen", e);
			throw new IllegalArgumentException("Invalid journey", e);
		}
	}

	@Override
	public Journey getJourney(int index) {
		CSVDataRow data = journeysCSV.getDataRow(index);
		if (data == null) {
			return null;
		} else {
			Journey result = new Journey();

			return result;
		}
	}

}