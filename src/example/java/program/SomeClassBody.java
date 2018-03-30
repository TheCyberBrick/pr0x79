package program;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import proxy.Instrumentor;
import proxy.accessors.IMainAccessor;

public class SomeClassBody<G extends Main & IMainAccessor, K extends Instrumentor> extends SigTestCls<G> {
	public SomeClass create(String val) {
		return new SomeClass(val);
	}

	public interface TestIntfs<K> {

	}

	public class SomeClass<G extends Main & IMainAccessor & TestIntfs<K>> {
		private String value;

		public SomeClass(String value) {
			this.value = value;
		}

		class Test {
			
		}
		
		public <A extends G, T extends Main & TestIntfs<G> & IMainAccessor, F extends TestIntfs<ArrayList<? super G>>, Agent> Map<Agent[], List<? extends T>> print(String input) {
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
