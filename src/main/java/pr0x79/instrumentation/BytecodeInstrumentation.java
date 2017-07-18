package pr0x79.instrumentation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import pr0x79.Bootstrapper;
import pr0x79.instrumentation.accessor.Accessors;
import pr0x79.instrumentation.accessor.ClassAccessor;
import pr0x79.instrumentation.accessor.ClassAccessorData;
import pr0x79.instrumentation.accessor.ClassAccessorData.FieldAccessorData;
import pr0x79.instrumentation.accessor.ClassAccessorData.FieldGeneratorData;
import pr0x79.instrumentation.accessor.ClassAccessorData.MethodAccessorData;
import pr0x79.instrumentation.accessor.IAccessor;
import pr0x79.instrumentation.accessor.Interceptor;
import pr0x79.instrumentation.accessor.InterceptorConditional;
import pr0x79.instrumentation.accessor.InterceptorReturn;
import pr0x79.instrumentation.accessor.LocalVar;
import pr0x79.instrumentation.accessor.LocalVarData;
import pr0x79.instrumentation.accessor.MethodInterceptorData;
import pr0x79.instrumentation.exception.InstrumentorException;
import pr0x79.instrumentation.exception.accessor.field.FieldAccessorTakenException;
import pr0x79.instrumentation.exception.accessor.field.InvalidGetterTypeException;
import pr0x79.instrumentation.exception.accessor.field.InvalidSetterTypeException;
import pr0x79.instrumentation.exception.accessor.fieldgenerator.FieldGeneratorTakenException;
import pr0x79.instrumentation.exception.accessor.method.InvalidMethodDescriptorException;
import pr0x79.instrumentation.exception.accessor.method.InvalidMethodExceptionsException;
import pr0x79.instrumentation.exception.accessor.method.InvalidMethodModifierException;
import pr0x79.instrumentation.exception.accessor.method.InvalidParameterTypeException;
import pr0x79.instrumentation.exception.accessor.method.InvalidReturnTypeException;
import pr0x79.instrumentation.exception.accessor.method.MethodAccessorTakenException;
import pr0x79.instrumentation.exception.identifier.field.FieldNotFoundException;
import pr0x79.instrumentation.exception.identifier.field.MultipleFieldsIdentifiedException;
import pr0x79.instrumentation.exception.identifier.instruction.ImportInstructionNotFoundException;
import pr0x79.instrumentation.exception.identifier.instruction.InstructionNotFoundException;
import pr0x79.instrumentation.exception.identifier.instruction.InstructionOutOfBoundsException;
import pr0x79.instrumentation.exception.identifier.instruction.InvalidJumpTargetException;
import pr0x79.instrumentation.exception.identifier.instruction.JumpInstructionNotFoundException;
import pr0x79.instrumentation.exception.identifier.method.MethodNotFoundException;
import pr0x79.instrumentation.exception.identifier.method.MultipleMethodsIdentifiedException;
import pr0x79.instrumentation.identification.IClassIdentifier;
import pr0x79.instrumentation.identification.IFieldIdentifier;
import pr0x79.instrumentation.identification.IMethodIdentifier;
import pr0x79.instrumentation.identification.IMethodIdentifier.MethodDescription;
import pr0x79.instrumentation.identification.Identifiers;

/**
 * Instruments classes using the registered {@link IAccessor}s
 */
public class BytecodeInstrumentation {
	private Accessors accessors;
	private final Identifiers identifiers;

	//A map is used so that there are no duplicates, the key consists of: <class name>#<method name><method descriptor>
	private final Map<String, MethodInterceptorData> interceptors;

	public BytecodeInstrumentation(Identifiers identifiers, Map<String, MethodInterceptorData> interceptors) {
		this.identifiers = identifiers;
		this.interceptors = interceptors;
	}

	/**
	 * Sets the accessors
	 * @param accessors
	 */
	public void setAccessors(Accessors accessors) {
		this.accessors = accessors;
	}

