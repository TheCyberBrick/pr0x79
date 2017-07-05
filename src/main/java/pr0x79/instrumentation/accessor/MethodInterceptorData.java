package pr0x79.instrumentation.accessor;

import java.util.Collections;
import java.util.List;

import pr0x79.instrumentation.exception.InstrumentorException;
import pr0x79.instrumentation.identification.IClassIdentifier;
import pr0x79.instrumentation.identification.IInstructionIdentifier;
import pr0x79.instrumentation.identification.IInstructionIdentifier.InstructionType;
import pr0x79.instrumentation.identification.IMethodIdentifier;
import pr0x79.instrumentation.identification.Identifiers;

public class MethodInterceptorData {
	private final List<LocalVarData> localVars;
	private final String accessorClass, classIdentifierId, methodIdentifierId, instructionIdentifierId, jumpInstructionIdentifierId;
	private final String interceptorMethod, interceptorMethodDesc;
	private final boolean isReturn;
	private IMethodIdentifier methodIdentifier;
	private IInstructionIdentifier instructionIdentifier;
	private IInstructionIdentifier jumpInstructionIdentifier;
	private IClassIdentifier classIdentifier;

	public MethodInterceptorData(String classIdentifierId, String methodIdentifierId, String instructionIdentifierId, String jumpInstructionIdentifierId, String accessorClass, String interceptorMethod, String interceptorMethodDesc, List<LocalVarData> importers, boolean isReturn) {
		this.classIdentifierId = classIdentifierId;
		this.methodIdentifierId = methodIdentifierId;
		this.accessorClass = accessorClass;
		this.instructionIdentifierId = instructionIdentifierId;
		this.jumpInstructionIdentifierId = jumpInstructionIdentifierId;
		this.interceptorMethod = interceptorMethod;
		this.interceptorMethodDesc = interceptorMethodDesc;
		this.localVars = importers;
		this.isReturn = isReturn;
	}

	/**
	 * If true causes the intercepted method to exit after the interceptor has been called
	 * @return
	 */
	public boolean isReturn() {
		return this.isReturn;
	}

	/**
	 * Returns the list of local variables that are imported and exported
	 * @return
	 */
	public List<LocalVarData> getLocalVars() {
		return Collections.unmodifiableList(this.localVars);
	}

	/**
	 * Returns the name of the {@link IAccessor} class
	 * @return
	 */
	public String getAccessorClass() {
		return this.accessorClass;
	}

	/**
	 * Returns the class identifier ID for the class with the method to be intercepted
	 * @return
	 */
	public String getClassIdentifierId() {
		return this.classIdentifierId;
	}

	/**
	 * Returns the class identifier for the class with the method to be intercepted
	 * @return
	 */
	public IClassIdentifier getClassIdentifier() {
		return this.classIdentifier;
	}

	/**
	 * Returns the method identifier ID for the method to be intercepted
	 * @return
	 */
	public String getMethodIdentifierId() {
		return this.methodIdentifierId;
	}

	/**
	 * Returns the method identifier for the method to be intercepted
	 * @return
	 */
	public IMethodIdentifier getMethodIdentifier() {
		return this.methodIdentifier;
	}

	/**
	 * Returns the instruction identifier ID where the interception is inserted
	 * @return
	 */
	public String getInstructionIdentifierId() {
		return this.instructionIdentifierId;
	}

	/**
	 * Returns the instruction identifier where the interception is inserted
	 * @return
	 */
	public IInstructionIdentifier getInstructionIdentifier() {
		return this.instructionIdentifier;
	}

	/**
	 * Returns the jump instruction identifier ID
	 * @return
	 */
	public String getJumpInstructionIdentifierId() {
		return this.jumpInstructionIdentifierId;
	}

	/**
	 * Returns the instruction identifier where the interception is inserted
	 * @return
	 */
	public IInstructionIdentifier getJumpInstructionIdentifier() {
		return this.jumpInstructionIdentifier;
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
		this.classIdentifier = identifiers.getClassIdentifier(this.classIdentifierId);
		if(this.classIdentifier == null) {
			throw new InstrumentorException(String.format("Class identifier %s:%s is not registered", this.accessorClass, this.classIdentifierId));
		}
		this.methodIdentifier = identifiers.getMethodIdentifier(this.methodIdentifierId);
		if(this.methodIdentifier == null) {
			throw new InstrumentorException(String.format("Method identifier %s for interceptor %s#%s is not registered", this.methodIdentifierId, this.accessorClass, this.interceptorMethod + this.interceptorMethodDesc));
		}
		this.instructionIdentifier = identifiers.getInstructionIdentifier(this.instructionIdentifierId);
		if(this.instructionIdentifier == null) {
			throw new InstrumentorException(String.format("Instruction identifier %s for interceptor %s#%s is not registered", this.instructionIdentifierId, this.accessorClass, this.interceptorMethod + this.interceptorMethodDesc));
		}
		if(this.instructionIdentifier.getType() != InstructionType.INSTRUCTION) {
			throw new InstrumentorException(String.format("Instruction identifier %s for interceptor %s#%s is not of type INSTRUCTION", this.instructionIdentifierId, this.accessorClass, this.interceptorMethod + this.interceptorMethodDesc));
		}
		if(this.jumpInstructionIdentifierId != null) {
			this.jumpInstructionIdentifier = identifiers.getInstructionIdentifier(this.jumpInstructionIdentifierId);
			if(this.jumpInstructionIdentifier == null) {
				throw new InstrumentorException(String.format("Jump instruction identifier %s for interceptor %s#%s is not registered", this.jumpInstructionIdentifierId, this.accessorClass, this.interceptorMethod + this.interceptorMethodDesc));
			}
			if(this.jumpInstructionIdentifier.getType() != InstructionType.INSTRUCTION) {
				throw new InstrumentorException(String.format("Jump instruction identifier %s for interceptor %s#%s is not of type INSTRUCTION", this.jumpInstructionIdentifierId, this.accessorClass, this.interceptorMethod + this.interceptorMethodDesc));
			}
		}
		for(LocalVarData localVar : this.localVars) {
			localVar.initIdentifiers(identifiers);
		}
	}
}
