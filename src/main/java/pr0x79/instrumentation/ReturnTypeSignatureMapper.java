package pr0x79.instrumentation;

import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

public class ReturnTypeSignatureMapper extends SignatureVisitor {
	private SignatureWriter writer;
	
	public ReturnTypeSignatureMapper(int api) {
		super(api);
	}

	@Override
	public SignatureVisitor visitReturnType() {
		this.writer = new SignatureWriter();
		return this.writer;
	}
	
	@Override
	public String toString() {
		return this.writer == null ? "" : this.writer.toString();
	}
	
	public String getSignature() {
		return this.writer == null ? null : this.writer.toString();
	}
}
