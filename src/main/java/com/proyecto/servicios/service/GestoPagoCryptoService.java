package com.proyecto.servicios.service;

public interface GestoPagoCryptoService {

    /**
     * Cifra una cadena de texto en formato JSON utilizando AES-256-CBC con clave e IV configurados.
     *
     * @param plainText Contenido JSON a cifrar
     * @return Cadena cifrada en Base64
     */
    String encrypt(String plainText);

    /**
     * Descifra una cadena en Base64 previamente cifrada con AES-256-CBC.
     *
     * @param cipherTextBase64 Cadena cifrada en Base64
     * @return Cadena descifrada en UTF-8
     */
    String decrypt(String cipherTextBase64);

    /**
     * Serializa un objeto a JSON y lo cifra con AES-256-CBC.
     *
     * @param payload Objeto a serializar y cifrar
     * @return Cadena cifrada en Base64
     */
    String encryptPayload(Object payload);
}
