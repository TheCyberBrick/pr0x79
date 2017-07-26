package pr0x79.instrumentation;

import java.io.IOException;
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
import pr0x79.instrumentation.accessor.ClassAccessorData;
import pr0x79.instrumentation.accessor.ClassAccessorData.FieldAccessorData;
import pr0x79.instrumentation.accessor.ClassAccessorData.FieldGeneratorData;
import pr0x79.instrumentation.accessor.ClassAccessorData.MethodAccessorData;
import pr0x79.instrumentation.accessor.IAccessor;
import pr0x79.instrumentation.accessor.LocalVarData;
import pr0x79.instrumentation.accessor.MethodInterceptorData;
import pr0x79.instrumentation.exception.accessor.ClassRelationResolverException;
import pr0x79.instrumentation.exception.accessor.field.FieldAccessorTakenException;
import pr0x79.instrumentation.exception.accessor.field.InvalidGetterTypeException;
import pr0x79.instrumentation.exception.accessor.field.InvalidSetterTypeException;
import pr0x79.instrumentation.exception.accessor.fieldgenerator.FieldGeneratorTakenException;
import pr0x79.instrumentation.exception.accessor.method.InvalidMethodDescriptorException;
import pr0x79.instrumentation.exception.accessor.method.InvalidMethodExceptionsException;
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

/**
 * Instruments classes using the registered {@link IAccessor}s
 */
public class BytecodeInstrumentation {
	private Accessors accessors;

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
			if(accessor.getClassIdentifier() != null && isIdentifiedClass(accessor.getClassIdentifier(), cls)) {
				return true;
			}

