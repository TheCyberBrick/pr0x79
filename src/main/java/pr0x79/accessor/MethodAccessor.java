package pr0x79.accessor;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import pr0x79.Internal;
import pr0x79.identification.IMethodIdentifier;

/**
 * This annotation can be added to abstract methods in an {@link IAccessor}.
 * The parent {@link IAccessor} will generate the code for the invocation
 * of the proxied method.
 * <p>
 * The return and parameter types must match exactly the types of the method to be proxied.
 * The only exception are other {@link IAccessor}s for these types, which may be used
 * instead of the original type.
 * <p>
 * If the method is static, the accessor method must also be static. If the accessor is static
 * the method identifier must be static (see {@link IMethodIdentifier#isStatic()})
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface MethodAccessor {
	/**
	 * The ID of the method identifier that is responsible
	 * for identifying the method
	 * @return
	 */
	@Internal(id = "method_identifier")
	public String methodIdentifier();
}
