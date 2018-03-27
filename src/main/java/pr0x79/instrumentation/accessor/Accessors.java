package pr0x79.instrumentation.accessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import pr0x79.Bootstrapper;
import pr0x79.instrumentation.BytecodeInstrumentation;
import pr0x79.instrumentation.exception.InstrumentorException;
import pr0x79.instrumentation.identification.IClassIdentifier;
import pr0x79.instrumentation.identification.Identifiers;

/**
 * Registry for accessors
 */
public final class Accessors {
	private final Bootstrapper bootstrapper;
	private final Identifiers identifiers;
	private final BytecodeInstrumentation instrumentor;
	private final Map<String, List<ClassAccessorData>> accessorsById = new ConcurrentHashMap<>();
	private final Map<String, ClassAccessorData> accessorsByClassName = new ConcurrentHashMap<>();

	public Accessors(Bootstrapper bootstrapper, Identifiers identifiers, BytecodeInstrumentation instrumentor) {
		this.bootstrapper = bootstrapper;
		this.identifiers = identifiers;
		this.instrumentor = instrumentor;
	}

	/**
	 * Registers an accessor. The accessor class must not be loaded before or during
	 * the Bootstrapper initialization
	 * @param className
	 */
	public void registerAccessor(String className) {
		if(!this.bootstrapper.isInitializing()) {
			throw new InstrumentorException(String.format("Accessor %s must be registered during the bootstrap initialization", className));
		}

		ClassNode clsNode = null;
		try {
			ClassReader clsReader = new ClassReader(className);
			clsNode = new ClassNode();
			clsReader.accept(clsNode, ClassReader.SKIP_FRAMES);
		} catch (IOException e) {
			throw new InstrumentorException(String.format("Could not load accessor class %s", className));
		}

		if((clsNode.access & Opcodes.ACC_INTERFACE) == 0) {
			throw new InstrumentorException(String.format("Accessor %s is not an interface", className));
		}

		String classIdentifierId = BytecodeInstrumentation.getAnnotationValue(clsNode.visibleAnnotations, ClassAccessor.class, BytecodeInstrumentation.getInternalMethod(ClassAccessor.class, "class_identifier").getName(), String.class, null);
		if(classIdentifierId == null) {
			throw new InstrumentorException(String.format("Accessor %s does not have a class accessor annotation", className));
		}

		IClassIdentifier clsIdentifier = null;
		if(classIdentifierId != null) {
			clsIdentifier = this.identifiers.getClassIdentifier(classIdentifierId);
		}
		if(clsIdentifier == null) {
			throw new InstrumentorException(String.format("Class identifier %s:%s is not registered", className, classIdentifierId));
		}

		ClassAccessorData accessorData = new ClassAccessorData(classIdentifierId, this.identifiers, className, clsNode, clsIdentifier, this.instrumentor);
		List<ClassAccessorData> accessors = this.accessorsById.get(classIdentifierId);
		if(accessors == null) {
			this.accessorsById.put(classIdentifierId, accessors = new ArrayList<>());
		}
		accessors.add(accessorData);
		this.accessorsByClassName.put(className, accessorData);
	}

	/**
	 * Gets an accessor by class name
	 * @param name
	 * @return
	 */
	public ClassAccessorData getAccessorByClassName(String name) {
		return this.accessorsByClassName.get(name);
	}

	/**
	 * Gets a list of accessors with the specified id, can be null if no accessors are found
	 * @param name
	 * @return
	 */
	public List<ClassAccessorData> getAccessorsById(String name) {
		return this.accessorsById.get(name);
	}

	/**
	 * Returns all class accessors
	 * @return
	 */
	public Collection<ClassAccessorData> getClassAccessors() {
		return Collections.unmodifiableCollection(this.accessorsByClassName.values());
	}
}
