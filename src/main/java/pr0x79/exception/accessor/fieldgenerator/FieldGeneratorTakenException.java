package pr0x79.exception.accessor.fieldgenerator;

import pr0x79.identification.IMethodIdentifier.MethodDescription;

/**
 * Thrown when the name of a field accessor, or its field to generate, is already present in the class to instrument
 */
public class FieldGeneratorTakenException extends FieldGeneratorException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3008812796055579550L;

	private final String field;

	public FieldGeneratorTakenException(String msg, String accessor, MethodDescription method, String field) {
		this(msg, null, accessor, method, field);
	}

	public FieldGeneratorTakenException(String msg, Exception excp, String accessor, MethodDescription method, String field) {
		super(msg, excp, accessor, method);
		this.field = field;
	}

	public String getGeneratedFieldName() {
		return this.field;
	}
}
