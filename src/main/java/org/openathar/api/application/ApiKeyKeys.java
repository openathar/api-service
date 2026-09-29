package org.openathar.api.application;

/**
 * Shared Redis key naming for API keys — used by both {@link ApiKeyService}
 * (issuance/lookup) and the rate limiter (which only needs to know whether
 * a presented key exists, and where to bump its usage counter).
 */
public final class ApiKeyKeys {

    private ApiKeyKeys() {
    }

    public static String hash(String value) {
        return "athar:apikey:" + value;
    }

    public static String usage(String value) {
        return "athar:apikey:usage:" + value;
    }
}
