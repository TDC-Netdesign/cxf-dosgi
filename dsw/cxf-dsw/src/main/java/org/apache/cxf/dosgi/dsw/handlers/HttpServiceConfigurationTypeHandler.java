/** 
  * Licensed to the Apache Software Foundation (ASF) under one 
  * or more contributor license agreements. See the NOTICE file 
  * distributed with this work for additional information 
  * regarding copyright ownership. The ASF licenses this file 
  * to you under the Apache License, Version 2.0 (the 
  * "License"); you may not use this file except in compliance 
  * with the License. You may obtain a copy of the License at 
  * 
  * http://www.apache.org/licenses/LICENSE-2.0 
  * 
  * Unless required by applicable law or agreed to in writing, 
  * software distributed under the License is distributed on an 
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
  * KIND, either express or implied. See the License for the 
  * specific language governing permissions and limitations 
  * under the License. 
  */
package org.apache.cxf.dosgi.dsw.handlers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.OsgiUtils;
import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.discovery.ServiceEndpointDescription;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

public class HttpServiceConfigurationTypeHandler extends AbstractPojoConfigurationTypeHandler {
    private static final Logger LOG = Logger.getLogger(HttpServiceConfigurationTypeHandler.class.getName());

    Set<ServiceReference> httpServiceReferences = new CopyOnWriteArraySet<ServiceReference>(); 

    protected HttpServiceConfigurationTypeHandler(BundleContext dswBC,
                                                  CxfDistributionProvider dp,
                                                  Map<String, Object> handlerProps) {
        super(dswBC, dp, handlerProps);

        ServiceTracker st = new ServiceTracker(dswBC, HttpService.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                httpServiceReferences.add(reference);
                return super.addingService(reference);
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                httpServiceReferences.remove(reference);
                super.removedService(reference, service);
            }                        
        };
        st.open();
    }

    public Object createProxy(ServiceReference serviceReference,
            BundleContext dswContext, BundleContext callingContext,
            Class<?> iClass, ServiceEndpointDescription sd) {
        // This handler doesn't make sense on the client side
        return null;
    }

    public Server createServer(ServiceReference serviceReference,
                               BundleContext dswContext, 
                               BundleContext callingContext,
                               ServiceEndpointDescription sd, 
                               Class<?> iClass, 
                               Object serviceBean) {
        String contextRoot = getServletContextRoot(sd, iClass);
        if (contextRoot == null) {
            LOG.warning("Remote address is unavailable");
            return null;
        }

        CXFNonSpringServlet cxf = new CXFNonSpringServlet();
        HttpService httpService = getHttpService();
        try {
            httpService.registerServlet(contextRoot, cxf, new Hashtable<String, String>(), null);
            LOG.info("Successfully registered CXF DOSGi servlet at " + contextRoot);
        } catch (Exception e) {
            throw new ServiceException("CXF DOSGi: problem registering CXF HTTP Servlet", e);
        }        
        Bus bus = cxf.getBus();
        
        String address = constructAddress(dswContext, contextRoot);
        ServerFactoryBean factory = createServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceClass(iClass);
        factory.setAddress("/");
        factory.getServiceFactory().setDataBinding(new AegisDatabinding());
        factory.setServiceBean(serviceBean);

        try {
            String [] intents = 
                applyIntents(dswContext, callingContext, factory.getFeatures(), factory, sd);

            Server server = factory.create();
            getDistributionProvider().addExposedService(serviceReference, registerPublication(server, intents, address));
            addAddressProperty(sd.getProperties(), address);
            return server;
        } catch (IntentUnsatifiedException iue) {
            getDistributionProvider().intentsUnsatisfied(serviceReference);
            throw iue;
        }       
    }
    
    private Map<String, String> registerPublication(Server server, String[] intents, String address) {
        Map<String, String> publicationProperties = super.registerPublication(server, intents);

        
        publicationProperties.put(Constants.POJO_ADDRESS_PROPERTY, address);

        return publicationProperties;
    }    

    private String constructAddress(BundleContext ctx, String contextRoot) {
        String port = null;
        boolean https = false;
        if ("true".equalsIgnoreCase(ctx.getProperty("org.osgi.service.http.secure.enabled"))) {
            https = true;
            port = ctx.getProperty("org.osgi.service.http.port.secure");            
        } else {
            port = ctx.getProperty("org.osgi.service.http.port");            
        }
        if (port == null) {
            port = "8080";
        }
        
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostName = "localhost";
        }

        return getAddress(https ? "https" : "http", hostName, port, contextRoot);
    }

    private HttpService getHttpService() {
        for (ServiceReference sr : httpServiceReferences) {
            Object svc = bundleContext.getService(sr);
            if (svc instanceof HttpService) {
                return (HttpService) svc;
            }
        }
        throw new ServiceException("CXF DOSGi: No HTTP Service could be found to publish CXF endpoint in.");
    }

    private String getServletContextRoot(ServiceEndpointDescription sd, Class<?> iClass) {
        String context = OsgiUtils.getProperty(sd, Constants.POJO_HTTP_SERVICE_CONTEXT);
        if (context == null) {
            context = "/" + iClass.getName().replace('.', '/');
            LOG.info("Using a default address : " + context);
        }
        return context;
    }
}