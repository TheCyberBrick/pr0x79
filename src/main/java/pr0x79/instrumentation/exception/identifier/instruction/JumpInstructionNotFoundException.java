package pr0x79.instrumentation.exception.identifier.instruction;

import pr0x79.instrumentation.identification.IInstructionIdentifier;
import pr0x79.instrumentation.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when an {@link IInstructionIdentifier} fails to identify the jump instruction 
 */
public class JumpInstructionNotFoundException extends InstructionNotFoundException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1709704515366053314L;

	public JumpInstructionNotFoundException(String accessor, MethodDescription method, String identifierId, IInstructionIdentifier identifier) {
		super(String.format("Instruction identifier for the instruction jump of %s#%s:%s was unable to identify the instruction", accessor, method.getName() + method.getDescriptor(), identifierId), null, accessor, method, identifierId, identifier);
	}

	public JumpInstructionNotFoundException(String msg, Exception excp, String accessor, MethodDescription method, String identifierId, IInstructionIdentifier identifier) {
		super(msg, excp, accessor, method, identifierId, identifier);
	}
}
