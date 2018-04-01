package pr0x79.exception.identifier.method;

import pr0x79.identification.IMethodIdentifier;
import pr0x79.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when multiple methods are identified by one {@link IMethodIdentifier}
 */
public class MultipleMethodsIdentifiedException extends MethodIdentifierException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3935137290069904342L;

	public MultipleMethodsIdentifiedException(String accessor, MethodDescription method, String identifierId, IMethodIdentifier identifier) {
		super(String.format("Method identifier %s#%s[%s] has identified multiple methods", accessor, method.getName() + method.getDescriptor(), identifierId), accessor, method, identifierId, identifier);
	}

	public MultipleMethodsIdentifiedException(String msg, Exception excp, String accessor, MethodDescription method, String identifierId, IMethodIdentifier identifier) {
		super(msg, excp, accessor, method, identifierId, identifier);
	}
}
