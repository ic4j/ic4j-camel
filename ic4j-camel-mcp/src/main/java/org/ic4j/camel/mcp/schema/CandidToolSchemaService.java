package org.ic4j.camel.mcp.schema;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.camel.BindToRegistry;
import org.ic4j.agent.Agent;
import org.ic4j.agent.AgentBuilder;
import org.ic4j.agent.ReplicaTransport;
import org.ic4j.agent.http.ReplicaJavaHttpTransport;
import org.ic4j.agent.identity.AnonymousIdentity;
import org.ic4j.candid.annotations.Ignore;
import org.ic4j.candid.annotations.Name;
import org.ic4j.candid.parser.IDLParser;
import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Type;
import org.ic4j.types.Principal;

@BindToRegistry("icpCandidToolSchema")
public class CandidToolSchemaService {

	public static final String DEFAULT_LOCATION = "http://127.0.0.1:4943/";
	public static final String DEFAULT_DID_RESOURCE = "/candid/service.did";
	public static final String LEGACY_DID_RESOURCE = "/candid/motoko-sample.did";
	public static final String METADATA_KEY = "x-ic4j-candid";
	public static final String PROPERTY_LOCATION = "icp.mcp.location";
	public static final String PROPERTY_CANISTER_ID = "icp.mcp.canisterId";
	public static final String PROPERTY_DID_RESOURCE = "icp.mcp.didResource";
	public static final String PROPERTY_DEFAULT_POJO_CLASS = "icp.mcp.defaultPojoClass";

	public Map<String, Object> fromEndpoint(Map<String, Object> arguments) throws Exception {
		String methodName = requiredString(arguments, "method");
		String location = stringOrDefault(arguments.get("location"), System.getProperty("sample.ic.location", DEFAULT_LOCATION));
		String canisterId = stringOrDefault(arguments.get("canisterId"), System.getProperty("sample.ic.canister", ""));

		if (canisterId.isBlank()) {
			throw new IllegalArgumentException("sample.ic.canister is not set");
		}

		String did = loadDidFromEndpoint(location, canisterId);
		MethodSchema methodSchema = methodSchemaFromDid(did, methodName);
		return toOpenAiTool(methodName, "Call Candid method " + methodName, methodSchema,
				metadata("endpoint", methodName, methodSchema.modes, canisterId));
	}

	public Map<String, Object> fromDid(Map<String, Object> arguments) throws Exception {
		String methodName = requiredString(arguments, "method");
		String didText = resolveDidText(arguments);
		MethodSchema methodSchema = methodSchemaFromDid(didText, methodName);
		return toOpenAiTool(methodName, "Call Candid method " + methodName, methodSchema,
				metadata("did", methodName, methodSchema.modes, null));
	}

	public Map<String, Object> fromEndpointCatalog(Map<String, Object> arguments) throws Exception {
		String location = stringOrDefault(arguments.get("location"), System.getProperty("sample.ic.location", DEFAULT_LOCATION));
		String canisterId = stringOrDefault(arguments.get("canisterId"), System.getProperty("sample.ic.canister", ""));

		if (canisterId.isBlank()) {
			throw new IllegalArgumentException("sample.ic.canister is not set");
		}

		String did = loadDidFromEndpoint(location, canisterId);
		return toolCatalogFromDid(did, "endpoint", canisterId);
	}

	public Map<String, Object> fromDidCatalog(Map<String, Object> arguments) throws Exception {
		return toolCatalogFromDid(resolveDidText(arguments), "did", null);
	}

	public Map<String, Object> toolsListFromEndpoint(Map<String, Object> arguments) throws Exception {
		String location = stringOrDefault(arguments.get("location"), System.getProperty("sample.ic.location", DEFAULT_LOCATION));
		String canisterId = stringOrDefault(arguments.get("canisterId"), System.getProperty("sample.ic.canister", ""));

		if (canisterId.isBlank()) {
			throw new IllegalArgumentException("sample.ic.canister is not set");
		}

		String did = loadDidFromEndpoint(location, canisterId);
		return toolsListResultFromDid(did, "endpoint", canisterId);
	}

