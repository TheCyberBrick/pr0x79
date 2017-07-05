package pr0x79.instrumentation.exception.accessor.fieldgenerator;

import pr0x79.instrumentation.exception.accessor.AccessorException;
import pr0x79.instrumentation.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when something goes wrong with a field generator
 */
public class FieldGeneratorException extends AccessorException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5589064862787370805L;

	private final String accessor;
	private final MethodDescription method;

	public FieldGeneratorException(String msg, String accessor, MethodDescription method) {
		this(msg, null, accessor, method);
	}

	public FieldGeneratorException(String msg, Exception excp, String accessor, MethodDescription method) {
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
