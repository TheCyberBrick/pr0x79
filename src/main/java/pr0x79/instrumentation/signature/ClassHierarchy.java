package pr0x79.instrumentation.signature;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;

public class ClassHierarchy {
	//TODO Replace ClassNode with extends, implements and signature structure only?
	private Map<ClassLoader, Map<String, ClassData>> classes = new WeakHashMap<>();

	private Map<ClassLoader, Map<String, String>> outerClassNames = new WeakHashMap<>();

	public static class ClassData {
		public final String name;
		public final String signature;
		public final String superclass;
		public final List<String> interfaces;
		public final String outerclass;

		private ClassData(String name, String signature, String superclass, List<String> interfaces, String outerclass) {
			this.name = name;
			this.signature = signature;
			this.superclass = superclass;
			this.interfaces = interfaces;
			this.outerclass = outerclass;
		}
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
		map.put(cls.name, new ClassData(cls.name, cls.signature, cls.superName, cls.interfaces, cls.outerClass));

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
	 * @return
	 */
	public synchronized ClassData getOuterClass(ClassLoader loader, String name) {
		return getOuterClass(loader, name, true);
	}

	/**
	 * Returns the class data for the specified class
	 * @param loader The class loader that loaded the specified class
	 * @param name The internal name of the class
	 * @return
	 */
	public synchronized ClassData getClass(ClassLoader loader, String name) {
		return this.getClass(loader, name, true);
	}

	/**
	 * Returns the outer class data for the specified class
	 * @param loader The class loader that loaded the specified class
	 * @param name The internal name of the class
	 * @param classFileFallback Whether the class should be read from a file if it is not found in the hierarchy
	 * @return
	 */
	public synchronized ClassData getOuterClass(ClassLoader loader, String name, boolean classFileFallback) {
		ClassLoader l = loader;
		do {
			Map<String, String> map = this.outerClassNames.get(l);
			if(map != null) {
				String outerName = map.get(name);
				if(outerName != null) {
					ClassData outer = this.getClass(loader, outerName, classFileFallback);
					if(outer != null) {
						return outer;
					}
				}
			}
			l = l.getParent();
		} while(l != null);

		ClassData data = this.getClass(loader, name, classFileFallback);
		if(data != null && data.outerclass != null) {
			return this.getClass(loader, data.outerclass, classFileFallback);
		}

		return null;
	}

	/**
	 * Returns the class data for the specified class
	 * @param loader The class loader that loaded the specified class
	 * @param name The internal name of the class
	 * @param classFileFallback Whether the class should be read from a file if it is not found in the hierarchy
	 * @return
	 */
	public synchronized ClassData getClass(ClassLoader loader, String name, boolean classFileFallback) {
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
			InputStream is = loader.getResourceAsStream(name + ".class");
			try {
				ClassReader reader = new ClassReader(is);
				ClassNode cls = new ClassNode();
				reader.accept(cls, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
				//TODO Cache? But which class loader?
				return new ClassData(cls.name, cls.signature, cls.superName, cls.interfaces, cls.outerClass);
			} catch (IOException e) { } finally {
				try {
					is.close();
				} catch (IOException e) { }
			}
		}

		return null;
	}
}
