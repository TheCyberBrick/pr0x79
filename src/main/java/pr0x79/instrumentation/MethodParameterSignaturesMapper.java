package pr0x79.instrumentation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

public class MethodParameterSignaturesMapper extends SignatureVisitor {
	private int param = -1;
	private Map<Integer, SignatureWriter> sigWriters = new HashMap<>();

	public MethodParameterSignaturesMapper(int api) {
		super(api);
	}

	@Override
	public SignatureVisitor visitParameterType() {
		this.param++;
		SignatureWriter sigWriter = new SignatureWriter();
		this.sigWriters.put(this.param, sigWriter);
		return sigWriter;
	}

	public void fill(Map<Integer, String> sigs) {
		for(Entry<Integer, SignatureWriter> sig : this.sigWriters.entrySet()) {
			sigs.put(sig.getKey(), sig.getValue().toString());
		}
		this.sigWriters.clear();
		this.param = 0;
	}
}
