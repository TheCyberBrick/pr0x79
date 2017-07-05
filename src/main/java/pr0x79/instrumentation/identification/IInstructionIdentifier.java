package pr0x79.instrumentation.identification;

import org.objectweb.asm.tree.MethodNode;

/**
 * Identifies the index of an instruction in a {@link MethodNode}
 */
public interface IInstructionIdentifier {
	public static enum InstructionType {
		INSTRUCTION,
		LOCAL_VARIABLE
	}

	/**
	 * Returns the instruction type this identifier can identify
	 * @return
	 */
	public InstructionType getType();

	/**
	 * Returns the index of the instruction to identify, or -1 if not found
	 * @param method
	 * @return
	 */
	public int identify(MethodNode method);
}
