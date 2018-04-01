package proxy.identifiers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pr0x79.identification.IClassIdentifier;


/**
 * A simple implementation of {@link IClassIdentifier} that
 * identifies classes by matching the class name with a list of specified
 * names
 */
public class StringClassIdentifier implements IClassIdentifier {
	private final Set<String> classNames;

	public StringClassIdentifier(List<String> classNames) {
		this.classNames = new HashSet<>(classNames);
	}

	@Override
	public Set<String> getClassNames() {
		return this.classNames;
	}

	@Override
	public boolean isStatic() {
		return true;
	}
}
