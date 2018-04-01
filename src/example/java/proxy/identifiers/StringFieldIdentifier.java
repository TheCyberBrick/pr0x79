package proxy.identifiers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pr0x79.identification.IFieldIdentifier;

/**
 * A simple implementation of {@link IFieldIdentifier} that
 * identifies fields by matching the field descriptor
 * and the field name with a list of specified names
 */
public class StringFieldIdentifier implements IFieldIdentifier {
	private final Set<FieldDescription> mappings = new HashSet<>();

	public StringFieldIdentifier(List<String> fieldNames, List<String> fieldDescriptors) {
		for(int i = 0; i < fieldNames.size(); i++) {
			this.mappings.add(new FieldDescription(fieldNames.get(i), fieldDescriptors.get(i)));
		}
	}

	@Override
	public Set<FieldDescription> getFields() {
		return this.mappings;
	}

	@Override
	public boolean isStatic() {
		return true;
	}
}
