package pr0x79.instrumentation.accessor;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * See {@link FieldAccessor}.
 * This annotation additionally causes the parent {@link IAccessor}
 * to generate a private field, if the specified field does not
 * exist yet.
 * 
 * @see FieldAccessor
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface FieldGenerator {
	public static final String FIELD_NAME = "fieldName";
	
	/**
	 * Name of the field to be generated and proxied
	 * @return
	 */
	public String fieldName();
}
