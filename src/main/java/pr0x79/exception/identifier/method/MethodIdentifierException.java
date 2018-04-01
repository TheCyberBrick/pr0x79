package pr0x79.exception.identifier.method;

import pr0x79.exception.identifier.IdentifierException;
import pr0x79.identification.IMethodIdentifier;
import pr0x79.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when something goes wrong during identification of methods by an {@link IMethodIdentifier}
 */
public class MethodIdentifierException extends IdentifierException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -698120271459930196L;

	private final IMethodIdentifier identifier;

	private final String accessor, identifierId;
	private final MethodDescription method;

	public MethodIdentifierException(String msg, String accessor, MethodDescription method, String identifierId, IMethodIdentifier identifier) {
		this(msg, null, accessor, method, identifierId, identifier);
	}

	public MethodIdentifierException(String msg, Exception excp, String accessor, MethodDescription method, String identifierId, IMethodIdentifier identifier) {
		super(msg, excp);
		this.identifier = identifier;
		this.accessor = accessor;
		this.method = method;
		this.identifierId = identifierId;
	}

	public IMethodIdentifier getIdentifier() {
		return this.identifier;
	}

	public String getAccessorClass() {
		return this.accessor;
	}

	public MethodDescription getAccessorMethod() {
		return this.method;
	}

	public String getIdentifierId() {
		return this.identifierId;
	}
}
