package pr0x79.instrumentation;

import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

public class SignatureRewriter extends SignatureVisitor {
	public SignatureWriter writer;
	
	public SignatureRewriter(int api) {
		super(api);
	}
	
	@Override
	public SignatureVisitor visitArrayType() {
		if(this.writer != null) {
			this.writer.visitArrayType();
		}
		return super.visitArrayType();
	}
	
	@Override
	public void visitBaseType(char descriptor) {
		if(this.writer != null) {
			this.writer.visitBaseType(descriptor);
		}
		super.visitBaseType(descriptor);
	}
	
	@Override
	public SignatureVisitor visitClassBound() {
		if(this.writer != null) {
			this.writer.visitClassBound();
		}
		return super.visitClassBound();
	}
	
	@Override
	public void visitClassType(String name) {
		if(this.writer != null) {
			this.writer.visitClassType(name);
		}
		super.visitClassType(name);
	}
	
	@Override
	public void visitEnd() {
		if(this.writer != null) {
			this.writer.visitEnd();
		}
		super.visitEnd();
	}
	
	@Override
	public SignatureVisitor visitExceptionType() {
		if(this.writer != null) {
			this.writer.visitExceptionType();
		}
		return super.visitExceptionType();
	}
	
	@Override
	public void visitFormalTypeParameter(String name) {
		if(this.writer != null) {
			this.writer.visitFormalTypeParameter(name);
		}
		super.visitFormalTypeParameter(name);
	}
	
	@Override
	public void visitInnerClassType(String name) {
		if(this.writer != null) {
			this.writer.visitInnerClassType(name);
		}
		super.visitInnerClassType(name);
	}
	
	@Override
	public SignatureVisitor visitInterface() {
		if(this.writer != null) {
			this.writer.visitInterface();
		}
		return super.visitInterface();
	}
	
	@Override
	public SignatureVisitor visitInterfaceBound() {
		if(this.writer != null) {
			this.writer.visitInterfaceBound();
		}
		return super.visitInterfaceBound();
	}
	
	@Override
	public SignatureVisitor visitParameterType() {
		if(this.writer != null) {
			this.writer.visitParameterType();
		}
		return super.visitParameterType();
	}
	
	@Override
	public SignatureVisitor visitReturnType() {
		if(this.writer != null) {
			this.writer.visitReturnType();
		}
		return super.visitReturnType();
	}
	
	@Override
	public SignatureVisitor visitSuperclass() {
		if(this.writer != null) {
			this.writer.visitSuperclass();
		}
		return super.visitSuperclass();
	}
	
	@Override
	public void visitTypeArgument() {
		if(this.writer != null) {
			this.writer.visitTypeArgument();
		}
		super.visitTypeArgument();
	}
	
	@Override
	public SignatureVisitor visitTypeArgument(char wildcard) {
		if(this.writer != null) {
			this.writer.visitTypeArgument(wildcard);
		}
		return super.visitTypeArgument(wildcard);
	}
	
	@Override
	public void visitTypeVariable(String name) {
		if(this.writer != null) {
			this.writer.visitTypeVariable(name);
		}
		super.visitTypeVariable(name);
	}
}
