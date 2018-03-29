package pr0x79.instrumentation;

import java.util.List;
import java.util.function.Function;

import org.objectweb.asm.tree.ClassNode;

import pr0x79.instrumentation.signature.ClassHierarchy;

public class ClassRelationResolver {
	private final ClassLoader loader;
	private final ClassHierarchy hierarchy;
	private final String name;

	/**
	 * Creates a new class relation resolver
	 * @param hierarchy The class hierarchy
	 * @param loader The class loader that is loading or has loaded the class
	 * @param name The class name
	 */
	public ClassRelationResolver(ClassHierarchy hierarchy, ClassLoader loader, String name) {
		this.loader = loader;
		this.hierarchy = hierarchy;
		this.name = name;
	}

	/**
	 * Traverses all superclasses in DFS order
	 * @param traverser
	 * @param traverseInterfaces
	 */
	public boolean traverseSuperclasses(Function<String, Boolean> traverser, boolean traverseInterfaces) {
		ClassNode cls = this.hierarchy.getClass(this.loader, this.name);
		if(cls == null) {
			throw new RuntimeException(String.format("Class %s was not found in class hierarchy", this.name));
		}
		return this.traverseSuperclasses(cls, traverser, traverseInterfaces);
	}

	private boolean traverseSuperclasses(ClassNode cls, Function<String, Boolean> traverser, boolean traverseInterfaces) {
		String type = cls.name;
		ClassNode info = cls;
		while (!"java/lang/Object".equals(type)) {
			if(traverser.apply(type)) {
				return true;
			}
			if(traverseInterfaces) {
				List<String> itfs = info.interfaces;
				for(String itf : itfs) {
					if(traverser.apply(itf)) {
						return true;
					}
				}
				for(String itf : itfs) {
					ClassNode itfNode = this.hierarchy.getClass(this.loader, itf);
					if(itfNode == null) {
						throw new RuntimeException(String.format("Class %s was not found in class hierarchy", itf));
					}
					if(this.traverseSuperclasses(itfNode, traverser, true)) {
						return true;
					}
				}
			}
			type = info.superName;
			if(type != null) {
				info = this.hierarchy.getClass(this.loader, type);
				if(info == null) {
					throw new RuntimeException(String.format("Class %s was not found in class hierarchy", type));
				}
			}
		}
		return false;
	}
}
