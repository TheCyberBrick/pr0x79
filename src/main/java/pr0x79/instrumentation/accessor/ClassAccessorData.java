package pr0x79.instrumentation.accessor;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.SourceVersion;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import pr0x79.instrumentation.BytecodeInstrumentation;
import pr0x79.instrumentation.exception.InstrumentorException;
import pr0x79.instrumentation.exception.accessor.method.InvalidMethodModifierException;
import pr0x79.instrumentation.exception.accessor.method.InvalidReturnTypeException;
import pr0x79.instrumentation.identification.IClassIdentifier;
import pr0x79.instrumentation.identification.IFieldIdentifier;
import pr0x79.instrumentation.identification.IFieldIdentifier.FieldDescription;
import pr0x79.instrumentation.identification.IMethodIdentifier;
import pr0x79.instrumentation.identification.IMethodIdentifier.MethodDescription;
import pr0x79.instrumentation.identification.Mappers;
import pr0x79.instrumentation.identification.Mappers.FieldSearchType;
import pr0x79.instrumentation.identification.Mappers.MethodSearchType;
import pr0x79.instrumentation.signature.SignatureParser;
import pr0x79.instrumentation.signature.SignatureParser.Signature;
import pr0x79.instrumentation.signature.SignatureParser.TypeSymbol;

/**
 * Stores the data for a registered class accessor and its
 * field and method accessors and field generators
 */
public final class ClassAccessorData {
	private final String identifierId;
	private final String accessorClass;
	private final IClassIdentifier classIdentifier;
	private final List<MethodAccessorData> methodAccessors = new ArrayList<>();
	private final List<FieldAccessorData> fieldAccessors = new ArrayList<>();
	private final List<FieldGeneratorData> fieldGenerators = new ArrayList<>();
	private final List<MethodInterceptorData> methodInterceptors = new ArrayList<>();

	ClassAccessorData(String identifierId, Mappers mappers, String accessorClass, ClassNode clsNode, IClassIdentifier classIdentifier, BytecodeInstrumentation instrumentor) {
		this.identifierId = identifierId;
		this.accessorClass = accessorClass;
		this.classIdentifier = classIdentifier;

		for(MethodNode method : clsNode.methods) {
			boolean isAccessorOrInterceptor = false;

			isAccessorOrInterceptor |= this.identifyMethodAccessor(method, mappers);
			isAccessorOrInterceptor |= this.identifyFieldAccessor(method, mappers);
			isAccessorOrInterceptor |= this.identifyFieldGenerator(method, mappers);
			isAccessorOrInterceptor |= this.identifyMethodInterceptor(method, mappers, clsNode.name, identifierId);

			if(!isAccessorOrInterceptor && (method.access & Opcodes.ACC_ABSTRACT) != 0 && (method.access & Opcodes.ACC_STATIC) == 0) {
				throw new InstrumentorException(String.format("Class accessor %s has an abstract method: %s", accessorClass, method.name + method.desc));
			}
		}
	}

	/**
	 * Identifies and validates a method accessor
	 * @param method
	 * @param mappers
	 * @return
	 */
	private boolean identifyMethodAccessor(MethodNode method, Mappers mappers) {
		String methodIdentifierId = BytecodeInstrumentation.getAnnotationValue(method.visibleAnnotations, MethodAccessor.class, BytecodeInstrumentation.getInternalMethod(MethodAccessor.class, "method_identifier").getName(), String.class, null);
		if(methodIdentifierId != null) {
			if((method.access & Opcodes.ACC_ABSTRACT) == 0) {
				throw new InstrumentorException(String.format("Method accessor %s#%s is a default method", accessorClass, method.name + method.desc));
			}
			if((method.access & Opcodes.ACC_STATIC) != 0) {
				throw new InstrumentorException(String.format("Method accessor %s#%s is a static method", accessorClass, method.name + method.desc));
			}
			IMethodIdentifier methodIdentifier = mappers.getMethodIdentifier(methodIdentifierId, MethodSearchType.ACCESSOR);
			if(methodIdentifier == null) {
				throw new InstrumentorException(String.format("Method identifier %s[%s] is not mapped", accessorClass, methodIdentifierId));
			}
			this.methodAccessors.add(new MethodAccessorData(methodIdentifierId, method, methodIdentifier));
			return true;
		}
		return false;
	}

