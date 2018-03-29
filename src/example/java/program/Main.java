package program;

import program.SomeClassBody.SomeClass;

public class Main {

	/*
	 * This is the program that will be intercepted.
	 * Compile as a jar with this class as Main class.
	 * Run with -javaagent:agent.jar (-javaagent arg must be before -jar!)
	 */

	public static void main(String[] args) {
		System.out.println("Starting program!\n");

		new Main();
	}

	private SomeClass obj;

	private Main() {
		this.init();
	}

	private void init() {
		this.obj = new SomeClassBody().create("Hello World!");

		this.obj.print("Testing input value");
	}
}
