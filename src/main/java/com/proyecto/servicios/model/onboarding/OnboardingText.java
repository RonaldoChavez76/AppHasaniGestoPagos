package com.proyecto.servicios.model.onboarding;

final class OnboardingText {

    private OnboardingText() {
    }

    static String strip(String value) {
        return value == null ? null : value.strip();
    }

    static String optional(String value) {
        String stripped = strip(value);
        return stripped == null || stripped.isEmpty() ? null : stripped;
    }
}
