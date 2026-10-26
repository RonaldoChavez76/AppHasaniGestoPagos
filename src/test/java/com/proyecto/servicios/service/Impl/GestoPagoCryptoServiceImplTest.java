package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class GestoPagoCryptoServiceImplTest {

    private static final String TEST_SECRET_KEY = "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=";
    private static final String TEST_IV = "p5ldmBPdd/9pjC0bDC/nSg==";

    private GestoPagoCryptoServiceImpl cryptoService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        cryptoService = new GestoPagoCryptoServiceImpl(TEST_SECRET_KEY, TEST_IV, objectMapper);
    }

    @Test
    void debeCifrarYDescifrarTextoPlanoCorrectamente() {
        String originalJson = "{\"idServicio\":79,\"idProducto\":209,\"referencia\":\"40425475190118187271\",\"montoPago\":9999,\"telefono\":\"1111111111\",\"horaLocal\":\"20200401222821\"}";

        String encrypted = cryptoService.encrypt(originalJson);
        assertNotNull(encrypted);
        assertFalse(encrypted.isBlank());
        assertNotEquals(originalJson, encrypted);

        String decrypted = cryptoService.decrypt(encrypted);
        assertEquals(originalJson, decrypted);
    }

    @Test
    void debeSerializarYCifrarPayloadSendTx() {
        GestoPagoSendTxPayload payload = GestoPagoSendTxPayload.builder()
                .idProducto(582)
                .idServicio(133)
                .telefono("5555555551")
                .horaLocal("20261026150000")
                .upc("UPC_TEST_001")
                .unidad("083")
                .montoPago(new BigDecimal("100.00"))
                .build();

        String encrypted = cryptoService.encryptPayload(payload);
        assertNotNull(encrypted);

        String decryptedJson = cryptoService.decrypt(encrypted);
        assertTrue(decryptedJson.contains("582"));
        assertTrue(decryptedJson.contains("5555555551"));
        assertTrue(decryptedJson.contains("UPC_TEST_001"));
    }

    @Test
    void debeLanzarExcepcionSiClavesSonInvalidas() {
        assertThrows(IllegalArgumentException.class, () ->
                new GestoPagoCryptoServiceImpl("", TEST_IV, objectMapper));

        assertThrows(IllegalArgumentException.class, () ->
                new GestoPagoCryptoServiceImpl(TEST_SECRET_KEY, null, objectMapper));
    }
}
