package pr0x79.instrumentation.exception.accessor.method;

import pr0x79.instrumentation.exception.accessor.AccessorException;
import pr0x79.instrumentation.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when something goes wrong with a method accessor
 */
public class MethodAccessorException extends AccessorException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3804720363531842287L;

	private final String accessor;
	private final MethodDescription method;

	public MethodAccessorException(String msg, String accessor, MethodDescription method) {
		this(msg, null, accessor, method);
	}

	public MethodAccessorException(String msg, Exception excp, String accessor, MethodDescription method) {
		super(msg);
		this.accessor = accessor;
		this.method = method;
	}

	public String getAccessorClass() {
		return this.accessor;
	}

	public MethodDescription getAccessorMethod() {
		return this.method;
	}
}
