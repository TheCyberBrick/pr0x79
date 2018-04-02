package pr0x79;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import pr0x79.signature.ClassHierarchy;
import pr0x79.signature.ClassHierarchy.ClassData;

public class ClassRelationResolver {
	private final ClassLoader loader;
	private final ClassHierarchy hierarchy;

	@FunctionalInterface
	public static interface Traverser {
		/**
		 * Called when a class is traversed
		 * @param cls The internal name of the class
		 * @param itf Whether the class is an interface
		 * @param clsNode {@link ClassNode} of the class, may be null
		 * @param flags {@link ClassReader#accept(ClassVisitor, int)} flags of the class node
		 * @return If true the traversal immediately returns with true
		 */
		public boolean traverse(String cls, boolean itf, ClassNode clsNode, int flags);
	}

	/**
	 * Creates a new class relation resolver
	 * @param hierarchy The class hierarchy
	 * @param loader The class loader that is loading or has loaded the class
	 */
	public ClassRelationResolver(ClassHierarchy hierarchy, ClassLoader loader) {
		this.loader = loader;
		this.hierarchy = hierarchy;
	}

	/**
	 * Traverses all superclasses and interfaces (if traverseInterfaces is true) in DFS order
	 * @param clsName The internal name of the class
	 * @param traverser The traverse that traverses the superclasses and interfaces
	 * @param traverseInterfaces Whether interfaces should be traversed too
	 */
	public boolean traverseHierarchy(String clsName, Traverser traverser, boolean traverseInterfaces) {
		final Object[] node = new Object[] {null, 0};
		ClassData cls = this.hierarchy.getClass(this.loader, clsName, (n, f) -> {node[0] = n; node[1] = f;});
		if(cls == null) {
			throw new RuntimeException(String.format("Class %s was not found in class hierarchy", clsName));
		}
		return this.traverseHierarchy(cls, (ClassNode) node[0], (int) node[1], traverser, traverseInterfaces, new HashSet<>());
	}

	private boolean traverseHierarchy(ClassData cls, ClassNode clsNode, int flags, Traverser traverser, boolean traverseInterfaces, Set<String> visitedItfs) {
		String type = cls.name;
		ClassData info = cls;
		final Object[] node = new Object[] {clsNode, flags};
		while (!"java/lang/Object".equals(type)) {
			if(traverser.traverse(type, (info.access & Opcodes.ACC_INTERFACE) != 0, (ClassNode) node[0], (int) node[1])) {
				return true;
			}
			if(traverseInterfaces) {
				List<String> itfs = info.interfaces;
				/*for(String itf : itfs) {
					if(traverser.apply(itf, true)) {
						return true;
					}
				}*/
				for(String itf : itfs) {
					if(visitedItfs.contains(itf)) {
						continue;
					}
					final Object[] itfNode = new Object[] {null, 0};
					ClassData itfData = this.hierarchy.getClass(this.loader, itf, (n, f) -> {itfNode[0] = n; itfNode[1] = f;});
					if(itfData == null) {
						throw new RuntimeException(String.format("Class %s was not found in class hierarchy", itf));
					}
					if(this.traverseHierarchy(itfData, (ClassNode) node[0], (int) node[1], traverser, true, visitedItfs)) {
						return true;
					}
				}
			}
			type = info.superclass;
			if(type != null) {
				info = this.hierarchy.getClass(this.loader, type, (n, f) -> {node[0] = n; node[1] = f;});
				if(info == null) {
					throw new RuntimeException(String.format("Class %s was not found in class hierarchy", type));
				}
			}
		}
		return traverser.traverse("java/lang/Object", false, null, 0); //Visit Object at the end
	}
}
