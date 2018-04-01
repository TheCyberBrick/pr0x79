package pr0x79.exception.identifier.method;

import pr0x79.identification.IMethodIdentifier;
import pr0x79.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when an {@link IMethodIdentifier} fails to identify a method
 */
public class MethodNotFoundException extends MethodIdentifierException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 9036686398085016462L;

	public MethodNotFoundException(String accessor, MethodDescription method, String identifierId, IMethodIdentifier identifier) {
		super(String.format("Method identifier %s#%s[%s] was unable to identify a method", accessor, method.getName() + method.getDescriptor(), identifierId), accessor, method, identifierId, identifier);
	}

	public MethodNotFoundException(String msg, Exception excp, String accessor, MethodDescription method, String identifierId, IMethodIdentifier identifier) {
		super(msg, excp, accessor, method, identifierId, identifier);
	}
}
