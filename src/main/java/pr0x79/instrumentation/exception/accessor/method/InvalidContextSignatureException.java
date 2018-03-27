package pr0x79.instrumentation.exception.accessor.method;

import pr0x79.instrumentation.accessor.IInterceptorContext;
import pr0x79.instrumentation.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when an {@link IInterceptorContext} has an invalid signature
 */
public class InvalidContextSignatureException extends MethodInterceptorException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6989033597572208643L;

	private final String currentSig, expectedSig;

	public InvalidContextSignatureException(String msg, String accessor, MethodDescription method, String currentSig, String expectedSig) {
		super(msg, accessor, method);
		this.currentSig = currentSig;
		this.expectedSig = expectedSig;
	}

	public String getCurrentSignature() {
		return this.currentSig;
	}

	public String getExpectedSignature() {
		return this.expectedSig;
	}
}