	public Map<String, Object> toolsListFromDid(Map<String, Object> arguments) throws Exception {
		return toolsListResultFromDid(resolveDidText(arguments), "did", null);
	}

	public Map<String, Object> fromPojo(Map<String, Object> arguments) throws Exception {
		String toolName = requiredString(arguments, "toolName");
		String description = requiredString(arguments, "description");
		String className = stringOrDefault(arguments.get("className"), System.getProperty(PROPERTY_DEFAULT_POJO_CLASS, ""));

		if (className.isBlank()) {
			throw new IllegalArgumentException("Missing required argument: className");
		}

		Class<?> pojoClass = Class.forName(className);
		Map<String, Object> schema = schemaFromPojo(pojoClass);
		MethodSchema methodSchema = new MethodSchema(schema, schema, List.of());
		return toOpenAiTool(toolName, description, methodSchema,
				metadata("pojo", pojoClass.getName(), List.of(), null));
	}

	private String loadDidFromEndpoint(String location, String canisterId) throws Exception {
		ReplicaTransport transport = ReplicaJavaHttpTransport.create(location);
		Agent agent = new AgentBuilder().transport(transport).identity(new AnonymousIdentity()).build();
		agent.fetchRootKey();
		return agent.getIDL(Principal.fromString(canisterId));
	}

	private String resolveDidText(Map<String, Object> arguments) throws IOException {
		String didText = stringOrDefault(arguments.get("didText"), "");
		if (!didText.isBlank()) {
			return didText;
		}
		String didResource = stringOrDefault(arguments.get("didResource"), System.getProperty(PROPERTY_DID_RESOURCE, DEFAULT_DID_RESOURCE));
		return loadBundledDid(didResource);
	}

	private MethodSchema methodSchemaFromDid(String didText, String methodName) throws Exception {
		Map<String, MethodSchema> methodSchemas = methodSchemasFromDid(didText);
		MethodSchema methodSchema = methodSchemas.get(methodName);
		if (methodSchema == null) {
			throw new IllegalArgumentException("Method not found in DID: " + methodName);
		}
		return methodSchema;
	}

	private Map<String, Object> toolCatalogFromDid(String didText, String source, String canisterId) throws Exception {
		Map<String, MethodSchema> methodSchemas = methodSchemasFromDid(didText);
		List<Map<String, Object>> tools = new ArrayList<>();
		Map<String, List<String>> groups = new LinkedHashMap<>();
		groups.put("query", new ArrayList<>());
		groups.put("update", new ArrayList<>());
		groups.put("oneway", new ArrayList<>());
		groups.put("unannotated", new ArrayList<>());

		List<Map.Entry<String, MethodSchema>> orderedEntries = new ArrayList<>(methodSchemas.entrySet());
		orderedEntries.sort(Comparator.comparing(Map.Entry::getKey));

		for (Map.Entry<String, MethodSchema> entry : orderedEntries) {
			String methodName = entry.getKey();
			MethodSchema methodSchema = entry.getValue();
			addToGroups(groups, methodName, methodSchema.modes);
			tools.add(toOpenAiTool(methodName, "Call Candid method " + methodName, methodSchema,
					metadata(source, methodName, methodSchema.modes, canisterId)));
		}

		Map<String, Object> service = new LinkedHashMap<>();
		service.put("source", source);
		service.put("methodCount", tools.size());
		service.put("toolNames", orderedEntries.stream().map(Map.Entry::getKey).toList());
		service.put("groups", groups);

		Map<String, Object> catalog = new LinkedHashMap<>();
		catalog.put("tools", tools);
		catalog.put("count", tools.size());
		catalog.put("service", service);
		catalog.put("order", Map.of("type", "lexicographic", "field", "function.name"));
		catalog.put(METADATA_KEY, metadata(source, "*", List.of(), canisterId));
		return catalog;
	}

