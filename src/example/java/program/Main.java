package program;

import org.objectweb.asm.Type;

import program.SomeClassBody.SomeClass;
import program.SomeClassBody.TestIntfs;
import proxy.accessors.IMainAccessor;
import proxy.accessors.ISomeClassAccessor;

public class Main implements TestIntfs {

	/*
	 * This is the program that will be intercepted.
	 * Compile as a jar with this class as Main class.
	 * Run with -javaagent:agent.jar (-javaagent arg must be before -jar!)
	 */

	public static void main(String[] args) {
		System.out.println("Starting program!\n");

		new Main();
	}

	public static class MainSub extends Main {
		
	}
	
	private SomeClass obj;

	public Main() {
		this.init();
	}

	static interface ArrItf {

	}

	static class ArrTest implements ArrItf {

	}

	private void init() {
		this.obj = new SomeClassBody().create("Hello World!");

		this.obj.print("Testing input value");

		try {
			System.out.println("INNER CLASS: " + Type.getType(Class.forName("program.SomeClassBody$SomeClass")).getInternalName());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Type type = Type.getType(Integer.class);
		System.out.println("INTEGER SORT: " + type.getSort());
	}
}
