package pr0x79.instrumentation.signature;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pr0x79.instrumentation.signature.ClassHierarchy.ClassData;
import pr0x79.instrumentation.signature.SignatureParser.FormalTypeParameterSymbol;
import pr0x79.instrumentation.signature.SignatureParser.Signature;
import pr0x79.instrumentation.signature.SignatureParser.TypeVariableSymbol;

public class SignatureTypesResolver {
	/**
	 * Attempts to resolve all variables in the specified method signature.
	 * May fail for local classes.
	 * @param loader The class loader that loaded the class of the method
	 * @param hierarchy The class hierarchy
	 * @param owner The internal class name of the owner class
	 * @param signature The signature to resolve
	 */
	public static Map<String, FormalTypeParameterSymbol> resolve(ClassLoader loader, ClassHierarchy hierarchy, String owner, Signature sig) {
		Set<String> variables = findVariables(sig);

		Map<String, FormalTypeParameterSymbol> resolved = new HashMap<>();

		resolve(loader, hierarchy, owner, variables, resolved, sig);

		return resolved;
	}

	/**
	 * Resolves all variables in the specified class signature.
	 * May fail for local classes.
	 * @param loader The class loader that loaded the class
	 * @param hierarchy The class hierarchy
	 * @param cls The class of the signature
	 * @param sig The signature to resolve
	 */
	public static Map<String, FormalTypeParameterSymbol> resolve(ClassLoader loader, ClassHierarchy hierarchy, ClassData cls, Signature sig) {
		Set<String> variables = findVariables(sig);

		Map<String, FormalTypeParameterSymbol> resolved = new HashMap<>();

		resolve(loader, hierarchy, cls, variables, resolved, sig);

		return resolved;
	}

	private static void resolve(ClassLoader loader, ClassHierarchy hierarchy, String owner, Set<String> variables, Map<String, FormalTypeParameterSymbol> resolved, Signature sig) {
		resolveVariables(resolved, variables, sig);
		variables.addAll(findVariables(sig));
		for(String key : resolved.keySet()) {
			variables.remove(key);
		}

		if(!variables.isEmpty()) {
			ClassData ownerCls = hierarchy.getClass(loader, owner);
			if(ownerCls.signature != null) {
				Signature ownerSig = SignatureParser.parse(ownerCls.signature);
				resolve(loader, hierarchy, ownerCls, variables, resolved, ownerSig);
			}
		}
	}

	private static void resolve(ClassLoader loader, ClassHierarchy hierarchy, ClassData cls, Set<String> variables, Map<String, FormalTypeParameterSymbol> resolved, Signature sig) {
		resolveVariables(resolved, variables, sig);
		variables.addAll(findVariables(sig));
		for(String key : resolved.keySet()) {
			variables.remove(key);
		}

		if(!variables.isEmpty()) {
			ClassData outerCls = hierarchy.getOuterClass(loader, cls.name);
			if(outerCls != null) {
				if(outerCls.signature != null) {
					Signature outerSig = SignatureParser.parse(outerCls.signature);
					resolve(loader, hierarchy, outerCls, variables, resolved, outerSig);
				}
			}
		}
	}

	private static void resolveVariables(Map<String, FormalTypeParameterSymbol> resolved, Set<String> variables, Signature sig) {
		for(FormalTypeParameterSymbol param : sig.formalTypeParameters) {
			if(variables.contains(param.getName()) && !resolved.containsKey(param.getName())) {
				resolved.put(param.getName(), param);
			}
		}
	}

	private static Set<String> findVariables(Signature sig) {
		Set<String> variables = new HashSet<>();
		sig.traverseDFS(symbol -> {
			if(symbol instanceof TypeVariableSymbol) {
				variables.add(symbol.getName());
			}
		});
		return variables;
	}
}
