package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoProductDto {

    private String id;
    private String code;
    private String name;
    private JsonNode price;
    private Boolean active;
    private final Map<String, JsonNode> additionalFields = new LinkedHashMap<>();

    @JsonAnySetter
    public void addAdditionalField(String name, JsonNode value) {
        additionalFields.put(name, value);
    }
}