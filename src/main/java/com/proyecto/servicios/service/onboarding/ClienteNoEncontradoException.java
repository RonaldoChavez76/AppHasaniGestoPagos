package com.proyecto.servicios.service.onboarding;

public class ClienteNoEncontradoException extends RuntimeException {

    public ClienteNoEncontradoException(Long clienteId) {
        super("No existe el cliente con ID " + clienteId + ".");
    }

    public ClienteNoEncontradoException(String campo, String valor) {
        super("No existe un cliente con " + campo + " " + valor + ".");
    }
}
