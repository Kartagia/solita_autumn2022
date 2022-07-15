package solita.helsinki.citybikeapp.controller;

import java.util.List;

import solita.helsinki.citybikeapp.controller.CSVReader.CSVHandler;
import solita.helsinkicitybikeapp.model.CSVDocument;
import solita.helsinkicitybikeapp.model.CSVException;

/**
 * Handler storing the data to the given CSV document. 
 * @author Antti Kautiainen
 *
 */
public class DocumentHandler implements CSVHandler {

	/** The document containing the read data.
	 * 
	 */
	private CSVDocument doc; 
	
	/**
	 * Creates a new document handler using given document. 
	 */
	public DocumentHandler(CSVDocument document) {
		this.doc = document; 
	}
	
	@Override
	public void handleRow(List<String> rowFields) throws CSVException {
		doc.addDataRow(rowFields);
	}

	@Override
	public void handleHeaders(List<String> headerFields) throws CSVException {
		try {
			doc.setFields(headerFields);
		} catch(IllegalStateException ex) {
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
	}

	/**
	 * The Document result of the handler. 
	 * @return The resulting document. 
	 */
	public CSVDocument getDocument() {
		return this.doc; 
	}
}