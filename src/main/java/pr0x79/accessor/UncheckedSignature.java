package pr0x79.accessor;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import pr0x79.Internal;

/**
 * This annotation can be added to field accessors, method accessors and interceptor parameters
 * to disable signature compatibility checking.
 * 
 * @see Interceptor
 * @see IAccessor
 */
@Retention(RUNTIME)
@Target({METHOD, TYPE, PARAMETER})
public @interface UncheckedSignature {
	/**
	 * Whether signature compatibility should be checked for inputs (i.e. field getters and interceptor parameters)
	 * @return
	 */
	@Internal(id = "in")
	public boolean in() default false;

	/**
	 * Whether signature compatibility should be checked for outputs (i.e. return types, field setters and interceptor parameters)
	 * @return
	 */
	@Internal(id = "out")
	public boolean out() default false;
}
