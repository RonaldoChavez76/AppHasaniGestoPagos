package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoProductListResponse {

    private Integer code;
    private String message;
    private JsonNode data;
    private final Map<String, JsonNode> additionalFields = new LinkedHashMap<>();

    @JsonAnySetter
    public void addAdditionalField(String name, JsonNode value) {
        additionalFields.put(name, value);
    }
}