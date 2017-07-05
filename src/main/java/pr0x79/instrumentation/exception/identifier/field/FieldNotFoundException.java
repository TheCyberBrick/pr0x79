package pr0x79.instrumentation.exception.identifier.field;

import pr0x79.instrumentation.identification.IFieldIdentifier;
import pr0x79.instrumentation.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when an {@link IFieldIdentifier} fails to identify a field
 */
public class FieldNotFoundException extends FieldIdentifierException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1453444951899664209L;

	public FieldNotFoundException(String accessor, MethodDescription method, String identifierId, IFieldIdentifier identifier) {
		super(String.format("Field identifier %s#%s:%s was unable to identify a field", accessor, method.getName() + method.getDescriptor(), identifierId), accessor, method, identifierId, identifier);
	}

	public FieldNotFoundException(String msg, Exception excp, String accessor, MethodDescription method, String identifierId, IFieldIdentifier identifier) {
		super(msg, excp, accessor, method, identifierId, identifier);
	}
}
