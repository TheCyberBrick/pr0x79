package pr0x79.exception;

import pr0x79.accessor.IAccessor;

/**
 * This exception is thrown when something goes wrong with
 * an {@link IAccessor} during the registration or instrumentation
 */
public class InstrumentorException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5129156768385369898L;

	public InstrumentorException(String msg) {
		super(msg);
	}

	public InstrumentorException(String msg, Exception exc) {
		super(msg, exc);
	}
}
