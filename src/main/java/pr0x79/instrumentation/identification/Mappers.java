package pr0x79.instrumentation.identification;

import java.util.Map;
import java.util.TreeMap;

import pr0x79.Bootstrapper;

/**
 * Registry for mappers
 */
public class Mappers {
	public static enum ClassSearchType {
		/**
		 * When a class accessor target is being searched
		 */
		ACCESSOR,

		OTHER;
	}

	public static enum FieldSearchType {
		/**
		 * When a field accessor target is being searched
		 */
		ACCESSOR,

		/**
		 * When a field name generator is being searched
		 */
		NAME_GENERATOR,

		OTHER;
	}

	public static enum MethodSearchType {
		/**
		 * When a method accessor target is being searched
		 */
		ACCESSOR,

		/**
		 * When a method interceptor target is being searched
		 */
		INTERCEPTOR,

		OTHER;
	}

	public static enum InstructionSearchType {
		/**
		 * When a local variable target is being searched
		 */
		LOCAL_VARIABLE,

		/**
		 * When a method interceptor entry instruction is being searched
		 */
		INTERCEPTOR_ENTRY,

		/**
		 * When a method interceptor exit instruction is being searched
		 */
		INTERCEPTOR_EXIT,

		OTHER;
	}

	@FunctionalInterface
	public static interface IClassMapper {
		/**
		 * Maps a string identifier to a {@link IClassIdentifier}.
		 * @param identifier
		 * @param search
		 * @return
		 */
		public IClassIdentifier map(String identifier, ClassSearchType search);
	}

	@FunctionalInterface
	public static interface IFieldMapper {
		/**
		 * Maps a string identifier to a {@link IFieldIdentifier}.
		 * @param identifier
		 * @param search
		 * @return
		 */
		public IFieldIdentifier map(String identifier, FieldSearchType search);
	}

	@FunctionalInterface
	public static interface IMethodMapper {
		/**
		 * Maps a string identifier to a {@link IMethodIdentifier}.
		 * @param identifier
		 * @param search
		 * @return
		 */
		public IMethodIdentifier map(String identifier, MethodSearchType search);
	}

	@FunctionalInterface
	public static interface IInstructionMapper {
		/**
		 * Maps a string identifier to a {@link IInstructionIdentifier}.
		 * @param identifier
		 * @param search
		 * @return
		 */
		public IInstructionIdentifier map(String identifier, InstructionSearchType search);
	}

	private final Bootstrapper bootstrapper;

	private final Map<String, IClassMapper> classIdentifierMappers = new TreeMap<>();
	private final Map<String, IFieldMapper> fieldIdentifierMappers = new TreeMap<>();
	private final Map<String, IMethodMapper> methodIdentifierMappers = new TreeMap<>();
	private final Map<String, IInstructionMapper> instructionIdentifierMappers = new TreeMap<>();

	public Mappers(Bootstrapper bootstrapper) {
		this.bootstrapper = bootstrapper;
	}

	/**
	 * Registers a class mapper that maps a string
	 * to a {@link IClassIdentifier}.
	 * @param id The ID of the mapper
	 * @param mapper The mapper that maps a string to a {@link IClassIdentifier}
	 */
	public synchronized void registerClassMapper(String id, IClassMapper mapper) {
		this.checkBootstrapperState();
		this.classIdentifierMappers.put(id, mapper);
	}

	/**
	 * Returns the class identifier for the specified identifier string.
	 * @param identifier The identifier string
	 * @param search What is being searched
	 * @return
	 */
	public synchronized IClassIdentifier getClassIdentifier(String identifier, ClassSearchType search) {
		for(IClassMapper mapper : this.classIdentifierMappers.values()) {
			IClassIdentifier i = mapper.map(identifier, search);
			if(i != null) {
				return i;
			}
		}
		return null;
	}

	/**
	 * Registers a field mapper that maps a string
	 * to a {@link IFieldIdentifier}.
	 * @param id The ID of the mapper
	 * @param mapper The mapper that maps a string to a {@link IFieldIdentifier}
	 */
	public synchronized void registerFieldMapper(String id, IFieldMapper mapper) {
		this.checkBootstrapperState();
		this.fieldIdentifierMappers.put(id, mapper);
	}

	/**
	 * Returns the field identifier for the specified identifier string.
	 * @param identifier The identifier string
	 * @param search What is being searched
	 * @return
	 */
	public synchronized IFieldIdentifier getFieldIdentifier(String identifier, FieldSearchType search) {
		for(IFieldMapper mapper : this.fieldIdentifierMappers.values()) {
			IFieldIdentifier i = mapper.map(identifier, search);
			if(i != null) {
				return i;
			}
		}
		return null;
	}

	/**
	 * Registers a method mapper that maps a string
	 * to a {@link IMethodIdentifier}.
	 * @param id The ID of the mapper
	 * @param mapper The mapper that maps a string to a {@link IMethodIdentifier}
	 */
	public synchronized void registerMethodMapper(String id, IMethodMapper mapper) {
		this.checkBootstrapperState();
		this.methodIdentifierMappers.put(id, mapper);
	}

	/**
	 * Returns the method identifier for the specified identifier string.
	 * @param identifier The identifier string
	 * @param search What is being searched
	 * @return
	 */
	public synchronized IMethodIdentifier getMethodIdentifier(String identifier, MethodSearchType search) {
		for(IMethodMapper mapper : this.methodIdentifierMappers.values()) {
			IMethodIdentifier i = mapper.map(identifier, search);
			if(i != null) {
				return i;
			}
		}
		return null;
	}

	/**
	 * Registers an instruction mapper that maps a string
	 * to a {@link IInstructionIdentifier}.
	 * @param id The ID of the mapper
	 * @param mapper The mapper that maps a string to a {@link IInstructionIdentifier}
	 */
	public synchronized void registerInstructionMapper(String id, IInstructionMapper mapper) {
		this.checkBootstrapperState();
		this.instructionIdentifierMappers.put(id, mapper);
	}

	/**
	 * Returns the instruction identifier for the specified identifier string.
	 * @param identifier The identifier string
	 * @param search What is being searched
	 * @return
	 */
	public synchronized IInstructionIdentifier getInstructionIdentifier(String identifier, InstructionSearchType search) {
		for(IInstructionMapper mapper : this.instructionIdentifierMappers.values()) {
			IInstructionIdentifier i = mapper.map(identifier, search);
			if(i != null) {
				return i;
			}
		}
		return null;
	}

	/**
	 * Validates the boostrapper state and throws an exception if the bootstrapper is no longer initializing
	 */
	private void checkBootstrapperState() {
		if(!this.bootstrapper.isInitializing()) {
			throw new RuntimeException("Mappers must be registered during the bootstrap initialization");
		}
	}
}
