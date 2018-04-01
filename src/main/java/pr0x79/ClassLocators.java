package pr0x79;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class ClassLocators {
	private final Bootstrapper bootstrapper;

	private final Map<String, IClassLocator> classLocators = new TreeMap<>();

	ClassLocators(Bootstrapper bootstrapper) {
		this.bootstrapper = bootstrapper;

		this.registerClassLocator("default", new IClassLocator() {
			@Override
			public ClassNode locate(ClassLoader loader, String internalClassName, int flags) throws IOException {
				//TODO Implement caching for JRE classes?

				try(InputStream stream = loader.getResourceAsStream(internalClassName + ".class")) {
					ClassNode cls = new ClassNode();
					new ClassReader(stream).accept(cls, flags);
					return cls;
				}
			}
		});
	}

	/**
	 * Registers a class locator that maps an internal class name
	 * to a {@link ClassNode}.
	 * @param id The ID of the locator
	 * @param locator The locator that maps an internal class name to a {@link ClassNode}
	 */
	public synchronized void registerClassLocator(String id, IClassLocator locator) {
		this.checkBootstrapperState();
		this.classLocators.put(id, locator);
	}

	/**
	 * Unregisters a class locator
	 * @param id The ID of the locator
	 * @return
	 */
	public synchronized IClassLocator unregisterClassLocator(String id) {
		this.checkBootstrapperState();
		return this.classLocators.remove(id);
	}

	/**
	 * Returns all registered class locator IDs
	 * @return
	 */
	public synchronized Set<String> getClassLocators() {
		return new HashSet<>(this.classLocators.keySet());
	}

	/**
	 * Returns a {@link ClassNode} of the specified class
	 * @param loader The classloader
	 * @param internalClassName The internal name of the class
	 * @param flags The {@link ClassReader} flags
	 * @return
	 */
	public synchronized ClassNode getClass(ClassLoader loader, String internalClassName, int flags) {
		for(IClassLocator locator : this.classLocators.values()) {
			try {
				ClassNode node = locator.locate(loader, internalClassName, flags);
				if(node != null) {
					return node;
				}
			} catch(IOException ex) {}
		}
		return null;
	}

	/**
	 * Validates the boostrapper state and throws an exception if the bootstrapper is no longer initializing
	 */
	private void checkBootstrapperState() {
		if(!this.bootstrapper.isInitializing()) {
			throw new RuntimeException("Class locators must be (un-)registered during the bootstrap initialization");
		}
	}
}
