package pr0x79.instrumentation.signature;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.objectweb.asm.tree.ClassNode;

public class ClassHierarchy {
	//TODO Replace ClassNode with extends and implements structure only?
	private Map<ClassLoader, Map<String, ClassNode>> classes = new WeakHashMap<>();

	public synchronized void addClass(ClassLoader loader, ClassNode cls) {
		Map<String, ClassNode> map = this.classes.get(loader);
		if(map == null) {
			this.classes.put(loader, map = new HashMap<>());
		}
		map.put(cls.name, cls);
	}

	public synchronized ClassNode getClass(ClassLoader loader, String name) {
		do {
			Map<String, ClassNode> map = this.classes.get(loader);
			if(map != null) {
				ClassNode cls = map.get(name);
				if(cls != null) {
					return cls;
				}
			}
			loader = loader.getParent();
		} while(loader != null);
		return null;
	}
}
