package proxy.identifiers;

import org.objectweb.asm.tree.MethodNode;

import pr0x79.instrumentation.identification.IInstructionIdentifier;


public class IndexLocalVariableIdentifier implements IInstructionIdentifier {
	private final int index;
	private final boolean reversed;

	public IndexLocalVariableIdentifier(int index) {
		this.index = index;
		this.reversed = false;
	}

	public IndexLocalVariableIdentifier(int index, boolean reversed) {
		this.index = index;
		this.reversed = reversed;
	}

	@Override
	public InstructionType getType() {
		return InstructionType.LOCAL_VARIABLE;
	}

	@Override
	public int identify(MethodNode method) {
		return this.reversed ? method.localVariables.size() - this.index - 1 : this.index;
	}
}
