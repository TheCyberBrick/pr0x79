package pr0x79.instrumentation.exception.accessor;

import pr0x79.instrumentation.accessor.IAccessor;
import pr0x79.instrumentation.exception.InstrumentorException;

/**
 * Thrown when something goes wrong with an {@link IAccessor}
 */
public class AccessorException extends InstrumentorException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5778137463465864291L;

	public AccessorException(String msg) {
		super(msg);
	}

	public AccessorException(String msg, Exception exc) {
		super(msg, exc);
	}
}
