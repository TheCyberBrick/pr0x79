package pr0x79;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import pr0x79.accessor.ClassAccessor;
import pr0x79.accessor.IAccessor;
import pr0x79.exception.InstrumentorException;
import pr0x79.signature.ClassHierarchy;

public class Bootstrapper {
	private static final Bootstrapper INSTANCE = new Bootstrapper();

	private final Mappers mappers;
	private final Accessors accessors;
	private final ClassLocators classLocators;
	private final BytecodeInstrumentation instrumentor;

	//Contains all accessor classes that were loaded through the class transformer
	private final ClassHierarchy hierarchy;

	private Set<IInstrumentor> instrumentors;
	private boolean initializing = true;

	private Bootstrapper() {
		this.mappers = new Mappers(this);
		this.classLocators = new ClassLocators(this);
		this.hierarchy = new ClassHierarchy(this.classLocators);
		this.instrumentor = new BytecodeInstrumentation(this.hierarchy, this.classLocators);
		this.accessors = new Accessors(this, this.mappers, this.instrumentor);
		this.instrumentor.setAccessors(this.accessors);
	}

	/**
	 * Initializes the bootstrapper, called from the agent
	 * @param args The instrumentor class names
	 * @param inst The bytecode instrumentation
	 */
	public static void initialize(String[] instrumentorClasses, Instrumentation inst) {
		if(!INSTANCE.isInitializing()) {
			throw new RuntimeException("Bootstrapper can only be initialized once");
		}
		INSTANCE.init(instrumentorClasses, inst);
	}