	private Map<String, Object> toolsListResultFromDid(String didText, String source, String canisterId) throws Exception {
		Map<String, MethodSchema> methodSchemas = methodSchemasFromDid(didText);
		List<Map<String, Object>> tools = new ArrayList<>();
		List<Map.Entry<String, MethodSchema>> orderedEntries = new ArrayList<>(methodSchemas.entrySet());
		orderedEntries.sort(Comparator.comparing(Map.Entry::getKey));

		for (Map.Entry<String, MethodSchema> entry : orderedEntries) {
			tools.add(toMcpToolEntry(entry.getKey(), entry.getValue(), source, canisterId));
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("tools", tools);
		return result;
	}

	private void addToGroups(Map<String, List<String>> groups, String methodName, List<String> modes) {
		if (modes.isEmpty()) {
			groups.get("unannotated").add(methodName);
			return;
		}

		boolean grouped = false;
		for (String mode : modes) {
			String normalized = mode == null ? "" : mode.trim().toLowerCase();
			if (groups.containsKey(normalized)) {
				groups.get(normalized).add(methodName);
				grouped = true;
			}
		}
		if (!grouped) {
			groups.get("unannotated").add(methodName);
		}
	}

	private Map<String, MethodSchema> methodSchemasFromDid(String didText) throws Exception {
		IDLParser parser = new IDLParser(reader(didText));
		parser.parse();

		Map<String, IDLType> services = parser.getServices();
		if (services.isEmpty()) {
			throw new IllegalArgumentException("No service definitions found in DID");
		}

		IDLType serviceType = services.values().iterator().next();
		Map<String, MethodSchema> methodSchemas = new LinkedHashMap<>();
		for (Map.Entry<String, IDLType> entry : serviceType.getMeths().entrySet()) {
			IDLType methodType = entry.getValue();
			methodSchemas.put(entry.getKey(), new MethodSchema(
					schemaFromMethodArgs(methodType.getArgs()),
					schemaFromMethodReturns(methodType.getRets()),
					toModeNames(methodType.getModes())));
		}
		return methodSchemas;
	}

	private Map<String, Object> schemaFromMethodArgs(List<IDLType> args) {
		if (args.isEmpty()) {
			return objectSchema(Map.of(), List.of());
		}

		if (args.size() == 1) {
			IDLType onlyArg = args.get(0);
			if (onlyArg.getType() == Type.RECORD) {
				return schemaForType(onlyArg);
			}

			Map<String, Object> properties = new LinkedHashMap<>();
			properties.put("value", schemaForType(onlyArg));
			return objectSchema(properties, List.of("value"));
		}

		Map<String, Object> properties = new LinkedHashMap<>();
		List<String> required = new ArrayList<>();
		for (int index = 0; index < args.size(); index++) {
			String argName = "arg" + index;
			properties.put(argName, schemaForType(args.get(index)));
			required.add(argName);
		}

		return objectSchema(properties, required);
	}

	private Map<String, Object> schemaFromPojo(Class<?> pojoClass) {
		return schemaFromPojo(pojoClass, new HashSet<>());
	}

	private Map<String, Object> schemaFromPojo(Class<?> pojoClass, Set<Class<?>> visiting) {
		if (!visiting.add(pojoClass)) {
			return simpleSchema("object", "Recursive reference to " + pojoClass.getName());
		}

		Map<String, Object> properties = new LinkedHashMap<>();
		List<String> required = new ArrayList<>();

		try {
			for (Field field : pojoClass.getFields()) {
				if (Modifier.isStatic(field.getModifiers()) || field.isAnnotationPresent(Ignore.class)) {
					continue;
				}

				String fieldName = field.isAnnotationPresent(Name.class)
						? field.getAnnotation(Name.class).value()
						: field.getName();

				org.ic4j.candid.annotations.Field candidField = field.getAnnotation(org.ic4j.candid.annotations.Field.class);
				Map<String, Object> fieldSchema = candidField != null
						? schemaForType(IDLType.createType(candidField.value()))
						: schemaForJavaType(field.getGenericType(), visiting);

				properties.put(fieldName, fieldSchema);
				if (!isOptionalJavaType(field.getGenericType())) {
					required.add(fieldName);
				}
			}
		} finally {
			visiting.remove(pojoClass);
		}

		return objectSchema(properties, required);
	}

	private Map<String, Object> schemaFromMethodReturns(List<IDLType> rets) {
		if (rets.isEmpty()) {
			return Map.of("type", "null");
		}

		if (rets.size() == 1) {
			return schemaForType(rets.get(0));
		}

		Map<String, Object> properties = new LinkedHashMap<>();
		List<String> required = new ArrayList<>();
		for (int index = 0; index < rets.size(); index++) {
			String resultName = "result" + index;
			properties.put(resultName, schemaForType(rets.get(index)));
			required.add(resultName);
		}

		return objectSchema(properties, required);
	}

	private Map<String, Object> schemaForType(IDLType idlType) {
		Type type = idlType.getType();

		return switch (type) {
		case BOOL -> simpleSchema("boolean");
		case TEXT -> simpleSchema("string");
		case PRINCIPAL -> simpleSchema("string", "Internet Computer principal");
		case FLOAT32, FLOAT64 -> simpleSchema("number");
		case NAT, INT, NAT8, NAT16, NAT32, NAT64, INT8, INT16, INT32, INT64 -> simpleSchema("integer");
		case NULL -> Map.of("type", "null");
		case RESERVED -> Map.of("description", "Candid reserved value");
		case EMPTY -> Map.of("not", new LinkedHashMap<>());
		case OPT -> optionalSchema(idlType.getInnerType());
		case VEC -> {
			if (isByteVector(idlType)) {
				yield binarySchema();
			}
			Map<String, Object> arraySchema = new LinkedHashMap<>();
			arraySchema.put("type", "array");
			arraySchema.put("items", schemaForType(idlType.getInnerType()));
			yield arraySchema;
		}
		case RECORD -> {
			Map<String, Object> properties = new LinkedHashMap<>();
			List<String> required = new ArrayList<>();
			for (Map.Entry<Label, IDLType> entry : idlType.getTypeMap().entrySet()) {
				String name = entry.getKey().toString();
				IDLType valueType = entry.getValue();
				properties.put(name, schemaForType(valueType));
				if (valueType.getType() != Type.OPT) {
					required.add(name);
				}
			}
			yield objectSchema(properties, required);
		}
		case VARIANT -> {
			List<Map<String, Object>> oneOf = new ArrayList<>();
			for (Map.Entry<Label, IDLType> entry : idlType.getTypeMap().entrySet()) {
				Map<String, Object> variantProperties = new LinkedHashMap<>();
				variantProperties.put(entry.getKey().toString(), schemaForType(entry.getValue()));
				oneOf.add(objectSchema(variantProperties, List.of(entry.getKey().toString())));
			}
			Map<String, Object> variantSchema = new LinkedHashMap<>();
			variantSchema.put("oneOf", oneOf);
			yield variantSchema;
		}
		case FUNC -> simpleSchema("string", "Candid function reference");
		case SERVICE -> simpleSchema("string", "Candid service reference");
		default -> new LinkedHashMap<>();
		};
	}

	private Map<String, Object> schemaForJavaType(java.lang.reflect.Type reflectedType, Set<Class<?>> visiting) {
		if (reflectedType instanceof ParameterizedType parameterizedType) {
			Class<?> rawClass = rawClass(parameterizedType.getRawType());
			if (rawClass != null) {
				if (Optional.class.isAssignableFrom(rawClass)) {
					return optionalJavaSchema(typeArgument(parameterizedType, 0), visiting);
				}
				if (Collection.class.isAssignableFrom(rawClass) || Iterable.class.isAssignableFrom(rawClass)) {
					return arraySchema(schemaForJavaType(typeArgument(parameterizedType, 0), visiting));
				}
				if (Map.class.isAssignableFrom(rawClass)) {
					return mapSchema(typeArgument(parameterizedType, 1), visiting);
				}
				return schemaForJavaClass(rawClass, reflectedType, visiting);
			}
		}

		if (reflectedType instanceof GenericArrayType genericArrayType) {
			return arraySchema(schemaForJavaType(genericArrayType.getGenericComponentType(), visiting));
		}

		if (reflectedType instanceof TypeVariable<?> typeVariable) {
			java.lang.reflect.Type[] bounds = typeVariable.getBounds();
			return bounds.length == 0 ? unconstrainedObjectSchema() : schemaForJavaType(bounds[0], visiting);
		}

		if (reflectedType instanceof WildcardType wildcardType) {
			java.lang.reflect.Type[] upperBounds = wildcardType.getUpperBounds();
			return upperBounds.length == 0 ? unconstrainedObjectSchema() : schemaForJavaType(upperBounds[0], visiting);
		}

		Class<?> javaType = rawClass(reflectedType);
		if (javaType == null) {
			return unconstrainedObjectSchema();
		}
		return schemaForJavaClass(javaType, reflectedType, visiting);
	}

	private Map<String, Object> schemaForJavaClass(Class<?> javaType, java.lang.reflect.Type reflectedType, Set<Class<?>> visiting) {
		if (javaType == String.class || javaType == Character.class || javaType == char.class) {
			return simpleSchema("string");
		}
		if (javaType == Principal.class) {
			return simpleSchema("string", "Internet Computer principal");
		}
		if (javaType == byte[].class || javaType == Byte[].class) {
			return binarySchema();
		}
		if (javaType == Boolean.class || javaType == boolean.class) {
			return simpleSchema("boolean");
		}
		if (isFloatingPointType(javaType)) {
			return simpleSchema("number");
		}
		if (Number.class.isAssignableFrom(javaType) || isIntegerLikeType(javaType) || javaType == BigInteger.class) {
			return simpleSchema("integer");
		}
		if (javaType.isEnum()) {
			return enumSchema(javaType);
		}
		if (javaType.isArray()) {
			return arraySchema(schemaForJavaType(javaType.getComponentType(), visiting));
		}
		if (Collection.class.isAssignableFrom(javaType) || Iterable.class.isAssignableFrom(javaType)) {
			if (reflectedType instanceof ParameterizedType parameterizedType) {
				return arraySchema(schemaForJavaType(typeArgument(parameterizedType, 0), visiting));
			}
			return arraySchema(unconstrainedObjectSchema());
		}
		if (Map.class.isAssignableFrom(javaType)) {
			if (reflectedType instanceof ParameterizedType parameterizedType) {
				return mapSchema(typeArgument(parameterizedType, 1), visiting);
			}
			return mapSchema(null, visiting);
		}
		if (Optional.class.isAssignableFrom(javaType)) {
			if (reflectedType instanceof ParameterizedType parameterizedType) {
				return optionalJavaSchema(typeArgument(parameterizedType, 0), visiting);
			}
			return optionalJavaSchema(null, visiting);
		}
		if (!javaType.getName().startsWith("java.")) {
			return schemaFromPojo(javaType, visiting);
		}
		return unconstrainedObjectSchema();
	}

	private Map<String, Object> optionalJavaSchema(java.lang.reflect.Type innerType, Set<Class<?>> visiting) {
		Map<String, Object> schema = new LinkedHashMap<>();
		List<Object> anyOf = new ArrayList<>();
		anyOf.add(innerType == null ? unconstrainedObjectSchema() : schemaForJavaType(innerType, visiting));
		anyOf.add(Map.of("type", "null"));
		schema.put("anyOf", anyOf);
		return schema;
	}

	private Map<String, Object> arraySchema(Map<String, Object> itemSchema) {
		Map<String, Object> arraySchema = new LinkedHashMap<>();
		arraySchema.put("type", "array");
		arraySchema.put("items", itemSchema);
		return arraySchema;
	}

	private Map<String, Object> mapSchema(java.lang.reflect.Type valueType, Set<Class<?>> visiting) {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		schema.put("additionalProperties", valueType == null ? Boolean.TRUE : schemaForJavaType(valueType, visiting));
		return schema;
	}

	private Map<String, Object> enumSchema(Class<?> enumType) {
		Map<String, Object> schema = simpleSchema("string", "Java enum " + enumType.getSimpleName());
		Object[] constants = enumType.getEnumConstants();
		List<String> values = new ArrayList<>(constants.length);
		for (Object constant : constants) {
			values.add(((Enum<?>) constant).name());
		}
		schema.put("enum", values);
		return schema;
	}

	private Map<String, Object> unconstrainedObjectSchema() {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		return schema;
	}

	private Map<String, Object> optionalSchema(IDLType innerType) {
		Map<String, Object> schema = new LinkedHashMap<>();
		List<Object> anyOf = new ArrayList<>();
		anyOf.add(schemaForType(innerType));
		anyOf.add(Map.of("type", "null"));
		schema.put("anyOf", anyOf);
		return schema;
	}

	private Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		schema.put("additionalProperties", false);
		schema.put("properties", properties);
		if (!required.isEmpty()) {
			schema.put("required", required);
		}
		return schema;
	}

