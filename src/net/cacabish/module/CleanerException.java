package net.cacabish.module;

/** An exception to be thrown whenever a cleaning module has an error. */
public class CleanerException extends Exception {

	private static final long serialVersionUID = -6825508605007147451L;

	public CleanerException(String message) {
		super(message);
	}

	public CleanerException(Throwable cause) {
		super(cause);
	}

	public CleanerException(String message, Throwable cause) {
		super(message, cause);
	}

	public CleanerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