	/**
	 * Returns whether the specified class is accepted and has to be instrumented
	 * @param cls
	 * @return
	 */
	public boolean acceptsClass(String cls) {
		for(ClassAccessorData accessor : this.accessors.getClassAccessors()) {
			if(isIdentifiedClass(accessor.getClassIdentifier(), cls)) {
				return true;
			}
		}
		for(MethodInterceptorData interceptor : this.interceptors.values()) {
			if(isIdentifiedClass(interceptor.getClassIdentifier(), cls)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isIdentifiedMethod(IMethodIdentifier identifier, MethodNode methodNode) {
		return (!identifier.isStatic() && identifier.isIdentifiedMethod(methodNode)) || (identifier.isStatic() && Arrays.asList(identifier.getMethods()).contains(new IMethodIdentifier.MethodDescription(methodNode.name, methodNode.desc)));
	}

	private static boolean isIdentifiedField(IFieldIdentifier identifier, FieldNode fieldNode) {
		return (!identifier.isStatic() && identifier.isIdentifiedField(fieldNode)) || (identifier.isStatic() && Arrays.asList(identifier.getFields()).contains(new IFieldIdentifier.FieldDescription(fieldNode.name, fieldNode.desc)));
	}

	private static boolean isIdentifiedClass(IClassIdentifier identifier, String name) {
		return (!identifier.isStatic() && identifier.isIdentifiedClass(name)) || (identifier.isStatic() && Arrays.asList(identifier.getClassNames()).contains(name));
	}

	/**
	 * Instruments the specified {@link ClassNode} of
	 * an {@link IAccessor} class
	 * @param clsNode
	 * @param identifiers
	 * @return True if modified
	 */
	public boolean instrumentAccessorClass(ClassNode clsNode, Bootstrapper bootstrapper) {
		AnnotationNode classAccessorAnnotation = null;
		if(clsNode.visibleAnnotations != null) {
			for(AnnotationNode annotation : clsNode.visibleAnnotations) {
				if(annotation.desc.equals(Type.getDescriptor(ClassAccessor.class))) {
					classAccessorAnnotation = annotation;
					break;
				}
			}
		}
		if(classAccessorAnnotation == null) {
			throw new InstrumentorException(String.format("Accessor %s does not have a class accessor annotation", clsNode.name));
		}

		String classIdentifierId = this.getAnnotationValue(classAccessorAnnotation, "classIdentifierId", String.class);
		if(classIdentifierId == null) {
			throw new InstrumentorException(String.format("Class accessor %s has invalid arguments", clsNode.name));
		}

		Set<String> takenMethodNames = new HashSet<>();
		for(MethodNode method : clsNode.methods) {
			takenMethodNames.add(method.name);
		}

		List<LocalVarData> fieldRequiringLocalVars = new ArrayList<>();

		for(MethodNode method : clsNode.methods) {
			AnnotationNode interceptorAnnotation = null;
			AnnotationNode instructionJumpAnnotation = null;
			AnnotationNode returnAnnotation = null;

			if(method.visibleAnnotations != null) {
				for(AnnotationNode annotation : method.visibleAnnotations) {
					if(annotation.desc.equals(Type.getDescriptor(Interceptor.class))) {
						interceptorAnnotation = annotation;
					}
					if(annotation.desc.equals(Type.getDescriptor(InterceptorConditional.class))) {
						instructionJumpAnnotation = annotation;
					}
					if(annotation.desc.equals(Type.getDescriptor(InterceptorReturn.class))) {
						returnAnnotation = annotation;
					}
				}
			}

			if(interceptorAnnotation != null) {
				if(instructionJumpAnnotation != null && returnAnnotation != null) {
					throw new InstrumentorException(String.format("Method interceptor %s#%s has both the instruction jump annotation and a return annotation", clsNode.name, method.name + method.desc));
				}

				if(instructionJumpAnnotation == null) {
					if(returnAnnotation == null && Type.getReturnType(method.desc).getSort() != Type.VOID) {
						throw new InvalidReturnTypeException(String.format("Return type of method interceptor %s#%s is not void", clsNode.name, method.name + method.desc), null, clsNode.name, new MethodDescription(method.name, method.desc), void.class.getName(), Type.getReturnType(method.desc).getClassName());
					}
				} else {
					if(Type.getReturnType(method.desc).getSort() != Type.BOOLEAN) {
						throw new InvalidReturnTypeException(String.format("Return type of method interceptor %s#%s with instruction jump is not boolean", clsNode.name, method.name + method.desc), null, clsNode.name, new MethodDescription(method.name, method.desc), boolean.class.getName(), Type.getReturnType(method.desc).getClassName());
					}
				}

				if((method.access & Opcodes.ACC_STATIC) != 0) {
					throw new InvalidMethodModifierException(String.format("Method interceptor %s#%s is a static method", clsNode.name, method.name + method.desc), null, clsNode.name, new MethodDescription(method.name, method.desc), Modifier.STATIC);
				}

				List<LocalVarData> methodLocalVars = new ArrayList<>();
				Type[] params = Type.getArgumentTypes(method.desc);
				for(int i = 0; i < params.length; i++) {
					AnnotationNode importAnnotation = null;
					if(method.visibleParameterAnnotations != null && i < method.visibleParameterAnnotations.length) {
						List<AnnotationNode> paramAnnotations = method.visibleParameterAnnotations[i];
						if(paramAnnotations != null) {
							for(AnnotationNode annotation : paramAnnotations) {
								if(annotation.desc.equals(Type.getDescriptor(LocalVar.class))) {
									importAnnotation = annotation;
									break;
								}
							}
						}
					}
					if(importAnnotation == null) {
						throw new InstrumentorException(String.format("Parameter %d for method interceptor %s#%s does not have an @Import annotation", i, clsNode.name, method.name + method.desc));
					}
					String instructionIdentifierId = this.getAnnotationValue(importAnnotation, LocalVar.INSTRUCTION_IDENTIFIER_ID, String.class);
					if(instructionIdentifierId == null) {
						throw new InstrumentorException(String.format("Import for parameter %d of method %s#%s has invalid arguments", i, clsNode.name, method.name + method.desc));
					}
					String generatedSetterName = this.getUniqueName(takenMethodNames);
					takenMethodNames.add(generatedSetterName);
					String generatedGetterName = this.getUniqueName(takenMethodNames);
					takenMethodNames.add(generatedGetterName);
					LocalVarData localVar = new LocalVarData(method.name, method.desc, i, Type.getObjectType(clsNode.name).getClassName(), instructionIdentifierId, generatedSetterName, generatedGetterName);
					methodLocalVars.add(localVar);
					if(returnAnnotation == null) {
						fieldRequiringLocalVars.add(localVar);
					}
				}

				if(params.length > 0) {
					Map<AbstractInsnNode, InsnList> insertionPoints = new HashMap<>();
					Iterator<AbstractInsnNode> insnIT = method.instructions.iterator();
					while(insnIT.hasNext()) {
						AbstractInsnNode node = insnIT.next();
						if(node.getOpcode() == Opcodes.RETURN ||
								node.getOpcode() == Opcodes.ARETURN ||
								node.getOpcode() == Opcodes.DRETURN ||
								node.getOpcode() == Opcodes.FRETURN ||
								node.getOpcode() == Opcodes.IRETURN ||
								node.getOpcode() == Opcodes.LRETURN ||
								node.getOpcode() == Opcodes.ATHROW) {
							InsnList insertions = new InsnList();
							int stackIndex = 1;
							for(int i = 0; i < params.length; i++) {
								LocalVarData localVar = methodLocalVars.get(i);
								Type varType = Type.getArgumentTypes(localVar.getInterceptorMethodDesc())[localVar.getParameterIndex()];
								insertions.add(new VarInsnNode(Opcodes.ALOAD, 0));
								insertions.add(new VarInsnNode(varType.getOpcode(Opcodes.ILOAD), stackIndex));
								insertions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, clsNode.name, localVar.getGeneratedSetterMethod(), Type.getMethodDescriptor(Type.VOID_TYPE, varType), true));
								stackIndex += varType.getSize();
							}
							insertionPoints.put(node, insertions);
						}
					}
					for(Entry<AbstractInsnNode, InsnList> insertion : insertionPoints.entrySet()) {
						method.instructions.insertBefore(insertion.getKey(), insertion.getValue());
					}
				}

				String methodIdentifierId = this.getAnnotationValue(interceptorAnnotation, Interceptor.METHOD_IDENTIFIER_ID, String.class);
				String instructionIdentifierId = this.getAnnotationValue(interceptorAnnotation, Interceptor.INSTRUCTION_IDENTIFIER_ID, String.class);
				String jumpInstructionIdentifierId = null;
				if(instructionJumpAnnotation != null) {
					jumpInstructionIdentifierId = this.getAnnotationValue(instructionJumpAnnotation, InterceptorConditional.INSTRUCTION_IDENTIFIER_ID, String.class);
				}
				if(methodIdentifierId == null || instructionIdentifierId == null || (jumpInstructionIdentifierId != null && instructionJumpAnnotation == null)) {
					throw new InstrumentorException(String.format("Method interceptor for method %s#%s has invalid arguments", clsNode.name, method.name + method.desc));
				}

				MethodInterceptorData methodInterceptor = new MethodInterceptorData(classIdentifierId, methodIdentifierId, instructionIdentifierId, jumpInstructionIdentifierId, Type.getObjectType(clsNode.name).getClassName(), method.name, method.desc, methodLocalVars, returnAnnotation != null);
				if(!bootstrapper.isInitializing()) {
					methodInterceptor.initIdentifiers(this.identifiers);
				}
				this.interceptors.put(clsNode.name + "#" + method.name + method.desc, methodInterceptor);
			}
		}

		for(LocalVarData localVar : fieldRequiringLocalVars) {
			//Setter
			MethodVisitor mvSetter = clsNode.visitMethod(Opcodes.ACC_ABSTRACT | Opcodes.ACC_PUBLIC, localVar.getGeneratedSetterMethod(), Type.getMethodDescriptor(Type.VOID_TYPE, Type.getArgumentTypes(localVar.getInterceptorMethodDesc())[localVar.getParameterIndex()]), null, null);
			mvSetter.visitEnd();

			//Getter
			MethodVisitor mvGetter = clsNode.visitMethod(Opcodes.ACC_ABSTRACT | Opcodes.ACC_PUBLIC , localVar.getGeneratedGetterMethod(), Type.getMethodDescriptor(Type.getArgumentTypes(localVar.getInterceptorMethodDesc())[localVar.getParameterIndex()]), null, null);
			mvGetter.visitEnd();
		}

		return !fieldRequiringLocalVars.isEmpty();
	}

	/**
	 * Generates a unique name with the specified exclusions
	 * @param clsNode
	 * @return
	 */
	private String getUniqueName(Set<String> exclusions) {
		List<String> checked = new ArrayList<String>();
		checked.add("");
		for(int maxLength = 1; maxLength <= 32; ++maxLength) {
			int words = checked.size();
			for(int i = 0; i < words; ++i) {
				for(char c = 'a'; c <= 'z'; ++c) {
					String name = checked.get(i) + c;
					if(!exclusions.contains(name)) {
						return name;
					}
					checked.add(name);
				}
			}
		}
		return null;
	}

	/**
	 * Gets the specified value of an {@link AnnotationNode}
	 * @param annotation
	 * @param name
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T> T getAnnotationValue(AnnotationNode annotation, String name, Class<T> type) {
		for(int i = 0; i < annotation.values.size() / 2 + 1; i += 2) {
			if(name.equals(((String) annotation.values.get(i))) && annotation.values.get(i + 1) != null && type.isAssignableFrom(annotation.values.get(i + 1).getClass())) {
				return (T) annotation.values.get(i + 1);
			}
		}
		return null;
	}

	/**
	 * Instruments the specified {@link ClassNode} according to the
	 * registered {@link IAccessor}s
	 * @param clsNode
	 */
	public void instrumentClass(ClassNode clsNode) {
		List<ClassAccessorData> classAccessors = new ArrayList<>();
		for(ClassAccessorData accessor : this.accessors.getClassAccessors()) {
			if(isIdentifiedClass(accessor.getClassIdentifier(), clsNode.name)) {
				classAccessors.add(accessor);
			}
		}

		//Instrument class body
		String[] interfaces = new String[classAccessors.size()];
		int i = 0;
		for(ClassAccessorData accessor : classAccessors) {
			interfaces[i] = Type.getInternalName(accessor.getAccessorClass());
			i++;
		}
		clsNode.visit(clsNode.version, clsNode.access, clsNode.name, clsNode.signature, clsNode.superName, interfaces);

		//Instrument accessor methods
		for(ClassAccessorData classAccessor : classAccessors) {
			this.instrumentFieldAccessors(clsNode, classAccessor);
			this.instrumentFieldGenerators(clsNode, classAccessor);
			this.instrumentMethodAccessors(clsNode, classAccessor);
		}

		//Instrument interceptors
		this.instrumentMethodInterceptors(clsNode);
	}

	/**
	 * Instruments all registered field accessors
	 * @param clsNode
	 * @param classAccessor
	 */
	private void instrumentFieldAccessors(ClassNode clsNode, ClassAccessorData classAccessor) {
		for(FieldAccessorData fieldAccessor : classAccessor.getFieldAccessors()) {
			this.instrumentFieldAccessor(fieldAccessor, clsNode, classAccessor.getAccessorClass().getName());
		}
	}

	/**
	 * Instruments a single field accessor
	 * @param fieldAccessor
	 * @param clsNode
	 * @param accessorClass
	 */
	private void instrumentFieldAccessor(FieldAccessorData fieldAccessor, ClassNode clsNode, String accessorClass) {
		Method accessorMethod = fieldAccessor.getAccessorMethod();
		for(MethodNode methodNode : clsNode.methods) {
			if(methodNode.name.equals(accessorMethod.getName()) && methodNode.desc.equals(Type.getMethodDescriptor(accessorMethod))) {
				throw new FieldAccessorTakenException(String.format("Method for field accessor %s#%s is already taken", accessorClass, accessorMethod.getName() + Type.getMethodDescriptor(accessorMethod)), accessorClass, new MethodDescription(accessorMethod.getName(), Type.getMethodDescriptor(accessorMethod)));
			}
		}
		FieldNode targetField = null;
		IFieldIdentifier identifier = fieldAccessor.getFieldIdentifier();
		for(FieldNode fieldNode : clsNode.fields) {
			if(isIdentifiedField(identifier, fieldNode)) {
				if(targetField != null) {
					throw new MultipleFieldsIdentifiedException(accessorClass, new MethodDescription(accessorMethod.getName(), Type.getMethodDescriptor(accessorMethod)), fieldAccessor.getIdentifierId(), identifier);
				}
				if(fieldAccessor.isSetter()) {
					if(!this.isSetterTypeValidForField(fieldNode.desc, accessorMethod)) {
						throw new InvalidSetterTypeException(accessorClass, new MethodDescription(accessorMethod.getName(), Type.getMethodDescriptor(accessorMethod)), accessorMethod.getParameterTypes()[0].getName(), Type.getType(fieldNode.desc).getClassName());
					}
				} else {
					if(!this.isGetterTypeValidForField(fieldNode.desc, accessorMethod)) {
						throw new InvalidGetterTypeException(accessorClass, new MethodDescription(accessorMethod.getName(), Type.getMethodDescriptor(accessorMethod)), accessorMethod.getReturnType().getName(), Type.getType(fieldNode.desc).getClassName());
					}
				}
				targetField = fieldNode;
			}
		}
		if(targetField == null) {
			throw new FieldNotFoundException(accessorClass, new MethodDescription(accessorMethod.getName(), Type.getMethodDescriptor(accessorMethod)), fieldAccessor.getIdentifierId(), identifier);
		}
		MethodVisitor mv = clsNode.visitMethod(Opcodes.ACC_PUBLIC, accessorMethod.getName(), 
				Type.getMethodDescriptor(accessorMethod), 
				fieldAccessor.isSetter() ? null : Type.getDescriptor(accessorMethod.getReturnType()), null);
		if(fieldAccessor.isSetter()) {
			this.instrumentFieldSetter(mv, clsNode, targetField.name, targetField.desc, accessorMethod);
		} else {
			this.instrumentFieldGetter(mv, clsNode, targetField.name, targetField.desc, accessorMethod);
		}
	}

	/**
	 * Instruments all registered field generators
	 * @param clsNode
	 * @param classAccessor
	 */
	private void instrumentFieldGenerators(ClassNode clsNode, ClassAccessorData classAccessor) {
		for(FieldGeneratorData fieldGenerator : classAccessor.getFieldGenerators()) {
			this.instrumentFieldGenerator(fieldGenerator, clsNode, classAccessor.getAccessorClass().getName());
		}
	}

	/**
	 * Instruments a single field generator
	 * @param fieldGenerator
	 * @param clsNode
	 * @param accessorClass
	 */
	private void instrumentFieldGenerator(FieldGeneratorData fieldGenerator, ClassNode clsNode, String accessorClass) {
		Method accessorMethod = fieldGenerator.getAccessorMethod();
		for(MethodNode methodNode : clsNode.methods) {
			if(methodNode.name.equals(accessorMethod.getName()) && methodNode.desc.equals(Type.getMethodDescriptor(accessorMethod))) {
				throw new FieldAccessorTakenException(String.format("Method for field generator %s#%s is already taken", accessorClass, accessorMethod.getName() + Type.getMethodDescriptor(accessorMethod)), accessorClass, new MethodDescription(accessorMethod.getName(), Type.getMethodDescriptor(accessorMethod)));
			}
		}
		boolean generate = true;
		for(FieldNode field : clsNode.fields) {
			if(field.name.equals(fieldGenerator.getFieldName())) {
				if(!this.isTypeEqualOrAccessor(Type.getType(field.desc), fieldGenerator.getFieldType())) {
					throw new FieldGeneratorTakenException(String.format("Field %s for field generator %s#%s is already taken", fieldGenerator.getFieldName(), accessorClass, accessorMethod.getName() + Type.getMethodDescriptor(accessorMethod)), accessorClass, new MethodDescription(accessorMethod.getName(), Type.getMethodDescriptor(accessorMethod)), fieldGenerator.getFieldName());
				}
				generate = false;
			}
		}
		String fieldName = fieldGenerator.getFieldName();
		String fieldDesc = Type.getDescriptor(fieldGenerator.getFieldType());
		if(generate) {
			clsNode.visitField(Opcodes.ACC_PUBLIC, fieldName, fieldDesc, null, null);
		}
		MethodVisitor mv = clsNode.visitMethod(Opcodes.ACC_PUBLIC, accessorMethod.getName(), 
				Type.getMethodDescriptor(accessorMethod), 
				fieldGenerator.isSetter() ? null : Type.getDescriptor(accessorMethod.getReturnType()), null);
		if(fieldGenerator.isSetter()) {
			this.instrumentFieldSetter(mv, clsNode, fieldName, fieldDesc, accessorMethod);
		} else {
			this.instrumentFieldGetter(mv, clsNode, fieldName, fieldDesc, accessorMethod);
		}
	}

	/**
	 * Instruments all registered method accessors
	 * @param clsNode
	 * @param classAccessor
	 */
	private void instrumentMethodAccessors(ClassNode clsNode, ClassAccessorData classAccessor) {
		for(MethodAccessorData methodAccessor : classAccessor.getMethodAccessors()) {
			Method accessorMethod = methodAccessor.getAccessorMethod();
			MethodNode targetMethod = null;
			for(MethodNode methodNode : clsNode.methods) {
				if(methodNode.name.equals(accessorMethod.getName())) {
					throw new MethodAccessorTakenException(String.format("Method for method accessor %s#%s is already taken", classAccessor.getAccessorClass().getName(), accessorMethod.getName() + Type.getMethodDescriptor(accessorMethod)), classAccessor.getAccessorClass().getName(), new MethodDescription(accessorMethod.getName(), Type.getMethodDescriptor(accessorMethod)));
				}
			}
			IMethodIdentifier identifier = methodAccessor.getMethodIdentifier();
			for(MethodNode methodNode : clsNode.methods) {
				if(isIdentifiedMethod(identifier, methodNode)) {
					if(targetMethod != null) {
						throw new MultipleMethodsIdentifiedException(classAccessor.getAccessorClass().getName(), new MethodDescription(methodAccessor.getAccessorMethod().getName(), Type.getMethodDescriptor(methodAccessor.getAccessorMethod())), methodAccessor.getIdentifierId(), identifier);
					}
					if(!this.isMethodAccessorValid(methodNode.desc, accessorMethod)) {
						throw new InvalidMethodDescriptorException(String.format("Method accessor %s#%s descriptor does not match. Current: %s, Expected: %s, or accessors of those classes", classAccessor.getAccessorClass().getName(), accessorMethod.getName() + Type.getMethodDescriptor(accessorMethod), Type.getMethodDescriptor(accessorMethod), methodNode.desc), classAccessor.getAccessorClass().getName(), new MethodDescription(accessorMethod.getName(), Type.getMethodDescriptor(accessorMethod)), Type.getMethodDescriptor(accessorMethod), methodNode.desc);
					}
					targetMethod = methodNode;
				}
			}
			if(targetMethod == null) {
				throw new MethodNotFoundException(classAccessor.getAccessorClass().getName(), new MethodDescription(methodAccessor.getAccessorMethod().getName(), Type.getMethodDescriptor(methodAccessor.getAccessorMethod())), methodAccessor.getIdentifierId(), identifier);
			}
			Set<String> accessorExceptions = new HashSet<>();
			for(Class<?> exceptionType : accessorMethod.getExceptionTypes()) {
				accessorExceptions.add(Type.getInternalName(exceptionType));
			}
			Set<String> targetExceptions = new HashSet<>(targetMethod.exceptions);
			if(!targetExceptions.equals(accessorExceptions)) {
				String currExcp = Arrays.toString(accessorExceptions.toArray(new String[0]));
				String expectedExcp = Arrays.toString(targetExceptions.toArray(new String[0]));
				throw new InvalidMethodExceptionsException(String.format("Method accessor %s#%s exceptions do not match. Current: %s, Expected: %s", classAccessor.getAccessorClass().getName(), accessorMethod.getName() + Type.getMethodDescriptor(accessorMethod), currExcp, expectedExcp), classAccessor.getAccessorClass().getName(), new MethodDescription(accessorMethod.getName(), Type.getMethodDescriptor(accessorMethod)), currExcp, expectedExcp);
			}
			MethodVisitor mv = clsNode.visitMethod(Opcodes.ACC_PUBLIC, accessorMethod.getName(), 
					Type.getMethodDescriptor(accessorMethod), 
					Type.getDescriptor(accessorMethod.getReturnType()), targetExceptions.toArray(new String[0]));
			this.instrumentMethodCaller(mv, clsNode, targetMethod, accessorMethod, methodAccessor.isInterfaceMethod());
		}
	}

	/**
	 * Implements a method caller method in bytecode
	 * @param mv The method visitor of the class to be proxied
	 * @param clsNode The class node of the class to be proxied
	 * @param targetMethod The method node of the method to be proxied
	 * @param accessorMethod The proxy method
	 * @param isInterfaceMethod Whether the method to be proxied is from an interface
	 */
	private void instrumentMethodCaller(MethodVisitor mv, ClassNode clsNode, MethodNode targetMethod, Method accessorMethod, boolean isInterfaceMethod) {
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		int stackIndex = 1;
		int paramIndex = 0;
		Type[] targetMethodParams = Type.getArgumentTypes(targetMethod.desc);
		for(Class<?> param : accessorMethod.getParameterTypes()) {
			if(param.isPrimitive()) {
				if(param == int.class) {
					mv.visitVarInsn(Opcodes.ILOAD, stackIndex);
				} else if(param == boolean.class) {
					mv.visitVarInsn(Opcodes.ILOAD, stackIndex);
				} else if(param == byte.class) {
					mv.visitVarInsn(Opcodes.ILOAD, stackIndex);
				} else if(param == char.class) {
					mv.visitVarInsn(Opcodes.ILOAD, stackIndex);
				} else if(param == double.class) {
					mv.visitVarInsn(Opcodes.DLOAD, stackIndex);
				} else if(param == float.class) {
					mv.visitVarInsn(Opcodes.FLOAD, stackIndex);
				} else if(param == long.class) {
					mv.visitVarInsn(Opcodes.LLOAD, stackIndex);
				} else if(param == short.class) {
					mv.visitVarInsn(Opcodes.ILOAD, stackIndex);
				}
			} else {
				mv.visitVarInsn(Opcodes.ALOAD, stackIndex);
			}
			if(targetMethodParams[paramIndex].getSort() == Type.OBJECT) {
				//This makes sure that if an accessor is used as parameter it is cast properly
				mv.visitTypeInsn(Opcodes.CHECKCAST, targetMethodParams[paramIndex].getInternalName());
			}
			stackIndex += targetMethodParams[paramIndex].getSize();
			paramIndex++;
		}
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clsNode.name, targetMethod.name, targetMethod.desc, isInterfaceMethod);
		Class<?> returnType = accessorMethod.getReturnType();
		if(returnType == void.class || returnType == Void.class) {
			mv.visitInsn(Opcodes.RETURN);
		} else if(returnType.isPrimitive()) {
			if(returnType == int.class) {
				mv.visitInsn(Opcodes.IRETURN);
			} else if(returnType == boolean.class) {
				mv.visitInsn(Opcodes.IRETURN);
			} else if(returnType == byte.class) {
				mv.visitInsn(Opcodes.IRETURN);
			} else if(returnType == char.class) {
				mv.visitInsn(Opcodes.IRETURN);
			} else if(returnType == double.class) {
				mv.visitInsn(Opcodes.DRETURN);
			} else if(returnType == float.class) {
				mv.visitInsn(Opcodes.FRETURN);
			} else if(returnType == long.class) {
				mv.visitInsn(Opcodes.LRETURN);
			} else if(returnType == short.class) {
				mv.visitInsn(Opcodes.IRETURN);
			}
		} else {
			mv.visitInsn(Opcodes.ARETURN);
		}
		mv.visitMaxs(0, 0);
	}

