package com.proyecto.servicios.model;

import lombok.Data;

@Data
public class ResponseDTO<T> {

    private Integer codigo;
    private String mensaje;
    private T data;

    public ResponseDTO() {
    }

    public ResponseDTO(Integer codigo, String mensaje, T data) {
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.data = data;
    }

    public static <T> ResponseDTO<T> success(Integer codigo, String mensaje, T data) {
        return new ResponseDTO<>(codigo, mensaje, data);
    }

    public static <T> ResponseDTO<T> failure(Integer codigo, String mensaje) {
        return new ResponseDTO<>(codigo, mensaje, null);
    }
}
