package pr0x79.instrumentation;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

public class SignatureParser {
	public static class FormalSymbol { }

	public static class FormalTypeSymbol extends FormalSymbol {
		public final Type type;
		public final boolean iface;
		public final FormalSymbol[] signature;

		private FormalTypeSymbol(Type type, boolean iface, FormalSymbol[] signature) {
			this.type = type;
			this.iface = iface;
			this.signature = signature;
			for(FormalSymbol symbol : signature) {
				if(symbol instanceof FormalTypeSymbol == false && symbol instanceof FormalTypeParameterSymbol == false) {
					throw new RuntimeException("Signature symbol " + symbol + " is neither a type nor a type variable");
				}
			}
		}
	}

	public static class FormalTypeVariableSymbol extends FormalSymbol {
		public final String name;

		private FormalTypeVariableSymbol(String name) {
			this.name = name;
		}
		
		//TODO FormalTypeVariableSymbol will later be resolved to FormalTypeParameterSymbols
	}
	
	public static class FormalTypeParameterSymbol extends FormalSymbol {
		public final String name;
		public final FormalTypeSymbol extendsType;
		public final FormalTypeSymbol[] implementsTypes;

		private FormalTypeParameterSymbol(String name, FormalTypeSymbol extendsType, FormalTypeSymbol[] implementsTypes) {
			this.name = name;
			this.extendsType = extendsType;
			this.implementsTypes = implementsTypes;
			for(FormalTypeSymbol type : implementsTypes) {
				if(!type.iface) {
					throw new RuntimeException("Implements type " + type.type + " is not an interface");
				}
			}
		}
	}



	public static List<FormalSymbol> parseFormalSignature(String signature) {
		new SignatureReader(signature).accept(new FormalSignatureVisitor(Opcodes.ASM5));

		return null;
	}
	
	public static void parseMethodSignature(String methodSignature) {
		
	}

	public static class FormalSignatureVisitor extends SignatureVisitor {

		private static final int FORMAL = 1 << 1;

		private static final int BOUND = 1 << 2;

		private static final int BOUND_IFACE = 1 << 3;

		private static final int STOP = 1 << 4;

		private int state;

		public FormalSignatureVisitor(int api) {
			super(api);
		}

		@Override
		public void visitFormalTypeParameter(String name) {
			System.out.println("visit type param: " + name);
			this.state = FORMAL;
		}

		@Override
		public void visitTypeVariable(String name) {
			if(this.state != STOP) {
				System.out.println("visit type var: " + name);
			}
		}

		@Override
		public SignatureVisitor visitClassBound() {
			this.state = BOUND;
			System.out.println("visit class bound");
			return this;
		}

		@Override
		public SignatureVisitor visitInterfaceBound() {
			this.state = BOUND_IFACE;
			System.out.println("visit interface bound");
			return this;
		}

		@Override
		public void visitClassType(String name) {
			System.out.println("visit class type: " + name);
		}

		@Override
		public void visitBaseType(char descriptor) {
			System.out.println("visit base type: " + descriptor);
		}

		@Override
		public SignatureVisitor visitArrayType() {
			//TODO Arrays shouldn't be possible in formal state??
			return this;
		}

		@Override
		public void visitInnerClassType(String name) {
			//TODO Inner class type shouldn't be possible in formal state??
			System.out.println("visit inner class type: " + name);
		}

		@Override
		public SignatureVisitor visitParameterType() {
			this.state = STOP;
			return this;
		}

		@Override
		public SignatureVisitor visitReturnType() {
			this.state = STOP;
			return this;
		}
	}
}
