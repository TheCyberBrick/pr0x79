package proxy.identifiers;

import java.util.List;

import pr0x79.instrumentation.identification.IMethodIdentifier;

/**
 * A simple implementation of {@link IMethodIdentifier} that
 * identifies methods by matching the method descriptor
 * and the method name with a list of specified names
 */
public class StringMethodIdentifier implements IMethodIdentifier {
	private final MethodDescription[] mappings;

	public StringMethodIdentifier(List<String> methodNames, List<String> methodDescriptors) {
		this.mappings = new MethodDescription[methodNames.size()];
		for(int i = 0; i < methodNames.size(); i++) {
			this.mappings[i] = new MethodDescription(methodNames.get(i), methodDescriptors.get(i));
		}
	}

	@Override
	public MethodDescription[] getMethods() {
		return this.mappings;
	}

	@Override
	public boolean isStatic() {
		return true;
	}
}
