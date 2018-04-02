package pr0x79.signature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;

import pr0x79.ClassLocators;

public class ClassHierarchy {
	//TODO Replace ClassNode with extends, implements and signature structure only?
	private Map<ClassLoader, Map<String, ClassData>> classes = new WeakHashMap<>();

	private Map<ClassLoader, Map<String, String>> outerClassNames = new WeakHashMap<>();

	private final ClassLocators locators;

	public static class ClassData {
		public final String name;
		public final String signature;
		public final String superclass;
		public final List<String> interfaces;
		public final String outerclass;
		public final int access;

		private ClassData(String name, String signature, String superclass, List<String> interfaces, String outerclass, int access) {
			this.name = name;
			this.signature = signature;
			this.superclass = superclass;
			this.interfaces = interfaces;
			this.outerclass = outerclass;
			this.access = access;
		}
	}

	public ClassHierarchy(ClassLocators locators) {
		this.locators = locators;
	}

	/**
	 * Adds a class to the hierarchy
	 * @param loader The class loader that loaded the class
	 * @param cls The class to add
	 */
	public synchronized void addClass(ClassLoader loader, ClassNode cls) {
		Map<String, ClassData> map = this.classes.get(loader);
		if(map == null) {
			this.classes.put(loader, map = new HashMap<>());
		}
		map.put(cls.name, new ClassData(cls.name, cls.signature, cls.superName, cls.interfaces, cls.outerClass, cls.access));

		if(!cls.innerClasses.isEmpty()) {
			Map<String, String> outerMap = this.outerClassNames.get(loader);
			if(outerMap == null) {
				this.outerClassNames.put(loader, outerMap = new HashMap<>());
			}
			for(InnerClassNode innerCls : cls.innerClasses) {
				outerMap.put(innerCls.name, innerCls.outerName);
			}
		}
	}

	/**
	 * Returns the outer class data for the specified class
	 * @param loader The class loader that loaded the specified class
	 * @param name The internal name of the inner class
	 * @param onFallback Called when a class had to be loaded from stream with parameters {@link ClassNode} and {@link ClassReader} flags
	 * @return
	 */
	public synchronized ClassData getOuterClass(ClassLoader loader, String name, BiConsumer<ClassNode, Integer> onFallback) {
		return getOuterClass(loader, name, true, onFallback);
	}

	/**
	 * Returns the class data for the specified class
	 * @param loader The class loader that loaded the specified class
	 * @param name The internal name of the class
	 * @param onFallback Called when a class had to be loaded from stream with parameters {@link ClassNode} and {@link ClassReader} flags
	 * @return
	 */
	public synchronized ClassData getClass(ClassLoader loader, String name, BiConsumer<ClassNode, Integer> onFallback) {
		return this.getClass(loader, name, true, onFallback);
	}

	/**
	 * Returns the outer class data for the specified class
	 * @param loader The class loader that loaded the specified class
	 * @param name The internal name of the class
	 * @param classFileFallback Whether the class should be read from a file if it is not found in the hierarchy
	 * @param onFallback Called when a class had to be loaded from stream with parameters {@link ClassNode} and {@link ClassReader} flags
	 * @return
	 */
	public synchronized ClassData getOuterClass(ClassLoader loader, String name, boolean classFileFallback, BiConsumer<ClassNode, Integer> onFallback) {
		ClassLoader l = loader;
		do {
			Map<String, String> map = this.outerClassNames.get(l);
			if(map != null) {
				String outerName = map.get(name);
				if(outerName != null) {
					ClassData outer = this.getClass(loader, outerName, classFileFallback, onFallback);
					if(outer != null) {
						return outer;
					}
				}
			}
			l = l.getParent();
		} while(l != null);

		ClassData data = this.getClass(loader, name, classFileFallback, onFallback);
		if(data != null && data.outerclass != null) {
			return this.getClass(loader, data.outerclass, classFileFallback, onFallback);
		}

		return null;
	}

	/**
	 * Returns the class data for the specified class
	 * @param loader The class loader that loaded the specified class
	 * @param name The internal name of the class
	 * @param classFileFallback Whether the class should be read from a file if it is not found in the hierarchy
	 * @param onFallback Called when a class had to be loaded from stream with parameters {@link ClassNode} and {@link ClassReader} flags
	 * @return
	 */
	public synchronized ClassData getClass(ClassLoader loader, String name, boolean classFileFallback, BiConsumer<ClassNode, Integer> onFallback) {
		//Try to get class from loaded hierarchy first. Speeds up lookup and
		//works for special custom class loaders unlike getResourceAsStream
		ClassLoader l = loader;
		do {
			Map<String, ClassData> map = this.classes.get(l);
			if(map != null) {
				ClassData cls = map.get(name);
				if(cls != null) {
					return cls;
				}
			}
			l = l.getParent();
		} while(l != null);

		if(classFileFallback) {
			//Class was not found, probably loaded before the class transformer was attached
			ClassNode node = this.locators.getClass(loader, name, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
			if(node != null) {
				if(onFallback != null) {
					onFallback.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
				}
				return new ClassData(node.name, node.signature, node.superName, node.interfaces, node.outerClass, node.access);
			}
		}

		return null;
	}
}
