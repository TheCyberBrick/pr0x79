package pr0x79.exception.accessor.field;

import pr0x79.exception.accessor.AccessorException;
import pr0x79.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when something goes wrong with a field accessor
 */
public class FieldAccessorException extends AccessorException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7112058799886783845L;

	private final String accessor;
	private final MethodDescription method;

	public FieldAccessorException(String msg, String accessor, MethodDescription method) {
		this(msg, null, accessor, method);
	}

	public FieldAccessorException(String msg, Exception excp, String accessor, MethodDescription method) {
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
