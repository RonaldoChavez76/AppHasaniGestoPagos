package com.proyecto.servicios.model.gestopago;

public final class GestoPagoLogoUrl {

    public static final String BASE_URL_DEFECTO =
            "https://gestopago.portalventas.net/sistema/images/gestopago/servicios";

    private GestoPagoLogoUrl() {
    }

    public static String construir(String baseUrl, String idServicio) {
        if (idServicio == null) {
            return null;
        }
        String idLimpio = idServicio.strip();
        if (idLimpio.isEmpty()) {
            return null;
        }
        String baseLimpia = baseUrl == null || baseUrl.isBlank() ? BASE_URL_DEFECTO : baseUrl.strip();
        while (baseLimpia.endsWith("/")) {
            baseLimpia = baseLimpia.substring(0, baseLimpia.length() - 1);
        }
        if (baseLimpia.isEmpty()) {
            baseLimpia = BASE_URL_DEFECTO;
        }
        return baseLimpia + "/" + idLimpio + ".png";
    }
}