	/**
	 * Implements a field getter in bytecode
	 * @param mv The method visitor of the class to be proxied
	 * @param clsNode The class node of the class to be proxied
	 * @param fieldName The field for which a getter is generated
	 * @param fieldDesc The desc of the field for which a getter is generated
	 * @param accessorMethod The proxy method
	 */
	private void instrumentFieldGetter(MethodVisitor mv, ClassNode clsNode, String fieldName, String fieldDesc, Method accessorMethod) {
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, clsNode.name, fieldName, fieldDesc);
		Class<?> returnType = accessorMethod.getReturnType();
		if(returnType.isPrimitive()) {
			if(returnType == int.class) {
				mv.visitInsn(Opcodes.IRETURN);
			} else if(returnType == boolean.class) {
				mv.visitInsn(Opcodes.IRETURN);
			} else if(returnType == byte.class) {
				mv.visitInsn(Opcodes.IRETURN);
			} else if(returnType == char.class) {
				mv.visitInsn(Opcodes.IRETURN);
			} else if(returnType == double.class) {
				mv.visitInsn(Opcodes.DRETURN);
			} else if(returnType == float.class) {
				mv.visitInsn(Opcodes.FRETURN);
			} else if(returnType == long.class) {
				mv.visitInsn(Opcodes.LRETURN);
			} else if(returnType == short.class) {
				mv.visitInsn(Opcodes.IRETURN);
			}
		} else {
			mv.visitInsn(Opcodes.ARETURN);
		}
		mv.visitMaxs(0, 0);
	}

	/**
	 * Implements a field setter in bytecode
	 * @param mv The method visitor of the class to be proxied
	 * @param clsNode The class node of the class to be proxied
	 * @param fieldName The field for which a setter is generated
	 * @param fieldDesc The desc of the field for which a setter is generated
	 * @param accessorMethod The proxy method
	 */
	private void instrumentFieldSetter(MethodVisitor mv, ClassNode clsNode, String fieldName, String fieldDesc, Method accessorMethod) {
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		Class<?> param = accessorMethod.getParameterTypes()[0];
		if(param.isPrimitive()) {
			if(param == int.class) {
				mv.visitVarInsn(Opcodes.ILOAD, 1);
			} else if(param == boolean.class) {
				mv.visitVarInsn(Opcodes.ILOAD, 1);
			} else if(param == byte.class) {
				mv.visitVarInsn(Opcodes.ILOAD, 1);
			} else if(param == char.class) {
				mv.visitVarInsn(Opcodes.ILOAD, 1);
			} else if(param == double.class) {
				mv.visitVarInsn(Opcodes.DLOAD, 1);
			} else if(param == float.class) {
				mv.visitVarInsn(Opcodes.FLOAD, 1);
			} else if(param == long.class) {
				mv.visitVarInsn(Opcodes.LLOAD, 1);
			} else if(param == short.class) {
				mv.visitVarInsn(Opcodes.ILOAD, 1);
			}
		} else {
			mv.visitVarInsn(Opcodes.ALOAD, 1);
		}
		if(Type.getType(fieldDesc).getSort() == Type.OBJECT) {
			//This makes sure that if an accessor is used as parameter it is cast properly
			mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getType(fieldDesc).getInternalName());
		}
		mv.visitFieldInsn(Opcodes.PUTFIELD, clsNode.name, fieldName, fieldDesc);
		if(!accessorMethod.getReturnType().equals(Void.TYPE)) {
			Type returnType = Type.getType(accessorMethod.getReturnType());
			mv.visitVarInsn(returnType.getOpcode(Opcodes.ILOAD), accessorMethod.getReturnType() == accessorMethod.getDeclaringClass() ? 0 /*return this*/ : 1 /*return parameter*/);
			mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
		} else {
			mv.visitInsn(Opcodes.RETURN);
		}
		mv.visitMaxs(0, 0);
	}

	/**
	 * Instruments all interceptors
	 * @param interceptor
	 * @param clsNode
	 */
	private void instrumentMethodInterceptors(ClassNode clsNode) {
		List<MethodInterceptorData> classInterceptors = new ArrayList<>();
		for(MethodInterceptorData interceptor : this.interceptors.values()) {
			if(isIdentifiedClass(interceptor.getClassIdentifier(), clsNode.name)) {
				classInterceptors.add(interceptor);
			}
		}

		//Implement setters and getters for LocalVars
		for(MethodInterceptorData interceptor : classInterceptors) {
			if(!interceptor.isReturn()) {
				for(LocalVarData localVar : interceptor.getLocalVars()) {
					Set<String> takenFieldNames = new HashSet<>();
					for(FieldNode field : clsNode.fields) {
						takenFieldNames.add(field.name);
					}
					String generatedField = this.getUniqueName(takenFieldNames);
					Type fieldType = Type.getArgumentTypes(interceptor.getInterceptorMethodDesc())[localVar.getParameterIndex()];

					clsNode.visitField(Opcodes.ACC_PRIVATE, generatedField, fieldType.getDescriptor(), null, null);

					//Setter
					MethodVisitor mvSetter = clsNode.visitMethod(Opcodes.ACC_PUBLIC, localVar.getGeneratedSetterMethod(), Type.getMethodDescriptor(Type.VOID_TYPE, Type.getArgumentTypes(localVar.getInterceptorMethodDesc())[localVar.getParameterIndex()]), null, null);
					mvSetter.visitVarInsn(Opcodes.ALOAD, 0);
					mvSetter.visitVarInsn(fieldType.getOpcode(Opcodes.ILOAD), 1);
					mvSetter.visitFieldInsn(Opcodes.PUTFIELD, clsNode.name, generatedField, fieldType.getDescriptor());
					mvSetter.visitInsn(Opcodes.RETURN);
					mvSetter.visitEnd();

					//Getter
					MethodVisitor mvGetter = clsNode.visitMethod(Opcodes.ACC_PUBLIC, localVar.getGeneratedGetterMethod(), Type.getMethodDescriptor(Type.getArgumentTypes(localVar.getInterceptorMethodDesc())[localVar.getParameterIndex()]), null, null);
					mvGetter.visitVarInsn(Opcodes.ALOAD, 0);
					mvGetter.visitFieldInsn(Opcodes.GETFIELD, clsNode.name, generatedField, fieldType.getDescriptor());
					mvGetter.visitInsn(fieldType.getOpcode(Opcodes.IRETURN));
					mvGetter.visitEnd();
				}
			}
		}

		Map<MethodInterceptorData, Object[]> interceptorInsertions = new HashMap<>();

		//Find insertion nodes for the interceptors
		for(MethodInterceptorData interceptor : classInterceptors) {
			MethodNode targetMethod = null;
			IMethodIdentifier identifier = interceptor.getMethodIdentifier();
			for(MethodNode method : clsNode.methods) {
				if(isIdentifiedMethod(identifier, method)) {
					if(targetMethod != null) {
						throw new MultipleMethodsIdentifiedException(interceptor.getAccessorClass(), new MethodDescription(interceptor.getInterceptorMethod(), interceptor.getInterceptorMethodDesc()), interceptor.getMethodIdentifierId(), identifier);
					}
					targetMethod = method;
				}
			}
			if(targetMethod == null) {
				throw new MethodNotFoundException(interceptor.getAccessorClass(), new MethodDescription(interceptor.getInterceptorMethod(), interceptor.getInterceptorMethodDesc()), interceptor.getMethodIdentifierId(), identifier);
			}

			int instructionIndex = interceptor.getInstructionIdentifier().identify(targetMethod);
			if(instructionIndex == -1) {
				throw new InstructionNotFoundException(interceptor.getAccessorClass(), new MethodDescription(interceptor.getInterceptorMethod(), interceptor.getInterceptorMethodDesc()), interceptor.getInstructionIdentifierId(), interceptor.getInstructionIdentifier());
			}
			if(instructionIndex < 0 || instructionIndex >= targetMethod.instructions.size()) {
				throw new InstructionOutOfBoundsException(String.format("Instruction index of %s#%s:%s is out of bounds. Current: %s, Expected: [%d, %d]", interceptor.getAccessorClass(), interceptor.getInterceptorMethod() + interceptor.getInterceptorMethodDesc(), interceptor.getInstructionIdentifierId(), instructionIndex, 0, targetMethod.instructions.size() - 1), null, instructionIndex, 0, targetMethod.instructions.size() - 1, clsNode.name, new MethodDescription(targetMethod.name, targetMethod.desc), interceptor.getInstructionIdentifierId(), interceptor.getInstructionIdentifier());
			}

			int jumpInstructionIndex = -1;
			if(interceptor.getJumpInstructionIdentifier() != null) {
				if(Type.getReturnType(interceptor.getInterceptorMethodDesc()) != Type.BOOLEAN_TYPE) {
					throw new InvalidReturnTypeException(String.format("Return type of method interceptor %s#%s with instruction jump is not boolean", clsNode.name, targetMethod.name + targetMethod.desc), null, clsNode.name, new MethodDescription(targetMethod.name, targetMethod.desc), boolean.class.getName(), Type.getReturnType(interceptor.getInterceptorMethodDesc()).getClassName());
				}
				jumpInstructionIndex = interceptor.getJumpInstructionIdentifier().identify(targetMethod);
				if(jumpInstructionIndex == -1) {
					throw new JumpInstructionNotFoundException(clsNode.name, new MethodDescription(targetMethod.name, targetMethod.desc), interceptor.getJumpInstructionIdentifierId(), interceptor.getJumpInstructionIdentifier());
				}
				if(jumpInstructionIndex < 0 || jumpInstructionIndex >= targetMethod.instructions.size()) {
					throw new InstructionOutOfBoundsException(String.format("Instruction jump index of %s#%s:%s is out of bounds. Current: %s, Expected: [%d, %d]", clsNode.name, targetMethod.name + targetMethod.desc, interceptor.getJumpInstructionIdentifierId(), jumpInstructionIndex, 0, targetMethod.instructions.size() - 1), null, jumpInstructionIndex, 0, targetMethod.instructions.size() - 1, clsNode.name, new MethodDescription(targetMethod.name, targetMethod.desc), interceptor.getJumpInstructionIdentifierId(), interceptor.getJumpInstructionIdentifier());
				}
				if(jumpInstructionIndex <= instructionIndex) {
					throw new InstructionOutOfBoundsException(String.format("Instruction jump index of %s#%s:%s must be after the interceptor instruction index. Current: %s, Expected: >%d", clsNode.name, targetMethod.name + targetMethod.desc, interceptor.getJumpInstructionIdentifierId(), jumpInstructionIndex, instructionIndex), null, jumpInstructionIndex, instructionIndex + 1, targetMethod.instructions.size() - 1, clsNode.name, new MethodDescription(targetMethod.name, targetMethod.desc), interceptor.getJumpInstructionIdentifierId(), interceptor.getJumpInstructionIdentifier());
				}
			}

			interceptorInsertions.put(interceptor, new Object[]{targetMethod, targetMethod.instructions.get(instructionIndex), jumpInstructionIndex == -1 ? null : targetMethod.instructions.get(jumpInstructionIndex)});
		}

		//Insert instructions for the interceptors at the insertion node
		for(Entry<MethodInterceptorData, Object[]> interceptorInsertion : interceptorInsertions.entrySet()) {
			MethodInterceptorData interceptor = interceptorInsertion.getKey();
			MethodNode targetMethod = (MethodNode) interceptorInsertion.getValue()[0];
			AbstractInsnNode insertionNode = (AbstractInsnNode) interceptorInsertion.getValue()[1];
			AbstractInsnNode jumpNode = (AbstractInsnNode) interceptorInsertion.getValue()[2];

			InsnList insertions = new InsnList();

			insertions.add(new VarInsnNode(Opcodes.ALOAD, 0));

			//Load local variables
			for(LocalVarData importData : interceptor.getLocalVars()) {
				int importLocalVariableIndex = importData.getInstructionIdentifier().identify(targetMethod);
				LocalVariableNode importLocalVariable = null;
				for(LocalVariableNode localVar : targetMethod.localVariables) {
					if(localVar.index == importLocalVariableIndex) {
						importLocalVariable = localVar;
					}
				}
				if(importLocalVariable == null) {
					throw new ImportInstructionNotFoundException(importData.getParameterIndex(), interceptor.getAccessorClass(), new MethodDescription(interceptor.getInterceptorMethod(), interceptor.getInterceptorMethodDesc()), importData.getInstructionIdentifierId(), importData.getInstructionIdentifier());
				}
				Type localVarType = Type.getType(importLocalVariable.desc);
				Type paramType = Type.getArgumentTypes(interceptor.getInterceptorMethodDesc())[importData.getParameterIndex()];
				ClassAccessorData paramAsAccessor = this.accessors.getAccessorByClassName(paramType.getClassName());
				if((paramAsAccessor != null && !this.isTypeEqualOrAccessor(localVarType, paramAsAccessor.getAccessorClass())) || (paramAsAccessor == null && !paramType.equals(localVarType))) {
					throw new InvalidParameterTypeException(String.format("Import parameter %d of method %s#%s does not match. Current: %s, Expected: %s, or an accessor of that class", importData.getParameterIndex(), interceptor.getAccessorClass(), interceptor.getInterceptorMethod() + interceptor.getInterceptorMethodDesc(), paramType.getClassName(), Type.getType(importLocalVariable.desc).getClassName()), interceptor.getAccessorClass(), new MethodDescription(interceptor.getInterceptorMethod(), interceptor.getInterceptorMethodDesc()), importData.getParameterIndex(), paramType.getClassName(), Type.getType(importLocalVariable.desc).getClassName());
				}
				insertions.add(new VarInsnNode(paramType.getOpcode(Opcodes.ILOAD), importLocalVariable.index));
			}

			//Call interceptor method
			insertions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, interceptor.getAccessorClass().replace('.', '/'), interceptor.getInterceptorMethod(), interceptor.getInterceptorMethodDesc(), true));

			if(interceptor.isReturn()) {
				//Add return instruction
				Type returnType = Type.getReturnType(targetMethod.desc);
				Type interceptorReturnType = Type.getReturnType(interceptor.getInterceptorMethodDesc());
				ClassAccessorData paramAsAccessor = this.accessors.getAccessorByClassName(interceptorReturnType.getClassName());
				if((paramAsAccessor != null && !this.isTypeEqualOrAccessor(returnType, paramAsAccessor.getAccessorClass())) || (paramAsAccessor == null && !returnType.equals(interceptorReturnType))) {
					throw new InvalidReturnTypeException(String.format("Return type of method interceptor for method %s#%s does not match. Current: %s, Expected: %s, or an accessor of that class", interceptor.getAccessorClass(), interceptor.getInterceptorMethod() + interceptor.getInterceptorMethodDesc(), interceptorReturnType.getClassName(), returnType.getClassName()), null, interceptor.getAccessorClass(), new MethodDescription(interceptor.getInterceptorMethod(), interceptor.getInterceptorMethodDesc()), interceptorReturnType.getClassName(), returnType.getClassName());
				}
				if(paramAsAccessor != null) {
					//Interceptor return type is an accessor, cast to the intercepted method return type
					insertions.add(new TypeInsnNode(Opcodes.CHECKCAST, returnType.getDescriptor()));
				}
				insertions.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
			} else {
				//Store local variables
				for(LocalVarData importData : interceptor.getLocalVars()) {
					int importLocalVariableIndex = importData.getInstructionIdentifier().identify(targetMethod);
					LocalVariableNode importLocalVariable = null;
					for(LocalVariableNode localVar : targetMethod.localVariables) {
						if(localVar.index == importLocalVariableIndex) {
							importLocalVariable = localVar;
						}
					}
					if(importLocalVariable == null) {
						throw new ImportInstructionNotFoundException(importData.getParameterIndex(), interceptor.getAccessorClass(), new MethodDescription(interceptor.getInterceptorMethod(), interceptor.getInterceptorMethodDesc()), importData.getInstructionIdentifierId(), importData.getInstructionIdentifier());
					}
					Type localVarType = Type.getType(importLocalVariable.desc);
					Type paramType = Type.getArgumentTypes(importData.getInterceptorMethodDesc())[importData.getParameterIndex()];
					insertions.add(new VarInsnNode(Opcodes.ALOAD, 0));
					insertions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, clsNode.name, importData.getGeneratedGetterMethod(), Type.getMethodDescriptor(paramType), false));
					if(!paramType.equals(localVarType)) {
						//Parameter type is an accessor, cast to the local variable type
						insertions.add(new TypeInsnNode(Opcodes.CHECKCAST, localVarType.getInternalName()));
					}
					insertions.add(new VarInsnNode(paramType.getOpcode(Opcodes.ISTORE), importLocalVariable.index));
				}

				if(jumpNode != null) {
					LabelNode jumpTarget = new LabelNode();

					int jumpNodeIndex = targetMethod.instructions.indexOf(jumpNode);

					Analyzer<BasicValue> a = new Analyzer<>(new BasicInterpreter());
					try {
						Frame<BasicValue>[] frames = a.analyze(clsNode.name, targetMethod);
						Frame<BasicValue> stackFrame = frames[jumpNodeIndex];
						if(stackFrame.getStackSize() > 0) {
							StringBuilder stackStr = new StringBuilder();
							stackStr.append("[");
							for(int i = 0; i < stackFrame.getStackSize(); i++) {
								stackStr.append(stackFrame.getStack(i).getType().getClassName());
								if(i != stackFrame.getStackSize() - 1) {
									stackStr.append(", ");
								}
							}
							stackStr.append("]");
							throw new InvalidJumpTargetException(String.format("Cannot insert jump target of %s#%s:%s at index %d. Stack must be empty. Current stack: %s", clsNode.name, targetMethod.name + targetMethod.desc, interceptorInsertion.getKey().getJumpInstructionIdentifierId(), jumpNodeIndex, stackStr.toString()), jumpNodeIndex, clsNode.name, new MethodDescription(targetMethod.name, targetMethod.desc), interceptorInsertion.getKey().getJumpInstructionIdentifierId(), interceptorInsertion.getKey().getJumpInstructionIdentifier());
						}
					} catch (AnalyzerException ex) {
						throw new InvalidJumpTargetException(String.format("Cannot insert jump target of %s#%s:%s at index %d due to an unknown reason", clsNode.name, targetMethod.name + targetMethod.desc, interceptorInsertion.getKey().getJumpInstructionIdentifierId(), jumpNodeIndex), ex, jumpNodeIndex, clsNode.name, new MethodDescription(targetMethod.name, targetMethod.desc), interceptorInsertion.getKey().getJumpInstructionIdentifierId(), interceptorInsertion.getKey().getJumpInstructionIdentifier());
					}

					//Insert jump target
					targetMethod.instructions.insertBefore(jumpNode, jumpTarget);

					insertions.add(new JumpInsnNode(Opcodes.IFNE, jumpTarget));
				}
			}

			//Insert instructions
			targetMethod.instructions.insertBefore(insertionNode, insertions);
		}
	}

	/**
	 * Checks if the method accessor parameters and return types are valid for the specified desc of the method
	 * @param desc The desc of the original method
	 * @param method The proxy method
	 * @return
	 */
	private boolean isMethodAccessorValid(String desc, Method method) {
		Type returnType = Type.getReturnType(desc);
		if(!this.isTypeEqualOrAccessor(returnType, method.getReturnType())) {
			return false;
		}
		Type[] params = Type.getArgumentTypes(desc);
		if(params.length != method.getParameterTypes().length) {
			return false;
		}
		for(int i = 0; i < params.length; i++) {
			if(!this.isTypeEqualOrAccessor(params[i], method.getParameterTypes()[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if the getter type is valid for the specified desc of the field
	 * @param desc The desc of the original field
	 * @param method The proxy method
	 * @return
	 */
	private boolean isGetterTypeValidForField(String desc, Method method) {
		return this.isTypeEqualOrAccessor(Type.getType(desc), method.getReturnType());
	}

	/**
	 * Checks if the setter type is valid for the specified desc of the field
	 * @param desc The desc of the original field
	 * @param method The proxy method
	 * @return
	 */
	private boolean isSetterTypeValidForField(String desc, Method method) {
		return this.isTypeEqualOrAccessor(Type.getType(desc), method.getParameterTypes()[0]);
	}

	/**
	 * Compares two specified types if they are equal or a corresponding {@link IAccessor}.
	 * If cls is an {@link IAccessor} a check is run if
	 * that accessor was registered and belongs to the first specified type.
	 * @param type The type of the original, to be proxied, code
	 * @param cls The type of the proxy's method
	 * @return
	 */
	private boolean isTypeEqualOrAccessor(Type type, Class<?> cls) {
		if(cls != null && type.getSort() == Type.OBJECT && IAccessor.class.isAssignableFrom(cls)) {
			ClassAccessor classAccessor = cls.getAnnotation(ClassAccessor.class);
			if(classAccessor == null) {
				return false;
			}
			ClassAccessorData accessorInstance = this.accessors.getAccessorById(classAccessor.classIdentifierId());
			if(accessorInstance == null) {
				return false;
			}
			if(isIdentifiedClass(accessorInstance.getClassIdentifier(), type.getInternalName())) {
				return true;
			}
		}
		return type.getClassName().equals(cls.getName());
	}

	/**
	 * Returns whether the specified method is a generated method
	 * @return
	 */
	public boolean isGeneratedMethod(Method method) {
		for(MethodInterceptorData interceptor : this.interceptors.values()) {
			if(interceptor.getAccessorClass().equals(method.getDeclaringClass().getName())) {
				for(LocalVarData localVar : interceptor.getLocalVars()) {
					Type localVarType = Type.getArgumentTypes(localVar.getInterceptorMethodDesc())[localVar.getParameterIndex()];
					boolean isGetter = 
							localVar.getGeneratedGetterMethod().equals(method.getName()) && 
							Type.getMethodDescriptor(localVarType).equals(Type.getMethodDescriptor(method));
					boolean isSetter = 
							localVar.getGeneratedSetterMethod().equals(method.getName()) && 
							Type.getMethodDescriptor(Type.VOID_TYPE, localVarType).equals(Type.getMethodDescriptor(method));
					if(isGetter || isSetter) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
