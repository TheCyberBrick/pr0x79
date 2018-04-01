package pr0x79.accessor;

/**
 * <b>The class that implements {@link IAccessor} must not be loaded before the bootstrapper is initialized</b>.
 * Any accessor must extend this interface and be annotated with {@link ClassAccessor}.
 * Accessor interfaces must not contain any abstract (in other words non-default)
 * methods except those marked with {@link FieldAccessor}, {@link FieldGenerator} or
 * {@link MethodAccessor}.
 * Methods can be annotated with {@link FieldAccessor}, {@link FieldGenerator}, {@link MethodAccessor} or
 * {@link Interceptor}
 */
public interface IAccessor {

}