	/**
	 * Identifies and validates a method interceptor
	 * @param method
	 * @param mappers
	 * @param className
	 * @param classIdentifierId
	 * @return
	 */
	private boolean identifyMethodInterceptor(MethodNode method, Mappers mappers, String className, String classIdentifierId) {
		AnnotationNode interceptorAnnotation = null;

		if(method.visibleAnnotations != null) {
			for(AnnotationNode annotation : method.visibleAnnotations) {
				if(annotation.desc.equals(Type.getDescriptor(Interceptor.class))) {
					interceptorAnnotation = annotation;
				}
			}
		}

		if(interceptorAnnotation != null) {
			if(Type.getReturnType(method.desc).getSort() != Type.VOID) {
				throw new InvalidReturnTypeException(String.format("Return type of method interceptor %s#%s is not void", className, method.name + method.desc), null, className, new MethodDescription(method.name, method.desc), void.class.getName(), Type.getReturnType(method.desc).getClassName());
			}

			if((method.access & Opcodes.ACC_STATIC) != 0) {
				throw new InvalidMethodModifierException(String.format("Method interceptor %s#%s is a static method", className, method.name + method.desc), null, className, new MethodDescription(method.name, method.desc), Modifier.STATIC);
			}

			TypeSymbol contextSig = null;
			int contextParam = -1;
			List<LocalVarData> methodLocalVars = new ArrayList<>();
			Type[] params = Type.getArgumentTypes(method.desc);
			for(int i = 0; i < params.length; i++) {
				if(params[i].getClassName().equals(IInterceptorContext.class.getName())) {
					if(contextParam >= 0) {
						throw new InstrumentorException(String.format("Method interceptor %s#%s has multiple IInterceptorContext parameters", className, method.name + method.desc));
					}
					contextParam = i;

					if(method.signature != null) {
						Signature sig = SignatureParser.parse(method.signature);
						for(TypeSymbol paramSig : sig.parameters) {
							if(!paramSig.isVariable() && paramSig.getAsClass().getType().getClassName().equals(IInterceptorContext.class.getName())) {
								contextSig = paramSig;
							}
						}
					}

					continue;
				}
				AnnotationNode localVarAnnotation = null;
				if(method.visibleParameterAnnotations != null && i < method.visibleParameterAnnotations.length) {
					List<AnnotationNode> paramAnnotations = method.visibleParameterAnnotations[i];
					if(paramAnnotations != null) {
						for(AnnotationNode annotation : paramAnnotations) {
							if(annotation.desc.equals(Type.getDescriptor(LocalVar.class))) {
								localVarAnnotation = annotation;
								break;
							}
						}
					}
				}
				if(localVarAnnotation == null) {
					throw new InstrumentorException(String.format("Parameter %d for method interceptor %s#%s does not have an @LocalVar annotation and is not IInterceptorContext", i, className, method.name + method.desc));
				}
				String instructionIdentifierId = BytecodeInstrumentation.getAnnotationValue(localVarAnnotation, BytecodeInstrumentation.getInternalMethod(LocalVar.class, "instruction_identifier").getName(), String.class);
				if(instructionIdentifierId == null) {
					throw new InstrumentorException(String.format("@LocalVar for parameter %d of method %s#%s has invalid arguments", i, className, method.name + method.desc));
				}
				LocalVarData localVar = new LocalVarData(method.name, method.desc, i, Type.getObjectType(className).getClassName(), instructionIdentifierId);
				methodLocalVars.add(localVar);
			}

			if(contextParam < 0) {
				throw new InstrumentorException(String.format("Method interceptor %s#%s has no IInterceptorContext parameter", className, method.name + method.desc));
			}

			String methodIdentifierId = BytecodeInstrumentation.getAnnotationValue(interceptorAnnotation, BytecodeInstrumentation.getInternalMethod(Interceptor.class, "method_identifier").getName(), String.class);
			String instructionIdentifierId = BytecodeInstrumentation.getAnnotationValue(interceptorAnnotation, BytecodeInstrumentation.getInternalMethod(Interceptor.class, "instruction_identifier").getName(), String.class);
			List<?> exitInstructionIdentifierIdObjs = BytecodeInstrumentation.getAnnotationValue(interceptorAnnotation, BytecodeInstrumentation.getInternalMethod(Interceptor.class, "exit_instruction_identifiers").getName(), ArrayList.class);
			String[] exitInstructionIdentifierIds = new String[exitInstructionIdentifierIdObjs == null ? 0 : exitInstructionIdentifierIdObjs.size()];
			if(exitInstructionIdentifierIdObjs != null) {
				for(int i = 0; i < exitInstructionIdentifierIdObjs.size(); i++) {
					exitInstructionIdentifierIds[i] = (String) exitInstructionIdentifierIdObjs.get(i);
				}
			}

			if(methodIdentifierId == null || instructionIdentifierId == null || exitInstructionIdentifierIds == null) {
				throw new InstrumentorException(String.format("Method interceptor for method %s#%s has invalid arguments", className, method.name + method.desc));
			}

			MethodInterceptorData methodInterceptor = new MethodInterceptorData(
					classIdentifierId, methodIdentifierId, instructionIdentifierId, 
					exitInstructionIdentifierIds, Type.getObjectType(className).getClassName(), 
					method.name, method.desc, method.signature, methodLocalVars, contextParam,
					contextSig);
			methodInterceptor.initIdentifiers(mappers);
			this.methodInterceptors.add(methodInterceptor);

			return true;
		}
		return false;
	}

