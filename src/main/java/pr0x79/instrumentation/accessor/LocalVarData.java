package pr0x79.instrumentation.accessor;

import pr0x79.instrumentation.exception.InstrumentorException;
import pr0x79.instrumentation.identification.IInstructionIdentifier;
import pr0x79.instrumentation.identification.IInstructionIdentifier.InstructionType;
import pr0x79.instrumentation.identification.Identifiers;

public class LocalVarData {
	private final String interceptorMethod, interceptorMethodDesc;
	private final int parameterIndex;
	private final String instructionIdentifierId, accessorClass;
	private IInstructionIdentifier instructionIdentifier;

	public LocalVarData(String interceptorMethod, String interceptorMethodDesc, int parameterIndex, String accessorClass, String instructionIdentifierId) {
		this.interceptorMethod = interceptorMethod;
		this.interceptorMethodDesc = interceptorMethodDesc;
		this.parameterIndex = parameterIndex;
		this.instructionIdentifierId = instructionIdentifierId;
		this.accessorClass = accessorClass;
	}

	/**
	 * Returns the index of the parameter of the interception method
	 * @return
	 */
	public int getParameterIndex() {
		return this.parameterIndex;
	}

	/**
	 * Returns the local variable instruction identifier ID of the local variable to be imported
	 * @return
	 */
	public String getInstructionIdentifierId() {
		return this.instructionIdentifierId;
	}

	/**
	 * Returns the local variable instruction identifier of the local variable to be imported
	 * @return
	 */
	public IInstructionIdentifier getInstructionIdentifier() {
		return this.instructionIdentifier;
	}

	/**
	 * Returns the interceptor method name
	 * @return
	 */
	public String getInterceptorMethod() {
		return this.interceptorMethod;
	}

	/**
	 * Returns the interceptor method descriptor
	 * @return
	 */
	public String getInterceptorMethodDesc() {
		return this.interceptorMethodDesc;
	}

	/**
	 * Initializes the identifiers
	 * @param identifiers
	 */
	public void initIdentifiers(Identifiers identifiers) {
		this.instructionIdentifier = identifiers.getInstructionIdentifier(this.instructionIdentifierId);
		if(this.instructionIdentifier == null) {
			throw new InstrumentorException(String.format("Instruction identifier %s for importer %s#%s is not registered", this.instructionIdentifierId, this.accessorClass, this.interceptorMethod + this.interceptorMethodDesc));
		}
		if(this.instructionIdentifier.getType() != InstructionType.LOCAL_VARIABLE) {
			throw new InstrumentorException(String.format("Instruction identifier %s for importer %s#%s is not of type LOCAL_VARIABLE", this.instructionIdentifierId, this.accessorClass, this.interceptorMethod + this.interceptorMethodDesc));
		}
	}
}
