package pr0x79.instrumentation.accessor;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation can be added to accessor classes, method accessors and interceptor parameters
 * to disable signature compatibility checking.
 * 
 * @see Interceptor
 * @see IAccessor
 */
@Retention(RUNTIME)
@Target({METHOD, TYPE, PARAMETER})
public @interface UncheckedSignature {

}
