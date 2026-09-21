package com.proyecto.servicios.config;

import feign.Response;
import feign.codec.Decoder;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Type;

public class FeignXmlConfig {

    @Bean
    public Decoder feignDecoder() {
        return new Decoder() {
            @Override
            public Object decode(Response response, Type type) {
                if (response.body() == null) {
                    return null;
                }
                try {
                    JAXBContext context = JAXBContext.newInstance(Class.forName(type.getTypeName()));
                    Unmarshaller unmarshaller = context.createUnmarshaller();
                    return unmarshaller.unmarshal(response.body().asInputStream());
                } catch (Exception exception) {
                    throw new IllegalStateException("No fue posible decodificar la respuesta XML de GestoPago", exception);
                }
            }
        };
    }
}
