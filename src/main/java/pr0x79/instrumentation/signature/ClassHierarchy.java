package pr0x79.instrumentation.signature;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class ClassHierarchy {
	//TODO Replace ClassNode with extends, implements and signature structure only?
	private Map<ClassLoader, Map<String, ClassNode>> classes = new WeakHashMap<>();

	public synchronized void addClass(ClassLoader loader, ClassNode cls) {
		Map<String, ClassNode> map = this.classes.get(loader);
		if(map == null) {
			this.classes.put(loader, map = new HashMap<>());
		}
		map.put(cls.name, cls);
	}
	
	public synchronized ClassNode getClass(ClassLoader loader, String name) {
		return this.getClass(loader, name, true);
	}
	
	public synchronized ClassNode getClass(ClassLoader loader, String name, boolean classFileFallback) {
		//Try to get class from loaded hierarchy first. Speeds up lookup and
		//works for special custom class loaders unlike getResourceAsStream
		ClassLoader l = loader;
		do {
			Map<String, ClassNode> map = this.classes.get(l);
			if(map != null) {
				ClassNode cls = map.get(name);
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
				reader.accept(cls, 0);
				//TODO Cache? But which class loader?
				return cls;
			} catch (IOException e) { } finally {
				try {
					is.close();
				} catch (IOException e) { }
			}
		}

		return null;
	}
}
