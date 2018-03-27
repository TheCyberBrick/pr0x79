package pr0x79.instrumentation;

import java.util.Map;

import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureVisitor;

public class SignatureTypesMapper extends SignatureVisitor {
	private final int type;
	private final Map<Integer, Type> types;
	private final int depth;

	public SignatureTypesMapper(int api, Map<Integer, Type> types, int depth) {
		this(api, types, depth, 0);
	}

	private SignatureTypesMapper(int api, Map<Integer, Type> types, int depth, int type) {
		super(api);
		this.types = types;
		this.depth = depth;
		this.type = type;
	}

	@Override
	public SignatureVisitor visitParameterType() {
		return new SignatureTypesMapper(this.api, this.types, this.depth - 1, this.type + 1);
	}

	@Override
	public void visitClassType(String name) {
		if(this.depth >= 0) {
			this.types.put(this.type, Type.getObjectType(name));
		}
	}

	@Override
	public void visitInnerClassType(String name) {
		if(this.depth >= 0) {
			this.types.put(this.type, Type.getObjectType(name));
		}
	}
}
