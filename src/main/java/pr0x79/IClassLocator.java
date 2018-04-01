package pr0x79;

import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

@FunctionalInterface
public interface IClassLocator {
	/**
	 * Locates and creates a {@link ClassNode} of a class based on the internal name of the class.
	 * The locator may cache the result to speed up instrumentation. It may return a {@link ClassNode}
	 * without the {@link ClassReader#SKIP_CODE}, {@link ClassReader#SKIP_DEBUG} or {@link ClassReader#SKIP_FRAMES} flags set
	 * regardless of <code>flags</code>, but is not allowed to return a {@link ClassNode} with a flag set that isn't in <code>flags</code>.
	 * The {@link ClassReader#EXPAND_FRAMES} flag in <code>flags</code> must always be respected.
	 * @param loader The classloader of the class that needs to locate this class. May not be the same classloader as the one of the specified class
	 * @param internalClassName The internal name of the class
	 * @param flags The {@link ClassReader} flags
	 * @throws IOException
	 */
	public ClassNode locate(ClassLoader loader, String internalClassName, int flags) throws IOException;
}
