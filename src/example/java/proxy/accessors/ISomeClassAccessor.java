package proxy.accessors;

import pr0x79.instrumentation.accessor.ClassAccessor;
import pr0x79.instrumentation.accessor.FieldAccessor;
import pr0x79.instrumentation.accessor.FieldGenerator;
import pr0x79.instrumentation.accessor.IAccessor;
import pr0x79.instrumentation.accessor.Interceptor;
import pr0x79.instrumentation.accessor.LocalVar;
import pr0x79.instrumentation.accessor.MethodAccessor;

@ClassAccessor(classIdentifierId = "SomeClass")
public interface ISomeClassAccessor extends IAccessor {

	/*
	 * This method runs SomeClass#print(String)
	 */
	@MethodAccessor(methodIdentifierId = "SomeClass_print")
	public void printAccessor(String input);

	/*
	 * This method intercepts SomeClass#print(String) right at the beginning of the method and
	 * sets the "input" parameter value to "intercepted input!"
	 */
	@Interceptor(methodIdentifierId = "SomeClass_print", instructionIdentifierId = "start")
	public default void interceptPrint(@LocalVar(instructionIdentifierId = "local_var_1") String input) {
		System.out.println("\n--------Interception--------");
		System.out.println("SomeClass#print(String) intercepted!");
		System.out.println("Parameter \"input\" is: " + input);
		System.out.println("Changing parameter \"input\" to: \"intercepted input!\"");
		input = "intercepted input!";
		System.out.println("-----------------------------\n");
	}

	/*
	 * With @FieldAccessor field getters and setters can be generated.
	 * The names of these methods do not matter, but it is important that they either have one parameter and no return type (setter),
	 * or a return type and no parameter (getter).
	 * 
	 * In this case, the field SomeClass#value is get/set
	 */
	@FieldAccessor(fieldIdentifierId = "SomeClass_value") public String getValue();
	@FieldAccessor(fieldIdentifierId = "SomeClass_value") public void setValue(String value);


	///// Mixins /////

	/*
	 * Mixin methods can easily be added by using default methods
	 */
	public default void testMixinMethod() {
		System.out.println("Mixin method working!");
	}

	/*
	 * Mixin fields can be done with a getter and setter using @FieldGenerator.
	 * Again, the names of the methods do not matter.
	 */
	@FieldGenerator(fieldName = "generatedFieldName") public void setGeneratedFieldValue(float value);
	@FieldGenerator(fieldName = "generatedFieldName") public float getGeneratedFieldValue();
}
