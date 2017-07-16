package proxy.accessors;

import pr0x79.instrumentation.accessor.ClassAccessor;
import pr0x79.instrumentation.accessor.FieldAccessor;
import pr0x79.instrumentation.accessor.IAccessor;
import pr0x79.instrumentation.accessor.Interceptor;

@ClassAccessor(classIdentifierId = "Main")
public interface IMainAccessor extends IAccessor {

	/*
	 * This method intercepts Main's constructor at the first return instruction (or at the end of the method, if not return is used)
	 */
	@Interceptor(methodIdentifierId = "Main_ctor", instructionIdentifierId = "first_return")
	public default void ctor() {
		System.out.println("\n--------Interception--------");
		System.out.println("Main constructor intercepted!");

		System.out.println("Getting SomeClass proxy object:");
		ISomeClassAccessor someClassProxy = this.getSomeClassObject();
		System.out.println(someClassProxy + "\n");

		System.out.println("Testing mixin method");
		someClassProxy.testMixinMethod();
		System.out.println();

		System.out.println("Testing generated field");
		System.out.println("Current value: " + someClassProxy.getGeneratedFieldValue());
		float newValue = 1.234F;
		System.out.println("Setting value to: " + newValue);
		someClassProxy.setGeneratedFieldValue(newValue);
		System.out.println("Current value: " + someClassProxy.getGeneratedFieldValue() + "\n");

		System.out.println("Getting SomeClass#value through proxy: " + someClassProxy.getValue() + "\n");

		System.out.println("Running SomeClass#print through proxy");
		someClassProxy.printAccessor("Testing print accessor");

		System.out.println("-----------------------------\n");
	}

	/*
	 * These two methods intercept before and after the SomeClass#print call in Main#init
	 */
	@Interceptor(methodIdentifierId = "Main_init", instructionIdentifierId = "before_init_print")
	public default void interceptInitBeforePrint() {
		System.out.println("--------Interception--------");
		System.out.println("Main#init before SomeClass#print call intercepted!");
		System.out.println("-----------------------------");
	}
	@Interceptor(methodIdentifierId = "Main_init", instructionIdentifierId = "after_init_print")
	public default void interceptInitAfterPrint() {
		System.out.println("\n--------Interception--------");
		System.out.println("Main#init after SomeClass#print call intercepted!");
		System.out.println("-----------------------------");
	}

	/*
	 * This method returns the value of the field Main#obj.
	 * Since we have an accessor for SomeClass called ISomeClassAccessor,
	 * we can use ISomeClassAccessor as return type.
	 * Simply using SomeClass as return type would also work
	 */
	@FieldAccessor(fieldIdentifierId = "Main_obj")
	public ISomeClassAccessor getSomeClassObject();

}