	/**
	 * Initializes the bootstrapper
	 * @param instrumentorClasses The instrumentor class names
	 * @param inst The bytecode instrumentation
	 */
	private void init(String[] instrumentorClasses, Instrumentation inst) {
		//All exceptions before the IInstrumentors have been registered go into this list and are later redirected to the IInstrumentors after initialization
		List<Exception> bootstrapperInitExceptions = Collections.synchronizedList(new ArrayList<>());

		inst.addTransformer(new ClassFileTransformer() {
			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDoman, byte[] bytes) throws IllegalClassFormatException {
				try {
					boolean modified = false;

					ClassReader classReader = new ClassReader(bytes);
					ClassNode clsNode = new ClassNode();
					classReader.accept(clsNode, ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);

					hierarchy.addClass(loader, clsNode);

					final String classIdentifier = BytecodeInstrumentation.getAnnotationValue(clsNode.visibleAnnotations, ClassAccessor.class, BytecodeInstrumentation.getInternalMethod(ClassAccessor.class, "class_identifier").getName(), String.class, null);

					if(classIdentifier != null) {
						clsNode = new ClassNode();
						classReader.accept(clsNode, ClassReader.SKIP_FRAMES);
						if(instrumentor.instrumentAccessorClass(clsNode, Bootstrapper.this)) {
							modified = true;
						}
					}

					final ClassNode acceptsClassNode = clsNode;
					if(className != null && instrumentor.acceptsClass(clsNode, classIdentifier != null ? ClassReader.SKIP_FRAMES : ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG, flags -> {
						if((classIdentifier != null && flags == ClassReader.SKIP_FRAMES || classIdentifier == null) && (flags == (ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG))) {
							return acceptsClassNode;
						}
						ClassNode newNode = new ClassNode();
						classReader.accept(newNode, flags);
						return newNode;
					})) {
						if(classIdentifier == null) {
							clsNode = new ClassNode();
							classReader.accept(clsNode, ClassReader.SKIP_FRAMES);
						}
						final ClassNode instrumentClassNode = clsNode;
						instrumentor.instrumentClass(loader, clsNode, ClassReader.SKIP_FRAMES, flags -> {
							if(flags == ClassReader.SKIP_FRAMES) {
								return instrumentClassNode;
							}
							ClassNode newNode = new ClassNode();
							classReader.accept(newNode, flags);
							return newNode;
						});
						modified = true;
					}

					if(modified) {
						ClassWriter classWriter = new InstrumentationClassWriter(hierarchy, loader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
						clsNode.accept(classWriter);
						return classWriter.toByteArray();
					}
				} catch(Exception ex) {
					if(!isInitializing()) {
						onBootstrapperException(ex);
					} else {
						synchronized(bootstrapperInitExceptions) {
							bootstrapperInitExceptions.add(ex);
						}
					}
				}

				return bytes;
			}
		});

		List<IInstrumentor> instrumentorInstances = new ArrayList<>();
		for(String instrumentorClass : instrumentorClasses) {
			IInstrumentor instrumentor = null;
			try {
				instrumentor = this.initInstrumentor(instrumentorClass);
			} catch(ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException ex) {
				bootstrapperInitExceptions.add(ex);
			}
			if(instrumentor != null) {
				instrumentorInstances.add(instrumentor);
			}
		}

		this.instrumentors = new TreeSet<IInstrumentor>((c1, c2) -> c1.getClass().getName().compareTo(c2.getClass().getName()));
		this.instrumentors.addAll(instrumentorInstances);

		for(IInstrumentor instrumentor : this.instrumentors) {
			try {
				instrumentor.initBootstrapper(this);
			} catch(Exception ex) {
				bootstrapperInitExceptions.add(ex);
			}
		}

		for(ClassAccessorData accessor : this.accessors.getClassAccessors()) {
			if(this.hierarchy.getClass(Bootstrapper.class.getClassLoader(), accessor.getAccessorClass().replace(".", "/"), false, null) != null) {
				throw new InstrumentorException(String.format("Accessor class %s was already loaded before or during the bootstrapper initialization!", accessor.getAccessorClass()));
			}
		}

		for(ClassAccessorData accessor : this.accessors.getClassAccessors()) {
			try {
				@SuppressWarnings("unchecked")
				Class<IAccessor> accessorCls = (Class<IAccessor>) Bootstrapper.class.getClassLoader().loadClass(accessor.getAccessorClass());

				if(this.hierarchy.getClass(Bootstrapper.class.getClassLoader(), accessorCls.getName().replace(".", "/"), false, null) == null) {
					throw new InstrumentorException(String.format("Accessor class %s could not be loaded properly!", accessorCls.getName()));
				}
			} catch (ClassNotFoundException e) {
				bootstrapperInitExceptions.add(e);
			}
		}

		synchronized(this) {
			this.initializing = false;
		}

		synchronized(bootstrapperInitExceptions) {
			for(Exception ex : bootstrapperInitExceptions) {
				this.onBootstrapperException(ex);
			}
		}

		for(IInstrumentor instrumentor : this.instrumentors) {
			try {
				instrumentor.onInstrumentorRegistered(instrumentor);
			} catch(Exception ex) {
				this.onBootstrapperException(ex);
			}
		}

		for(IInstrumentor instrumentor : this.instrumentors) {
			try {
				instrumentor.postInitBootstrapper(this);
			} catch(Exception ex) {
				this.onBootstrapperException(ex);
			}
		}
	}

	/**
	 * Creates a new instance of
	 * the specified instrumentor class
	 * @param instrumentorClass
	 * @return
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	private IInstrumentor initInstrumentor(String instrumentorClass) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		@SuppressWarnings("unchecked")
		Class<IInstrumentor> instrumentorCls = (Class<IInstrumentor>) Bootstrapper.class.getClassLoader().loadClass(instrumentorClass);
		return instrumentorCls.getDeclaredConstructor().newInstance();
	}

	/**
	 * Returns the mapper registry
	 * @return
	 */
	public Mappers getMappers() {
		return this.mappers;
	}

	/**
	 * Returns the accessor registry
	 * @return
	 */
	public Accessors getAccessors() {
		return this.accessors;
	}

	/**
	 * Returns the class locator registry
	 * @return
	 */
	public ClassLocators getClassLocators() {
		return this.classLocators;
	}

	/**
	 * Returns whether the bootstrapper is in the initialization phase
	 * @return
	 */
	public synchronized boolean isInitializing() {
		return this.initializing;
	}

	/**
	 * Called when an exception occurs caused by the bootstrapper
	 * @param ex
	 */
	protected void onBootstrapperException(Exception ex) {
		for(IInstrumentor instrumentor : this.instrumentors) {
			instrumentor.onBootstrapperException(ex);
		}
	}
}
