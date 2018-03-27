package pr0x79.instrumentation.accessor;

import pr0x79.instrumentation.Internal;
import pr0x79.instrumentation.identification.IInstructionIdentifier;

public interface IInterceptorContext<T> {
	@Internal(id = "do_not_exit")
	public static final int DO_NOT_EXIT = -1;
	
	@Internal(id = "do_not_return")
	public static final Object DO_NOT_RETURN = new Object();
	
	/**
	 * Causes the interception to exit at the instruction identified by the specified {@link IInstructionIdentifier}
	 * @param index The index of the {@link IInstructionIdentifier} specified in {@link Interceptor#exitInstructionIdentifiers()}
	 */
	public void exitAt(int index);
	
	/**
	 * Cancels {@link #exitAt(int)}
	 */
	public void cancelExit();
	
	/**
	 * Returns the exit index (see {@link #exitAt(int)})
	 * or {@link #DO_NOT_EXIT} if no exit index was specified
	 * @return
	 */
	@Internal(id = "get_exit")
	public int getExit();
	
	/**
	 * Causes the intercepted method to return with the specified value
	 * @param obj The object to be returned
	 */
	public void returnWith(T obj);
	
	/**
	 * Cancels {@link #returnWith(Object)}
	 */
	public void cancelReturn();
	
	/**
	 * Returns the return value (see {@link #returnWith(Object)}
	 * or {@link #DO_NOT_RETURN} if not return value was specified
	 * @return
	 */
	@Internal(id = "get_return")
	public T getReturn();
	
	/**
	 * Populated with all local variables after the interceptor terminates
	 * @return
	 */
	@Internal(id = "get_local_variables")
	public Object[] getLocalVariables();
}
