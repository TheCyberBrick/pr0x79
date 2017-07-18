package proxy.mappings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pr0x79.instrumentation.identification.IClassIdentifier;
import pr0x79.instrumentation.identification.IFieldIdentifier;
import pr0x79.instrumentation.identification.IInstructionIdentifier;
import pr0x79.instrumentation.identification.IMethodIdentifier;
import pr0x79.instrumentation.identification.Identifiers;
import proxy.identifiers.IndexInstructionIdentifier;
import proxy.identifiers.IndexLocalVariableIdentifier;
import proxy.identifiers.MethodCallInstructionIdentifier;
import proxy.identifiers.ReturnInstructionIdentifier;
import proxy.identifiers.StringClassIdentifier;
import proxy.identifiers.StringFieldIdentifier;
import proxy.identifiers.StringLocalVariableIdentifier;
import proxy.identifiers.StringMethodIdentifier;

public class MappingsParser {
	/**
	 * Parses a json object into {@link IClassIdentifier}s and registers them
	 * @param json
	 * @param identifiers
	 */
	public static void parseClassIdentifiers(JsonObject json, Identifiers identifiers) {
		for(Entry<String, JsonElement> entry : json.entrySet()) {
			JsonObject entryJson = entry.getValue().getAsJsonObject();
			JsonArray values = entryJson.get("names").getAsJsonArray();
			List<String> mappedNames = new ArrayList<>(values.size());
			for(JsonElement e : values) {
				mappedNames.add(e.getAsString());
			}
			identifiers.registerClassIdentifier(entry.getKey(), new StringClassIdentifier(mappedNames));
		}
	}

	/**
	 * Parses a json object into {@link IFieldIdentifier}s and registers them
	 * @param json
	 * @param identifiers
	 */
	public static void parseFieldIdentifiers(JsonObject json, Identifiers identifiers) {
		for(Entry<String, JsonElement> entry : json.entrySet()) {
			JsonObject entryJson = entry.getValue().getAsJsonObject();
			JsonArray desc = entryJson.get("desc").getAsJsonArray();
			List<String> mappedDescs = new ArrayList<>(desc.size());
			for(JsonElement e : desc) {
				mappedDescs.add(e.getAsString());
			}
			JsonArray values = entryJson.get("names").getAsJsonArray();
			List<String> mappedFieldNames = new ArrayList<>(values.size());
			for(JsonElement e : values) {
				mappedFieldNames.add(e.getAsString());
			}
			identifiers.registerFieldIdentifier(entry.getKey(), new StringFieldIdentifier(mappedFieldNames, mappedDescs));
		}
	}

	/**
	 * Parses a json object into {@link IMethodIdentifier}s and registers them
	 * @param json
	 * @param identifiers
	 */
	public static void parseMethodIdentifiers(JsonObject json, Identifiers identifiers) {
		for(Entry<String, JsonElement> entry : json.entrySet()) {
			JsonObject entryJson = entry.getValue().getAsJsonObject();
			JsonArray descs = entryJson.get("desc").getAsJsonArray();
			List<String> mappedDescs = new ArrayList<>(descs.size());
			for(JsonElement e : descs) {
				mappedDescs.add(e.getAsString());
			}
			JsonArray values = entryJson.get("names").getAsJsonArray();
			List<String> mappedNames = new ArrayList<>(values.size());
			for(JsonElement e : values) {
				mappedNames.add(e.getAsString());
			}
			identifiers.registerMethodIdentifier(entry.getKey(), new StringMethodIdentifier(mappedNames, mappedDescs));
		}
	}

	/**
	 * Parses a json object into {@link IInstructionIdentifier}s and registers them
	 * @param json
	 * @param identifiers
	 */
	public static void parseInstructionIdentifiers(JsonObject json, Identifiers identifiers) {
		for(Entry<String, JsonElement> entry : json.entrySet()) {
			JsonObject entryJson = entry.getValue().getAsJsonObject();
			String type = entryJson.get("type").getAsString().toLowerCase();
			switch(type) {
			case "instruction": {
				String identificationMethod = entryJson.get("identification").getAsString().toLowerCase();
				switch(identificationMethod) {
				case "index": {
					boolean reversed = false;
					if(entryJson.has("reversed")) {
						reversed = entryJson.get("reversed").getAsBoolean();
					}
					int index = entryJson.get("index").getAsInt();
					identifiers.registerInstructionIdentifier(entry.getKey(), new IndexInstructionIdentifier(index, reversed));
					break;
				}
				case "first_return": {
					int offset = 0;
					if(entryJson.has("offset")) {
						offset = entryJson.get("offset").getAsInt();
					}
					identifiers.registerInstructionIdentifier(entry.getKey(), new ReturnInstructionIdentifier(offset, false));
					break;
				}
				case "last_return": {
					int offset = 0;
					if(entryJson.has("offset")) {
						offset = entryJson.get("offset").getAsInt();
					}
					identifiers.registerInstructionIdentifier(entry.getKey(), new ReturnInstructionIdentifier(offset, true));
					break;
				}
				case "method_call": {
					boolean before = true;
					if(entryJson.has("before")) {
						before = entryJson.get("before").getAsBoolean();
					}
					JsonArray desc = entryJson.get("desc").getAsJsonArray();
					List<String> mappedDescs = new ArrayList<>(desc.size());
					for(JsonElement e : desc) {
						mappedDescs.add(e.getAsString());
					}
					JsonArray values = entryJson.get("names").getAsJsonArray();
					List<String> mappedNames = new ArrayList<>(values.size());
					for(JsonElement e : values) {
						mappedNames.add(e.getAsString());
					}
					JsonArray owners = entryJson.get("owners").getAsJsonArray();
					List<String> mappedOwners = new ArrayList<>(owners.size());
					for(JsonElement e : owners) {
						mappedOwners.add(e.getAsString());
					}
					identifiers.registerInstructionIdentifier(entry.getKey(), new MethodCallInstructionIdentifier(mappedOwners, mappedNames, mappedDescs, before));
					break;
				}
				default:
					throw new RuntimeException("Invalid identification type");
				}
				break;
			}
			case "local_variable": {
				String identificationMethod = entryJson.get("identification").getAsString().toLowerCase();
				switch(identificationMethod) {
				case "string":
					JsonArray values = entryJson.get("names").getAsJsonArray();
					List<String> mappedNames = new ArrayList<>(values.size());
					for(JsonElement e : values) {
						mappedNames.add(e.getAsString());
					}
					identifiers.registerInstructionIdentifier(entry.getKey(), new StringLocalVariableIdentifier(mappedNames));
					break;
				case "index":
					boolean reversed = false;
					if(entryJson.has("reversed")) {
						reversed = entryJson.get("reversed").getAsBoolean();
					}
					int index = entryJson.get("index").getAsInt();
					identifiers.registerInstructionIdentifier(entry.getKey(), new IndexLocalVariableIdentifier(index, reversed));
					break;
				default:
					throw new RuntimeException("Invalid identification type");
				}
				break;
			}
			default:
				throw new RuntimeException("Invalid instruction identifier type");
			}
		}
	}
}
