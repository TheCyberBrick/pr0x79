package pr0x79;

import java.util.Collections;
import java.util.List;

import pr0x79.Mappers.ClassSearchType;
import pr0x79.Mappers.InstructionSearchType;
import pr0x79.Mappers.MethodSearchType;
import pr0x79.accessor.IAccessor;
import pr0x79.accessor.IInterceptorContext;
import pr0x79.exception.InstrumentorException;
import pr0x79.identification.IClassIdentifier;
import pr0x79.identification.IInstructionIdentifier;
import pr0x79.identification.IInstructionIdentifier.InstructionType;
import pr0x79.identification.IMethodIdentifier;
import pr0x79.identification.IMethodIdentifier.MethodDescription;
import pr0x79.signature.SignatureParser.TypeClassSymbol;

public final class MethodInterceptorData {
	private final List<LocalVarData> localVars;
	private final String accessorClass, classIdentifierId, methodIdentifierId, instructionIdentifierId;
	private final String[] exitInstructionIdentifierIds;
	private final String interceptorMethod, interceptorMethodDesc, interceptorMethodSig;
	private IMethodIdentifier methodIdentifier;
	private IInstructionIdentifier instructionIdentifier;
	private IInstructionIdentifier[] exitInstructionIdentifiers;
	private IClassIdentifier classIdentifier;
	private final int contextParam;
	private final TypeClassSymbol contextSig;
	private final boolean checkReturnTypeSignature;

	private String identifiedClass;
	private MethodDescription identifiedMethod;

	MethodInterceptorData(String classIdentifierId, String methodIdentifierId, String instructionIdentifierId, String[] exitInstructionIdentifierIds, 
			String accessorClass, String interceptorMethod, String interceptorMethodDesc, String interceptorMethodSig, List<LocalVarData> localVars, int contextParam,
			TypeClassSymbol contextSig, boolean checkReturnTypeSignature) {
		this.classIdentifierId = classIdentifierId;
		this.methodIdentifierId = methodIdentifierId;
		this.accessorClass = accessorClass;
		this.instructionIdentifierId = instructionIdentifierId;
		this.exitInstructionIdentifierIds = exitInstructionIdentifierIds;
		this.interceptorMethod = interceptorMethod;
		this.interceptorMethodDesc = interceptorMethodDesc;
		this.interceptorMethodSig = interceptorMethodSig;
		this.localVars = localVars;
		this.contextParam = contextParam;
		this.contextSig = contextSig;
		this.checkReturnTypeSignature = checkReturnTypeSignature;
	}

	/**
	 * Returns the identified class
	 * @return
	 */
	public String getIdentifiedClass() {
		return this.identifiedClass;
	}

	void setIdentifiedClass(String cls) {
		this.identifiedClass = cls;
	}

	/**
	 * Returns whether the return type signature should be checked
	 * @return
	 */
	public boolean getCheckReturnTypeSignature() {
		return this.checkReturnTypeSignature;
	}

	/**
	 * Returns the identified method
	 * @return
	 */
	public MethodDescription getIdentifiedMethod() {
		return this.identifiedMethod;
	}

	void setIdentifiedMethod(MethodDescription method) {
		this.identifiedMethod = method;
	}

	/**
	 * Returns the index of the context parameter
	 * @return
	 */
	public int getContextParameter() {
		return this.contextParam;
	}

	/**
	 * Returns signature of the context parameter (including the IInterceptorContext type!)
	 * @return
	 */
	public TypeClassSymbol getContextSignature() {
		return this.contextSig;
	}

	/**
	 * Returns the list of local variables that are imported and exported
	 * @return
	 */
	public List<LocalVarData> getLocalVars() {
		return Collections.unmodifiableList(this.localVars);
	}

	/**
	 * Returns the (not internal!) name of the {@link IAccessor} class
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
	 * Returns the exit instruction identifier IDs
	 * @return
	 */
	public String[] getExitInstructionIdentifierIds() {
		return this.exitInstructionIdentifierIds;
	}

