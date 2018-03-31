package pr0x79.instrumentation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
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
import pr0x79.instrumentation.accessor.IInterceptorContext;
import pr0x79.instrumentation.accessor.LocalVarData;
import pr0x79.instrumentation.accessor.MethodInterceptorData;
import pr0x79.instrumentation.accessor.MethodInterceptorData.InterceptorContext;
import pr0x79.instrumentation.exception.InstrumentorException;
import pr0x79.instrumentation.exception.InvalidInterceptionExitException;
import pr0x79.instrumentation.exception.accessor.ClassRelationResolverException;
import pr0x79.instrumentation.exception.accessor.field.FieldAccessorTakenException;
import pr0x79.instrumentation.exception.accessor.field.InvalidGetterTypeException;
import pr0x79.instrumentation.exception.accessor.field.InvalidSetterTypeException;
import pr0x79.instrumentation.exception.accessor.fieldgenerator.FieldGeneratorTakenException;
import pr0x79.instrumentation.exception.accessor.method.InvalidMethodDescriptorException;
import pr0x79.instrumentation.exception.accessor.method.InvalidMethodExceptionsException;
import pr0x79.instrumentation.exception.accessor.method.InvalidParameterTypeException;
import pr0x79.instrumentation.exception.accessor.method.MethodAccessorTakenException;
import pr0x79.instrumentation.exception.identifier.field.FieldNotFoundException;
import pr0x79.instrumentation.exception.identifier.field.MultipleFieldsIdentifiedException;
import pr0x79.instrumentation.exception.identifier.instruction.ExitInstructionNotFoundException;
import pr0x79.instrumentation.exception.identifier.instruction.InstructionNotFoundException;
import pr0x79.instrumentation.exception.identifier.instruction.InstructionOutOfBoundsException;
import pr0x79.instrumentation.exception.identifier.instruction.InvalidExitTargetException;
import pr0x79.instrumentation.exception.identifier.instruction.LocalVarInstructionNotFoundException;
import pr0x79.instrumentation.exception.identifier.method.MethodNotFoundException;
import pr0x79.instrumentation.exception.identifier.method.MultipleMethodsIdentifiedException;
import pr0x79.instrumentation.identification.IClassIdentifier;
import pr0x79.instrumentation.identification.IFieldIdentifier;
import pr0x79.instrumentation.identification.IMethodIdentifier;
import pr0x79.instrumentation.identification.IMethodIdentifier.MethodDescription;
import pr0x79.instrumentation.signature.ClassHierarchy;

/**
 * Instruments classes using the registered {@link IAccessor}s
 */
public class BytecodeInstrumentation {
	private Accessors accessors;
	private final ClassHierarchy hierarchy;

