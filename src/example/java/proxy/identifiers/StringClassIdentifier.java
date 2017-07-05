package proxy.identifiers;

import java.util.List;

import pr0x79.instrumentation.identification.IClassIdentifier;


/**
 * A simple implementation of {@link IClassIdentifier} that
 * identifies classes by matching the class name with a list of specified
 * names
 */
public class StringClassIdentifier implements IClassIdentifier {
	private final String[] classNames;

	public StringClassIdentifier(List<String> classNames) {
		this.classNames = classNames.toArray(new String[0]);
	}

	@Override
	public String[] getClassNames() {
		return this.classNames;
	}

	@Override
	public boolean isStatic() {
		return true;
	}
}
