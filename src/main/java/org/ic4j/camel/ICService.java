/*
 * Copyright 2021 Exilor Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.ic4j.camel;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ic4j.agent.Agent;
import org.ic4j.agent.AgentBuilder;
import org.ic4j.agent.AgentError;
import org.ic4j.agent.FuncProxy;
import org.ic4j.agent.NonceFactory;
import org.ic4j.agent.ProxyBuilder;
import org.ic4j.agent.ReplicaTransport;
import org.ic4j.agent.Waiter;
import org.ic4j.agent.http.ReplicaApacheHttpTransport;
import org.ic4j.agent.http.ReplicaJavaHttpTransport;
import org.ic4j.agent.http.ReplicaOkHttpTransport;
import org.ic4j.agent.identity.AnonymousIdentity;
import org.ic4j.agent.identity.BasicIdentity;
import org.ic4j.agent.identity.Identity;
import org.ic4j.agent.identity.Secp256k1Identity;
import org.ic4j.candid.ObjectDeserializer;
import org.ic4j.candid.ObjectSerializer;
import org.ic4j.candid.dom.DOMDeserializer;
import org.ic4j.candid.dom.DOMSerializer;
import org.ic4j.candid.gson.GsonDeserializer;
import org.ic4j.candid.gson.GsonSerializer;
import org.ic4j.candid.jackson.JacksonDeserializer;
import org.ic4j.candid.jackson.JacksonSerializer;
import org.ic4j.candid.jaxb.JAXBDeserializer;
import org.ic4j.candid.jaxb.JAXBSerializer;
import org.ic4j.candid.pojo.PojoDeserializer;
import org.ic4j.candid.pojo.PojoSerializer;
import org.ic4j.candid.types.Mode;
import org.ic4j.types.Func;
import org.ic4j.types.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonElement;

public class ICService {
	
	private static final Logger LOG = LoggerFactory.getLogger(ICService.class);	
	static int WAITER_TIMEOUT = 60;
	static int WAITER_SLEEP = 5;
	
	private ICEndpoint endpoint;
	
	private Agent agent;
	
	private int waiterTimeout = WAITER_TIMEOUT;
	private int waiterSleep = WAITER_SLEEP;
	
	ProxyBuilder proxyBuilder;
	

	public ICService(ICEndpoint endpoint) {
		this.endpoint = endpoint;
		
		
		Security.addProvider(new BouncyCastleProvider());
		
		String transportType = endpoint.getTransportType();
	
		
		ReplicaTransport transport;
		
		String url = endpoint.getUrl();
		
		try {
		
		switch (transportType) {
		case "okhttp":
			transport = ReplicaOkHttpTransport.create(url);
			break;
		case "apache":
			transport = ReplicaApacheHttpTransport.create(url);		
			break;
		default:
			transport = ReplicaJavaHttpTransport.create(url);
			break;
		}
		
		String identityType = endpoint.getIdentityType();
		
		Identity identity;
		
		
		String pemFileName = endpoint.getPemFile();
		
		switch (identityType) {
		case "basic":		
			if(pemFileName == null)
			{
				KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
				identity = BasicIdentity.fromKeyPair(keyPair);
			}
			else
				identity = BasicIdentity.fromPEMFile(Paths.get(pemFileName));	
			break;
		case "secp256k1":				
				identity = Secp256k1Identity.fromPEMFile(Paths.get(pemFileName));	
			break;			
		default:
			identity = new AnonymousIdentity();
			break;
		}
			
		
		if(endpoint.getIngressExpiryDuration() == null)
			this.agent = new AgentBuilder().transport(transport).identity(identity).nonceFactory(new NonceFactory())
					.build();
		else
			this.agent = new AgentBuilder().transport(transport).identity(identity).nonceFactory(new NonceFactory()).ingresExpiry(endpoint.getIngressExpiryDuration())
			.build();	
		
		if(endpoint.getFetchRootKey())
			this.agent.fetchRootKey();
		
		if(endpoint.getWaiterTimeout() != null)
			this.waiterTimeout = endpoint.getWaiterTimeout();
		
		if(endpoint.getWaiterSleep() != null)
			this.waiterSleep = endpoint.getWaiterSleep();	
		
		this.proxyBuilder = ProxyBuilder.create(agent);
		
		} catch (Exception e) {
			LOG.error(e.getLocalizedMessage(), e);
		}
	}
	
    public ICEndpoint getEndpoint() {
        return this.endpoint;
    }	


	boolean process(Exchange exchange, AsyncCallback callback) {
		Principal canisterId = Principal.fromString(this.getEndpoint().getCanisterId());
		
		String method = this.getEndpoint().getMethod();
		
		String methodType = this.getEndpoint().getMethodType();
		
		Waiter waiter = Waiter.create(this.waiterTimeout, this.waiterSleep);
		this.proxyBuilder = this.proxyBuilder.waiter(waiter).disableRangeCheck(this.getEndpoint().getFetchRootKey());
		
		if(this.getEndpoint().getEffectiveCanisterId() != null)
		{
			Principal effectiveCanisterId = Principal.fromString(this.getEndpoint().getEffectiveCanisterId());
			this.proxyBuilder = this.proxyBuilder.effectiveCanisterId(effectiveCanisterId);
		}
		
		if(this.getEndpoint().getIdlFile() != null)
		{
			Path idlFilePath = Path.of(this.getEndpoint().getIdlFile());
			this.proxyBuilder = this.proxyBuilder.idlFile(idlFilePath);
		}
		
		this.proxyBuilder = this.proxyBuilder.loadIDL(this.getEndpoint().getLoadIDL());
		
		Func func = new Func(canisterId, method);
		
		FuncProxy<?> funcProxy = this.proxyBuilder.getFuncProxy(func);
		
		ObjectSerializer objectSerializer = this.getSerializer();
		ObjectDeserializer objectDeserializer = this.getDeserializer();
		
		funcProxy.setSerializers(objectSerializer);
		funcProxy.setDeserializer(objectDeserializer);
		
		if(methodType != null)
		{
            if (methodType.equals(ICConfiguration.QUERY_PREFIX)) 
            {
            	Mode[] modes = {Mode.QUERY};
            	funcProxy.setModes(modes);
            }
            else if (methodType.equals(ICConfiguration.ONEWAY_PREFIX))
            {
            	Mode[] modes = {Mode.ONEWAY};
            	funcProxy.setModes(modes);
            }
            else 
            	funcProxy.setModes(new Mode[0]);
		}
					
		try {
			funcProxy.setResponseClass(this.getOutClass());
			Object arg = exchange.getIn().getBody();
			Object response = funcProxy.call(arg);
			
			exchange.getMessage().setBody(response);
		} catch (Exception e) {
			if(e.getCause() != null && e.getCause() instanceof AgentError)
				exchange.setException((AgentError)e.getCause());
			else	
				exchange.setException(AgentError.create(AgentError.AgentErrorCode.CUSTOM_ERROR, e, e.getLocalizedMessage()));			
		}
		
		callback.done(true);
		return true;
	}
	
	
	ObjectSerializer  getSerializer()
	{
			
		ObjectSerializer serializer  = new PojoSerializer();
		
		String type = this.getEndpoint().getInType();
		if(type != null)	
			switch(type) {
				case "jackson":
					return new JacksonSerializer();
				case "gson":
					return new GsonSerializer();					
				case "dom":
					return new DOMSerializer();	
				case "jaxb":
					return new JAXBSerializer();						
			}
		
		return serializer;	
		
	}
	
    
	ObjectDeserializer getDeserializer()
	{
		ObjectDeserializer deserializer = new PojoDeserializer();
		
		String type = this.getEndpoint().getOutType();
		if(type != null)	
			switch(type) {
				case "jackson":
					return new JacksonDeserializer();
				case "gson":
					return new GsonDeserializer();					
				case "dom":
					return new DOMDeserializer();	
				case "jaxb":
					return new JAXBDeserializer();						
			}
		
		return deserializer;
	}
	
	Class<?> getOutClass() throws ClassNotFoundException
	{
		String className = this.getEndpoint().getOutClass();
			
		if(className == null)
		{	
			// assign default jackson, gson and dom classes
			String type = this.getEndpoint().getOutType();
			if(type != null)	
				switch(type) {
					case "jackson":
						return JsonNode.class;
					case "gson":
						return JsonElement.class;					
					case "dom":
						return Node.class;	
					default:
						return null;						
				}
			else
				return null;
		}
		else
		{
			// if is array
			if(className.endsWith("[]"))
			{
				className = className.substring(0, className.length() - 2);
				return Class.forName("[L" + className + ";");
			}
			else
				return Class.forName(className);						
		}
	}
	

}
