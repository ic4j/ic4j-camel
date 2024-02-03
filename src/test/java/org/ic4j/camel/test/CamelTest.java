package org.ic4j.camel.test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.security.Security;
import java.util.Properties;

import javax.xml.bind.JAXBContext;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.prowidesoftware.swift.model.mx.MxPain00100103;

public final class CamelTest extends CamelTestSupport {
	static Logger LOG;

	static String PROPERTIES_FILE_NAME = "application.properties";
	static final String SIMPLE_XML_NODE_FILE = "SimpleNode.xml";
	static final String SIMPLE_JSON_NODE_FILE = "SimpleNode.json";
	
	static final String SWIFT_XML_NODE_FILE = "CustomerCreditTransferInitiationV03.xml";
	
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
			JAXBContext context = JAXBContext.newInstance(MxPain00100103.class);
	        
	        MxPain00100103 swiftJAXBValue =  (MxPain00100103) context.createUnmarshaller()		
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
	        
		    context = JAXBContext.newInstance(JAXBPojo.class);
		    JAXBPojo pojoJAXBValue =  (JAXBPojo) context.createUnmarshaller()		
		      .unmarshal(new File(getClass().getClassLoader().getResource(SIMPLE_XML_NODE_FILE).getFile()));
		    
	        getMockEndpoint("mock:jaxb").expectedBodiesReceived(pojoJAXBValue);

	        template.sendBody("direct:jaxb", pojoJAXBValue);        

	        MockEndpoint.assertIsSatisfied(this.context());			     
	        
		    // create object mapper instance
		    ObjectMapper mapper = new ObjectMapper();

		    // convert a JSON string to a JacksonPojo object
		    JacksonPojo pojoJacksonValue = mapper.readValue(new File(getClass().getClassLoader().getResource(SIMPLE_JSON_NODE_FILE).getFile()), JacksonPojo.class);
		    
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

//            	from("direct:apache").to("ic:query?url=" + icLocation + "&method=getName&transportType=apache&canisterId=" + icCanister).to("mock:apache");            	

            	from("direct:basic").to("ic:query?url=" + icLocation + "&method=getName&identityType=basic&pemFile=" + ED25519_IDENTITY_FILE + "&canisterId=" + icCanister).to("mock:basic");            	

            	from("direct:secp256k1").to("ic:query?url=" + icLocation + "&method=getName&identityType=secp256k1&pemFile=" + SECP256K1_IDENTITY_FILE + "&canisterId=" + icCanister).to("mock:secp256k1");            	
           	
            	from("direct:pojo").to("ic:query?url=" + icLocation + "&method=echoPojo&canisterId=" + icCanister + "&outClass=org.ic4j.camel.test.Pojo").to("mock:pojo");

            	from("direct:updatepojo").to("ic:update?url=" + icLocation + "&method=updatePojo&canisterId=" + icCanister + "&outClass=org.ic4j.camel.test.Pojo").to("mock:updatepojo");
            	
            	from("direct:jaxb").to("ic:query?url=" + icLocation + "&method=echoPojo&canisterId=" + icCanister + "&inType=jaxb&outType=jaxb&outClass=org.ic4j.camel.test.JAXBPojo").to("mock:jaxb");

               	from("direct:jackson").to("ic:query?url=" + icLocation + "&method=echoPojo&canisterId=" + icCanister + "&inType=jackson&outClass=org.ic4j.camel.test.JacksonPojo").to("mock:jackson");           	

              	from("direct:gson").to("ic:query?url=" + icLocation + "&method=echoPojo&canisterId=" + icCanister + "&inType=gson&outClass=org.ic4j.camel.test.GsonPojo").to("mock:gson");           	
               	
            }
        };
    }

}
