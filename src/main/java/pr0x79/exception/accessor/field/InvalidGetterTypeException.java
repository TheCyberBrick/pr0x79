package pr0x79.exception.accessor.field;

import pr0x79.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when a field accessor has an invalid return type
 */
public class InvalidGetterTypeException extends FieldAccessorException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5905302624286334226L;

	private final String currentType, expectedType;

	public InvalidGetterTypeException(String accessor, MethodDescription method, String currentReturnType, String expectedReturnType) {
		this(String.format("Field accessor %s#%s return type does not match. Current: %s, Expected: %s, or an accessor of that class", accessor, method.getName() + method.getDescriptor(), currentReturnType, expectedReturnType), null, accessor, method, currentReturnType, expectedReturnType);
	}

	public InvalidGetterTypeException(String msg, Exception excp, String accessor, MethodDescription method, String currentReturnType, String expectedReturnType) {
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
