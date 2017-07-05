package pr0x79;

import pr0x79.instrumentation.accessor.IAccessor;
import pr0x79.instrumentation.identification.IClassIdentifier;
import pr0x79.instrumentation.identification.IFieldIdentifier;
import pr0x79.instrumentation.identification.IInstructionIdentifier;
import pr0x79.instrumentation.identification.IMethodIdentifier;

/**
 * <b>The class that implements {@link IInstrumentor}, and any {@link IAccessor} used therein, must not be loaded before the bootstrapper is initialized</b>.
 * Instead, the full class name of the instrumentor must be passed in to {@link Bootstrapper#initialize(String[], java.lang.instrument.Instrumentation)}.
 * The instrumentor must have a no-args constructor.
 */
public interface IInstrumentor {
	/**
	 * Called after the bootstrapper has been initialized.
	 * @param bootstrapper
	 */
	default public void postInitBootstrapper(Bootstrapper bootstrapper) { }

	/**
	 * Called when the bootstrapper is initialized.
	 * Identifiers ({@link IClassIdentifier}, {@link IMethodIdentifier}, {@link IFieldIdentifier}, {@link IInstructionIdentifier}) and {@link IAccessor}s must be registered during the bootstrapper initialization.
	 * @param bootstrapper
	 */
	default public void initBootstrapper(Bootstrapper bootstrapper) { }

	/**
	 * Called when an instrumentor is registered to the bootstrapper
	 * @param instrumentor
	 */
	default public void onInstrumentorRegistered(IInstrumentor instrumentor) { }

	/**
	 * Called when an exception occurs caused by the bootstrapper
	 * @param ex
	 */
	default public void onBootstrapperException(Exception ex) {
		throw new RuntimeException(ex);
	}
}
