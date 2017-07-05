package program;

public class SomeClass {
	private String value;

	public SomeClass(String value) {
		this.value = value;
	}

	public void print(String input) {
		System.out.println("Running SomeClass#print(String)");
		System.out.println("Input: " + input);
		System.out.println("Value: " + this.value);
	}
}
