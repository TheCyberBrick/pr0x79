package pr0x79.instrumentation.exception;

import pr0x79.instrumentation.Internal;

/**
 * Thrown when an interceptor tries to exit with an invalid exit index
 */
public class InvalidInterceptionExitException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2878987809588296952L;

	public InvalidInterceptionExitException(String msg) {
		super(msg);
	}

	@Internal(id = "ctor")
	public InvalidInterceptionExitException(int exit) {
		super(String.format("Interceptor returned with invalid exit %d", exit));
	}
}
