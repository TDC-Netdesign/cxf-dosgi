package org.apache.cxf.dosgi.rest.intents;

import dk.netdesign.dialer.backend.properties.DialerProperties;
import io.swagger.models.Swagger;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.swagger.Swagger2Customizer;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.apache.cxf.service.model.BindingInfo;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by nmw on 06-04-2017.
 */
@Component //
        (
                property = "org.apache.cxf.dosgi.IntentName==dosgi.default.swagger"
        )
public class SwaggerIntent implements Callable<List<Object>> {

    private String version = "";
    private String name = "";
    private static final Logger LOG = LogUtils.getL7dLogger(SwaggerIntent.class);

    @Activate
    public void activate(BundleContext bundleContext)  {
        Dictionary<String, String> headers = bundleContext.getBundle().getHeaders();
        version = headers.get("Bundle-Version");
        name = headers.get("Bundle-Name");




    }


    @Override
    public List<Object> call() throws Exception {

        Swagger2Feature swagger = new Swagger2Feature();


        String port="9090";

        String hostOrIp= getPreferredHost();

        swagger.setHost(hostOrIp+":"+port);
        Swagger2Customizer swagger2Customizer = new Swagger2Customizer() {
            @Override
            public Swagger customize(Swagger data) {
                //Detect if wildcard (0.0.0.0) have been used and if so call getPreferredHost() and set that as part of the address.
                return super.customize(data);
            }
        };
        swagger.setCustomizer(swagger2Customizer);

        swagger.setUsePathBasedConfig(true);
        swagger.setPrettyPrint(true);
        swagger.setSupportSwaggerUi(true);
        swagger.setScan(false);
        swagger.setTitle(name);
        swagger.setDescription(name);
        swagger.setVersion(version);
        swagger.setLicenseUrl("NA");
        swagger.setLicense("Commercial");
        swagger.setContact("development@tdcnetdesign.dk");

        return Arrays.asList((swagger));
    }


    private String getPreferredHost(){

            List<InetAddress> addrList           = new ArrayList<InetAddress>();
            Enumeration<NetworkInterface> enumNI = null;
            try {
                enumNI = NetworkInterface.getNetworkInterfaces();
                while ( enumNI.hasMoreElements() ){
                    NetworkInterface ifc             = enumNI.nextElement();
                    if( ifc.isUp() ){
                        Enumeration<InetAddress> enumAdds     = ifc.getInetAddresses();
                        while ( enumAdds.hasMoreElements() ){
                            InetAddress addr                  = enumAdds.nextElement();
                            if(!addr.isLoopbackAddress()) {
                                addrList.add(addr);
                            }

                        }
                    }
                }
            } catch (SocketException e) {
                LOG.log(Level.SEVERE,"failure to get host info",e);
            }

            return addrList.get(0).getCanonicalHostName();


    }
}

