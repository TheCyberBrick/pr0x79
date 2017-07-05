package pr0x79.instrumentation.identification;

import org.objectweb.asm.tree.MethodNode;

import pr0x79.instrumentation.exception.InstrumentorException;

/**
 * Identifies classes
 */
public interface IClassIdentifier {
	/**
	 * Returns whether the class name matches
	 * @param cls
	 * @return
	 */
	public default boolean isIdentifiedClass(String cls) {
		throw new InstrumentorException("Dynamic mapping not implemented");
	}

	/**
	 * Returns the class' names
	 * @return
	 */
	public default String[] getClassNames() {
		throw new InstrumentorException("Static mapping not implemented");
	}

	/**
	 * Returns whether the identification is static ({@link #getClassNames()}) or dynamic ({@link #isIdentifiedClass(MethodNode)})
	 * @return
	 */
	public boolean isStatic();
}
