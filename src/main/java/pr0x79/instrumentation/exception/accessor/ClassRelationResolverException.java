package pr0x79.instrumentation.exception.accessor;

import org.objectweb.asm.Type;

/**
 * Thrown when the class relation resolver fails
 */
public class ClassRelationResolverException extends AccessorException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7421247919561658976L;

	private final Type type, otherType;

	public ClassRelationResolverException(String msg, Type type, Type otherType, Exception exc) {
		super(msg, exc);
		this.type = type;
		this.otherType = otherType;
	}

	public Type getType() {
		return this.type;
	}

	public Type getOtherType() {
		return this.otherType;
	}
}
