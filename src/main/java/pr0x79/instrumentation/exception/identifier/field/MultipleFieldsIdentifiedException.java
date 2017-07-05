package pr0x79.instrumentation.exception.identifier.field;

import pr0x79.instrumentation.identification.IFieldIdentifier;
import pr0x79.instrumentation.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when multiple fields are identified by one {@link IFieldIdentifier}
 */
public class MultipleFieldsIdentifiedException extends FieldIdentifierException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4795049985687138200L;

	public MultipleFieldsIdentifiedException(String accessor, MethodDescription method, String identifierId, IFieldIdentifier identifier) {
		super(String.format("Field identifier %s#%s:%s has identified multiple fields", accessor, method.getName() + method.getDescriptor(), identifierId), accessor, method, identifierId, identifier);
	}

	public MultipleFieldsIdentifiedException(String msg, Exception excp, String accessor, MethodDescription method, String identifierId, IFieldIdentifier identifier) {
		super(msg, excp, accessor, method, identifierId, identifier);
	}
}
