package pr0x79.instrumentation.accessor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import pr0x79.Bootstrapper;
import pr0x79.instrumentation.BytecodeInstrumentation;
import pr0x79.instrumentation.exception.InstrumentorException;
import pr0x79.instrumentation.identification.IClassIdentifier;
import pr0x79.instrumentation.identification.Identifiers;

/**
 * Registry for accessors
 */
public class Accessors {
	private final Bootstrapper bootstrapper;
	private final Identifiers identifiers;
	private final BytecodeInstrumentation instrumentor;
	private final Map<String, ClassAccessorData> accessorsById = new HashMap<>();
	private final Map<String, ClassAccessorData> accessorsByClassName = new HashMap<>();

	public Accessors(Bootstrapper bootstrapper, Identifiers identifiers, BytecodeInstrumentation instrumentor) {
		this.bootstrapper = bootstrapper;
		this.identifiers = identifiers;
		this.instrumentor = instrumentor;
	}

	/**
	 * Registers an accessor
	 * @param accessor
	 */
	public void registerAccessor(Class<? extends IAccessor> accessor) {
		if(!this.bootstrapper.isInitializing()) {
			throw new RuntimeException("Accessors must be registered during the bootstrap initialization");
		}
		if(!accessor.isInterface()) {
			throw new InstrumentorException(String.format("Accessor %s is not an interface", accessor.getName()));
		}
		ClassAccessor clsAccessor = accessor.getAnnotation(ClassAccessor.class);
		if(clsAccessor == null) {
			throw new InstrumentorException(String.format("Accessor %s does not have a class accessor", accessor.getName()));
		}
		IClassIdentifier clsIdentifier = this.identifiers.getClassIdentifier(clsAccessor.classIdentifierId());
		if(clsIdentifier == null) {
			throw new InstrumentorException(String.format("Class identifier %s:%s is not registered", accessor.getName(), clsAccessor.classIdentifierId()));
		}
		ClassAccessorData accessorData = new ClassAccessorData(clsAccessor.classIdentifierId(), this.identifiers, accessor, clsIdentifier, this.instrumentor);
		this.accessorsById.put(clsAccessor.classIdentifierId(), accessorData);
		this.accessorsByClassName.put(accessor.getName(), accessorData);
	}

	/**
	 * Gets an accessor by ID (classIdentifierId)
	 * @param id
	 * @return
	 */
	public ClassAccessorData getAccessorById(String id) {
		return this.accessorsById.get(id);
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
	 * Returns all class accessors
	 * @return
	 */
	public Collection<ClassAccessorData> getClassAccessors() {
		return Collections.unmodifiableCollection(this.accessorsById.values());
	}
}
