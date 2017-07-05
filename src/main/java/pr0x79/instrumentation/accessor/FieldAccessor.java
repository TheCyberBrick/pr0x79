package pr0x79.instrumentation.accessor;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import pr0x79.instrumentation.identification.IFieldIdentifier;

/**
 * This annotation can be added to abstract or static methods in an {@link IAccessor} with either
 * a return type and no parameters (getter), or a single parameter and the return type void,
 * this class or the type of the parameter (setter). If this class or the parameter type is used
 * as return type, the method will return this object respectively the parameter, similar to the builder pattern.
 * The parent {@link IAccessor} will generate the code for a setter, for a
 * method with a single parameter and without a return type, or a getter, for a method
 * with a return type and no parameters.
 * <p>
 * The return (getter) and parameter (setter) types must match exactly the type of the targetted field.
 * The only exception are other {@link IAccessor}s for these types, which may be used
 * instead of the original type.
 * <p>
 * If the field is static, the accessor method must also be static. If the accessor is static
 * the field identifier must be static (see {@link IFieldIdentifier#isStatic()})
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface FieldAccessor {
	/**
	 * The ID of the field identifier that is responsible
	 * for identifying the field
	 * @return
	 */
	public String fieldIdentifierId();
}
