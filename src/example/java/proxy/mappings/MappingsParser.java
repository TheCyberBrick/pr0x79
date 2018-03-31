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
import pr0x79.instrumentation.identification.Mappers;
import proxy.identifiers.IndexInstructionIdentifier;
import proxy.identifiers.IndexLocalVariableIdentifier;
import proxy.identifiers.MethodCallInstructionIdentifier;
import proxy.identifiers.ReturnInstructionIdentifier;
import proxy.identifiers.StringClassIdentifier;
import proxy.identifiers.StringFieldIdentifier;
import proxy.identifiers.StringLocalVariableIdentifier;
import proxy.identifiers.StringMethodIdentifier;

public class MappingsParser {
	//TODO Fix this up with the new mappers
	
	/**
	 * Parses a json object into {@link IClassIdentifier}s and registers them
	 * @param json
	 * @param mappers
	 */
	public static void parseClassIdentifiers(JsonObject json, Mappers mappers) {
		for(Entry<String, JsonElement> entry : json.entrySet()) {
			JsonObject entryJson = entry.getValue().getAsJsonObject();
			JsonArray values = entryJson.get("names").getAsJsonArray();
			List<String> mappedNames = new ArrayList<>(values.size());
			for(JsonElement e : values) {
				mappedNames.add(e.getAsString());
			}
			mappers.registerClassMapper(entry.getKey(), (str, search) -> str.equals(entry.getKey()) ? new StringClassIdentifier(mappedNames) : null);
		}
	}

	/**
	 * Parses a json object into {@link IFieldIdentifier}s and registers them
	 * @param json
	 * @param mappers
	 */
	public static void parseFieldIdentifiers(JsonObject json, Mappers mappers) {
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
			mappers.registerFieldMapper(entry.getKey(), (str, search) -> str.equals(entry.getKey()) ? new StringFieldIdentifier(mappedFieldNames, mappedDescs) : null);
		}
	}

	/**
	 * Parses a json object into {@link IMethodIdentifier}s and registers them
	 * @param json
	 * @param mappers
	 */
	public static void parseMethodIdentifiers(JsonObject json, Mappers mappers) {
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
			mappers.registerMethodMapper(entry.getKey(), (str, search) -> str.equals(entry.getKey()) ? new StringMethodIdentifier(mappedNames, mappedDescs) : null);
		}
	}

	/**
	 * Parses a json object into {@link IInstructionIdentifier}s and registers them
	 * @param json
	 * @param mappers
	 */
	public static void parseInstructionIdentifiers(JsonObject json, Mappers mappers) {
		for(Entry<String, JsonElement> entry : json.entrySet()) {
			JsonObject entryJson = entry.getValue().getAsJsonObject();
			String type = entryJson.get("type").getAsString().toLowerCase();
			switch(type) {
			case "instruction": {
				String identificationMethod = entryJson.get("identification").getAsString().toLowerCase();
				switch(identificationMethod) {
				case "index": {
					final boolean reversed = entryJson.has("reversed") && entryJson.get("reversed").getAsBoolean();
					int index = entryJson.get("index").getAsInt();
					mappers.registerInstructionMapper(entry.getKey(), (str, search) -> str.equals(entry.getKey()) ? new IndexInstructionIdentifier(index, reversed) : null);
					break;
				}
				case "first_return": {
					final int offset = entryJson.has("offset") ? entryJson.get("offset").getAsInt() : 0;
					mappers.registerInstructionMapper(entry.getKey(), (str, search) -> str.equals(entry.getKey()) ? new ReturnInstructionIdentifier(offset, false) : null);
					break;
				}
				case "last_return": {
					final int offset = entryJson.has("offset") ? entryJson.get("offset").getAsInt() : 0;
					mappers.registerInstructionMapper(entry.getKey(), (str, search) -> str.equals(entry.getKey()) ? new ReturnInstructionIdentifier(offset, true) : null);
					break;
				}
				case "method_call": {
					final boolean before = entryJson.has("before") ? entryJson.get("before").getAsBoolean() : true;
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
					mappers.registerInstructionMapper(entry.getKey(), (str, search) -> str.equals(entry.getKey()) ? new MethodCallInstructionIdentifier(mappedOwners, mappedNames, mappedDescs, before) : null);
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
					mappers.registerInstructionMapper(entry.getKey(), (str, search) -> str.equals(entry.getKey()) ? new StringLocalVariableIdentifier(mappedNames) : null);
					break;
				case "index":
					final boolean reversed = entryJson.has("reversed") ? entryJson.get("reversed").getAsBoolean() : false;
					int index = entryJson.get("index").getAsInt();
					mappers.registerInstructionMapper(entry.getKey(), (str, search) -> str.equals(entry.getKey()) ? new IndexLocalVariableIdentifier(index, reversed) : null);
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
