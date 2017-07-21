package pr0x79.instrumentation.accessor;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation must be added to all the parameters of a {@link Interceptor}
 * method. The parameters will have the value of the identified local variable
 * at the time of the call. Any changes made to the parameters in the method body
 * will also change the local variables of the intercepted method.
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface LocalVar {
	public static final String INSTRUCTION_IDENTIFIER = "instructionIdentifier";

	/**
	 * The ID of the local variable instruction identifier that is responsible
	 * for identifying the local variable to be imported
	 * @return
	 */
	public String instructionIdentifier();
}
