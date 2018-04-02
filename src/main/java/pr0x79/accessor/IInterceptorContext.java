package pr0x79.accessor;

import pr0x79.Internal;
import pr0x79.identification.IInstructionIdentifier;

public interface IInterceptorContext<T> {
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
	 * @return
	 */
	@Internal(id = "get_exit")
	public int getExit();

	/**
	 * Returns whether an exit index was set and the
	 * interceptor will exit at a different instruction
	 * @return
	 */
	@Internal(id = "is_exiting")
	public boolean isExiting();

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
	 * @return
	 */
	@Internal(id = "get_return")
	public T getReturn();

	/**
	 * Returns whether a return value was set and the
	 * intercepted method will return after the interception
	 * is over
	 * @return
	 */
	@Internal(id = "is_returning")
	public boolean isReturning();

	/**
	 * Populated with all local variables after the interceptor terminates
	 * @return
	 */
	@Internal(id = "get_local_variables")
	public Object[] getLocalVariables();
}
