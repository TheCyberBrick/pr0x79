package pr0x79.instrumentation.accessor;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation can be added to interceptor methods marked
 * by {@link Interceptor}. The method interceptor return type
 * must be boolean. If the method interceptor returns true all instructions until
 * the identified instruction are skipped.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface InterceptorConditional {
	public static final String INSTRUCTION_IDENTIFIER_ID = "instructionIdentifierId";

	/**
	 * The ID of the instruction identifier that is responsible
	 * for identifying the instruction that should be jumped to
	 * @return
	 */
	public String instructionIdentifierId();
}
