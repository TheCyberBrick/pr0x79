package pr0x79.instrumentation.identification;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import pr0x79.Bootstrapper;

/**
 * Registry for identifiers
 */
public class Identifiers {
	private final Bootstrapper bootstrapper;
	private final Map<String, IClassIdentifier> classIdentifiers = new HashMap<>();
	private final Map<String, IFieldIdentifier> fieldIdentifiers = new HashMap<>();
	private final Map<String, IMethodIdentifier> methodIdentifiers = new HashMap<>();
	private final Map<String, IInstructionIdentifier> instructionIdentifiers = new HashMap<>();

	public Identifiers(Bootstrapper bootstrapper) {
		this.bootstrapper = bootstrapper;
	}

	/**
	 * Registers a class identifier
	 * @param id
	 * @param identifier
	 */
	public void registerClassIdentifier(String id, IClassIdentifier identifier) {
		this.checkBootstrapperState();
		if(this.classIdentifiers.containsKey(id)) {
			throw new RuntimeException(String.format("Duplicate class identifiers: %s", id));
		}
		this.classIdentifiers.put(id, identifier);
	}

	/**
	 * Gets a class identifier by ID
	 * @param id
	 * @return
	 */
	public IClassIdentifier getClassIdentifier(String id) {
		return this.classIdentifiers.get(id);
	}

	/**
	 * Returns all class identifiers
	 * @return
	 */
	public Collection<IClassIdentifier> getClassIdentifiers() {
		return Collections.unmodifiableCollection(this.classIdentifiers.values());
	}

	/**
	 * Registers a field identifier
	 * @param id
	 * @param identifier
	 */
	public void registerFieldIdentifier(String id, IFieldIdentifier identifier) {
		this.checkBootstrapperState();
		if(this.fieldIdentifiers.containsKey(id)) {
			throw new RuntimeException(String.format("Duplicate field identifiers: %s", id));
		}
		this.fieldIdentifiers.put(id, identifier);
	}

	/**
	 * Gets a field identifier by ID
	 * @param id
	 * @return
	 */
	public IFieldIdentifier getFieldIdentifier(String id) {
		return this.fieldIdentifiers.get(id);
	}

	/**
	 * Returns all field identifiers
	 * @return
	 */
	public Collection<IFieldIdentifier> getFieldIdentifiers() {
		return Collections.unmodifiableCollection(this.fieldIdentifiers.values());
	}

	/**
	 * Register a method identifier
	 * @param id
	 * @param identifier
	 */
	public void registerMethodIdentifier(String id, IMethodIdentifier identifier) {
		this.checkBootstrapperState();
		if(this.methodIdentifiers.containsKey(id)) {
			throw new RuntimeException(String.format("Duplicate method identifiers: %s", id));
		}
		this.methodIdentifiers.put(id, identifier);
	}

	/**
	 * Gets a method identifier by ID
	 * @param id
	 * @return
	 */
	public IMethodIdentifier getMethodIdentifier(String id) {
		return this.methodIdentifiers.get(id);
	}

	/**
	 * Returns all method identifiers
	 * @return
	 */
	public Collection<IMethodIdentifier> getMethodIdentifiers() {
		return Collections.unmodifiableCollection(this.methodIdentifiers.values());
	}

	/**
	 * Register an instruction identifier
	 * @param id
	 * @param identifier
	 */
	public void registerInstructionIdentifier(String id, IInstructionIdentifier identifier) {
		this.checkBootstrapperState();
		if(this.instructionIdentifiers.containsKey(id)) {
			throw new RuntimeException(String.format("Duplicate instruction identifiers: %s", id));
		}
		this.instructionIdentifiers.put(id, identifier);
	}

	/**
	 * Gets a instruction identifier by ID
	 * @param id
	 * @return
	 */
	public IInstructionIdentifier getInstructionIdentifier(String id) {
		return this.instructionIdentifiers.get(id);
	}

	/**
	 * Returns all instruction identifiers
	 * @return
	 */
	public Collection<IInstructionIdentifier> getInstructionIdentifiers() {
		return Collections.unmodifiableCollection(this.instructionIdentifiers.values());
	}

	/**
	 * Validates the boostrapper state and throws an exception if the bootstrapper is no longer initializing
	 */
	private void checkBootstrapperState() {
		if(!this.bootstrapper.isInitializing()) {
			throw new RuntimeException("Identifiers must be registered during the bootstrap initialization");
		}
	}
}
