package proxy;

import java.lang.instrument.Instrumentation;

import pr0x79.Bootstrapper;

public class Agent {
	/*
	 * This is the java agent.
	 * Compile the code as jar.
	 * The full name of this class has to be specified in the MANIFEST.MF
	 * like this:
	 * 
	 * Premain-Class: proxy.Agent
	 */

	public static void premain(String args, Instrumentation inst) {
		System.out.println("Agent premain called!\n");
		
		//The first parameter specifies all instrumentors.
		//It is important that you do _not_ load any of the instrumentor classes yourself before the Bootstrapper does
		Bootstrapper.initialize(new String[]{ "proxy.Instrumentor" }, inst);
	}
}