	/**
	 * Identifies and validates a field accessor
	 * @param method
	 * @param mappers
	 * @return
	 */
	private boolean identifyFieldAccessor(MethodNode method, Mappers mappers) {
		String fieldIdentifierId = BytecodeInstrumentation.getAnnotationValue(method.visibleAnnotations, FieldAccessor.class, BytecodeInstrumentation.getInternalMethod(FieldAccessor.class, "field_identifier").getName(), String.class, null);
		if(fieldIdentifierId != null) {
			if((method.access & Opcodes.ACC_ABSTRACT) == 0) {
				throw new InstrumentorException(String.format("Field accessor %s#%s is a default method", accessorClass, method.name + method.desc));
			}
			if((method.access & Opcodes.ACC_STATIC) != 0) {
				throw new InstrumentorException(String.format("Field accessor %s#%s is a static method", accessorClass, method.name + method.desc));
			}
			if(method.exceptions.size() > 0) {
				throw new InstrumentorException(String.format("Field accessor %s#%s throws Exceptions", accessorClass, method.name + method.desc));
			}
			IFieldIdentifier fieldIdentifier = mappers.getFieldIdentifier(fieldIdentifierId, FieldSearchType.ACCESSOR);
			if(fieldIdentifier == null) {
				throw new InstrumentorException(String.format("Field identifier %s#%s[%s] is not mapped", accessorClass, method.name + method.desc, fieldIdentifierId));
			}
			Type[] params = Type.getArgumentTypes(method.desc);
			Type returnType = Type.getReturnType(method.desc);
			if(params.length > 0) {
				if(params.length != 1) {
					throw new InstrumentorException(String.format("Field accessor (setter?) %s#%s does not have exactly one parameter", accessorClass, method.name + method.desc));
				}
				if(returnType.getSort() != Type.VOID && !returnType.getClassName().equals(accessorClass) && !returnType.getClassName().equals(params[0].getClassName())) {
					throw new InstrumentorException(String.format("Field accessor (setter?) %s#%s does not have return type void, %s or %s", accessorClass, method.name + method.desc, accessorClass, params[0].getClassName()));
				}
				this.fieldAccessors.add(new FieldAccessorData(fieldIdentifierId, true, method, fieldIdentifier));
			} else {
				this.fieldAccessors.add(new FieldAccessorData(fieldIdentifierId, false, method, fieldIdentifier));
			}
			return true;
		}
		return false;
	}

