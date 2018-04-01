package proxy.identifiers;

import org.objectweb.asm.tree.MethodNode;

import pr0x79.identification.IInstructionIdentifier;

public class IndexInstructionIdentifier implements IInstructionIdentifier {
	private final int index;
	private final boolean reversed;

	public IndexInstructionIdentifier(int index) {
		this.index = index;
		this.reversed = false;
	}

	public IndexInstructionIdentifier(int index, boolean reversed) {
		this.index = index;
		this.reversed = reversed;
	}

	@Override
	public InstructionType getType() {
		return InstructionType.INSTRUCTION;
	}

	@Override
	public int identify(MethodNode method) {
		return this.reversed ? method.instructions.size() - this.index - 1 : this.index;
	}
}