	private Map<String, Object> simpleSchema(String type) {
		return simpleSchema(type, null);
	}

	private Map<String, Object> simpleSchema(String type, String description) {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", type);
		if (description != null) {
			schema.put("description", description);
		}
		return schema;
	}

	private Map<String, Object> binarySchema() {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "string");
		schema.put("contentEncoding", "base64");
		schema.put("description", "Byte vector encoded as base64");
		return schema;
	}

	private boolean isByteVector(IDLType idlType) {
		IDLType innerType = idlType.getInnerType();
		if (innerType == null) {
			return false;
		}
		Type inner = innerType.getType();
		return inner == Type.NAT8 || inner == Type.INT8;
	}

	private boolean isIntegerLikeType(Class<?> javaType) {
		return javaType == byte.class || javaType == short.class || javaType == int.class || javaType == long.class
				|| javaType == Byte.class || javaType == Short.class || javaType == Integer.class || javaType == Long.class;
	}

	private boolean isFloatingPointType(Class<?> javaType) {
		return javaType == float.class || javaType == double.class
				|| javaType == Float.class || javaType == Double.class || javaType == BigDecimal.class;
	}

	private boolean isOptionalJavaType(java.lang.reflect.Type reflectedType) {
		Class<?> rawClass = rawClass(reflectedType);
		return rawClass != null && Optional.class.isAssignableFrom(rawClass);
	}

	private Class<?> rawClass(java.lang.reflect.Type reflectedType) {
		if (reflectedType instanceof Class<?> javaClass) {
			return javaClass;
		}
		if (reflectedType instanceof ParameterizedType parameterizedType) {
			return rawClass(parameterizedType.getRawType());
		}
		if (reflectedType instanceof GenericArrayType genericArrayType) {
			Class<?> componentType = rawClass(genericArrayType.getGenericComponentType());
			if (componentType != null) {
				return componentType.arrayType();
			}
		}
		return null;
	}

	private java.lang.reflect.Type typeArgument(ParameterizedType parameterizedType, int index) {
		java.lang.reflect.Type[] typeArguments = parameterizedType.getActualTypeArguments();
		if (index < 0 || index >= typeArguments.length) {
			return null;
		}
		return typeArguments[index];
	}

	private List<String> toModeNames(List<?> modes) {
		List<String> values = new ArrayList<>();
		for (Object mode : modes) {
			values.add(String.valueOf(mode));
		}
		return values;
	}

	private Map<String, Object> metadata(String source, String methodName, List<String> modes, String canisterId) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("source", source);
		metadata.put("method", methodName);
		metadata.put("modes", modes);
		if (canisterId != null && !canisterId.isBlank()) {
			metadata.put("canisterId", canisterId);
		}
		return metadata;
	}

	private Map<String, Object> toOpenAiTool(String toolName, String description, MethodSchema methodSchema, Map<String, Object> metadata) {
		Map<String, Object> function = new LinkedHashMap<>();
		function.put("name", toolName);
		function.put("description", description);
		function.put("parameters", methodSchema.inputSchema);

		Map<String, Object> tool = new LinkedHashMap<>();
		tool.put("type", "function");
		tool.put("function", function);
		tool.put("outputSchema", methodSchema.outputSchema);
		tool.put(METADATA_KEY, metadata);
		return tool;
	}

	private Map<String, Object> toMcpToolEntry(String toolName, MethodSchema methodSchema, String source, String canisterId) {
		Map<String, Object> tool = new LinkedHashMap<>();
		tool.put("name", toolName);
		tool.put("title", "Generated " + toolName);
		tool.put("description", "Generated from Candid metadata for method " + toolName);
		tool.put("inputSchema", methodSchema.inputSchema);
		tool.put("outputSchema", methodSchema.outputSchema);

		Map<String, Object> annotations = new LinkedHashMap<>();
		annotations.put("categories", toolCategories(source, methodSchema.modes));
		tool.put("annotations", annotations);
		tool.put("_meta", metadata(source, toolName, methodSchema.modes, canisterId));
		return tool;
	}

	private List<String> toolCategories(String source, List<String> modes) {
		List<String> categories = new ArrayList<>();
		categories.add("generated");
		categories.add("candid");
		categories.add(source);
		for (String mode : modes) {
			String normalized = mode == null ? "" : mode.trim().toLowerCase();
			if (!normalized.isEmpty() && !categories.contains(normalized)) {
				categories.add(normalized);
			}
		}
		return categories;
	}

	private String requiredString(Map<String, Object> arguments, String key) {
		String value = stringOrDefault(arguments.get(key), "");
		if (value.isBlank()) {
			throw new IllegalArgumentException("Missing required argument: " + key);
		}
		return value;
	}

	private String stringOrDefault(Object value, String defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		String text = value.toString();
		return text.isBlank() ? defaultValue : text;
	}

	private String loadBundledDid(String didResource) throws IOException {
		List<String> candidates = new ArrayList<>();
		if (didResource != null && !didResource.isBlank()) {
			candidates.add(didResource);
		}
		if (!candidates.contains(DEFAULT_DID_RESOURCE)) {
			candidates.add(DEFAULT_DID_RESOURCE);
		}
		if (!candidates.contains(LEGACY_DID_RESOURCE)) {
			candidates.add(LEGACY_DID_RESOURCE);
		}

		for (String candidate : candidates) {
			try (InputStream inputStream = getClass().getResourceAsStream(candidate)) {
				if (inputStream != null) {
					return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
				}
			}
		}

		throw new IllegalArgumentException("DID resource not found: " + didResource);
	}

	private Reader reader(String didText) {
		return new InputStreamReader(new java.io.ByteArrayInputStream(didText.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
	}

	private static final class MethodSchema {
		private final Map<String, Object> inputSchema;
		private final Map<String, Object> outputSchema;
		private final List<String> modes;

		private MethodSchema(Map<String, Object> inputSchema, Map<String, Object> outputSchema, List<String> modes) {
			this.inputSchema = inputSchema;
			this.outputSchema = outputSchema;
			this.modes = modes;
		}
	}
}