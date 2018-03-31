package program;

import java.util.List;
import java.util.Map;

import pr0x79.instrumentation.accessor.IAccessor;

public class SomeClassBody extends SigTestCls {
	public SomeClass create(String val) {
		return new SomeClass(val);
	}

	public interface TestIntfs<K> {

	}

	public class SomeClass {
		private String value;

		public SomeClass(String value) {
			this.value = value;
		}

		class Test extends SomeClass implements TestIntfs {

			public Test(SomeClassBody someClassBody, String value) {
				someClassBody.super(value);
				// TODO Auto-generated constructor stub
			}
			
		}
		
		public <M extends Main<M>> Map<String, List<? extends M>> print(String input) {
			System.out.println("Running SomeClass#print(String)");
			System.out.println("Input: " + input);
			System.out.println("Value: " + this.value);
			
			/*class InnerMethodTest implements TestIntfs<T> {
				
			}
			
			System.out.println(InnerMethodTest.class);*/
			
			return null;
		}
	}
}
