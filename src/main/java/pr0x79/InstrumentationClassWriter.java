package pr0x79;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import pr0x79.signature.ClassHierarchy;
import pr0x79.signature.ClassHierarchy.ClassData;

public class InstrumentationClassWriter extends ClassWriter {
	private final ClassHierarchy hierarchy;
	private final ClassRelationResolver resolver;
	private final ClassLoader loader;

	/**
	 * Creates a {@link ClassWriter} that does not load any classes.
	 * @param hierarchy The class hierarchy
	 * @param loader The classloader that is loading the class that is being writtern
	 * @param flags {@link ClassWriter} flags
	 */
	public InstrumentationClassWriter(ClassHierarchy hierarchy, ClassLoader loader, int flags) {
		super(flags);
		this.hierarchy = hierarchy;
		this.loader = loader;
		this.resolver = new ClassRelationResolver(hierarchy, loader);
	}

	@Override
	protected String getCommonSuperClass(String type1, String type2) {
		ClassData cls1 = this.hierarchy.getClass(this.loader, type1, null);
		ClassData cls2 = this.hierarchy.getClass(this.loader, type2, null);
		if((cls1.access & Opcodes.ACC_INTERFACE) != 0) {
			if(this.checkImplements(cls2, cls1.name)) {
				return cls2.name;
			} else {
				return "java/lang/Object";
			}
		}
		if((cls2.access & Opcodes.ACC_INTERFACE) != 0) {
			if(this.checkImplements(cls1, cls2.name)) {
				return cls1.name;
			} else {
				return "java/lang/Object";
			}
		}
		List<String> superclasses1 = this.getSuperclasses(cls1);
		List<String> superclasses2 = this.getSuperclasses(cls2);
		String commonSuperclass = "java/lang/Object";
		for(int i = Math.min(superclasses1.size(), superclasses2.size()); i > 1; i--) {
			String supercls1 = superclasses1.get(i);
			String supercls2 = superclasses2.get(i);
			if(!supercls1.equals(supercls2)) {
				break;
			}
			commonSuperclass = supercls1;
		}
		return commonSuperclass;
	}

	private List<String> getSuperclasses(ClassData cls) {
		List<String> classes = new ArrayList<>();
		this.resolver.traverseHierarchy(cls.name, (supercls, itf, clsNode, flags) -> classes.add(supercls), false);
		return classes;
	}

	private boolean checkImplements(ClassData cls, String itf) {
		for(String clsItf : cls.interfaces) {
			if(itf.equals(clsItf)) {
				return true;
			}
		}
		if(cls.superclass != null) {
			return this.resolver.traverseHierarchy(cls.superclass, (clsItf, isItf, clsNode, flags) -> isItf && itf.equals(clsItf), true);
		}
		return false;
	}
}