	/**
	 * Returns the instruction identifiers where the interception will return
	 * @return
	 */
	public IInstructionIdentifier[] getExitInstructionIdentifiers() {
		return this.exitInstructionIdentifiers;
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
	 * Returns the interceptor method signature
	 * @return
	 */
	public String getInterceptorMethodSignature() {
		return this.interceptorMethodSig;
	}

	/**
	 * Initializes the identifiers
	 * @param mappers
	 */
	public void initIdentifiers(Mappers mappers) {
		this.classIdentifier = mappers.getClassIdentifier(this.classIdentifierId, ClassSearchType.ACCESSOR);
		if(this.classIdentifier == null) {
			throw new InstrumentorException(String.format("Class identifier %s[%s] is not mapped", this.accessorClass, this.classIdentifierId));
		}
		this.methodIdentifier = mappers.getMethodIdentifier(this.methodIdentifierId, MethodSearchType.INTERCEPTOR);
		if(this.methodIdentifier == null) {
			throw new InstrumentorException(String.format("Method identifier %s#%s[%s] is not mapped", this.accessorClass, this.interceptorMethod + this.interceptorMethodDesc, this.methodIdentifierId));
		}
		this.instructionIdentifier = mappers.getInstructionIdentifier(this.instructionIdentifierId, InstructionSearchType.INTERCEPTOR_ENTRY);
		if(this.instructionIdentifier == null) {
			throw new InstrumentorException(String.format("Instruction identifier %s#%s[%s] is not mapped", this.accessorClass, this.interceptorMethod + this.interceptorMethodDesc, this.instructionIdentifierId));
		}
		if(this.instructionIdentifier.getType() != InstructionType.INSTRUCTION) {
			throw new InstrumentorException(String.format("Instruction identifier %s#%s[%s] is not of type INSTRUCTION", this.accessorClass, this.interceptorMethod + this.interceptorMethodDesc, this.instructionIdentifierId));
		}
		this.exitInstructionIdentifiers = new IInstructionIdentifier[this.getExitInstructionIdentifierIds().length];
		int i = 0;
		for(String exitInstructionIdentifierId : this.exitInstructionIdentifierIds) {
			this.exitInstructionIdentifiers[i] = mappers.getInstructionIdentifier(exitInstructionIdentifierId, InstructionSearchType.INTERCEPTOR_EXIT);
			if(this.exitInstructionIdentifiers[i] == null) {
				throw new InstrumentorException(String.format("Exit instruction identifier %s#%s[%s] is not mapped", this.accessorClass, this.interceptorMethod + this.interceptorMethodDesc, exitInstructionIdentifierId));
			}
			if(this.exitInstructionIdentifiers[i].getType() != InstructionType.INSTRUCTION) {
				throw new InstrumentorException(String.format("Exit instruction identifier %s#%s[%s] is not of type INSTRUCTION", this.accessorClass, this.interceptorMethod + this.interceptorMethodDesc, exitInstructionIdentifierId));
			}
			i++;
		}
		for(LocalVarData localVar : this.localVars) {
			localVar.initIdentifier(mappers);
		}
	}

	public static final class InterceptorContext implements IInterceptorContext<Object> {
		private final Object[] params;
		private int exit;
		private Object returnVal;
		private boolean returning = false;
		private boolean exiting = false;

		@Internal(id = "ctor")
		public InterceptorContext(int params) {
			this.params = new Object[params];
		}

		@Override
		public void exitAt(int index) {
			this.exit = index;
			this.exiting = true;
		}

		@Override
		public void cancelExit() {
			this.exiting = false;
		}

		@Override
		public int getExit() {
			return this.exit;
		}

		@Override
		public boolean isExiting() {
			return this.exiting;
		}

		@Override
		public void returnWith(Object obj) {
			this.returnVal = obj;
			this.returning = true;
		}

		@Override
		public void cancelReturn() {
			this.returning = false;
		}

		@Override
		public Object getReturn() {
			return this.returnVal;
		}

		@Override
		public boolean isReturning() {
			return this.returning;
		}

		@Override
		public Object[] getLocalVariables() {
			return this.params;
		}
	}
}
