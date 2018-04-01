package proxy.identifiers;

import java.util.Iterator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import pr0x79.identification.IInstructionIdentifier;

public class ReturnInstructionIdentifier implements IInstructionIdentifier {
	private final int offset;
	private final boolean last;

	public ReturnInstructionIdentifier() {
		this.offset = 0;
		this.last = true;
	}

	public ReturnInstructionIdentifier(int offset, boolean last) {
		this.offset = offset;
		this.last = last;
	}

	@Override
	public InstructionType getType() {
		return InstructionType.INSTRUCTION;
	}

	@Override
	public int identify(MethodNode method) {
		int returnIndex = -1;
		int index = 0;
		Iterator<AbstractInsnNode> nodeIT = method.instructions.iterator();
		while(nodeIT.hasNext()) {
			AbstractInsnNode node = nodeIT.next();
			if(node.getOpcode() == Opcodes.RETURN ||
					node.getOpcode() == Opcodes.ARETURN ||
					node.getOpcode() == Opcodes.DRETURN ||
					node.getOpcode() == Opcodes.FRETURN ||
					node.getOpcode() == Opcodes.IRETURN ||
					node.getOpcode() == Opcodes.LRETURN) {
				returnIndex = index + this.offset;
				if(!this.last) {
					return returnIndex;
				}
			}
			index++;
		}
		return returnIndex;
	}
}
