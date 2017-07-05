package pr0x79.instrumentation.exception.identifier;

import pr0x79.instrumentation.exception.InstrumentorException;

/**
 * Thrown when something goes wrong during identification of fields, instructions or methods
 */
public class IdentifierException extends InstrumentorException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2856662191469103059L;

	public IdentifierException(String msg) {
		super(msg);
	}

	public IdentifierException(String msg, Exception exc) {
		super(msg, exc);
	}
}
