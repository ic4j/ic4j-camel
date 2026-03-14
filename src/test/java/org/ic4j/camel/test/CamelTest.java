package org.ic4j.camel.test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ic4j.agent.AgentBuilder;
import org.ic4j.agent.ProxyBuilder;
import org.ic4j.agent.ReplicaTransport;
import org.ic4j.agent.http.ReplicaJavaHttpTransport;
import org.ic4j.agent.identity.AnonymousIdentity;
import org.ic4j.candid.jackson.JacksonDeserializer;
import org.ic4j.candid.jackson.JacksonSerializer;
import org.ic4j.types.Func;
import org.ic4j.types.Principal;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import com.prowidesoftware.swift.model.mx.MxPain00100103;

public final class CamelTest extends CamelTestSupport {
	static Logger LOG;

	static String PROPERTIES_FILE_NAME = "application.properties";
	static final String SIMPLE_XML_NODE_FILE = "SimpleNode.xml";
	static final String SIMPLE_JSON_NODE_FILE = "SimpleNode.json";
	
	static final String SWIFT_XML_NODE_FILE = "CustomerCreditTransferInitiationV03.xml";
	static final Path LOCAL_SAMPLE_CANISTER_IDS = Path.of("samples", "mcp-icp-motoko", ".dfx", "local", "canister_ids.json");
	static final String LOCAL_SAMPLE_LOCATION = "http://127.0.0.1:4943/";
	
	protected static String ED25519_IDENTITY_FILE = "Ed25519_identity.pem";	
	protected static String SECP256K1_IDENTITY_FILE = "Secp256k1_identity.pem";	
		
	static Properties env;

