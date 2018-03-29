package pr0x79.instrumentation.accessor;

import java.util.Collections;
import java.util.List;

import pr0x79.instrumentation.Internal;
import pr0x79.instrumentation.exception.InstrumentorException;
import pr0x79.instrumentation.exception.InvalidExitException;
import pr0x79.instrumentation.identification.IClassIdentifier;
import pr0x79.instrumentation.identification.IInstructionIdentifier;
import pr0x79.instrumentation.identification.IInstructionIdentifier.InstructionType;
import pr0x79.instrumentation.identification.IMethodIdentifier;
import pr0x79.instrumentation.identification.Identifiers;
import pr0x79.instrumentation.signature.SignatureParser.TypeSymbol;

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
	private final TypeSymbol contextSig;

	MethodInterceptorData(String classIdentifierId, String methodIdentifierId, String instructionIdentifierId, String[] exitInstructionIdentifierIds, 
			String accessorClass, String interceptorMethod, String interceptorMethodDesc, String interceptorMethodSig, List<LocalVarData> localVars, int contextParam,
			TypeSymbol contextSig) {
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
	}

	/**
	 * Returns the index of the context parameter
	 * @return
	 */
	public int getContextParameter() {
		return this.contextParam;
	}
	
	/**
	 * Returns signature of the context parameter
	 * @return
	 */
	public TypeSymbol getContextSignature() {
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
		this.exitInstructionIdentifiers = new IInstructionIdentifier[this.getExitInstructionIdentifierIds().length];
		int i = 0;
		for(String exitInstructionIdentifierId : this.exitInstructionIdentifierIds) {
			this.exitInstructionIdentifiers[i] = identifiers.getInstructionIdentifier(exitInstructionIdentifierId);
			if(this.exitInstructionIdentifiers[i] == null) {
				throw new InstrumentorException(String.format("Exit instruction identifier %s for interceptor %s#%s is not registered", exitInstructionIdentifierId, this.accessorClass, this.interceptorMethod + this.interceptorMethodDesc));
			}
			if(this.exitInstructionIdentifiers[i].getType() != InstructionType.INSTRUCTION) {
				throw new InstrumentorException(String.format("Exit instruction identifier %s for interceptor %s#%s is not of type INSTRUCTION", exitInstructionIdentifierId, this.accessorClass, this.interceptorMethod + this.interceptorMethodDesc));
			}
			i++;
		}
		for(LocalVarData localVar : this.localVars) {
			localVar.initIdentifiers(identifiers);
		}
	}
	
	@Internal(id = "create_interceptor_context")
	public static IInterceptorContext<Object> createInterceptorContext(int params) {
		return new InterceptorContext(params);
	}
	
	@Internal(id = "check_exit")
	public static boolean checkExit(int exit, int exits) {
		if(exit == IInterceptorContext.DO_NOT_EXIT) {
			return false;
		} else if(exit >= 0 && exit < exits) {
			return true;
		}
		throw new InvalidExitException(String.format("Interceptor returned with invalid exit %d", exit));
	}
	
	@Internal(id = "check_return")
	public static boolean checkReturn(Object val) {
		return val != IInterceptorContext.DO_NOT_RETURN;
	}
	
	private static class InterceptorContext implements IInterceptorContext<Object> {
		private Object[] params;
		private int exit = DO_NOT_EXIT;
		private Object returnVal = DO_NOT_RETURN;
		
		private InterceptorContext(int params) {
			this.params = new Object[params];
		}
		
		@Override
		public void exitAt(int index) {
			if(index >= 0) {
				this.exit = index;
			} else {
				this.exit = DO_NOT_EXIT;
			}
		}

		@Override
		public void cancelExit() {
			this.exit = DO_NOT_EXIT;
		}

		@Override
		public int getExit() {
			return this.exit;
		}

		@Override
		public void returnWith(Object obj) {
			this.returnVal = obj;
		}

		@Override
		public void cancelReturn() {
			this.returnVal = DO_NOT_RETURN;
		}

		@Override
		public Object getReturn() {
			return this.returnVal;
		}

		@Override
		public Object[] getLocalVariables() {
			return this.params;
		}
	}
}
