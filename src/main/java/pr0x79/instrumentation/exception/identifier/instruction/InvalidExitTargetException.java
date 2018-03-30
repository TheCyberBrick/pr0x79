package pr0x79.instrumentation.exception.identifier.instruction;

import pr0x79.instrumentation.identification.IInstructionIdentifier;
import pr0x79.instrumentation.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when an {@link IInstructionIdentifier} returns an invalid exit target
 */
public class InvalidExitTargetException extends InstructionIdentifierException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 400466053219303093L;

	private final int targetInstruction;

	public InvalidExitTargetException(String msg, int targetInstruction, String accessor, MethodDescription method, String identifierId, IInstructionIdentifier identifier) {
		this(msg, null, targetInstruction, accessor, method, identifierId, identifier);
	}

	public InvalidExitTargetException(String msg, Exception excp, int targetInstruction, String accessor, MethodDescription method, String identifierId, IInstructionIdentifier identifier) {
		super(msg, excp, accessor, method, identifierId, identifier);
		this.targetInstruction = targetInstruction;
	}

	public int getTargetInstruction() {
		return this.targetInstruction;
	}
}
