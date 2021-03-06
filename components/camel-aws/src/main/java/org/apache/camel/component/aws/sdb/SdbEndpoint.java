/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.aws.sdb;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DomainMetadataRequest;
import com.amazonaws.services.simpledb.model.NoSuchDomainException;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.component.aws.s3.S3Endpoint;
import org.apache.camel.impl.ScheduledPollEndpoint;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines the <a href="http://camel.apache.org/aws.html">AWS SDB Endpoint</a>.  
 */
@UriEndpoint(scheme = "aws-sdb", title = "AWS SimpleDB", syntax = "aws-sdb:domainName", producerOnly = true, label = "cloud,database,nosql")
public class SdbEndpoint extends ScheduledPollEndpoint {
    
    private static final Logger LOG = LoggerFactory.getLogger(S3Endpoint.class);

    private AmazonSimpleDB sdbClient;

    @UriParam
    private SdbConfiguration configuration;

    @Deprecated
    public SdbEndpoint(String uri, CamelContext context, SdbConfiguration configuration) {
        super(uri, context);
        this.configuration = configuration;
    }
    public SdbEndpoint(String uri, Component component, SdbConfiguration configuration) {
        super(uri, component);
        this.configuration = configuration;
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException("You cannot receive messages from this endpoint");
    }

    public Producer createProducer() throws Exception {
        return new SdbProducer(this);
    }

    public boolean isSingleton() {
        return true;
    }

    @Override
    public void doStart() throws Exception {
        super.doStart();
        
        sdbClient = configuration.getAmazonSDBClient() != null ? configuration.getAmazonSDBClient() : createSdbClient();
        if (ObjectHelper.isNotEmpty(configuration.getAmazonSdbEndpoint())) {
            sdbClient.setEndpoint(configuration.getAmazonSdbEndpoint());
        }
        
        String domainName = getConfiguration().getDomainName();
        LOG.trace("Querying whether domain [{}] already exists...", domainName);

        try {
            sdbClient.domainMetadata(new DomainMetadataRequest(domainName));
            LOG.trace("Domain [{}] already exists", domainName);
            return;
        } catch (NoSuchDomainException ase) {
            LOG.trace("Domain [{}] doesn't exist yet", domainName);
            LOG.trace("Creating domain [{}]...", domainName);
            sdbClient.createDomain(new CreateDomainRequest(domainName));
            LOG.trace("Domain [{}] created", domainName);
        }
    }

    public SdbConfiguration getConfiguration() {
        return configuration;
    }

    public AmazonSimpleDB getSdbClient() {
        return sdbClient;
    }

    AmazonSimpleDB createSdbClient() {
        AmazonSimpleDB client = null;
        AWSCredentials credentials = new BasicAWSCredentials(configuration.getAccessKey(), configuration.getSecretKey());
        if (ObjectHelper.isNotEmpty(configuration.getProxyHost()) && ObjectHelper.isNotEmpty(configuration.getProxyPort())) {
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            clientConfiguration.setProxyHost(configuration.getProxyHost());
            clientConfiguration.setProxyPort(configuration.getProxyPort());
            client = new AmazonSimpleDBClient(credentials, clientConfiguration);
        } else {
            client = new AmazonSimpleDBClient(credentials);
        }
        return client;
    }
}