			for(MethodInterceptorData interceptor : accessor.getMethodInterceptors()) {
				if(interceptor.getClassIdentifier() != null && isIdentifiedClass(interceptor.getClassIdentifier(), cls)) {
					return true;
				}
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
		ClassAccessorData accessor = this.accessors.getAccessorByClassName(Type.getObjectType(clsNode.name).getClassName());

		if(accessor != null) {
			List<LocalVarData> fieldRequiringLocalVars = new ArrayList<>();
			for(MethodInterceptorData interceptor : accessor.getMethodInterceptors()) {
				if(!interceptor.isReturn()) {
					for(MethodNode method : clsNode.methods) {
						if(interceptor.getInterceptorMethod().equals(method.name) && interceptor.getInterceptorMethodDesc().equals(method.desc)) {
							List<LocalVarData> methodLocalVars = interceptor.getLocalVars();
							Type[] params = Type.getArgumentTypes(method.desc);
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
						}
					}

					fieldRequiringLocalVars.addAll(interceptor.getLocalVars());
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

		return false;
	}

	/**
	 * Gets the specified value of an {@link AnnotationNode}
	 * @param annotations
	 * @param annotation
	 * @param name
	 * @param type
	 * @return
	 */
	public static <T> T getAnnotationValue(List<AnnotationNode> annotations, Class<?> annotation, String name, Class<T> type, T defaultVal) {
		if(annotations != null) {
			for(AnnotationNode ann : annotations) {
				if(ann.desc.equals(Type.getDescriptor(annotation))) {
					T val = getAnnotationValue(ann, name, type);
					if(val != null) {
						return val;
					}
					return defaultVal;
				}
			}
		}
		return defaultVal;
	}

	/**
	 * Gets the specified value of an {@link AnnotationNode}
	 * @param annotation
	 * @param name
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getAnnotationValue(AnnotationNode annotation, String name, Class<T> type) {
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
			interfaces[i] = accessor.getAccessorClass().replace('.', '/');
			i++;
		}

		clsNode.visit(clsNode.version, clsNode.access, clsNode.name, clsNode.signature, clsNode.superName, interfaces);

		//Instrument accessor and interceptor methods
		for(ClassAccessorData classAccessor : classAccessors) {
			this.instrumentFieldAccessors(clsNode, classAccessor);
			this.instrumentFieldGenerators(clsNode, classAccessor);
			this.instrumentMethodAccessors(clsNode, classAccessor);
			this.instrumentMethodInterceptors(clsNode, classAccessor);
		}
	}

	/**
	 * Instruments all registered field accessors
	 * @param clsNode
	 * @param classAccessor
	 */
	private void instrumentFieldAccessors(ClassNode clsNode, ClassAccessorData classAccessor) {
		for(FieldAccessorData fieldAccessor : classAccessor.getFieldAccessors()) {
			this.instrumentFieldAccessor(fieldAccessor, clsNode, classAccessor.getAccessorClass());
		}
	}

	/**
	 * Instruments a single field accessor
	 * @param fieldAccessor
	 * @param clsNode
	 * @param accessorClass
	 */
	private void instrumentFieldAccessor(FieldAccessorData fieldAccessor, ClassNode clsNode, String accessorClass) {
		MethodNode accessorMethod = fieldAccessor.getAccessorMethod();
		Type[] accessorParams = Type.getArgumentTypes(accessorMethod.desc);
		Type accessorReturnType = Type.getReturnType(accessorMethod.desc);
		for(MethodNode methodNode : clsNode.methods) {
			if(methodNode.name.equals(accessorMethod.name) && methodNode.desc.equals(accessorMethod.desc)) {
				throw new FieldAccessorTakenException(String.format("Method for field accessor %s#%s is already taken", accessorClass, accessorMethod.name + accessorMethod.desc), accessorClass, new MethodDescription(accessorMethod.name, accessorMethod.desc));
			}
		}
		FieldNode targetField = null;
		IFieldIdentifier identifier = fieldAccessor.getFieldIdentifier();
		for(FieldNode fieldNode : clsNode.fields) {
			if(isIdentifiedField(identifier, fieldNode)) {
				if(targetField != null) {
					throw new MultipleFieldsIdentifiedException(accessorClass, new MethodDescription(accessorMethod.name, accessorMethod.desc), fieldAccessor.getIdentifierId(), identifier);
				}
				if(fieldAccessor.isSetter()) {
					if(!this.isSetterTypeValidForField(fieldNode.desc, accessorMethod)) {
						throw new InvalidSetterTypeException(accessorClass, new MethodDescription(accessorMethod.name, accessorMethod.desc), accessorParams[0].getClassName(), Type.getType(fieldNode.desc).getClassName());
					}
				} else {
					if(!this.isGetterTypeValidForField(fieldNode.desc, accessorMethod)) {
						throw new InvalidGetterTypeException(accessorClass, new MethodDescription(accessorMethod.name, accessorMethod.desc), accessorReturnType.getClassName(), Type.getType(fieldNode.desc).getClassName());
					}
				}
				targetField = fieldNode;
			}
		}
		if(targetField == null) {
			throw new FieldNotFoundException(accessorClass, new MethodDescription(accessorMethod.name, accessorMethod.desc), fieldAccessor.getIdentifierId(), identifier);
		}
		MethodVisitor mv = clsNode.visitMethod(Opcodes.ACC_PUBLIC, accessorMethod.name, 
				accessorMethod.desc, 
				fieldAccessor.isSetter() ? null : accessorReturnType.getDescriptor(), null);
		if(fieldAccessor.isSetter()) {
			this.instrumentFieldSetter(mv, clsNode, targetField.name, targetField.desc, accessorMethod, accessorClass);
		} else {
			this.instrumentFieldGetter(mv, clsNode, targetField.name, targetField.desc, accessorMethod, accessorClass);
		}
	}

	/**
	 * Instruments all registered field generators
	 * @param clsNode
	 * @param classAccessor
	 */
	private void instrumentFieldGenerators(ClassNode clsNode, ClassAccessorData classAccessor) {
		for(FieldGeneratorData fieldGenerator : classAccessor.getFieldGenerators()) {
			this.instrumentFieldGenerator(fieldGenerator, clsNode, classAccessor.getAccessorClass());
		}
	}

	/**
	 * Instruments a single field generator
	 * @param fieldGenerator
	 * @param clsNode
	 * @param accessorClass
	 */
	private void instrumentFieldGenerator(FieldGeneratorData fieldGenerator, ClassNode clsNode, String accessorClass) {
		MethodNode accessorMethod = fieldGenerator.getAccessorMethod();
		Type accessorReturnType = Type.getReturnType(accessorMethod.desc);
		for(MethodNode methodNode : clsNode.methods) {
			if(methodNode.name.equals(accessorMethod.name) && methodNode.desc.equals(accessorMethod.desc)) {
				throw new FieldAccessorTakenException(String.format("Method for field generator %s#%s is already taken", accessorClass, accessorMethod.name + accessorMethod.desc), accessorClass, new MethodDescription(accessorMethod.name, accessorMethod.desc));
			}
		}
		boolean generate = true;
		for(FieldNode field : clsNode.fields) {
			if(field.name.equals(fieldGenerator.getFieldName())) {
				if(!this.isTypeInstanceof(Type.getType(field.desc), fieldGenerator.getFieldType())) {
					throw new FieldGeneratorTakenException(String.format("Field %s for field generator %s#%s is already taken", fieldGenerator.getFieldName(), accessorClass, accessorMethod.name + accessorMethod.desc), accessorClass, new MethodDescription(accessorMethod.name, accessorMethod.desc), fieldGenerator.getFieldName());
				}
				generate = false;
			}
		}

		String fieldName = fieldGenerator.getFieldName();
		String fieldDesc = fieldGenerator.getFieldType().getDescriptor();
		if(generate) {
			clsNode.visitField(Opcodes.ACC_PUBLIC, fieldName, fieldDesc, null, null);
		}
		MethodVisitor mv = clsNode.visitMethod(Opcodes.ACC_PUBLIC, accessorMethod.name, 
				accessorMethod.desc, 
				fieldGenerator.isSetter() ? null : accessorReturnType.getDescriptor(), null);
		if(fieldGenerator.isSetter()) {
			this.instrumentFieldSetter(mv, clsNode, fieldName, fieldDesc, accessorMethod, accessorClass);
		} else {
			this.instrumentFieldGetter(mv, clsNode, fieldName, fieldDesc, accessorMethod, accessorClass);
		}
	}

	/**
	 * Instruments all registered method accessors
	 * @param clsNode
	 * @param classAccessor
	 */
	private void instrumentMethodAccessors(ClassNode clsNode, ClassAccessorData classAccessor) {
		for(MethodAccessorData methodAccessor : classAccessor.getMethodAccessors()) {
			MethodNode accessorMethod = methodAccessor.getAccessorMethod();
			MethodNode targetMethod = null;
			for(MethodNode methodNode : clsNode.methods) {
				if(methodNode.name.equals(accessorMethod.name)) {
					throw new MethodAccessorTakenException(String.format("Method for method accessor %s#%s is already taken", classAccessor.getAccessorClass(), accessorMethod.name + accessorMethod.desc), classAccessor.getAccessorClass(), new MethodDescription(accessorMethod.name, accessorMethod.desc));
				}
			}
			IMethodIdentifier identifier = methodAccessor.getMethodIdentifier();
			for(MethodNode methodNode : clsNode.methods) {
				if(isIdentifiedMethod(identifier, methodNode)) {
					if(targetMethod != null) {
						throw new MultipleMethodsIdentifiedException(classAccessor.getAccessorClass(), new MethodDescription(methodAccessor.getAccessorMethod().name, accessorMethod.desc), methodAccessor.getIdentifierId(), identifier);
					}
					if(!this.isMethodAccessorValid(methodNode.desc, accessorMethod)) {
						throw new InvalidMethodDescriptorException(String.format("Method accessor %s#%s descriptor does not match. Current: %s, Expected: %s, or accessors of those classes", classAccessor.getAccessorClass(), accessorMethod.name + accessorMethod.desc, accessorMethod.desc, methodNode.desc), classAccessor.getAccessorClass(), new MethodDescription(accessorMethod.name, accessorMethod.desc), accessorMethod.desc, methodNode.desc);
					}
					targetMethod = methodNode;
				}
			}
			if(targetMethod == null) {
				throw new MethodNotFoundException(classAccessor.getAccessorClass(), new MethodDescription(methodAccessor.getAccessorMethod().name, accessorMethod.desc), methodAccessor.getIdentifierId(), identifier);
			}
			Set<String> accessorExceptions = new HashSet<>();
			accessorExceptions.addAll(accessorMethod.exceptions);
			Set<String> targetExceptions = new HashSet<>(targetMethod.exceptions);
			if(!targetExceptions.equals(accessorExceptions)) {
				String currExcp = Arrays.toString(accessorExceptions.toArray(new String[0]));
				String expectedExcp = Arrays.toString(targetExceptions.toArray(new String[0]));
				throw new InvalidMethodExceptionsException(String.format("Method accessor %s#%s exceptions do not match. Current: %s, Expected: %s", classAccessor.getAccessorClass(), accessorMethod.name + accessorMethod.desc, currExcp, expectedExcp), classAccessor.getAccessorClass(), new MethodDescription(accessorMethod.name, accessorMethod.desc), currExcp, expectedExcp);
			}
			MethodVisitor mv = clsNode.visitMethod(Opcodes.ACC_PUBLIC, accessorMethod.name, 
					accessorMethod.desc, 
					accessorMethod.signature, targetExceptions.toArray(new String[0]));
			this.instrumentMethodCaller(mv, clsNode, targetMethod, accessorMethod, (clsNode.access & Opcodes.ACC_INTERFACE) != 0);
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
	private void instrumentMethodCaller(MethodVisitor mv, ClassNode clsNode, MethodNode targetMethod, MethodNode accessorMethod, boolean isInterfaceMethod) {
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		int stackIndex = 1;
		int paramIndex = 0;
		Type[] params = Type.getArgumentTypes(accessorMethod.desc);
		Type[] targetMethodParams = Type.getArgumentTypes(targetMethod.desc);
		for(Type param : params) {
			switch(param.getSort()) {
			case Type.INT:
			case Type.BOOLEAN:
			case Type.BYTE:
			case Type.CHAR:
			case Type.SHORT:
				mv.visitVarInsn(Opcodes.ILOAD, stackIndex);
				break;
			case Type.LONG:
				mv.visitVarInsn(Opcodes.LLOAD, stackIndex);
				break;
			case Type.DOUBLE:
				mv.visitVarInsn(Opcodes.DLOAD, stackIndex);
				break;
			case Type.FLOAT:
				mv.visitVarInsn(Opcodes.FLOAD, stackIndex);
				break;
			default:
				mv.visitVarInsn(Opcodes.ALOAD, stackIndex);
				break;
			}
			if(targetMethodParams[paramIndex].getSort() == Type.OBJECT) {
				//This makes sure that if an accessor is used as parameter it is cast properly
				mv.visitTypeInsn(Opcodes.CHECKCAST, targetMethodParams[paramIndex].getInternalName());
			}
			stackIndex += targetMethodParams[paramIndex].getSize();
			paramIndex++;
		}
		mv.visitMethodInsn(isInterfaceMethod ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, clsNode.name, targetMethod.name, targetMethod.desc, isInterfaceMethod);
		Type returnType = Type.getReturnType(accessorMethod.desc);
		if(returnType.getSort() == Type.VOID) {
			mv.visitInsn(Opcodes.RETURN);
		} else {
			switch(returnType.getSort()) {
			case Type.INT:
			case Type.BOOLEAN:
			case Type.BYTE:
			case Type.CHAR:
			case Type.SHORT:
				mv.visitInsn(Opcodes.IRETURN);
				break;
			case Type.LONG:
				mv.visitInsn(Opcodes.LRETURN);
				break;
			case Type.DOUBLE:
				mv.visitInsn(Opcodes.DRETURN);
				break;
			case Type.FLOAT:
				mv.visitInsn(Opcodes.FRETURN);
				break;
			default:
				mv.visitInsn(Opcodes.ARETURN);
				break;
			}
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
	private void instrumentFieldGetter(MethodVisitor mv, ClassNode clsNode, String fieldName, String fieldDesc, MethodNode accessorMethod, String accessorClass) {
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, clsNode.name, fieldName, fieldDesc);
		Type returnType = Type.getReturnType(accessorMethod.desc);
		switch(returnType.getSort()) {
		case Type.INT:
		case Type.BOOLEAN:
		case Type.BYTE:
		case Type.CHAR:
		case Type.SHORT:
			mv.visitInsn(Opcodes.IRETURN);
			break;
		case Type.LONG:
			mv.visitInsn(Opcodes.LRETURN);
			break;
		case Type.DOUBLE:
			mv.visitInsn(Opcodes.DRETURN);
			break;
		case Type.FLOAT:
			mv.visitInsn(Opcodes.FRETURN);
			break;
		default:
			mv.visitInsn(Opcodes.ARETURN);
			break;
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
	private void instrumentFieldSetter(MethodVisitor mv, ClassNode clsNode, String fieldName, String fieldDesc, MethodNode accessorMethod, String accessorClass) {
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		Type param = Type.getArgumentTypes(accessorMethod.desc)[0];
		Type returnType = Type.getReturnType(accessorMethod.desc);
		switch(param.getSort()) {
		case Type.INT:
		case Type.BOOLEAN:
		case Type.BYTE:
		case Type.CHAR:
		case Type.SHORT:
			mv.visitVarInsn(Opcodes.ILOAD, 1);
			break;
		case Type.LONG:
			mv.visitVarInsn(Opcodes.LLOAD, 1);
			break;
		case Type.DOUBLE:
			mv.visitVarInsn(Opcodes.DLOAD, 1);
			break;
		case Type.FLOAT:
			mv.visitVarInsn(Opcodes.FLOAD, 1);
			break;
		default:
			mv.visitVarInsn(Opcodes.ALOAD, 1);
			break;
		}
		if(Type.getType(fieldDesc).getSort() == Type.OBJECT) {
			//This makes sure that if an accessor is used as parameter it is cast properly
			mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getType(fieldDesc).getInternalName());
		}
		mv.visitFieldInsn(Opcodes.PUTFIELD, clsNode.name, fieldName, fieldDesc);
		if(returnType.getSort() != Type.VOID) {
			mv.visitVarInsn(returnType.getOpcode(Opcodes.ILOAD), returnType.getClassName().equals(accessorClass) ? 0 /*return this*/ : 1 /*return parameter*/);
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
	private void instrumentMethodInterceptors(ClassNode clsNode, ClassAccessorData classAccessor) {
		List<MethodInterceptorData> classInterceptors = new ArrayList<>();
		for(MethodInterceptorData interceptor : classAccessor.getMethodInterceptors()) {
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
					String generatedField = getUniqueName(takenFieldNames);
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
				if((paramAsAccessor != null && !this.isTypeInstanceof(localVarType, Type.getObjectType(paramAsAccessor.getAccessorClass().replace('.', '/')))) || (paramAsAccessor == null && !paramType.equals(localVarType))) {
					throw new InvalidParameterTypeException(String.format("Import parameter %d of method %s#%s does not match. Current: %s, Expected: %s, or an accessor of that class. Local variable index: %d. Local variable identifier: %s", importData.getParameterIndex(), interceptor.getAccessorClass(), interceptor.getInterceptorMethod() + interceptor.getInterceptorMethodDesc(), paramType.getClassName(), Type.getType(importLocalVariable.desc).getClassName(), importLocalVariable.index, importData.getInstructionIdentifierId()), interceptor.getAccessorClass(), new MethodDescription(interceptor.getInterceptorMethod(), interceptor.getInterceptorMethodDesc()), importData.getParameterIndex(), paramType.getClassName(), Type.getType(importLocalVariable.desc).getClassName());
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
				if((paramAsAccessor != null && !this.isTypeInstanceof(returnType, Type.getObjectType(paramAsAccessor.getAccessorClass().replace('.', '/')))) || (paramAsAccessor == null && !returnType.equals(interceptorReturnType))) {
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
	 * Generates a unique name with the specified exclusions
	 * @param exclusions
	 * @return
	 */
	public static String getUniqueName(Set<String> exclusions) {
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
	 * Checks if the method accessor parameters and return types are valid for the specified desc of the method
	 * @param desc The desc of the original method
	 * @param method The proxy method
	 * @return
	 */
	private boolean isMethodAccessorValid(String desc, MethodNode method) {
		Type returnType = Type.getReturnType(desc);
		if(!this.isTypeInstanceof(returnType, Type.getReturnType(method.desc))) {
			return false;
		}
		Type[] methodParams = Type.getArgumentTypes(method.desc);
		Type[] descParams = Type.getArgumentTypes(desc);
		if(descParams.length != methodParams.length) {
			return false;
		}
		for(int i = 0; i < descParams.length; i++) {
			if(!this.isTypeInstanceof(descParams[i], methodParams[i])) {
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
	private boolean isGetterTypeValidForField(String desc, MethodNode method) {
		return this.isTypeInstanceof(Type.getType(desc), Type.getReturnType(method.desc));
	}

	/**
	 * Checks if the setter type is valid for the specified desc of the field
	 * @param desc The desc of the original field
	 * @param method The proxy method
	 * @return
	 */
	private boolean isSetterTypeValidForField(String desc, MethodNode method) {
		return this.isTypeInstanceof(Type.getType(desc), Type.getArgumentTypes(method.desc)[0]);
	}

	/**
	 * Compares two specified types if they are equal or a corresponding {@link IAccessor}.
	 * If cls is an {@link IAccessor} a check is run if
	 * that accessor was registered and belongs to the first specified type.
	 * @param type The type of the original, to be proxied, code
	 * @param otherType The type of the proxy's method
	 * @return
	 */

	/**
	 * Returns whether type is an instance of otherType taking the accessors into account.
	 * Equivalent to <pre>otherType.isAssignableFrom(type)</pre> 
	 * In case of arrays their elementary type is compared
	 * @param type
	 * @param otherType
	 * @return
	 */
	private boolean isTypeInstanceof(Type type, Type otherType) {
		if(type.getSort() == Type.ARRAY && otherType.getSort() == Type.ARRAY) {
			type = type.getElementType();
			otherType = otherType.getElementType();
		}
		if(type.getSort() == Type.OBJECT && otherType.getSort() == Type.OBJECT) {
			//If type is not a primitive it can always be cast to Object
			if("java/lang/Object".equals(otherType.getInternalName())) {
				return true;
			}

			//Get accessor data of otherType, if otherType is an accessor
			final ClassAccessorData accessorInstance = this.accessors.getAccessorByClassName(otherType.getClassName());

			IOException resolverException = null;

			final Type finalOtherType = otherType;

			try {
				ClassRelationResolver relation = new ClassRelationResolver(type.getInternalName());
				return relation.traverseSuperclasses(cls -> {
					if(cls.equals(finalOtherType.getInternalName())) {
						//type extends or implements otherType
						return true;
					}

					if(accessorInstance != null) {
						//Check if accessor is an accesor of this superclass/-interface
						if(isIdentifiedClass(accessorInstance.getClassIdentifier(), cls)) {
							return true;
						}
					}

					return false;
				}, true);
			} catch(IOException ex) {
				resolverException = ex;
			}

			//If resolver fails, try to directly check if otherType is an accessor of type
			if(accessorInstance != null && isIdentifiedClass(accessorInstance.getClassIdentifier(), type.getInternalName())) {
				return true;
			}

			throw new ClassRelationResolverException(String.format("Class relation resolver failed for classes %s and %s", type.getClassName(), otherType.getClassName()), type, otherType, resolverException);
		}
		return type.getClassName().equals(otherType.getClassName());
	}

	/**
	 * Returns whether the specified method is a generated method
	 * @return
	 */
	public boolean isGeneratedMethod(String className, MethodNode method) {
		for(ClassAccessorData accessor : this.accessors.getClassAccessors()) {
			if(isIdentifiedClass(accessor.getClassIdentifier(), className)) {
				for(MethodInterceptorData interceptor : accessor.getMethodInterceptors()) {
					if(interceptor.getAccessorClass().equals(className)) {
						for(LocalVarData localVar : interceptor.getLocalVars()) {
							Type localVarType = Type.getArgumentTypes(localVar.getInterceptorMethodDesc())[localVar.getParameterIndex()];
							boolean isGetter = 
									localVar.getGeneratedGetterMethod().equals(method.name) && 
									Type.getMethodDescriptor(localVarType).equals(method.desc);
							boolean isSetter = 
									localVar.getGeneratedSetterMethod().equals(method.name) && 
									Type.getMethodDescriptor(Type.VOID_TYPE, localVarType).equals(method.desc);
							if(isGetter || isSetter) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
}
