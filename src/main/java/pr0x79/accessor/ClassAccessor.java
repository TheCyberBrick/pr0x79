package pr0x79.accessor;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import pr0x79.Internal;

/**
 * This annotation must be added to all {@link IAccessor}s
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface ClassAccessor {
	/**
	 * The ID of the class identifier that is responsible
	 * for identifying the class
	 * @return
	 */
	@Internal(id = "class_identifier")
	public String classIdentifier();
}