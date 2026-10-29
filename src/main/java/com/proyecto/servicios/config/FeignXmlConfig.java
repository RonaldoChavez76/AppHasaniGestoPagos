package com.proyecto.servicios.config;

import feign.Response;
import feign.codec.Decoder;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FeignXmlConfig {

    private final ConcurrentMap<Class<?>, JAXBContext> jaxbContextCache = new ConcurrentHashMap<>();

    @Bean
    public Decoder feignDecoder() {
        return new Decoder() {
            @Override
            public Object decode(Response response, Type type) {
                if (response.body() == null) {
                    return null;
                }
                try {
                    Class<?> targetClass = Class.forName(type.getTypeName());
                    JAXBContext context = getOrCreateContext(targetClass);
                    Unmarshaller unmarshaller = context.createUnmarshaller();
                    return unmarshaller.unmarshal(response.body().asInputStream());
                } catch (Exception exception) {
                    throw new IllegalStateException("No fue posible decodificar la respuesta XML de GestoPago", exception);
                }
            }
        };
    }

    private JAXBContext getOrCreateContext(Class<?> clazz) {
        return jaxbContextCache.computeIfAbsent(clazz, target -> {
            try {
                return JAXBContext.newInstance(target);
            } catch (JAXBException e) {
                throw new IllegalStateException("No fue posible inicializar JAXBContext para " + target.getName(), e);
            }
        });
    }
}

