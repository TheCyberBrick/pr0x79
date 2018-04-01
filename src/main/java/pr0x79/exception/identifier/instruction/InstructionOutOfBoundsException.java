package pr0x79.exception.identifier.instruction;

import pr0x79.identification.IInstructionIdentifier;
import pr0x79.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when an {@link IInstructionIdentifier} returns an instruction index that is out of bounds
 */
public class InstructionOutOfBoundsException extends InstructionIdentifierException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8012394449082583525L;

	private final int targetInstruction;
	private final int min, max;

	public InstructionOutOfBoundsException(String msg, Exception excp, int targetInstruction, int min, int max, String accessor, MethodDescription method, String identifierId, IInstructionIdentifier identifier) {
		super(msg, excp, accessor, method, identifierId, identifier);
		this.targetInstruction = targetInstruction;
		this.min = min;
		this.max = max;
	}

	public int getTargetInstruction() {
		return this.targetInstruction;
	}

	public int getLowerBound() {
		return this.min;
	}

	public int getUpperBound() {
		return this.max;
	}
}
