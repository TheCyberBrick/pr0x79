package proxy.identifiers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pr0x79.identification.IMethodIdentifier;

/**
 * A simple implementation of {@link IMethodIdentifier} that
 * identifies methods by matching the method descriptor
 * and the method name with a list of specified names
 */
public class StringMethodIdentifier implements IMethodIdentifier {
	private final Set<MethodDescription> mappings = new HashSet<>();

	public StringMethodIdentifier(List<String> methodNames, List<String> methodDescriptors) {
		for(int i = 0; i < methodNames.size(); i++) {
			this.mappings.add(new MethodDescription(methodNames.get(i), methodDescriptors.get(i)));
		}
	}

	@Override
	public Set<MethodDescription> getMethods() {
		return this.mappings;
	}

	@Override
	public boolean isStatic() {
		return true;
	}
}
