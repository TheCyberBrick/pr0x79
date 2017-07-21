package pr0x79.instrumentation.accessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import pr0x79.instrumentation.BytecodeInstrumentation;
import pr0x79.instrumentation.exception.InstrumentorException;
import pr0x79.instrumentation.identification.IClassIdentifier;
import pr0x79.instrumentation.identification.IFieldIdentifier;
import pr0x79.instrumentation.identification.IMethodIdentifier;
import pr0x79.instrumentation.identification.Identifiers;

/**
 * Stores the data for a registered class accessor and its
 * field and method accessors and field generators
 */
public class ClassAccessorData {
	private final String identifierId;
	private final String accessorClass;
	private final IClassIdentifier classIdentifier;
	private final List<MethodAccessorData> methodAccessors = new ArrayList<>();
	private final List<FieldAccessorData> fieldAccessors = new ArrayList<>();
	private final List<FieldGeneratorData> fieldGenerators = new ArrayList<>();

	public ClassAccessorData(String identifierId, Identifiers identifiers, String accessorClass, ClassNode clsNode, IClassIdentifier classIdentifier, BytecodeInstrumentation instrumentor) {
		this.identifierId = identifierId;
		this.accessorClass = accessorClass;
		this.classIdentifier = classIdentifier;

		for(MethodNode method : clsNode.methods) {
			String methodIdentifierId = BytecodeInstrumentation.getAnnotationValue(method.visibleAnnotations, MethodAccessor.class, MethodAccessor.METHOD_IDENTIFIER, String.class, null);
			boolean interfaceMethod = BytecodeInstrumentation.getAnnotationValue(method.visibleAnnotations, MethodAccessor.class, MethodAccessor.IS_INTERFACE_METHOD, boolean.class, false);
			if(methodIdentifierId != null) {
				if((method.access & Opcodes.ACC_ABSTRACT) == 0) {
					throw new InstrumentorException(String.format("Method accessor %s#%s is a default method", accessorClass, method.name + method.desc));
				}
				if((method.access & Opcodes.ACC_STATIC) != 0) {
					throw new InstrumentorException(String.format("Method accessor %s#%s is a static method", accessorClass, method.name + method.desc));
				}
				IMethodIdentifier methodIdentifier = identifiers.getMethodIdentifier(methodIdentifierId);
				if(methodIdentifier == null) {
					throw new InstrumentorException(String.format("Method identifier %s:%s is not registered", accessorClass, methodIdentifierId));
				}
				this.methodAccessors.add(new MethodAccessorData(methodIdentifierId, interfaceMethod, method, methodIdentifier));
			}

			//Field accessors
			String fieldIdentifierId = BytecodeInstrumentation.getAnnotationValue(method.visibleAnnotations, FieldAccessor.class, FieldAccessor.FIELD_IDENTIFIER, String.class, null);
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
				IFieldIdentifier fieldIdentifier = identifiers.getFieldIdentifier(fieldIdentifierId);
				if(fieldIdentifier == null) {
					throw new InstrumentorException(String.format("Field identifier %s:%s is not registered", accessorClass, fieldIdentifierId));
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
			}

			//Field generators
			String fieldGeneratorField = BytecodeInstrumentation.getAnnotationValue(method.visibleAnnotations, FieldGenerator.class, FieldGenerator.FIELD_NAME, String.class, null);
			if(fieldGeneratorField != null) {
				if((method.access & Opcodes.ACC_ABSTRACT) == 0) {
					throw new InstrumentorException(String.format("Field generator %s#%s is a default method", accessorClass, method.name + method.desc));
				}
				if((method.access & Opcodes.ACC_STATIC) != 0) {
					throw new InstrumentorException(String.format("Field generator %s#%s is a static method", accessorClass, method.name + method.desc));
				}
				if(method.exceptions.size() > 0) {
					throw new InstrumentorException(String.format("Field generator %s#%s throws Exceptions", accessorClass, method.name + method.desc));
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
					this.fieldGenerators.add(new FieldGeneratorData(fieldGeneratorField, params[0], true, method));
				} else {
					this.fieldGenerators.add(new FieldGeneratorData(fieldGeneratorField, returnType, false, method));
				}
			}

			if(methodIdentifierId == null && fieldIdentifierId == null && fieldGeneratorField == null && (method.access & Opcodes.ACC_ABSTRACT) != 0 && (method.access & Opcodes.ACC_STATIC) == 0 && !instrumentor.isGeneratedMethod(clsNode.name, method)) {
				throw new InstrumentorException(String.format("Class accessor %s has an abstract method: %s", accessorClass, method.name + method.desc));
			}
		}

		/*for(Method method : accessorClass.getDeclaredMethods()) {
			//Method accessors
			MethodAccessor methodAccessor = method.getAnnotation(MethodAccessor.class);
			if(methodAccessor != null) {
				if(method.isDefault()) {
					throw new InstrumentorException(String.format("Method accessor %s#%s is a default method", accessorClass.getName(), method.getName() + Type.getMethodDescriptor(method)));
				}
				if((method.getModifiers() & Modifier.STATIC) != 0) {
					throw new InstrumentorException(String.format("Method accessor %s#%s is a static method", accessorClass.getName(), method.getName() + Type.getMethodDescriptor(method)));
				}
				IMethodIdentifier methodIdentifier = identifiers.getMethodIdentifier(methodAccessor.methodIdentifierId());
				if(methodIdentifier == null) {
					throw new InstrumentorException(String.format("Method identifier %s:%s is not registered", accessorClass.getName(), methodAccessor.methodIdentifierId()));
				}
				this.methodAccessors.add(new MethodAccessorData(methodAccessor.methodIdentifierId(), methodAccessor.isInterfaceMethod(), method, methodIdentifier));
			}

			//Field accessors
			FieldAccessor fieldAccessor = method.getAnnotation(FieldAccessor.class);
			if(fieldAccessor != null) {
				if(method.isDefault()) {
					throw new InstrumentorException(String.format("Field accessor %s#%s is a default method", accessorClass.getName(), method.getName()));
				}
				if((method.getModifiers() & Modifier.STATIC) != 0) {
					throw new InstrumentorException(String.format("Field accessor %s#%s is a static method", accessorClass.getName(), method.getName()));
				}
				if(method.getExceptionTypes().length > 0) {
					throw new InstrumentorException(String.format("Field accessor %s#%s throws Exceptions", accessorClass.getName(), method.getName()));
				}
				IFieldIdentifier fieldIdentifier = identifiers.getFieldIdentifier(fieldAccessor.fieldIdentifierId());
				if(fieldIdentifier == null) {
					throw new InstrumentorException(String.format("Field identifier %s:%s is not registered", accessorClass.getName(), fieldAccessor.fieldIdentifierId()));
				}
				if(method.getParameterTypes().length > 0) {
					if(method.getParameterTypes().length != 1) {
						throw new InstrumentorException(String.format("Field accessor (setter?) %s#%s does not have exactly one parameter", accessorClass.getName(), method.getName() + Type.getMethodDescriptor(method)));
					}
					if(!method.getReturnType().equals(Void.TYPE) && !method.getReturnType().equals(accessorClass) && !method.getReturnType().equals(method.getParameterTypes()[0])) {
						throw new InstrumentorException(String.format("Field accessor (setter?) %s#%s does not have return type void, %s or %s", accessorClass.getName(), method.getName() + Type.getMethodDescriptor(method), accessorClass.getName(), method.getParameterTypes()[0].getName()));
					}
					this.fieldAccessors.add(new FieldAccessorData(fieldAccessor.fieldIdentifierId(), true, method, fieldIdentifier));
				} else {
					this.fieldAccessors.add(new FieldAccessorData(fieldAccessor.fieldIdentifierId(), false, method, fieldIdentifier));
				}
			}

			//Field generators
			FieldGenerator fieldGenerator = method.getAnnotation(FieldGenerator.class);
			if(fieldGenerator != null) {
				if(method.isDefault()) {
					throw new InstrumentorException(String.format("Field generator %s#%s is a default method", accessorClass.getName(), method.getName()));
				}
				if((method.getModifiers() & Modifier.STATIC) != 0) {
					throw new InstrumentorException(String.format("Field generator %s#%s is a static method", accessorClass.getName(), method.getName()));
				}
				if(method.getExceptionTypes().length > 0) {
					throw new InstrumentorException(String.format("Field generator %s#%s throws Exceptions", accessorClass.getName(), method.getName()));
				}
				if(method.getParameterTypes().length > 0) {
					if(method.getParameterTypes().length != 1) {
						throw new InstrumentorException(String.format("Field generator (setter?) %s#%s does not have exactly one parameter", accessorClass.getName(), method.getName() + Type.getMethodDescriptor(method)));
					}
					if(!method.getReturnType().equals(Void.TYPE) && !method.getReturnType().equals(accessorClass) && !method.getReturnType().equals(method.getParameterTypes()[0])) {
						throw new InstrumentorException(String.format("Field generator (setter?) %s#%s does not have return type void, %s or %s", accessorClass.getName(), method.getName() + Type.getMethodDescriptor(method), accessorClass.getName(), method.getParameterTypes()[0].getName()));
					}
					this.fieldGenerators.add(new FieldGeneratorData(fieldGenerator.fieldName(), method.getParameterTypes()[0], true, method));
				} else {
					this.fieldGenerators.add(new FieldGeneratorData(fieldGenerator.fieldName(), method.getReturnType(), false, method));
				}
			}

			if(fieldAccessor == null && methodAccessor == null && fieldGenerator == null && !method.isDefault() && !Modifier.isStatic(method.getModifiers()) && !instrumentor.isGeneratedMethod(method)) {
				throw new InstrumentorException(String.format("Class accessor %s has an abstract method: %s", accessorClass.getName(), method.getName() + Type.getMethodDescriptor(method)));
			}
		}*/
	}

	/**
	 * Returns the ID of the class identifier
	 * @return
	 */
	public String getIdentifierId() {
		return this.identifierId;
	}

	/**
	 * Returns the proxy class
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
		private final boolean isInterfaceMethod;
		private final MethodNode accessorMethod;
		private final IMethodIdentifier methodIdentifier;

		private MethodAccessorData(String identifierId, boolean isInterfaceMethod, MethodNode accessorMethod, IMethodIdentifier methodIdentifier) {
			this.identifierId = identifierId;
			this.accessorMethod = accessorMethod;
			this.methodIdentifier = methodIdentifier;
			this.isInterfaceMethod = isInterfaceMethod;
		}

		/**
		 * Returns whether the method to be proxied is from an interface
		 * @return
		 */
		public boolean isInterfaceMethod() {
			return this.isInterfaceMethod;
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
