package net.cacabish;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A helper class which servers as the container for handling any errors related to validating an XML document.
 * @author cacabish
 * @version 1.0.0
 *
 */
public class XMLValidationErrorHandler implements ErrorHandler {

	/**
	 * Any exception that gets thrown, resulting from an error or fatal error, to the error handler is put here.
	 */
	private SAXParseException exception;
	
	/**
	 * Constructs an empty container handler.
	 */
	public XMLValidationErrorHandler() {
		exception = null;
	}
	
	/**
	 * Resets the exception state.
	 */
	public void reset() {
		exception = null;
	}
	
	/**
	 * Checks if there was an exception thrown.
	 * @return true if there was no expection thrown, false otherwise
	 */
	public boolean isValid() {
		return exception == null;
	}
	
	/**
	 * Returns the thrown exception.
	 * @return {@code null} if there is no exception thrown, else it returns the exception 
	 */
	public SAXParseException getException() {
		return exception;
	}
	
	@Override
	public void warning(SAXParseException exception) throws SAXException {
		// Warnings don't warrant being flagged as failure
	}

	@Override
	public void error(SAXParseException exception) throws SAXException {
		this.exception = exception;
	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		this.exception = exception;
	}

}
