package pr0x79.exception.identifier.instruction;

import pr0x79.identification.IInstructionIdentifier;
import pr0x79.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when an {@link IInstructionIdentifier} fails to identify the exit instruction 
 */
public class ExitInstructionNotFoundException extends InstructionNotFoundException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1709704515366053314L;

	public ExitInstructionNotFoundException(String accessor, MethodDescription method, String identifierId, IInstructionIdentifier identifier) {
		super(String.format("Instruction identifier for the instruction exit of %s#%s[%s] was unable to identify the instruction", accessor, method.getName() + method.getDescriptor(), identifierId), null, accessor, method, identifierId, identifier);
	}

	public ExitInstructionNotFoundException(String msg, Exception excp, String accessor, MethodDescription method, String identifierId, IInstructionIdentifier identifier) {
		super(msg, excp, accessor, method, identifierId, identifier);
	}
}
