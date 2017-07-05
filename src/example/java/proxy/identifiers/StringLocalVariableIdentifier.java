package proxy.identifiers;

import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import pr0x79.instrumentation.identification.IInstructionIdentifier;

public class StringLocalVariableIdentifier implements IInstructionIdentifier {
	private final String name;

	public StringLocalVariableIdentifier(String name) {
		this.name = name;
	}

	@Override
	public InstructionType getType() {
		return InstructionType.LOCAL_VARIABLE;
	}

	@Override
	public int identify(MethodNode method) {
		for(int i = 0; i < method.localVariables.size(); i++) {
			LocalVariableNode localVariable = method.localVariables.get(i);
			if(localVariable.name.equals(this.name)) {
				return i;
			}
		}
		return -1;
	}
}
