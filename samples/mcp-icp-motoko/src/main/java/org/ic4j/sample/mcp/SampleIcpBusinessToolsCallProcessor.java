package org.ic4j.sample.mcp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dscope.camel.mcp.processor.AbstractMcpRequestProcessor;

@BindToRegistry("icpBusinessToolsCall")
public class SampleIcpBusinessToolsCallProcessor extends AbstractMcpRequestProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(SampleIcpBusinessToolsCallProcessor.class);

	@Override
	protected void handleRequest(Exchange exchange, Map<String, Object> parameters) throws Exception {
		String toolName = getToolName(exchange);
		String location = requiredExchangeProperty(exchange, "icp.mcp.location");
		String canisterId = requiredExchangeProperty(exchange, "icp.mcp.canisterId");

		switch (toolName) {
		case "greet" -> handleGreet(exchange, parameters, location, canisterId);
		case "get-name" -> handleGetName(exchange, location, canisterId);
		case "echo-pojo" -> handleEchoPojo(exchange, parameters, location, canisterId);
		default -> writeError(exchange, -32602, "Unknown tool: " + toolName, 400);
		}
	}

	private void handleGreet(Exchange exchange, Map<String, Object> parameters, String location, String canisterId)
			throws Exception {
		String name = requiredString(parameters, "name");
		try (ProducerTemplate template = exchange.getContext().createProducerTemplate()) {
			try {
				template.sendBody(onewayUri(location, canisterId, "greet"), name);
			} catch (Exception ex) {
				LOG.info("Ignoring local replica update verification error: {}", ex.getMessage());
			}

			Thread.sleep(250L);
			Object current = template.requestBody(queryUri(location, canisterId, "getName"), (Object) null);
			writeTextResult(exchange, "Hello, " + current + "!");
		}
	}

	private void handleGetName(Exchange exchange, String location, String canisterId) throws Exception {
		try (ProducerTemplate template = exchange.getContext().createProducerTemplate()) {
			Object current = template.requestBody(queryUri(location, canisterId, "getName"), (Object) null);
			writeTextResult(exchange, current == null ? "" : current.toString());
		}
	}

	private void handleEchoPojo(Exchange exchange, Map<String, Object> parameters, String location, String canisterId)
			throws Exception {
		try (ProducerTemplate template = exchange.getContext().createProducerTemplate()) {
			Object response = template.requestBody(echoPojoUri(location, canisterId), parameters);
			writeStructuredResult(exchange, response);
		}
	}

	private String requiredExchangeProperty(Exchange exchange, String key) {
		String value = exchange.getProperty(key, String.class);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Missing required exchange property: " + key);
		}
		return value;
	}

	private String requiredString(Map<String, Object> parameters, String key) {
		Object value = parameters == null ? null : parameters.get(key);
		if (value == null || value.toString().isBlank()) {
			throw new IllegalArgumentException("Missing required argument: " + key);
		}
		return value.toString();
	}

	private String onewayUri(String location, String canisterId, String method) {
		return "ic:oneway?url=" + location
				+ "&method=" + method
				+ "&canisterId=" + canisterId
				+ "&fetchRootKey=true";
	}

	private String queryUri(String location, String canisterId, String method) {
		return "ic:query?url=" + location
				+ "&method=" + method
				+ "&canisterId=" + canisterId
				+ "&fetchRootKey=true";
	}

	private String echoPojoUri(String location, String canisterId) {
		return "ic:query?url=" + location
				+ "&method=echoPojo"
				+ "&canisterId=" + canisterId
				+ "&inType=jackson&outType=jackson&loadIDL=true&fetchRootKey=true";
	}

	private void writeTextResult(Exchange exchange, String text) {
		Map<String, Object> envelope = createEnvelopeSkeleton();
		envelope.put("id", getJsonRpcId(exchange));
		envelope.put("result", Map.of("content", List.of(Map.of("type", "text", "text", text))));
		writeJson(exchange, envelope);
		applyJsonResponseHeaders(exchange, 200);
	}

	private void writeStructuredResult(Exchange exchange, Object payload) {
		Map<String, Object> envelope = createEnvelopeSkeleton();
		envelope.put("id", getJsonRpcId(exchange));

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("content", List.of(Map.of("type", "text", "text", "JSON payload returned")));
		result.put("structuredContent", payload);
		envelope.put("result", result);

		writeJson(exchange, envelope);
		applyJsonResponseHeaders(exchange, 200);
	}

	private void writeError(Exchange exchange, int code, String message, int statusCode) {
		Map<String, Object> envelope = createEnvelopeSkeleton();
		envelope.put("id", getJsonRpcId(exchange));
		envelope.put("error", Map.of("code", code, "message", message));
		writeJson(exchange, envelope);
		applyJsonResponseHeaders(exchange, statusCode);
	}
}