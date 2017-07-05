package pr0x79.instrumentation.exception.identifier.field;

import pr0x79.instrumentation.exception.identifier.IdentifierException;
import pr0x79.instrumentation.identification.IFieldIdentifier;
import pr0x79.instrumentation.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when something goes wrong during identification of fields by an {@link IFieldIdentifier}
 */
public class FieldIdentifierException extends IdentifierException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2578257736499007681L;

	private final IFieldIdentifier identifier;
	private final String accessor, identifierId;
	private final MethodDescription method;

	public FieldIdentifierException(String msg, String accessor, MethodDescription method, String identifierId, IFieldIdentifier identifier) {
		this(msg, null, accessor, method, identifierId, identifier);
	}

	public FieldIdentifierException(String msg, Exception excp, String accessor, MethodDescription method, String identifierId, IFieldIdentifier identifier) {
		super(msg);
		this.identifier = identifier;
		this.accessor = accessor;
		this.method = method;
		this.identifierId = identifierId;
	}

	public IFieldIdentifier getIdentifier() {
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
