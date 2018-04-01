package pr0x79.accessor;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import pr0x79.Internal;

/**
 * This annotation can be added to default methods in an {@link IAccessor}.
 * The parent {@link IAccessor} will generate the code for the invocation
 * of the proxied method. The return type of the method must be void, and
 * it must have a {@link IInterceptorContext} parameter.
 * @see {@link IAccessor}, {@link IInterceptorContext}, {@link LocalVar}
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Interceptor {
	/**
	 * The IDs of the instruction identifiers that are responsible
	 * for identifying the exit instructions used by {@link IInterceptorContext#exitAt(int)}
	 * @return
	 */
	@Internal(id = "exit_instruction_identifiers")
	public String[] exitInstructionIdentifiers() default {};
	
	/**
	 * The ID of the method identifier that is responsible
	 * for identifying the method
	 * @return
	 */
	@Internal(id = "method_identifier")
	public String methodIdentifier();

	/**
	 * The ID of the instruction identifier that is responsible
	 * for identifying the instruction where the interceptor is injected
	 * @return
	 */
	@Internal(id = "instruction_identifier")
	public String instructionIdentifier();
}
