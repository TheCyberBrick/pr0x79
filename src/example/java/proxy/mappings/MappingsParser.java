package proxy.mappings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pr0x79.instrumentation.identification.IClassIdentifier;
import pr0x79.instrumentation.identification.IFieldIdentifier;
import pr0x79.instrumentation.identification.IInstructionIdentifier;
import pr0x79.instrumentation.identification.IMethodIdentifier;
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
	 * @param map
	 */
	public static void parseClassIdentifiers(JsonObject json, Map<String, IClassIdentifier> map) {
		for(Entry<String, JsonElement> entry : json.entrySet()) {
			JsonObject entryJson = entry.getValue().getAsJsonObject();
			JsonArray values = entryJson.get("names").getAsJsonArray();
			List<String> mappedNames = new ArrayList<>(values.size());
			for(JsonElement e : values) {
				mappedNames.add(e.getAsString());
			}
			map.put(entry.getKey(), new StringClassIdentifier(mappedNames));
		}
	}

	/**
	 * Parses a json object into {@link IFieldIdentifier}s and registers them
	 * @param json
	 * @param map
	 */
	public static void parseFieldIdentifiers(JsonObject json, Map<String, IFieldIdentifier> map) {
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
			map.put(entry.getKey(), new StringFieldIdentifier(mappedFieldNames, mappedDescs));
		}
	}

	/**
	 * Parses a json object into {@link IMethodIdentifier}s and registers them
	 * @param json
	 * @param map
	 */
	public static void parseMethodIdentifiers(JsonObject json, Map<String, IMethodIdentifier> map) {
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
			map.put(entry.getKey(), new StringMethodIdentifier(mappedNames, mappedDescs));
		}
	}

	/**
	 * Parses a json object into {@link IInstructionIdentifier}s and registers them
	 * @param json
	 * @param map
	 */
	public static void parseInstructionIdentifiers(JsonObject json, Map<String, IInstructionIdentifier> map) {
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
					map.put(entry.getKey(), new IndexInstructionIdentifier(index, reversed));
					break;
				}
				case "first_return": {
					final int offset = entryJson.has("offset") ? entryJson.get("offset").getAsInt() : 0;
					map.put(entry.getKey(), new ReturnInstructionIdentifier(offset, false));
					break;
				}
				case "last_return": {
					final int offset = entryJson.has("offset") ? entryJson.get("offset").getAsInt() : 0;
					map.put(entry.getKey(), new ReturnInstructionIdentifier(offset, true));
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
					map.put(entry.getKey(), new MethodCallInstructionIdentifier(mappedOwners, mappedNames, mappedDescs, before));
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
					map.put(entry.getKey(), new StringLocalVariableIdentifier(mappedNames));
					break;
				case "index":
					final boolean reversed = entryJson.has("reversed") ? entryJson.get("reversed").getAsBoolean() : false;
					int index = entryJson.get("index").getAsInt();
					map.put(entry.getKey(), new IndexLocalVariableIdentifier(index, reversed));
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
