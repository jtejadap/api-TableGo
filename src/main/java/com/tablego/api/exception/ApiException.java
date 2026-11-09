package com.tablego.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Base de las excepciones de negocio. El GlobalExceptionHandler usa su status automaticamente. */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
