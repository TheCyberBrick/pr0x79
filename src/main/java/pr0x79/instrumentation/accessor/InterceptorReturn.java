package pr0x79.instrumentation.accessor;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation can be added to interceptor methods marked
 * by {@link Interceptor}. The method interceptor return type
 * must match the return type of the method to be intercepted.
 * Adding this annotation to an interceptor causes it
 * to exit the intercepted method with the returned value.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface InterceptorReturn { }
