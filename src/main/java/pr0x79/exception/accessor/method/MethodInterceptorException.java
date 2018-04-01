package pr0x79.exception.accessor.method;

import pr0x79.exception.accessor.AccessorException;
import pr0x79.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when something goes wrong with a method interceptor
 */
public class MethodInterceptorException extends AccessorException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7367122485446639843L;

	private final String accessor;
	private final MethodDescription method;

	public MethodInterceptorException(String msg, String accessor, MethodDescription method) {
		this(msg, null, accessor, method);
	}

	public MethodInterceptorException(String msg, Exception excp, String accessor, MethodDescription method) {
		super(msg);
		this.accessor = accessor;
		this.method = method;
	}

	public String getAccessorClass() {
		return this.accessor;
	}

	public MethodDescription getInterceptorMethod() {
		return this.method;
	}
}
