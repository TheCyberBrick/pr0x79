package proxy;

import java.io.InputStreamReader;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import pr0x79.Bootstrapper;
import pr0x79.IInstrumentor;
import proxy.accessors.IMainAccessor;
import proxy.accessors.ISomeClassAccessor;
import proxy.mappings.MappingsParser;

public class Instrumentor implements IInstrumentor {
	@Override
	public void initBootstrapper(Bootstrapper bootstrapper) {
		//This is called as soon as the java agent is initialized

		System.out.println("Instrumentor initialized!");


		System.out.println("Loading mappings");

		//The class, method, field and instruction mappings can be loaded from .json files as demonstrated

		JsonParser parser = new JsonParser();

		JsonElement classMappingsJson = parser.parse(new JsonReader(new InputStreamReader(this.getClass().getResourceAsStream("/mappings/class_mappings.json"))));
		MappingsParser.parseClassIdentifiers(classMappingsJson.getAsJsonObject(), bootstrapper.getIdentifiers());

		JsonElement fieldMappingsJson = parser.parse(new JsonReader(new InputStreamReader(this.getClass().getResourceAsStream("/mappings/field_mappings.json"))));
		MappingsParser.parseFieldIdentifiers(fieldMappingsJson.getAsJsonObject(), bootstrapper.getIdentifiers());

		JsonElement methodMappingsJson = parser.parse(new JsonReader(new InputStreamReader(this.getClass().getResourceAsStream("/mappings/method_mappings.json"))));
		MappingsParser.parseMethodIdentifiers(methodMappingsJson.getAsJsonObject(), bootstrapper.getIdentifiers());

		JsonElement instructionMappingsJson = parser.parse(new JsonReader(new InputStreamReader(this.getClass().getResourceAsStream("/mappings/instruction_mappings.json"))));
		MappingsParser.parseInstructionIdentifiers(instructionMappingsJson.getAsJsonObject(), bootstrapper.getIdentifiers());


		System.out.println("Registering accessors\n");

		//The accessor interfaces are registered here. Their classes must _not_ be loaded before initBootstrapper

		bootstrapper.getAccessors().registerAccessor(IMainAccessor.class);
		bootstrapper.getAccessors().registerAccessor(ISomeClassAccessor.class);
	}

	@Override
	public void onBootstrapperException(Exception ex) {
		//Any exceptions caused by the bootstrapper, bytecode modification or an identifier is redirected here
		//Useful for advanced exception handling

		ex.printStackTrace();
		System.exit(-1);
	}
}
