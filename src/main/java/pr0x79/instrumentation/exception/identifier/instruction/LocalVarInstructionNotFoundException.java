package pr0x79.instrumentation.exception.identifier.instruction;

import pr0x79.instrumentation.identification.IInstructionIdentifier;
import pr0x79.instrumentation.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when an {@link IInstructionIdentifier} fails to identify a local variable
 */
public class LocalVarInstructionNotFoundException extends InstructionNotFoundException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6715875622023464775L;

	private final int param;

	public LocalVarInstructionNotFoundException(int param, String accessor, MethodDescription method, String identifierId, IInstructionIdentifier identifier) {
		this(String.format("Instruction identifier of @LocalVar for parameter %d of %s#%s:%s was unable to identify the the local variable", param, accessor, method.getName() + method.getDescriptor(), identifierId), null, param, accessor, method, identifierId, identifier);
	}

	public LocalVarInstructionNotFoundException(String msg, Exception exc, int param, String accessor, MethodDescription method, String identifierId, IInstructionIdentifier identifier) {
		super(msg, exc, accessor, method, identifierId, identifier);
		this.param = param;
	}

	public int getParameterIndex() {
		return this.param;
	}
}
