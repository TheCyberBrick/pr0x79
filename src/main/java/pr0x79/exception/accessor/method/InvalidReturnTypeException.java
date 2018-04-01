package pr0x79.exception.accessor.method;

import pr0x79.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when a method accessor has an invalid return type
 */
public class InvalidReturnTypeException extends MethodAccessorException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6741742168906000373L;

	private final String currentType, expectedType;

	public InvalidReturnTypeException(String msg, Exception excp, String accessor, MethodDescription method, String currentReturnType, String expectedReturnType) {
		super(msg, excp, accessor, method);
		this.currentType = currentReturnType;
		this.expectedType = expectedReturnType;
	}

	public String getCurrentReturnType() {
		return this.currentType;
	}

	public String getExpectedReturnType() {
		return this.expectedType;
	}
}
