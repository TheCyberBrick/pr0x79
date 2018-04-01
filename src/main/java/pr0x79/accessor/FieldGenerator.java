package pr0x79.accessor;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import pr0x79.Internal;

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
	/**
	 * The ID of the field identifier that is responsible
	 * for generating the field name
	 * @return
	 */
	@Internal(id = "field_name_identifier")
	public String fieldNameIdentifier();
}
