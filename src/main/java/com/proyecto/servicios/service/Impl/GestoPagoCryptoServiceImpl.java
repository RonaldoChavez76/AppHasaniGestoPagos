package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.service.GestoPagoCryptoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Slf4j
public class GestoPagoCryptoServiceImpl implements GestoPagoCryptoService {

    private static final String ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private final byte[] secretKeyBytes;
    private final byte[] ivBytes;
    private final ObjectMapper objectMapper;

    public GestoPagoCryptoServiceImpl(
            @Value("${gestopago.crypto.secret-key}") String secretKeyBase64,
            @Value("${gestopago.crypto.iv}") String ivBase64,
            ObjectMapper objectMapper) {
        if (secretKeyBase64 == null || secretKeyBase64.isBlank()) {
            throw new IllegalArgumentException("La clave secreta de GestoPago (gestopago.crypto.secret-key) no puede estar vacía");
        }
        if (ivBase64 == null || ivBase64.isBlank()) {
            throw new IllegalArgumentException("El vector de inicialización de GestoPago (gestopago.crypto.iv) no puede estar vacío");
        }
        this.secretKeyBytes = Base64.getDecoder().decode(secretKeyBase64.trim());
        this.ivBytes = Base64.getDecoder().decode(ivBase64.trim());
        this.objectMapper = objectMapper;
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            SecretKey secretKey = new SecretKeySpec(secretKeyBytes, 0, secretKeyBytes.length, ALGORITHM);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Error al cifrar el payload con AES-256-CBC: {}", e.getMessage());
            throw new IllegalStateException("Error durante el cifrado AES de GestoPago", e);
        }
    }

    @Override
    public String decrypt(String cipherTextBase64) {
        if (cipherTextBase64 == null) {
            return null;
        }
        try {
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            SecretKey secretKey = new SecretKeySpec(secretKeyBytes, 0, secretKeyBytes.length, ALGORITHM);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decodedBytes = Base64.getDecoder().decode(cipherTextBase64.trim());
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error al descifrar el payload con AES-256-CBC: {}", e.getMessage());
            throw new IllegalStateException("Error durante el descifrado AES de GestoPago", e);
        }
    }

    @Override
    public String encryptPayload(Object payload) {
        if (payload == null) {
            throw new IllegalArgumentException("El payload no puede ser nulo");
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            return encrypt(json);
        } catch (Exception e) {
            log.error("Error al serializar el payload a JSON para cifrado: {}", e.getMessage());
            throw new IllegalStateException("Error al serializar el payload antes de cifrar", e);
        }
    }
}
