package pr0x79.identification;

import java.util.Set;
import java.util.function.Function;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import pr0x79.exception.InstrumentorException;

/**
 * Identifies a class
 */
public interface IClassIdentifier {
	/**
	 * Returns whether the class node matches
	 * @param cls The {@link ClassNode}
	 * @param flags The {@link ClassReader#accept(ClassVisitor, int)} flags
	 * @param reader Allows getting a {@link ClassNode} with different flags
	 * @return
	 */
	public default boolean isIdentifiedClass(ClassNode cls, int flags, Function<Integer, ClassNode> reader) {
		throw new InstrumentorException("Dynamic mapping not implemented");
	}

	/**
	 * Returns the class' names
	 * @return
	 */
	public default Set<String> getClassNames() {
		throw new InstrumentorException("Static mapping not implemented");
	}

	/**
	 * Returns whether the identification is static ({@link #getClassNames()}) or dynamic ({@link #isIdentifiedClass(MethodNode)})
	 * @return
	 */
	public boolean isStatic();
}
