package pr0x79.exception.identifier.instruction;

import pr0x79.exception.identifier.IdentifierException;
import pr0x79.identification.IInstructionIdentifier;
import pr0x79.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when something goes wrong during identification of instructions by an {@link IInstructionIdentifier}
 */
public class InstructionIdentifierException extends IdentifierException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -283728513729500070L;

	private final IInstructionIdentifier identifier;
	private final String accessor, identifierId;
	private final MethodDescription method;

	public InstructionIdentifierException(String msg, String accessor, MethodDescription method, String identifierId, IInstructionIdentifier identifier) {
		this(msg, null, accessor, method, identifierId, identifier);
	}

	public InstructionIdentifierException(String msg, Exception exc, String accessor, MethodDescription method, String identifierId, IInstructionIdentifier identifier) {
		super(msg, exc);
		this.identifier = identifier;
		this.accessor = accessor;
		this.method = method;
		this.identifierId = identifierId;
	}

	public IInstructionIdentifier getIdentifier() {
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
