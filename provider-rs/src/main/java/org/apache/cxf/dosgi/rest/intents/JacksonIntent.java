package org.apache.cxf.dosgi.rest.intents;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.apache.cxf.common.i18n.Exception;
import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationFeature;
import org.apache.cxf.jaxrs.validation.JAXRSParameterNameProvider;
import org.apache.cxf.jaxrs.validation.ValidationExceptionMapper;
import org.osgi.service.component.annotations.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Provides a default intent if your should should use Jackson, BeanValidation and Parameter mapping for web exceptions.
 * Created by nmwael.
 */

@Component
        (
                property = "org.apache.cxf.dosgi.IntentName=dosgi.default.jackson"
        )
public class JacksonIntent implements Callable<List<Object>> {

    @Override
    public List<Object> call() throws Exception {
        JacksonJaxbJsonProvider jacksonJaxbJsonProvider = new JacksonJaxbJsonProvider();

//        BeanValidationProvider beanValidationProvider=new BeanValidationProvider(new OSGIHibernateValidationDiscoverer());

        JAXRSBeanValidationFeature jaxRSBeanValidationFeature = new JAXRSBeanValidationFeature();
        //      jaxRSBeanValidationFeature.setProvider(beanValidationProvider);
        ValidationExceptionMapper validationExceptionMapper = new ValidationExceptionMapper();
        validationExceptionMapper.setAddMessageToResponse(true);

        WebApplicationExceptionMapper webApplicationExceptionMapper = new WebApplicationExceptionMapper();
        webApplicationExceptionMapper.setAddMessageToResponse(true);
        return Arrays.asList(jacksonJaxbJsonProvider, jaxRSBeanValidationFeature, validationExceptionMapper, webApplicationExceptionMapper, new JAXRSParameterNameProvider());
    }

}