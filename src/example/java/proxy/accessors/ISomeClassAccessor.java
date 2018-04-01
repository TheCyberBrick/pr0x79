package proxy.accessors;

import java.util.List;
import java.util.Map;

import pr0x79.accessor.ClassAccessor;
import pr0x79.accessor.FieldAccessor;
import pr0x79.accessor.FieldGenerator;
import pr0x79.accessor.IAccessor;
import pr0x79.accessor.IInterceptorContext;
import pr0x79.accessor.Interceptor;
import pr0x79.accessor.LocalVar;
import pr0x79.accessor.MethodAccessor;
import program.Main;
import program.Main.MainSub;
import proxy.Instrumentor;

@ClassAccessor(classIdentifier = "SomeClass")
public interface ISomeClassAccessor extends IAccessor {

	/*
	 * This method runs SomeClass#print(String)
	 */
	@MethodAccessor(methodIdentifier = "SomeClass_print")
	public Map printAccessor(String input);

	/*
	 * This method intercepts SomeClass#print(String) right at the beginning of the method and
	 * sets the "input" parameter value to "intercepted input!"
	 */
	@Interceptor(methodIdentifier = "SomeClass_print", instructionIdentifier = "start",
			exitInstructionIdentifiers = {"first_return-1", "first_return-2"})
	public default 

	<M extends Main<M>> 

	void interceptPrint(@LocalVar(instructionIdentifier = "local_var_1") String input, 

			IInterceptorContext<Map<String, List<? extends M>>> 

	context) {
		System.out.println("\n--------Interception--------");
		System.out.println("SomeClass#print(String) intercepted!");
		System.out.println("Parameter \"input\" is: " + input);
		System.out.println("Changing parameter \"input\" to: \"intercepted input!\"");
		input = "intercepted input!";
		System.out.println("-----------------------------\n");

		//context.exitAt(0);

		//context.returnWith(null);
	}

	/*
	 * With @FieldAccessor field getters and setters can be generated.
	 * The names of these methods do not matter, but it is important that they either have one parameter and no return type (setter),
	 * or a return type and no parameter (getter).
	 * 
	 * In this case, the field SomeClass#value is get/set
	 */
	@FieldAccessor(fieldIdentifier = "SomeClass_value") public String getValue();
	@FieldAccessor(fieldIdentifier = "SomeClass_value") public void setValue(String value);


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
	@FieldGenerator(fieldNameIdentifier = "generatedFieldName") public void setGeneratedFieldValue(float value);
	@FieldGenerator(fieldNameIdentifier = "generatedFieldName") public float getGeneratedFieldValue();
}
