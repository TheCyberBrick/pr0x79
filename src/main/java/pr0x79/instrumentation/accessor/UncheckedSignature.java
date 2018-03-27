package pr0x79.instrumentation.accessor;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation can be added to interceptors to disable signature compatibility checking.
 * 
 * @see Interceptor
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface UncheckedSignature {
	/**
	 * Whether the context's "return type" signature should <i>not</i> be checked
	 * @return
	 */
	public boolean context() default true;

	/**
	 * Whether the local variable signatures should <i>not</i> be checked
	 * @return
	 */
	public boolean parameters() default true;
}
