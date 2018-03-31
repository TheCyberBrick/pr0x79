package pr0x79.instrumentation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Used by internal methods for bytecode generation
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD, CONSTRUCTOR })
public @interface Internal {
	String id();
}
