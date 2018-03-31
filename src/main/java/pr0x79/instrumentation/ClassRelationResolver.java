package pr0x79.instrumentation;

import java.util.List;
import java.util.function.Function;

import pr0x79.instrumentation.signature.ClassHierarchy;
import pr0x79.instrumentation.signature.ClassHierarchy.ClassData;

public class ClassRelationResolver {
	private final ClassLoader loader;
	private final ClassHierarchy hierarchy;

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
	 * @param traverser The traverse that traverses the superclasses. The traverse method immediately returns true when the traverser returns true.
	 * @param traverseInterfaces Whether interfaces should be traversed too
	 */
	public boolean traverseHierarchy(String clsName, Function<String, Boolean> traverser, boolean traverseInterfaces) {
		ClassData cls = this.hierarchy.getClass(this.loader, clsName);
		if(cls == null) {
			throw new RuntimeException(String.format("Class %s was not found in class hierarchy", clsName));
		}
		return this.traverseHierarchy(cls, traverser, traverseInterfaces);
	}

	private boolean traverseHierarchy(ClassData cls, Function<String, Boolean> traverser, boolean traverseInterfaces) {
		String type = cls.name;
		ClassData info = cls;
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
					ClassData itfNode = this.hierarchy.getClass(this.loader, itf);
					if(itfNode == null) {
						throw new RuntimeException(String.format("Class %s was not found in class hierarchy", itf));
					}
					if(this.traverseHierarchy(itfNode, traverser, true)) {
						return true;
					}
				}
			}
			type = info.superclass;
			if(type != null) {
				info = this.hierarchy.getClass(this.loader, type);
				if(info == null) {
					throw new RuntimeException(String.format("Class %s was not found in class hierarchy", type));
				}
			}
		}
		return traverser.apply("java/lang/Object"); //Visit Object at the end
	}
}