	/**
	 * Identifies and validates a field generator
	 * @param method
	 * @param mappers
	 * @return
	 */
	private boolean identifyFieldGenerator(MethodNode method, Mappers mappers) {
		String fieldNameIdentifier = BytecodeInstrumentation.getAnnotationValue(method.visibleAnnotations, FieldGenerator.class, BytecodeInstrumentation.getInternalMethod(FieldGenerator.class, "field_name_identifier").getName(), String.class, null);
		if(fieldNameIdentifier != null) {
			if((method.access & Opcodes.ACC_ABSTRACT) == 0) {
				throw new InstrumentorException(String.format("Field generator %s#%s is a default method", accessorClass, method.name + method.desc));
			}
			if((method.access & Opcodes.ACC_STATIC) != 0) {
				throw new InstrumentorException(String.format("Field generator %s#%s is a static method", accessorClass, method.name + method.desc));
			}
			if(method.exceptions.size() > 0) {
				throw new InstrumentorException(String.format("Field generator %s#%s throws Exceptions", accessorClass, method.name + method.desc));
			}
			IFieldIdentifier identifier = mappers.getFieldIdentifier(fieldNameIdentifier, FieldSearchType.NAME_GENERATOR);
			if(identifier == null) {
				throw new InstrumentorException(String.format("Field name identifier %s#%s[%s] is not mapped", accessorClass, method.name + method.desc, fieldNameIdentifier));
			}
			if(!identifier.isStatic()) {
				throw new InstrumentorException(String.format("Field name identifier %s#%s[%s] is not static", accessorClass, method.name + method.desc, fieldNameIdentifier));
			}
			FieldDescription[] fieldDescriptions = identifier.getFields();
			if(fieldDescriptions.length == 0) {
				throw new InstrumentorException(String.format("Field name identifier %s#%s[%s] did not return any field", accessorClass, method.name + method.desc, fieldNameIdentifier));
			}
			if(fieldDescriptions.length > 1) {
				throw new InstrumentorException(String.format("Field name identifier %s#%s[%s] returned more than one field", accessorClass, method.name + method.desc, fieldNameIdentifier));
			}
			String fieldName = fieldDescriptions[0].getName();
			if(!SourceVersion.isName(fieldName)) {
				throw new InstrumentorException(String.format("Field name identifier %s#%s[%s] returned an invalid field name: %s", accessorClass, method.name + method.desc, fieldNameIdentifier, fieldName));
			}
			Type[] params = Type.getArgumentTypes(method.desc);
			Type returnType = Type.getReturnType(method.desc);
			if(params.length > 0) {
				if(params.length != 1) {
					throw new InstrumentorException(String.format("Field generator (setter?) %s#%s does not have exactly one parameter", accessorClass, method.name + method.desc));
				}
				if(returnType.getSort() != Type.VOID && !returnType.getClassName().equals(accessorClass) && !returnType.getClassName().equals(params[0].getClassName())) {
					throw new InstrumentorException(String.format("Field generator (setter?) %s#%s does not have return type void, %s or %s", accessorClass, method.name + method.desc, accessorClass, params[0].getClassName()));
				}
				this.fieldGenerators.add(new FieldGeneratorData(fieldName, params[0], true, method));
			} else {
				this.fieldGenerators.add(new FieldGeneratorData(fieldName, returnType, false, method));
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns the ID of the class identifier
	 * @return
	 */
	public String getIdentifierId() {
		return this.identifierId;
	}

	/**
	 * Returns (non internal!) name of the accessor class
	 * @return
	 */
	public String getAccessorClass() {
		return this.accessorClass;
	}

	/**
	 * Returns the class identifier of the class to be instrumented
	 * @return
	 */
	public IClassIdentifier getClassIdentifier() {
		return this.classIdentifier;
	}

	/**
	 * Returns a list of all method accessors
	 * @return
	 */
	public List<MethodAccessorData> getMethodAccessors() {
		return Collections.unmodifiableList(this.methodAccessors);
	}

	/**
	 * Returns a list of all method interceptors
	 * @return
	 */
	public List<MethodInterceptorData> getMethodInterceptors() {
		return Collections.unmodifiableList(this.methodInterceptors);
	}

	/**
	 * Returns a list of all field accessors
	 * @return
	 */
	public List<FieldAccessorData> getFieldAccessors() {
		return Collections.unmodifiableList(this.fieldAccessors);
	}

	/**
	 * Returns a list of all field generators
	 * @return
	 */
	public List<FieldGeneratorData> getFieldGenerators() {
		return Collections.unmodifiableList(this.fieldGenerators);
	}

	/**
	 * Stores the data for a registered method accessor
	 */
	public static class MethodAccessorData {
		private final String identifierId;
		private final MethodNode accessorMethod;
		private final IMethodIdentifier methodIdentifier;

		private MethodAccessorData(String identifierId, MethodNode accessorMethod, IMethodIdentifier methodIdentifier) {
			this.identifierId = identifierId;
			this.accessorMethod = accessorMethod;
			this.methodIdentifier = methodIdentifier;
		}

		/**
		 * Returns the ID of the method identifier
		 * @return
		 */
		public String getIdentifierId() {
			return this.identifierId;
		}

		/**
		 * Returns the proxy method
		 * @return
		 */
		public MethodNode getAccessorMethod() {
			return this.accessorMethod;
		}

		/**
		 * Returns the method identifier of the method to be proxied
		 * @return
		 */
		public IMethodIdentifier getMethodIdentifier() {
			return this.methodIdentifier;
		}
	}

	/**
	 * Stores the data for a registered field accessor
	 */
	public static class FieldAccessorData {
		private final String identifierId;
		private final boolean setter;
		private final MethodNode accessorMethod;
		private final IFieldIdentifier fieldIdentifier;

		private FieldAccessorData(String identifierId, boolean setter, MethodNode accessorMethod, IFieldIdentifier fieldIdentifier) {
			this.identifierId = identifierId;
			this.setter = setter;
			this.accessorMethod = accessorMethod;
			this.fieldIdentifier = fieldIdentifier;
		}

		/**
		 * Returns the ID of the field identifier
		 * @return
		 */
		public String getIdentifierId() {
			return this.identifierId;
		}

		/**
		 * Returns whether this field accessor is a setter (and if false, is a getter)
		 * @return
		 */
		public boolean isSetter() {
			return this.setter;
		}

		/**
		 * Returns the proxy method
		 * @return
		 */
		public MethodNode getAccessorMethod() {
			return this.accessorMethod;
		}

		/**
		 * Returns the field identifier of the field to be proxied
		 * @return
		 */
		public IFieldIdentifier getFieldIdentifier() {
			return this.fieldIdentifier;
		}
	}

	/**
	 * Stores the data for a registered field generator
	 */
	public static class FieldGeneratorData {
		private final String fieldName;
		private final Type fieldType;
		private final boolean setter;
		private final MethodNode accessorMethod;

		private FieldGeneratorData(String fieldName, Type fieldType, boolean setter, MethodNode accessorMethod) {
			this.fieldName = fieldName;
			this.fieldType = fieldType;
			this.setter = setter;
			this.accessorMethod = accessorMethod;
		}

		/**
		 * Returns the name of the field to be generated
		 * @return
		 */
		public String getFieldName() {
			return this.fieldName;
		}

		/**
		 * Returns the type of the field to be generated
		 * @return
		 */
		public Type getFieldType() {
			return this.fieldType;
		}

		/**
		 * Returns whether this field generator is a setter (and if false, is a getter)
		 * @return
		 */
		public boolean isSetter() {
			return this.setter;
		}

		/**
		 * Returns the proxy method
		 * @return
		 */
		public MethodNode getAccessorMethod() {
			return this.accessorMethod;
		}
	}
}
