package pr0x79.instrumentation.accessor;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation must be added to all {@link IAccessor}s
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface ClassAccessor {
	public static final String CLASS_IDENTIFIER = "classIdentifier";
	
	/**
	 * The ID of the class identifier that is responsible
	 * for identifying the class
	 * @return
	 */
	public String classIdentifier();
}
