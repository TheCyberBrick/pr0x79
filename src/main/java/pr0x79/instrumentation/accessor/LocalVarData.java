package pr0x79.instrumentation.accessor;

import pr0x79.instrumentation.exception.InstrumentorException;
import pr0x79.instrumentation.identification.IInstructionIdentifier;
import pr0x79.instrumentation.identification.IInstructionIdentifier.InstructionType;
import pr0x79.instrumentation.identification.Mappers.InstructionSearchType;
import pr0x79.instrumentation.identification.Mappers;

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
	 * Initializes the identifier
	 * @param mappers
	 */
	public void initIdentifier(Mappers mappers) {
		this.instructionIdentifier = mappers.getInstructionIdentifier(this.instructionIdentifierId, InstructionSearchType.LOCAL_VARIABLE);
		if(this.instructionIdentifier == null) {
			throw new InstrumentorException(String.format("Instruction identifier %s#%s[%s] is not mapped", this.accessorClass, this.interceptorMethod + this.interceptorMethodDesc, this.instructionIdentifierId));
		}
		if(this.instructionIdentifier.getType() != InstructionType.LOCAL_VARIABLE) {
			throw new InstrumentorException(String.format("Instruction identifier %s#%s[%s] is not of type LOCAL_VARIABLE", this.accessorClass, this.interceptorMethod + this.interceptorMethodDesc, this.instructionIdentifierId));
		}
	}
}
