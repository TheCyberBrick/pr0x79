package program;

import java.util.List;
import java.util.Map;

public class SomeClassBody extends SigTestCls {
	public SomeClass create(String val) {
		return new SomeClass(val);
	}

	public static interface TestIntfs {

	}

	public class SomeClass {
		private String value;

		public SomeClass(String value) {
			this.value = value;
		}

		public <M extends SomeClass> Map<String, List<M>> print(String input) {
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
