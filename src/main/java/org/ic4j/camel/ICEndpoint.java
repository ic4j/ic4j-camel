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

import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.camel.util.UnsafeUriCharactersEncoder;
import org.ic4j.agent.ReplicaTransport;
import org.ic4j.agent.identity.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UriEndpoint(firstVersion = "3.19.0", scheme = "ic", syntax = "ic:canister", title = "Internet Computer", category = { Category.BLOCKCHAIN })
public class ICEndpoint extends DefaultEndpoint {
	
	private static final Logger LOG = LoggerFactory.getLogger(ICEndpoint.class);
	
	@UriPath(label = "common")
	@Metadata(defaultValue = "update", enums = "query,update", description = "The kind of operation to use",required = true)
    private String methodType;
	
	@UriParam(label = "common")
	@Metadata(required = true)
    private String url;	
	
	@UriParam(label = "common")
	@Metadata(defaultValue = "anonymous", enums = "anonymous,basic,secp256k1", description = "The kind of identity to use",required = false)
    private String identityType = "anonymous"; 
	@UriParam(label = "common")
	@Metadata( defaultValue = "apache", enums = "apache,java,okhttp", description = "The kind of trtansport to use",required = false)
    private String transportType = "apache";    


	@UriPath(label = "common")
    @Metadata(required = true)
    private String method; 	

    @UriParam(label = "common")
    @Metadata
    private String canisterId;  
    
    @UriParam(label = "common")
    @Metadata
    private String effectiveCanisterId;  
    
    @UriParam(label = "common")
    @Metadata
    private String inType;
    @UriParam(label = "common")
    @Metadata
    private String outType;
    
    @UriParam(label = "common")
    @Metadata
    private String outClass;    
    
    @UriParam(label = "common")
    @Metadata
    private String pemFile;     
    
    @UriParam(label = "common")
    private ReplicaTransport transport;
    
    @UriParam(label = "common")
    private Identity identity;  
    
    public ICEndpoint(String uri, ICComponent component, String method, String canisterId, String effectiveCanisterId,
            ReplicaTransport transport, Identity identity) {
    	super(UnsafeUriCharactersEncoder.encode(uri), component);
    	
    	if(method != null)
    		this.method = method;
    	
    	if(canisterId != null)
    		this.canisterId = canisterId;
    	
    	if(effectiveCanisterId != null)
    		this.effectiveCanisterId = effectiveCanisterId;   	
    	
    	if(transport != null)
    		this.transport = transport;  
    	
    	if(identity != null)
    		this.identity = identity;     	
    }
    
    public ICEndpoint(String uri, ICComponent component, String method, String canisterId, String effectiveCanisterId) {
    	super(UnsafeUriCharactersEncoder.encode(uri), component);
    	
    	if(method != null)
    		this.method = method;
    	
    	if(canisterId != null)
    		this.canisterId = canisterId;
    	
    	if(effectiveCanisterId != null)
    		this.effectiveCanisterId = effectiveCanisterId;      	
    	
  	
    } 
    
    public ICEndpoint(String uri, ICComponent component) {
    	super(UnsafeUriCharactersEncoder.encode(uri), component);
    }   

	@Override
	public Producer createProducer() throws Exception {
		Producer producer = new ICProducer(this);
		
		return producer;
	}

	@Override
	public Consumer createConsumer(Processor processor) throws Exception {
		ICConsumer consumer = new ICConsumer(this, processor);
        configureConsumer(consumer);
        
        return consumer;
	}

	/**
	 * @return the methodType
	 */
	public String getMethodType() {
		return methodType;
	}

	/**
	 * @param methodType the methodType to set
	 */
	public void setMethodType(String methodType) {
		this.methodType = methodType;
	}

	/**
	 * @return the identityType
	 */
	public String getIdentityType() {
		return identityType;
	}

	/**
	 * @param identityType the identityType to set
	 */
	public void setIdentityType(String identityType) {
		this.identityType = identityType;
	}

	/**
	 * @return the transportType
	 */
	public String getTransportType() {
		return transportType;
	}

	/**
	 * @param transportType the transportType to set
	 */
	public void setTransportType(String transportType) {
		this.transportType = transportType;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return the method
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * @param method the method to set
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * @return the canisterId
	 */
	public String getCanisterId() {
		return canisterId;
	}

	/**
	 * @param canisterId the canisterId to set
	 */
	public void setCanisterId(String canisterId) {
		this.canisterId = canisterId;
	}

	/**
	 * @return the effectiveCanisterId
	 */
	public String getEffectiveCanisterId() {
		return effectiveCanisterId;
	}

	/**
	 * @param effectiveCanisterId the effectiveCanisterId to set
	 */
	public void setEffectiveCanisterId(String effectiveCanisterId) {
		this.effectiveCanisterId = effectiveCanisterId;
	}

	/**
	 * @return the pemFile
	 */
	public String getPemFile() {
		return pemFile;
	}

	/**
	 * @param pemFile the pemFile to set
	 */
	public void setPemFile(String pemFile) {
		this.pemFile = pemFile;
	}

	/**
	 * @return the transport
	 */
	public ReplicaTransport getTransport() {
		return transport;
	}

	/**
	 * @param transport the transport to set
	 */
	public void setTransport(ReplicaTransport transport) {
		this.transport = transport;
	}

	/**
	 * @return the identity
	 */
	public Identity getIdentity() {
		return identity;
	}

	/**
	 * @param identity the identity to set
	 */
	public void setIdentity(Identity identity) {
		this.identity = identity;
	}

	/**
	 * @return the inType
	 */
	public String getInType() {
		return inType;
	}

	/**
	 * @param inType the inType to set
	 */
	public void setInType(String inType) {
		this.inType = inType;
	}

	/**
	 * @return the outType
	 */
	public String getOutType() {
		return outType;
	}

	/**
	 * @param outType the outType to set
	 */
	public void setOutType(String outType) {
		this.outType = outType;
	}

	/**
	 * @return the outClass
	 */
	public String getOutClass() {
		return outClass;
	}

	/**
	 * @param outClass the outClass to set
	 */
	public void setOutClass(String outClass) {
		this.outClass = outClass;
	}
	
	

}
