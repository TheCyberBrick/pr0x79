package program;

import java.util.List;
import java.util.Map;

public class SomeClass {
	private String value;

	public SomeClass(String value) {
		this.value = value;
	}

	public <T extends Main> Map<String, List<T>> print(String input) {
		System.out.println("Running SomeClass#print(String)");
		System.out.println("Input: " + input);
		System.out.println("Value: " + this.value);
		return null;
	}
}
