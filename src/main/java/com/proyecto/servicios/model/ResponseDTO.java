package com.proyecto.servicios.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDTO<T> {

    private Integer codigo;
    private String mensaje;
    private T data;

    public static <T> ResponseDTO<T> success(Integer codigo, String mensaje, T data) {
        return new ResponseDTO<>(codigo, mensaje, data);
    }

    public static <T> ResponseDTO<T> failure(Integer codigo, String mensaje) {
        return new ResponseDTO<>(codigo, mensaje, null);
    }
}
