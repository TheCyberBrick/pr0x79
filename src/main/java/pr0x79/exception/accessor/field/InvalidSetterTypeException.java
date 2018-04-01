package pr0x79.exception.accessor.field;

import pr0x79.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when a field accessor has invalid parameters
 */
public class InvalidSetterTypeException extends FieldAccessorException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5254883400517059483L;

	private final String currentType, expectedType;

	public InvalidSetterTypeException(String accessor, MethodDescription method, String currentParameterType, String expectedParameterType) {
		this(String.format("Field accessor %s#%s parameter does not match. Current: %s, Expected: %s, or an accessor of that class", accessor, method.getName() + method.getDescriptor(), currentParameterType, expectedParameterType), null, accessor, method, currentParameterType, expectedParameterType);
	}

	public InvalidSetterTypeException(String msg, Exception excp, String accessor, MethodDescription method, String currentParameterType, String expectedParameterType) {
		super(msg, excp, accessor, method);
		this.currentType = currentParameterType;
		this.expectedType = expectedParameterType;
	}

	public String getCurrentParameterType() {
		return this.currentType;
	}

	public String getExpectedParameterType() {
		return this.expectedType;
	}
}
