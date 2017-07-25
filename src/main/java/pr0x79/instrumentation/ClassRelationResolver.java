package pr0x79.instrumentation;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import org.objectweb.asm.ClassReader;

public class ClassRelationResolver {
	private final ClassLoader loader;
	private final ClassReader cls;

	/**
	 * Creates a new class relation resolver
	 * @param cls The internal name of the class
	 * @throws IOException
	 */
	public ClassRelationResolver(String cls) throws IOException {
		this.loader = ClassRelationResolver.class.getClassLoader();
		this.cls = this.readClass(cls);
	}

	/**
	 * Reads a class into a {@link ClassReader}
	 * @param cls The internal name of the class
	 * @return
	 * @throws IOException
	 */
	private ClassReader readClass(String cls) throws IOException {
		InputStream is = this.loader.getResourceAsStream(cls + ".class");
		try {
			return new ClassReader(is);
		} finally {
			is.close();
		}
	}

	/**
	 * Traverses all superclasses in BFS order
	 * @param traverser
	 * @param traverseInterfaces
	 * @throws IOException 
	 */
	public boolean traverseSuperclasses(Function<String, Boolean> traverser, boolean traverseInterfaces) throws IOException {
		return this.traverseSuperclasses(this.cls, traverser, traverseInterfaces);
	}

	private boolean traverseSuperclasses(ClassReader cls, Function<String, Boolean> traverser, boolean traverseInterfaces) throws IOException {
		String type = cls.getClassName();
		ClassReader info = cls;
		while (!"java/lang/Object".equals(type)) {
			if(traverser.apply(type)) return true;
			if(traverseInterfaces) {
				String[] itfs = info.getInterfaces();
				for(String itf : itfs) {
					if(traverser.apply(itf)) return true;
				}
				for(String itf : itfs) {
					if(this.traverseSuperclasses(this.readClass(itf), traverser, true)) return true;
				}
			}
			type = info.getSuperName();
			info = this.readClass(type);
		}
		return false;
	}
}
