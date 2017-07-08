package pr0x79.instrumentation.accessor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.Type;

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
	private final Class<? extends IAccessor> accessorClass;
	private final IClassIdentifier classIdentifier;
	private final List<MethodAccessorData> methodAccessors = new ArrayList<>();
	private final List<FieldAccessorData> fieldAccessors = new ArrayList<>();
	private final List<FieldGeneratorData> fieldGenerators = new ArrayList<>();

	public ClassAccessorData(String identifierId, Identifiers identifiers, Class<? extends IAccessor> accessorClass, IClassIdentifier classIdentifier, BytecodeInstrumentation instrumentor) {
		this.identifierId = identifierId;
		this.accessorClass = accessorClass;
		this.classIdentifier = classIdentifier;

		for(Method method : accessorClass.getDeclaredMethods()) {
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
		}
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
	public Class<? extends IAccessor> getAccessorClass() {
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
		private final Method accessorMethod;
		private final IMethodIdentifier methodIdentifier;

		private MethodAccessorData(String identifierId, boolean isInterfaceMethod, Method accessorMethod, IMethodIdentifier methodIdentifier) {
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
		public Method getAccessorMethod() {
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
		private final Method accessorMethod;
		private final IFieldIdentifier fieldIdentifier;

		private FieldAccessorData(String identifierId, boolean setter, Method accessorMethod, IFieldIdentifier fieldIdentifier) {
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
		public Method getAccessorMethod() {
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
		private final Class<?> fieldType;
		private final boolean setter;
		private final Method accessorMethod;

		private FieldGeneratorData(String fieldName, Class<?> fieldType, boolean setter, Method accessorMethod) {
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
		public Class<?> getFieldType() {
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
		public Method getAccessorMethod() {
			return this.accessorMethod;
		}
	}
}
