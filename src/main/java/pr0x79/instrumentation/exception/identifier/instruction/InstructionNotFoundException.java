package pr0x79.instrumentation.exception.identifier.instruction;

import pr0x79.instrumentation.identification.IInstructionIdentifier;
import pr0x79.instrumentation.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when an {@link IInstructionIdentifier} fails to identify an instruction
 */
public class InstructionNotFoundException extends InstructionIdentifierException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6301836443408809031L;

	public InstructionNotFoundException(String accessor, MethodDescription method, String identifierId, IInstructionIdentifier identifier) {
		super(String.format("Instruction identifier %s#%s:%s was unable to identify the instruction", accessor, method.getName() + method.getDescriptor(), identifierId), accessor, method, identifierId, identifier);
	}

	public InstructionNotFoundException(String msg, Exception exc, String accessor, MethodDescription method, String identifierId, IInstructionIdentifier identifier) {
		super(msg, exc, accessor, method, identifierId, identifier);
	}
}
