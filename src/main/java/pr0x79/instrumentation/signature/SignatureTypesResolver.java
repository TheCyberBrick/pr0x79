package pr0x79.instrumentation.signature;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.tree.MethodNode;

import pr0x79.instrumentation.signature.SignatureParser.Signature;
import pr0x79.instrumentation.signature.SignatureParser.TypeVariableSymbol;

public class SignatureTypesResolver {
	/**
	 * Resolves all variables in the specified signature
	 * @param method The method that the signature is in
	 * @param signature The signature to resolve
	 */
	public static void resolve(MethodNode method, Signature sig) {
		Set<String> variables = new HashSet<>();
		sig.traverseDFS(symbol -> {
			if(symbol instanceof TypeVariableSymbol) {
				variables.add(symbol.getName());
			}
		});
		System.out.println("Variables: " + variables);
	}
}
