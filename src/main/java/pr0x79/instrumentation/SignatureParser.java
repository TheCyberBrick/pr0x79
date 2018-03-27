package pr0x79.instrumentation;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureVisitor;

public class SignatureParser {
	public static class Symbol {
		final boolean isClass;
		final String wildcard;
		final Type type;
		final List<Symbol> signature = new ArrayList<>();
		
		Symbol(Type type, String wildcard) {
			this.type = type;
			this.wildcard = wildcard;
			this.isClass = type != null;
		}
	}
	
	public static List<Symbol> parse(String signature) {
		List<Symbol> symbols = new ArrayList<>();
		
		SignatureVisitor visitor = new SignatureVisitor(Opcodes.ASM5) {
			@Override
			public void visitFormalTypeParameter(String name) {
				// TODO Auto-generated method stub
				super.visitFormalTypeParameter(name);
			}
			
			@Override
			public SignatureVisitor visitClassBound() {
				// TODO Auto-generated method stub
				return super.visitClassBound();
			}
			
			@Override
			public SignatureVisitor visitArrayType() {
				// TODO Auto-generated method stub
				return super.visitArrayType();
			}
			
			@Override
			public SignatureVisitor visitExceptionType() {
				// TODO Auto-generated method stub
				return super.visitExceptionType();
			}
			
			@Override
			public SignatureVisitor visitInterface() {
				// TODO Auto-generated method stub
				return super.visitInterface();
			}
			
			@Override
			public SignatureVisitor visitInterfaceBound() {
				// TODO Auto-generated method stub
				return super.visitInterfaceBound();
			}
			
			@Override
			public SignatureVisitor visitParameterType() {
				// TODO Auto-generated method stub
				return super.visitParameterType();
			}
			
			@Override
			public SignatureVisitor visitReturnType() {
				// TODO Auto-generated method stub
				return super.visitReturnType();
			}
			
			@Override
			public SignatureVisitor visitSuperclass() {
				// TODO Auto-generated method stub
				return super.visitSuperclass();
			}
			
			@Override
			public SignatureVisitor visitTypeArgument(char wildcard) {
				// TODO Auto-generated method stub
				return super.visitTypeArgument(wildcard);
			}
			
			@Override
			public void visitBaseType(char descriptor) {
				// TODO Auto-generated method stub
				super.visitBaseType(descriptor);
			}
			
			@Override
			public void visitClassType(String name) {
				// TODO Auto-generated method stub
				super.visitClassType(name);
			}
			
			@Override
			public void visitEnd() {
				// TODO Auto-generated method stub
				super.visitEnd();
			}
			
			@Override
			public void visitInnerClassType(String name) {
				// TODO Auto-generated method stub
				super.visitInnerClassType(name);
			}
			
			@Override
			public void visitTypeArgument() {
				// TODO Auto-generated method stub
				super.visitTypeArgument();
			}
			
			@Override
			public void visitTypeVariable(String name) {
				// TODO Auto-generated method stub
				super.visitTypeVariable(name);
			}
		};
		
		return null;
	}
}
