package pr0x79.instrumentation.accessor;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation can be added to default methods in an {@link IAccessor}.
 * The parent {@link IAccessor} will generate the code for the invocation
 * of the proxied method. The return type of the method must be void, unless an
 * {@link InterceptorConditional} or {@link InterceptorReturn} annotation is used.
 * <p>
 * If the method is static, the interceptor method must also be static.
 * @see {@link InterceptorConditional}, {@link InterceptorReturn}, {@link LocalVar}
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Interceptor {
	public static final String METHOD_IDENTIFIER = "methodIdentifier";
	public static final String INSTRUCTION_IDENTIFIER = "instructionIdentifier";

	/**
	 * The ID of the method identifier that is responsible
	 * for identifying the method
	 * @return
	 */
	public String methodIdentifier();

	/**
	 * The ID of the instruction identifier that is responsible
	 * for identifying the instruction where the interceptor is injected
	 * @return
	 */
	public String instructionIdentifier();
}
