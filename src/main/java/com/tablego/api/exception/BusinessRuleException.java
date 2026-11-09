package com.tablego.api.exception;

import org.springframework.http.HttpStatus;

/** Mapear a 409 o 422 en tu manejador global de errores (o reemplazar por tu excepción equivalente). */
public class BusinessRuleException extends ApiException {
    public BusinessRuleException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, message); 
    }
}
