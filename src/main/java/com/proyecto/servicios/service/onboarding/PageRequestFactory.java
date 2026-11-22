package com.proyecto.servicios.service.onboarding;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageRequestFactory {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PageRequestFactory() {
    }

    public static Pageable create(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("El número de página no puede ser negativo.");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y " + MAX_SIZE + ".");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
    }
}
