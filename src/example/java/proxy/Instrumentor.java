package proxy;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import pr0x79.Bootstrapper;
import pr0x79.IInstrumentor;
import pr0x79.identification.IClassIdentifier;
import pr0x79.identification.IFieldIdentifier;
import pr0x79.identification.IInstructionIdentifier;
import pr0x79.identification.IMethodIdentifier;
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
		Map<String, IClassIdentifier> classIdentifiers = new HashMap<>();
		MappingsParser.parseClassIdentifiers(classMappingsJson.getAsJsonObject(), classIdentifiers);
		bootstrapper.getMappers().registerClassMapper("json", (identifier, type) -> classIdentifiers.get(identifier));

		JsonElement fieldMappingsJson = parser.parse(new JsonReader(new InputStreamReader(this.getClass().getResourceAsStream("/mappings/field_mappings.json"))));
		Map<String, IFieldIdentifier> fieldIdentifiers = new HashMap<>();
		MappingsParser.parseFieldIdentifiers(fieldMappingsJson.getAsJsonObject(), fieldIdentifiers);
		bootstrapper.getMappers().registerFieldMapper("json", (identifier, type) -> fieldIdentifiers.get(identifier));

		JsonElement methodMappingsJson = parser.parse(new JsonReader(new InputStreamReader(this.getClass().getResourceAsStream("/mappings/method_mappings.json"))));
		Map<String, IMethodIdentifier> methodIdentifiers = new HashMap<>();
		MappingsParser.parseMethodIdentifiers(methodMappingsJson.getAsJsonObject(), methodIdentifiers);
		bootstrapper.getMappers().registerMethodMapper("json", (identifier, type) -> methodIdentifiers.get(identifier));

		JsonElement instructionMappingsJson = parser.parse(new JsonReader(new InputStreamReader(this.getClass().getResourceAsStream("/mappings/instruction_mappings.json"))));
		Map<String, IInstructionIdentifier> instructionIdentifier = new HashMap<>();
		MappingsParser.parseInstructionIdentifiers(instructionMappingsJson.getAsJsonObject(), instructionIdentifier);
		bootstrapper.getMappers().registerInstructionMapper("json", (identifier, type) -> instructionIdentifier.get(identifier));

		System.out.println("Registering accessors\n");

		//The accessor interfaces are registered here. Their classes must _not_ be loaded before or during initBootstrapper

		bootstrapper.getAccessors().registerAccessor("proxy.accessors.IMainAccessor");
		bootstrapper.getAccessors().registerAccessor("proxy.accessors.ISomeClassAccessor");
	}

	@Override
	public void onBootstrapperException(Exception ex) {
		//Any exceptions caused by the bootstrapper, bytecode modification or an identifier is redirected here
		//Useful for advanced exception handling

		ex.printStackTrace();
		System.exit(-1);
	}
}
