package program;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import proxy.Instrumentor;
import proxy.accessors.IMainAccessor;
import proxy.accessors.ISomeClassAccessor;

public class SomeClassBody<G extends Main & IMainAccessor> extends SigTestCls<G> {
	public SomeClass create(String val) {
		return new SomeClass(val);
	}
	
	public class SomeClass {
		private String value;

		public SomeClass(String value) {
			this.value = value;
		}

		public <T extends Main & ISomeClassAccessor & IMainAccessor, F extends ArrayList<G>, Agent> Map<String, List<? extends T>> print(String input) {
			System.out.println("Running SomeClass#print(String)");
			System.out.println("Input: " + input);
			System.out.println("Value: " + this.value);
			return null;
		}
	}
}
