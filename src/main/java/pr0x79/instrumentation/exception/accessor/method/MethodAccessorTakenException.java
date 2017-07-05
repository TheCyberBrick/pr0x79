package pr0x79.instrumentation.exception.accessor.method;

import pr0x79.instrumentation.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when the name of a method accessor is already present in the class to instrument
 */
public class MethodAccessorTakenException extends MethodAccessorException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2935900368573669929L;

	public MethodAccessorTakenException(String msg, String accessor, MethodDescription method) {
		super(msg, accessor, method);
	}

	public MethodAccessorTakenException(String msg, Exception excp, String accessor, MethodDescription method) {
		super(msg, excp, accessor, method);
	}
}