	static {
		LOG = LoggerFactory.getLogger(CamelTest.class);
		
		InputStream propInputStream = CamelTest.class.getClassLoader()
				.getResourceAsStream(PROPERTIES_FILE_NAME);

		env = new Properties();
		try {
			env.load(propInputStream);			
		} catch (IOException e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getMessage());
		}		
	}

	@Test
	public void test() {

		try {
			javax.xml.bind.JAXBContext legacyContext = javax.xml.bind.JAXBContext.newInstance(MxPain00100103.class);
	        
	        MxPain00100103 swiftJAXBValue =  (MxPain00100103) legacyContext.createUnmarshaller()		
				      .unmarshal(new File(getClass().getClassLoader().getResource(SWIFT_XML_NODE_FILE).getFile()));
	        
			Security.addProvider(new BouncyCastleProvider());
			
	        getMockEndpoint("mock:update").expectedBodiesReceived("Hello, World!");

	        template.sendBody("direct:update", "World");   	        

	        
	        MockEndpoint.assertIsSatisfied(this.context());
	        
	        
	        getMockEndpoint("mock:loadidl").expectedBodiesReceived("Hello, World!");

	        template.sendBody("direct:loadidl", "World");        

	        MockEndpoint.assertIsSatisfied(this.context());	        
	
	        getMockEndpoint("mock:query").expectedBodiesReceived("World");

	        template.sendBody("direct:query", null);        

	        MockEndpoint.assertIsSatisfied(this.context());	
	        
	        getMockEndpoint("mock:okhttp").expectedBodiesReceived("World");

	        template.sendBody("direct:okhttp", null);        

	        MockEndpoint.assertIsSatisfied(this.context());	
	        
//	        getMockEndpoint("mock:apache").expectedBodiesReceived("World");

//	        template.sendBody("direct:apache", null);        

//	        MockEndpoint.assertIsSatisfied(this.context());	
	        
	        getMockEndpoint("mock:basic").expectedBodiesReceived("World");

	        template.sendBody("direct:basic", null);        

	        MockEndpoint.assertIsSatisfied(this.context());		        
	        
			// Record POJO

			Pojo pojoValue = new Pojo();

			pojoValue.bar = Boolean.TRUE;
			pojoValue.foo = BigInteger.valueOf(42);
			
	        getMockEndpoint("mock:pojo").expectedBodiesReceived(pojoValue);

	        template.sendBody("direct:pojo", pojoValue);        

	        MockEndpoint.assertIsSatisfied(this.context());	
	        
	        getMockEndpoint("mock:updatepojo").expectedBodiesReceived(pojoValue);

	        template.sendBody("direct:updatepojo", pojoValue);        

	        MockEndpoint.assertIsSatisfied(this.context());		        
	        
		    jakarta.xml.bind.JAXBContext jakartaContext = jakarta.xml.bind.JAXBContext.newInstance(JAXBPojo.class);
		    JAXBPojo pojoJAXBValue =  (JAXBPojo) jakartaContext.createUnmarshaller()		
		      .unmarshal(new File(getClass().getClassLoader().getResource(SIMPLE_XML_NODE_FILE).getFile()));
		    
	        getMockEndpoint("mock:jaxb").expectedBodiesReceived(pojoJAXBValue);

	        template.sendBody("direct:jaxb", pojoJAXBValue);        

	        MockEndpoint.assertIsSatisfied(this.context());		
	        
	        jakarta.xml.bind.JAXBContext jakartaPojoContext = jakarta.xml.bind.JAXBContext.newInstance(JakartaJAXBPojo.class);
		    JakartaJAXBPojo pojoJakartaJAXBValue =  (JakartaJAXBPojo) jakartaPojoContext.createUnmarshaller()		
		      .unmarshal(new File(getClass().getClassLoader().getResource(SIMPLE_XML_NODE_FILE).getFile()));
		    
	        getMockEndpoint("mock:jakarta").expectedBodiesReceived(pojoJakartaJAXBValue);

	        template.sendBody("direct:jakarta", pojoJakartaJAXBValue);        

	        MockEndpoint.assertIsSatisfied(this.context());		        
	        
		    // create object mapper instance
		    ObjectMapper mapper = new ObjectMapper();

		    // convert a JSON string to a JacksonPojo object
		    JacksonPojo pojoJacksonValue = mapper.readValue(new File(getClass().getClassLoader().getResource(SIMPLE_JSON_NODE_FILE).getFile()), JacksonPojo.class);
		    JsonNode pojoJacksonNode = mapper.readTree(new File(getClass().getClassLoader().getResource(SIMPLE_JSON_NODE_FILE).getFile()));
		    
	        getMockEndpoint("mock:jackson").expectedBodiesReceived(pojoJacksonValue);

	        template.sendBody("direct:jackson", pojoJacksonValue);        

	        MockEndpoint.assertIsSatisfied(this.context());	
	        
	        // create Gson instance
	        Gson gson = new Gson();
	        
	        Reader reader = new FileReader(new File(getClass().getClassLoader().getResource(SIMPLE_JSON_NODE_FILE).getFile()));
	        
		    // convert a JSON string to a GsonPojo object
		    GsonPojo pojoGsonValue = gson.fromJson(reader, GsonPojo.class);
		    
	        getMockEndpoint("mock:gson").expectedBodiesReceived(pojoGsonValue);

	        template.sendBody("direct:gson", pojoGsonValue);        

	        MockEndpoint.assertIsSatisfied(this.context());	        

		} catch (Exception e) {
			LOG.error(e.getLocalizedMessage(), e);
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void testAgentJacksonLoadIdlMap() throws Exception {
		String localCanisterId = getLocalSampleCanisterId();

		ReplicaTransport transport = ReplicaJavaHttpTransport.create(LOCAL_SAMPLE_LOCATION);
		var agent = new AgentBuilder().transport(transport).identity(new AnonymousIdentity()).build();
		agent.fetchRootKey();
		Principal canister = Principal.fromString(localCanisterId);

		Map<String, Object> pojoJacksonMap = new LinkedHashMap<>();
		pojoJacksonMap.put("bar", Boolean.TRUE);
		pojoJacksonMap.put("foo", 42);

		var proxy = ProxyBuilder.create(agent).disableRangeCheck(true).loadIDL(true).getFuncProxy(new Func(canister, "echoPojo"));
		proxy.setSerializers(new JacksonSerializer());
		proxy.setDeserializer(new JacksonDeserializer());
		proxy.setResponseClass(JsonNode.class);

		JsonNode response = (JsonNode) proxy.call(pojoJacksonMap);

		Assertions.assertEquals(true, response.get("bar").booleanValue());
		Assertions.assertEquals(42, response.get("foo").intValue());
	}

	@Test
	public void testCamelJacksonLoadIdlMapLocal() throws Exception {
		String localCanisterId = getLocalSampleCanisterId();

		Map<String, Object> pojoJacksonMap = new LinkedHashMap<>();
		pojoJacksonMap.put("bar", Boolean.TRUE);
		pojoJacksonMap.put("foo", 42);

		String endpoint = "ic:query?url=" + LOCAL_SAMPLE_LOCATION
				+ "&method=echoPojo&canisterId=" + localCanisterId
				+ "&inType=jackson&outType=jackson&loadIDL=true&fetchRootKey=true";

		JsonNode response = template.requestBody(endpoint, pojoJacksonMap, JsonNode.class);

		Assertions.assertEquals(true, response.get("bar").booleanValue());
		Assertions.assertEquals(42, response.get("foo").intValue());
	}

	@Test
	public void testCamelJacksonLoadIdlMapFails() {
		Map<String, Object> pojoJacksonMap = new LinkedHashMap<>();
		pojoJacksonMap.put("bar", Boolean.TRUE);
		pojoJacksonMap.put("foo", 42);

		CamelExecutionException exception = Assertions.assertThrows(
			CamelExecutionException.class,
			() -> template.requestBody("direct:jacksonloadidl", pojoJacksonMap, JsonNode.class));

		Throwable cause = exception.getCause();
		Assertions.assertNotNull(cause);
		Assertions.assertTrue(cause.getMessage().contains("unexpected IDL type when parsing Int"));
	}

	private String getLocalSampleCanisterId() throws Exception {
		Assumptions.assumeTrue(Files.exists(LOCAL_SAMPLE_CANISTER_IDS), "Local dfx sample is not deployed");

		ObjectMapper mapper = new ObjectMapper();
		JsonNode canisterIds = mapper.readTree(LOCAL_SAMPLE_CANISTER_IDS.toFile());
		JsonNode localCanister = canisterIds.path("motoko_sample").path("local");
		Assumptions.assumeTrue(!localCanister.isMissingNode() && !localCanister.asText().isBlank(), "Local motoko_sample canister id is unavailable");
		return localCanister.asText();
	}
	
    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
            	
    			String icLocation = env.getProperty("ic.location");
    			String icCanister = env.getProperty("ic.canister");
    			
            	//ICEndpoint endpoint = (ICEndpoint) endpoint("ic:update:https://m7sm4-2iaaa-aaaab-qabra-cai.ic0.app/?method=greet");
            	
                from("direct:update").to("ic:update?url=" + icLocation + "&method=greet&canisterId=" + icCanister).to("mock:update");
                
                from("direct:loadidl").to("ic:update?url=" + icLocation + "&method=greet&loadIDL=true&canisterId=" + icCanister).to("mock:loadidl");

            	from("direct:query").to("ic:query?url=" + icLocation + "&method=getName&canisterId=" + icCanister).to("mock:query");
 
            	from("direct:okhttp").to("ic:query?url=" + icLocation + "&method=getName&transportType=okhttp&canisterId=" + icCanister).to("mock:okhttp");

            	from("direct:apache").to("ic:query?url=" + icLocation + "&method=getName&transportType=apache&canisterId=" + icCanister).to("mock:apache");            	

            	from("direct:basic").to("ic:query?url=" + icLocation + "&method=getName&identityType=basic&pemFile=" + ED25519_IDENTITY_FILE + "&canisterId=" + icCanister).to("mock:basic");            	

            	from("direct:secp256k1").to("ic:query?url=" + icLocation + "&method=getName&identityType=secp256k1&pemFile=" + SECP256K1_IDENTITY_FILE + "&canisterId=" + icCanister).to("mock:secp256k1");            	
           	
            	from("direct:pojo").to("ic:query?url=" + icLocation + "&method=echoPojo&canisterId=" + icCanister + "&outClass=org.ic4j.camel.test.Pojo").to("mock:pojo");

            	from("direct:updatepojo").to("ic:update?url=" + icLocation + "&method=updatePojo&canisterId=" + icCanister + "&outClass=org.ic4j.camel.test.Pojo").to("mock:updatepojo");
            	
            	from("direct:jaxb").to("ic:query?url=" + icLocation + "&method=echoPojo&canisterId=" + icCanister + "&inType=jaxb&outType=jaxb&outClass=org.ic4j.camel.test.JAXBPojo").to("mock:jaxb");

               	from("direct:jakarta").to("ic:query?url=" + icLocation + "&method=echoPojo&canisterId=" + icCanister + "&inType=jakarta&outType=jaxb&outClass=org.ic4j.camel.test.JakartaJAXBPojo").to("mock:jakarta");

            	from("direct:jackson").to("ic:query?url=" + icLocation + "&method=echoPojo&canisterId=" + icCanister + "&inType=jackson&outClass=org.ic4j.camel.test.JacksonPojo").to("mock:jackson");           	

	            	from("direct:jacksonloadidl").to("ic:query?url=" + icLocation + "&method=echoPojo&canisterId=" + icCanister + "&inType=jackson&outType=jackson&loadIDL=true").to("mock:jacksonloadidl");

              	from("direct:gson").to("ic:query?url=" + icLocation + "&method=echoPojo&canisterId=" + icCanister + "&inType=gson&outClass=org.ic4j.camel.test.GsonPojo").to("mock:gson");           	
               	
            }
        };
    }

}
