package com.overseas.purchase.common;

import lombok.extern.slf4j.Slf4j;

/** Centralizes logging while keeping raw exception details out of public JSON. */
@Slf4j
public final class PublicErrorResponse {

    private PublicErrorResponse() {
    }

    public static <T> Result<T> from(String publicMessage, Exception error) {
        if (error == null) {
            log.error("Public API request failed without an exception instance");
        } else {
            log.error("Public API request failed; failureType={}", error.getClass().getName(), error);
        }
        return Result.error(publicMessage);
    }
}