	public BytecodeInstrumentation(ClassHierarchy hierarchy) {
		this.hierarchy = hierarchy;
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
			boolean modified = false;
			for(MethodInterceptorData interceptor : accessor.getMethodInterceptors()) {
				for(MethodNode method : clsNode.methods) {
					if(interceptor.getInterceptorMethod().equals(method.name) && interceptor.getInterceptorMethodDesc().equals(method.desc)) {
						Type[] params = Type.getArgumentTypes(method.desc);
						if(params.length > 1) {
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

									LabelNode contextLocalsVarScopeStart = new LabelNode();
									LabelNode contextLocalsVarScopeEnd = new LabelNode();

									insertions.add(contextLocalsVarScopeStart);

									LocalVariableNode contextLocalVarsNode = this.generateLocalVariable(Type.getDescriptor(Object[].class), null, contextLocalsVarScopeStart, contextLocalsVarScopeEnd, method);

									Method contextLocalsGetter = getInternalMethod(IInterceptorContext.class, "get_local_variables");

									for(int i = 0; i < params.length; i++) {
										Type paramType = params[i];
										if(i == interceptor.getContextParameter()) {
											//Load and store contex local variables in contextLocalVarsNode
											insertions.add(new VarInsnNode(Opcodes.ALOAD, stackIndex));
											insertions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(InterceptorContext.class)));
											insertions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(InterceptorContext.class), contextLocalsGetter.getName(), Type.getMethodDescriptor(contextLocalsGetter), false));
											insertions.add(new VarInsnNode(Opcodes.ASTORE, contextLocalVarsNode.index));
											break;
										}
										stackIndex += paramType.getSize();
									}

									stackIndex = 1;
									int localVarIndex = 0;
									for(int i = 0; i < params.length; i++) {
										Type paramType = params[i];

										if(i != interceptor.getContextParameter()) {
											//Load context local variables and push insertion index onto stack
											insertions.add(new VarInsnNode(Opcodes.ALOAD, contextLocalVarsNode.index));
											insertions.add(this.instrumentOptimizedIntegerPush(localVarIndex));

											//Load local variable and autobox
											insertions.add(new VarInsnNode(paramType.getOpcode(Opcodes.ILOAD), stackIndex));
											if(paramType.getSort() != Type.OBJECT && paramType.getSort() != Type.ARRAY) {
												//Primitive needs to be boxed
												insertions.add(this.instrumentTypeBoxing(paramType));
											}

											//Set value in array
											insertions.add(new InsnNode(Opcodes.AASTORE));

											localVarIndex++;
										}

										stackIndex += paramType.getSize();
									}

									insertions.add(contextLocalsVarScopeEnd);

									insertionPoints.put(node, insertions);
								}
							}

							for(Entry<AbstractInsnNode, InsnList> insertion : insertionPoints.entrySet()) {
								method.instructions.insertBefore(insertion.getKey(), insertion.getValue());
								modified = true;
							}
						}
					}
				}
			}

			return modified;
		}

		return false;
	}

	/**
	 * Gets the specified value of an {@link AnnotationNode}.
	 * <p><b>Note: Annotation arrays are returned as ArrayList</b>
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
	 * Gets the specified value of an {@link AnnotationNode}.
	 * <p><b>Note: Annotation arrays are returned as ArrayList</b>
	 * @param annotation
	 * @param name
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getAnnotationValue(AnnotationNode annotation, String name, Class<T> type) {
		for(int i = 0; i < annotation.values.size(); i += 2) {
			if(name.equals(((String) annotation.values.get(i))) && annotation.values.get(i + 1) != null && type.isAssignableFrom(annotation.values.get(i + 1).getClass())) {
				return (T) annotation.values.get(i + 1);
			}
		}
		return null;
	}

	/**
	 * Instruments the specified {@link ClassNode} according to the
	 * registered {@link IAccessor}s
	 * @param loader
	 * @param clsNode
	 */
	public void instrumentClass(ClassLoader loader, ClassNode clsNode) {
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
			this.instrumentFieldAccessors(loader, clsNode, classAccessor);
			this.instrumentFieldGenerators(loader, clsNode, classAccessor);
			this.instrumentMethodAccessors(loader, clsNode, classAccessor);
			this.instrumentMethodInterceptors(loader, clsNode, classAccessor);
		}
	}

	/**
	 * Instruments all registered field accessors
	 * @param loader
	 * @param clsNode
	 * @param classAccessor
	 */
	private void instrumentFieldAccessors(ClassLoader loader, ClassNode clsNode, ClassAccessorData classAccessor) {
		for(FieldAccessorData fieldAccessor : classAccessor.getFieldAccessors()) {
			this.instrumentFieldAccessor(loader, fieldAccessor, clsNode, classAccessor.getAccessorClass());
		}
	}

	/**
	 * Instruments a single field accessor
	 * @param loader
	 * @param fieldAccessor
	 * @param clsNode
	 * @param accessorClass
	 */
	private void instrumentFieldAccessor(ClassLoader loader, FieldAccessorData fieldAccessor, ClassNode clsNode, String accessorClass) {
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
					if(!this.isSetterTypeValidForField(loader, fieldNode.desc, accessorMethod)) {
						throw new InvalidSetterTypeException(accessorClass, new MethodDescription(accessorMethod.name, accessorMethod.desc), accessorParams[0].getClassName(), Type.getType(fieldNode.desc).getClassName());
					}
				} else {
					if(!this.isGetterTypeValidForField(loader, fieldNode.desc, accessorMethod)) {
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
	 * @param loader
	 * @param clsNode
	 * @param classAccessor
	 */
	private void instrumentFieldGenerators(ClassLoader loader, ClassNode clsNode, ClassAccessorData classAccessor) {
		for(FieldGeneratorData fieldGenerator : classAccessor.getFieldGenerators()) {
			this.instrumentFieldGenerator(loader, fieldGenerator, clsNode, classAccessor.getAccessorClass());
		}
	}

	/**
	 * Instruments a single field generator
	 * @param loader
	 * @param fieldGenerator
	 * @param clsNode
	 * @param accessorClass
	 */
	private void instrumentFieldGenerator(ClassLoader loader, FieldGeneratorData fieldGenerator, ClassNode clsNode, String accessorClass) {
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
				if(!this.isTypeInstanceof(loader, Type.getType(field.desc), fieldGenerator.getFieldType())) {
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
	 * @param loader
	 * @param clsNode
	 * @param classAccessor
	 */
	private void instrumentMethodAccessors(ClassLoader loader, ClassNode clsNode, ClassAccessorData classAccessor) {
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
					if(!this.isMethodAccessorValid(loader, methodNode.desc, accessorMethod)) {
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
	 * Creates instructions to box the specified primitive type
	 * @param type Primitive type to box
	 * @return
	 */
	private InsnList instrumentTypeBoxing(Type type) {
		InsnList lst = new InsnList();
		switch(type.getDescriptor().charAt(0)) {
		case 'I':
			lst.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
			break;
		case 'Z':
			lst.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false));
			break;
		case 'B':
			lst.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false));
			break;
		case 'C':
			lst.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false));
			break;
		case 'S':
			lst.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false));
			break;
		case 'J':
			lst.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false));
			break;
		case 'D':
			lst.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
			break;
		case 'F':
			lst.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false));
			break;
		default:
			throw new InstrumentorException(String.format("Type %s is not a primitive and cannot be boxed", type.getDescriptor()));
		}
		return lst;
	}

	/**
	 * Creates instructions to unbox a type into the specified primitive type
	 * @param type Primitive type to unbox to
	 * @param cast Whether a cast needs to be done
	 * @return
	 */
	private InsnList instrumentTypeUnboxing(Type type, boolean cast) {
		InsnList lst = new InsnList();
		switch(type.getDescriptor().charAt(0)) {
		case 'I':
			if(cast) lst.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Integer"));
			lst.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false));
			break;
		case 'Z':
			if(cast) lst.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Boolean"));
			lst.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false));
			break;
		case 'B':
			if(cast) lst.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Byte"));
			lst.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false));
			break;
		case 'C':
			if(cast) lst.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Character"));
			lst.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false));
			break;
		case 'S':
			if(cast) lst.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Short"));
			lst.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false));
			break;
		case 'J':
			if(cast) lst.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Long"));
			lst.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false));
			break;
		case 'D':
			if(cast) lst.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Double"));
			lst.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false));
			break;
		case 'F':
			if(cast) lst.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Float"));
			lst.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false));
			break;
		default:
			throw new InstrumentorException(String.format("Type %s is not a primitive and cannot be a result of unboxing", type.getDescriptor()));
		}
		return lst;
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
			mv.visitVarInsn(param.getOpcode(Opcodes.ILOAD), stackIndex);
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
			mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
		}
		mv.visitMaxs(0, 0);
	}

	/**
	 * Creates an optimized integer push
	 * @param val The value to push
	 * @return
	 */
	private AbstractInsnNode instrumentOptimizedIntegerPush(int val) {
		if(val == -1) {
			return new InsnNode(Opcodes.ICONST_M1);
		} else if(val >= 0 && val <= 5) {
			return new InsnNode(Opcodes.ICONST_0 + val);
		} else if(val >= -128 && val <= 127) {
			return new IntInsnNode(Opcodes.BIPUSH, val);
		} else if(val >= -32768 && val <= 32767) {
			return new IntInsnNode(Opcodes.SIPUSH, val);
		}
		return new LdcInsnNode(val);
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
		mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
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
		mv.visitVarInsn(param.getOpcode(Opcodes.ILOAD), 1);
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
	 * @param loader
	 * @param interceptor
	 * @param clsNode
	 */
	private void instrumentMethodInterceptors(ClassLoader loader, ClassNode clsNode, ClassAccessorData classAccessor) {
		List<MethodInterceptorData> classInterceptors = new ArrayList<>();
		for(MethodInterceptorData interceptor : classAccessor.getMethodInterceptors()) {
			if(isIdentifiedClass(interceptor.getClassIdentifier(), clsNode.name)) {
				classInterceptors.add(interceptor);
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
				throw new InstructionOutOfBoundsException(String.format("Instruction index of %s#%s[%s] is out of bounds. Current: %s, Expected: [%d, %d]", interceptor.getAccessorClass(), interceptor.getInterceptorMethod() + interceptor.getInterceptorMethodDesc(), interceptor.getInstructionIdentifierId(), instructionIndex, 0, targetMethod.instructions.size() - 1), null, instructionIndex, 0, targetMethod.instructions.size() - 1, clsNode.name, new MethodDescription(targetMethod.name, targetMethod.desc), interceptor.getInstructionIdentifierId(), interceptor.getInstructionIdentifier());
			}

			AbstractInsnNode[] exitNodes = new AbstractInsnNode[interceptor.getExitInstructionIdentifiers().length];
			for(int i = 0; i < interceptor.getExitInstructionIdentifiers().length; i++) {
				int exitInstructionIndex = interceptor.getExitInstructionIdentifiers()[i].identify(targetMethod);
				if(exitInstructionIndex == -1) {
					throw new ExitInstructionNotFoundException(clsNode.name, new MethodDescription(targetMethod.name, targetMethod.desc), interceptor.getExitInstructionIdentifierIds()[i], interceptor.getExitInstructionIdentifiers()[i]);
				}
				if(exitInstructionIndex < 0 || exitInstructionIndex >= targetMethod.instructions.size()) {
					throw new InstructionOutOfBoundsException(String.format("Exit instruction index of %s#%s[%s] is out of bounds. Current: %s, Expected: [%d, %d]", clsNode.name, targetMethod.name + targetMethod.desc, interceptor.getExitInstructionIdentifierIds()[i], exitInstructionIndex, 0, targetMethod.instructions.size() - 1), null, exitInstructionIndex, 0, targetMethod.instructions.size() - 1, clsNode.name, new MethodDescription(targetMethod.name, targetMethod.desc), interceptor.getExitInstructionIdentifierIds()[i], interceptor.getExitInstructionIdentifiers()[i]);
				}
				if(exitInstructionIndex <= instructionIndex) {
					throw new InstructionOutOfBoundsException(String.format("Exit instruction index of %s#%s[%s] must be after the interceptor instruction index. Current: %s, Expected: >%d", clsNode.name, targetMethod.name + targetMethod.desc, interceptor.getExitInstructionIdentifierIds()[i], exitInstructionIndex, instructionIndex), null, exitInstructionIndex, instructionIndex + 1, targetMethod.instructions.size() - 1, clsNode.name, new MethodDescription(targetMethod.name, targetMethod.desc), interceptor.getExitInstructionIdentifierIds()[i], interceptor.getExitInstructionIdentifiers()[i]);
				}
				exitNodes[i] = targetMethod.instructions.get(exitInstructionIndex);
			}

			interceptorInsertions.put(interceptor, new Object[]{targetMethod, targetMethod.instructions.get(instructionIndex), exitNodes});
		}

		//Insert instructions for the interceptors at the insertion node
		for(Entry<MethodInterceptorData, Object[]> interceptorInsertion : interceptorInsertions.entrySet()) {
			MethodInterceptorData interceptor = interceptorInsertion.getKey();
			MethodNode targetMethod = (MethodNode) interceptorInsertion.getValue()[0];
			AbstractInsnNode insertionNode = (AbstractInsnNode) interceptorInsertion.getValue()[1];
			AbstractInsnNode[] exitNodes = (AbstractInsnNode[]) interceptorInsertion.getValue()[2];

			InsnList insertions = new InsnList();

			LabelNode interceptionScopeStart = new LabelNode();
			LabelNode interceptionScopeEnd = new LabelNode();

			/*System.out.println("FULL SIG: " + targetMethod.signature);

			if(targetMethod.signature != null) {
				Signature sig = SignatureParser.parse(targetMethod.signature);
				StringJoiner joiner = new StringJoiner(", ");
				for(SignatureSymbol param : sig.formalTypeParameters) {
					joiner.add(param.toString());
				}
				System.out.println("PARAMS: " + joiner.toString());
				SignatureTypesResolver.resolve(loader, this.hierarchy, clsNode.name, sig);
			}

			String returnSig = null;
			if(targetMethod.signature != null) {
				ReturnTypeSignatureMapper mapper = new ReturnTypeSignatureMapper(Opcodes.ASM5);
				new SignatureReader(targetMethod.signature).accept(mapper);
				returnSig = mapper.getSignature();
			}

			System.out.println("CLS SIG: " + clsNode.signature);
			System.out.println("RET SIG: " + returnSig);
			System.out.println("CONT SIG: " + interceptor.getContextSignature());

			Type contextReturnType;
			Map<Integer, Type> contextSigTypes = new HashMap<>();
			if(interceptor.getContextSignature() != null) {
				SignatureTypesMapper contextSigTypesMapper = new SignatureTypesMapper(Opcodes.ASM5, contextSigTypes, Integer.MAX_VALUE);
				new SignatureReader(interceptor.getContextSignature()).accept(contextSigTypesMapper);
				contextReturnType = contextSigTypes.get(0);
				System.out.println("SIGS: " + contextSigTypes);
			} else {
				contextReturnType = Type.VOID_TYPE;
			}

			System.out.println("RET TYPE: " + contextReturnType + " " + Type.VOID_TYPE.getSort());

			//TODO Verify context sig
			if((returnSig == null) != (interceptor.getContextSignature() == null)) {
				throw new InvalidContextSignatureException(String.format("Interceptor context signature of %s#%s is invalid. Current: %s, Expected: %s, or accessors of those classes", clsNode.name, targetMethod.name + targetMethod.desc, interceptor.getContextSignature(), returnSig), clsNode.name, new MethodDescription(targetMethod.name, targetMethod.desc), interceptor.getContextSignature(), returnSig);
			}
			if(returnSig != null && interceptor.getContextSignature() != null) {
				Map<Integer, Type> returnSigTypes = new HashMap<>();
				SignatureTypesMapper returnSigTypesMapper = new SignatureTypesMapper(Opcodes.ASM5, returnSigTypes, Integer.MAX_VALUE);
				new SignatureReader(returnSig).accept(returnSigTypesMapper);

				if(returnSigTypes.size() != contextSigTypes.size()) {
					throw new InvalidContextSignatureException(String.format("Interceptor context signature of %s#%s is invalid. Current: %s, Expected: %s, or accessors of those classes", clsNode.name, targetMethod.name + targetMethod.desc, interceptor.getContextSignature(), returnSig), clsNode.name, new MethodDescription(targetMethod.name, targetMethod.desc), interceptor.getContextSignature(), returnSig);
				}
				for(Entry<Integer, Type> returnSigTypeEntry : returnSigTypes.entrySet()) {
					Type contextSigType = contextSigTypes.get(returnSigTypeEntry.getKey());
					if(!this.isTypeInstanceof(loader, returnSigTypeEntry.getValue(), contextSigType)) {
						throw new InvalidContextSignatureException(String.format("Interceptor context signature of %s#%s is invalid. Current: %s, Expected: %s, or accessors of those classes", clsNode.name, targetMethod.name + targetMethod.desc, interceptor.getContextSignature(), returnSig), clsNode.name, new MethodDescription(targetMethod.name, targetMethod.desc), interceptor.getContextSignature(), returnSig);
					}
				}
			}*/

			//Write context signature to string
			String contextVarSig = null;
			if(interceptor.getContextSignature() != null) {
				SignatureWriter sigWriter = new SignatureWriter();
				interceptor.getContextSignature().accept(sigWriter);
				contextVarSig = sigWriter.toString();
			}

			LocalVariableNode contextVarNode = this.generateLocalVariable(Type.getDescriptor(InterceptorContext.class), contextVarSig, interceptionScopeStart, interceptionScopeEnd, targetMethod);

			insertions.add(interceptionScopeStart);

			insertions.add(new VarInsnNode(Opcodes.ALOAD, 0));

			//Load local variables
			int localVarParamIndex = 0;
			for(int i = 0; i < interceptor.getLocalVars().size() + 1; i++) {
				if(i == interceptor.getContextParameter()) {
					//Create context parameter
					insertions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(InterceptorContext.class)));
					insertions.add(new InsnNode(Opcodes.DUP));
					insertions.add(new InsnNode(Opcodes.DUP));
					insertions.add(this.instrumentOptimizedIntegerPush(interceptor.getLocalVars().size()));
					Constructor<?> interceptorCtr = getInternalConstructor(InterceptorContext.class, "ctor");
					insertions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, Type.getInternalName(InterceptorContext.class), "<init>", Type.getConstructorDescriptor(interceptorCtr), false));
					insertions.add(new VarInsnNode(Opcodes.ASTORE, contextVarNode.index));
				} else {
					LocalVarData localVarData = interceptor.getLocalVars().get(localVarParamIndex);
					int identifiedLocalVarIndex = localVarData.getInstructionIdentifier().identify(targetMethod);
					LocalVariableNode localVariable = null;
					for(LocalVariableNode targetLocalVar : targetMethod.localVariables) {
						if(targetLocalVar.index == identifiedLocalVarIndex) {
							localVariable = targetLocalVar;
						}
					}
					if(localVariable == null) {
						throw new LocalVarInstructionNotFoundException(localVarData.getParameterIndex(), interceptor.getAccessorClass(), new MethodDescription(interceptor.getInterceptorMethod(), interceptor.getInterceptorMethodDesc()), localVarData.getInstructionIdentifierId(), localVarData.getInstructionIdentifier());
					}
					Type localVarType = Type.getType(localVariable.desc);
					Type paramType = Type.getArgumentTypes(interceptor.getInterceptorMethodDesc())[localVarData.getParameterIndex()];
					ClassAccessorData paramAsAccessor = this.accessors.getAccessorByClassName(paramType.getClassName());
					if((paramAsAccessor != null && !this.isTypeInstanceof(loader, localVarType, Type.getObjectType(paramAsAccessor.getAccessorClass().replace('.', '/')))) || (paramAsAccessor == null && !paramType.equals(localVarType))) {
						throw new InvalidParameterTypeException(String.format("@LocalVar parameter %d of method %s#%s does not match. Current: %s, Expected: %s, or an accessor of that class. Local variable index: %d. Local variable identifier: %s", localVarData.getParameterIndex(), interceptor.getAccessorClass(), interceptor.getInterceptorMethod() + interceptor.getInterceptorMethodDesc(), paramType.getClassName(), Type.getType(localVariable.desc).getClassName(), localVariable.index, localVarData.getInstructionIdentifierId()), interceptor.getAccessorClass(), new MethodDescription(interceptor.getInterceptorMethod(), interceptor.getInterceptorMethodDesc()), localVarData.getParameterIndex(), paramType.getClassName(), Type.getType(localVariable.desc).getClassName());
					}
					insertions.add(new VarInsnNode(paramType.getOpcode(Opcodes.ILOAD), localVariable.index));
					localVarParamIndex++;
				}
			}

			//Call interceptor method
			insertions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, interceptor.getAccessorClass().replace('.', '/'), interceptor.getInterceptorMethod(), interceptor.getInterceptorMethodDesc(), true));

			//Get interception return value and return with value if it is set
			insertions.add(new VarInsnNode(Opcodes.ALOAD, contextVarNode.index));
			Method isReturningMethod = getInternalMethod(IInterceptorContext.class, "is_returning");
			insertions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(InterceptorContext.class), isReturningMethod.getName(), Type.getMethodDescriptor(isReturningMethod), false));
			LabelNode skipReturnTarget = new LabelNode();
			insertions.add(new JumpInsnNode(Opcodes.IFEQ, skipReturnTarget));
			insertions.add(new VarInsnNode(Opcodes.ALOAD, contextVarNode.index));
			Method getReturnMethod = getInternalMethod(IInterceptorContext.class, "get_return");
			insertions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(InterceptorContext.class), getReturnMethod.getName(), Type.getMethodDescriptor(getReturnMethod), false));
			Type returnType = Type.getReturnType(targetMethod.desc);
			if(returnType.getSort() == Type.OBJECT) {
				insertions.add(new TypeInsnNode(Opcodes.CHECKCAST, returnType.getDescriptor()));
			} else if(returnType.getSort() != Type.ARRAY && returnType.getSort() != Type.VOID) {
				//Unbox primitive
				insertions.add(this.instrumentTypeUnboxing(returnType, true));
			}
			insertions.add(new InsnNode(Type.getReturnType(targetMethod.desc).getOpcode(Opcodes.IRETURN)));
			insertions.add(skipReturnTarget);

			LabelNode contextLocalVarsScopeStart = new LabelNode();
			LabelNode contextLocalVarsScopeEnd = new LabelNode();

			LocalVariableNode contextLocalVarsNode = this.generateLocalVariable(Type.getDescriptor(Object[].class), null, contextLocalVarsScopeStart, contextLocalVarsScopeEnd, targetMethod);

			insertions.add(contextLocalVarsScopeStart);

			//Load and store context local variables in contextLocalVarsNode
			insertions.add(new VarInsnNode(Opcodes.ALOAD, contextVarNode.index));
			Method contextLocalsGetter = getInternalMethod(IInterceptorContext.class, "get_local_variables");
			insertions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(InterceptorContext.class), contextLocalsGetter.getName(), Type.getMethodDescriptor(contextLocalsGetter), false));
			insertions.add(new VarInsnNode(Opcodes.ASTORE, contextLocalVarsNode.index));

			//Store local variables
			int contextLocalVarIndex = 0;
			for(LocalVarData localVarData : interceptor.getLocalVars()) {
				int localVarIndex = localVarData.getInstructionIdentifier().identify(targetMethod);
				LocalVariableNode localVar = null;
				for(LocalVariableNode targetLocalVar : targetMethod.localVariables) {
					if(targetLocalVar.index == localVarIndex) {
						localVar = targetLocalVar;
					}
				}
				if(localVar == null) {
					throw new LocalVarInstructionNotFoundException(localVarData.getParameterIndex(), interceptor.getAccessorClass(), new MethodDescription(interceptor.getInterceptorMethod(), interceptor.getInterceptorMethodDesc()), localVarData.getInstructionIdentifierId(), localVarData.getInstructionIdentifier());
				}
				Type localVarType = Type.getType(localVar.desc);
				Type paramType = Type.getArgumentTypes(localVarData.getInterceptorMethodDesc())[localVarData.getParameterIndex()];

				insertions.add(new VarInsnNode(Opcodes.ALOAD, contextLocalVarsNode.index));
				insertions.add(this.instrumentOptimizedIntegerPush(contextLocalVarIndex));
				insertions.add(new InsnNode(Opcodes.AALOAD));
				if(localVarType.getSort() != Type.OBJECT && localVarType.getSort() != Type.ARRAY) {
					//Unbox to primitive type
					insertions.add(this.instrumentTypeUnboxing(localVarType, true));
				}
				insertions.add(new TypeInsnNode(Opcodes.CHECKCAST, localVarType.getInternalName()));
				insertions.add(new VarInsnNode(paramType.getOpcode(Opcodes.ISTORE), localVar.index));

				contextLocalVarIndex++;
			}

			insertions.add(contextLocalVarsScopeEnd);

			if(exitNodes.length != 0) {
				//Find exit targets
				LabelNode[] exitTargets = new LabelNode[exitNodes.length];
				for(int i = 0; i < exitNodes.length; i++) {
					AbstractInsnNode exitNode = exitNodes[i];
					LabelNode exitTarget = new LabelNode();

					int exitNodeIndex = targetMethod.instructions.indexOf(exitNode);

					Analyzer<BasicValue> a = new Analyzer<>(new BasicInterpreter());
					try {
						Frame<BasicValue>[] frames = a.analyze(clsNode.name, targetMethod);
						Frame<BasicValue> stackFrame = frames[exitNodeIndex];
						if(stackFrame.getStackSize() > 0) {
							StringBuilder stackStr = new StringBuilder();
							stackStr.append("[");
							for(int s = 0; s < stackFrame.getStackSize(); s++) {
								stackStr.append(stackFrame.getStack(s).getType().getClassName());
								if(s != stackFrame.getStackSize() - 1) {
									stackStr.append(", ");
								}
							}
							stackStr.append("]");
							throw new InvalidExitTargetException(String.format("Cannot insert exit target of %s#%s[%s] at index %d. Stack must be empty. Current stack: %s", clsNode.name, targetMethod.name + targetMethod.desc, interceptorInsertion.getKey().getExitInstructionIdentifierIds()[i], exitNodeIndex, stackStr.toString()), exitNodeIndex, clsNode.name, new MethodDescription(targetMethod.name, targetMethod.desc), interceptorInsertion.getKey().getExitInstructionIdentifierIds()[i], interceptorInsertion.getKey().getExitInstructionIdentifiers()[i]);
						}
					} catch (AnalyzerException ex) {
						throw new InvalidExitTargetException(String.format("Cannot insert exit target of %s#%s[%s] at index %d due to an unknown reason", clsNode.name, targetMethod.name + targetMethod.desc, interceptorInsertion.getKey().getExitInstructionIdentifierIds()[i], exitNodeIndex), ex, exitNodeIndex, clsNode.name, new MethodDescription(targetMethod.name, targetMethod.desc), interceptorInsertion.getKey().getExitInstructionIdentifierIds()[i], interceptorInsertion.getKey().getExitInstructionIdentifiers()[i]);
					}

					//Insert exit target
					targetMethod.instructions.insertBefore(exitNode, exitTarget);

					exitTargets[i] = exitTarget;
				}

				LabelNode skipExitLabel = new LabelNode();
				Method isExitingMethod = getInternalMethod(IInterceptorContext.class, "is_exiting");
				insertions.add(new VarInsnNode(Opcodes.ALOAD, contextVarNode.index));
				insertions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(InterceptorContext.class), isExitingMethod.getName(), Type.getMethodDescriptor(isExitingMethod), false));
				insertions.add(new JumpInsnNode(Opcodes.IFEQ, skipExitLabel));
				Method getExitMethod = getInternalMethod(IInterceptorContext.class, "get_exit");
				insertions.add(new VarInsnNode(Opcodes.ALOAD, contextVarNode.index));
				insertions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(InterceptorContext.class), getExitMethod.getName(), Type.getMethodDescriptor(getExitMethod), false));
				LabelNode invalidExitLabel = new LabelNode();
				LabelNode[] switchEntryLabels = new LabelNode[exitTargets.length];
				for(int i = 0; i < exitTargets.length; i++) {
					switchEntryLabels[i] = new LabelNode();
				}
				insertions.add(new TableSwitchInsnNode(0, exitTargets.length - 1, invalidExitLabel, exitTargets));
				insertions.add(invalidExitLabel);
				insertions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(InvalidInterceptionExitException.class)));
				insertions.add(new InsnNode(Opcodes.DUP));
				insertions.add(new VarInsnNode(Opcodes.ALOAD, contextVarNode.index));
				insertions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(InterceptorContext.class), getExitMethod.getName(), Type.getMethodDescriptor(getExitMethod), false));
				Constructor<?> exceptionCtor = getInternalConstructor(InvalidInterceptionExitException.class, "ctor");
				insertions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, Type.getInternalName(InvalidInterceptionExitException.class), "<init>", Type.getConstructorDescriptor(exceptionCtor), false));
				insertions.add(new InsnNode(Opcodes.ATHROW));
				for(int i = 0; i < exitTargets.length; i++) {
					insertions.add(switchEntryLabels[i]);
					insertions.add(new JumpInsnNode(Opcodes.GOTO, exitTargets[i]));
				}
				insertions.add(skipExitLabel);
			}

			insertions.add(interceptionScopeEnd);

			//Insert instructions
			targetMethod.instructions.insertBefore(insertionNode, insertions);
		}
	}

	/**
	 * Generates and adds a local variable to the method.
	 * Uses a unique index and a unique name provided by the name generator.
	 * @param desc Descriptor of the local variable
	 * @param sig Signature of the local variable
	 * @param scopeStart Scope start
	 * @param scopeEnd Scope end
	 * @param owner Owner method
	 * @return
	 */
	private LocalVariableNode generateLocalVariable(String desc, String sig, LabelNode scopeStart, LabelNode scopeEnd, MethodNode owner) {
		Set<String> methodLocalVarNames = new HashSet<>();
		for(LocalVariableNode localVar : owner.localVariables) {
			methodLocalVarNames.add(localVar.name);
		}

		String contextVarName = getUniqueName(methodLocalVarNames);
		methodLocalVarNames.add(contextVarName);

		int contextVarIndex = 1; //0 is always 'this'
		loop: while(true) {
			for(LocalVariableNode localVar : owner.localVariables) {
				if(localVar.index == contextVarIndex) {
					contextVarIndex++;
					continue loop;
				}
			}
			break;
		}

		LocalVariableNode variable = new LocalVariableNode(contextVarName, desc, sig, scopeStart, scopeEnd, contextVarIndex);
		owner.localVariables.add(variable);
		return variable;
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
	 * @param loader The class loader that is loading the accessor
	 * @param desc The desc of the original method
	 * @param method The proxy method
	 * @return
	 */
	private boolean isMethodAccessorValid(ClassLoader loader, String desc, MethodNode method) {
		Type returnType = Type.getReturnType(desc);
		if(!this.isTypeInstanceof(loader, returnType, Type.getReturnType(method.desc))) {
			return false;
		}
		Type[] methodParams = Type.getArgumentTypes(method.desc);
		Type[] descParams = Type.getArgumentTypes(desc);
		if(descParams.length != methodParams.length) {
			return false;
		}
		for(int i = 0; i < descParams.length; i++) {
			if(!this.isTypeInstanceof(loader, descParams[i], methodParams[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if the getter type is valid for the specified desc of the field
	 * @param loader The class loader that is loading the accessor
	 * @param desc The desc of the original field
	 * @param method The proxy method
	 * @return
	 */
	private boolean isGetterTypeValidForField(ClassLoader loader, String desc, MethodNode method) {
		return this.isTypeInstanceof(loader, Type.getType(desc), Type.getReturnType(method.desc));
	}

	/**
	 * Checks if the setter type is valid for the specified desc of the field
	 * @param loader The class loader that is loading the accessor
	 * @param desc The desc of the original field
	 * @param method The proxy method
	 * @return
	 */
	private boolean isSetterTypeValidForField(ClassLoader loader, String desc, MethodNode method) {
		return this.isTypeInstanceof(loader, Type.getType(desc), Type.getArgumentTypes(method.desc)[0]);
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
	private boolean isTypeInstanceof(ClassLoader loader, Type type, Type otherType) {
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

			Exception resolverException = null;

			final Type finalOtherType = otherType;

			try {
				ClassRelationResolver relation = new ClassRelationResolver(this.hierarchy, loader, type.getInternalName());
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
			} catch(Exception ex) {
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
	 * Returns the method for the specified class and internal ID
	 * @param cls
	 * @param id
	 * @return
	 */
	public static Method getInternalMethod(Class<?> cls, String id) {
		for(Method method : cls.getDeclaredMethods()) {
			Internal a = method.getAnnotation(Internal.class);
			if(a != null && id.equals(a.id())) {
				return method;
			}
		}
		throw new RuntimeException(String.format("Internal method of class %s with id %s was not found", cls.getName(), id));
	}

	/**
	 * Returns the constructor for the specified class and internal ID
	 * @param cls
	 * @param id
	 * @return
	 */
	public static Constructor<?> getInternalConstructor(Class<?> cls, String id) {
		for(Constructor<?> ctor : cls.getDeclaredConstructors()) {
			Internal a = ctor.getAnnotation(Internal.class);
			if(a != null && id.equals(a.id())) {
				return ctor;
			}
		}
		throw new RuntimeException(String.format("Internal constructor of class %s with id %s was not found", cls.getName(), id));
	}

	/**
	 * Returns the field for the specified class and internal ID
	 * @param cls
	 * @param id
	 * @return
	 */
	public static Field getInternalField(Class<?> cls, String id) {
		for(Field field : cls.getDeclaredFields()) {
			Internal a = field.getAnnotation(Internal.class);
			if(a != null && id.equals(a.id())) {
				return field;
			}
		}
		throw new RuntimeException(String.format("Internal field of class %s with id %s was not found", cls.getName(), id));
	}
}
