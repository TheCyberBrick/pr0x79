package pr0x79.instrumentation.exception.accessor.field;

import pr0x79.instrumentation.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when the name of a field accessor is already present in the class to instrument
 */
public class FieldAccessorTakenException extends FieldAccessorException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1789455878450140795L;

	public FieldAccessorTakenException(String msg, String accessor, MethodDescription method) {
		super(msg, accessor, method);
	}

	public FieldAccessorTakenException(String msg, Exception excp, String accessor, MethodDescription method) {
		super(msg, excp, accessor, method);
	}
}
