package proxy.identifiers;

import java.util.List;

import pr0x79.instrumentation.identification.IFieldIdentifier;

/**
 * A simple implementation of {@link IFieldIdentifier} that
 * identifies fields by matching the field descriptor
 * and the field name with a list of specified names
 */
public class StringFieldIdentifier implements IFieldIdentifier {
	private final FieldDescription[] mappings;

	public StringFieldIdentifier(List<String> fieldNames, List<String> fieldDescriptors) {
		this.mappings = new FieldDescription[fieldNames.size()];
		for(int i = 0; i < fieldNames.size(); i++) {
			this.mappings[i] = new FieldDescription(fieldNames.get(i), fieldDescriptors.get(i));
		}
	}

	@Override
	public FieldDescription[] getFields() {
		return this.mappings;
	}

	@Override
	public boolean isStatic() {
		return true;
	}
}
