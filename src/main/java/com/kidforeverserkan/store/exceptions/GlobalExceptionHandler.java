package com.kidforeverserkan.store.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> handleUnreadableMessage(){
        return ResponseEntity.badRequest().body(
                new ErrorDto("Invalid request body")
        );

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> handleValidationErrors (
            MethodArgumentNotValidException exception){
        var errors = new HashMap<String, String>();
        exception.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(errors);
    }

    // A path/query/cookie value that can't be converted to the expected type
    // (e.g. GET /products/not-a-number). Spring MVC already knows this is a
    // 400; it must be handled explicitly here or the catch-all below would
    // incorrectly turn it into a 500.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDto> handleTypeMismatch(){
        return ResponseEntity.badRequest().body(
                new ErrorDto("Invalid request parameter")
        );
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ErrorDto> handleMissingCookie(){
        return ResponseEntity.badRequest().body(
                new ErrorDto("A required cookie is missing")
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorDto> handleUnsupportedMediaType(){
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorDto("Unsupported content type"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorDto> handleMethodNotSupported(){
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorDto("HTTP method not supported for this endpoint"));
    }

    // Thrown by controllers doing their own ownership/role checks (e.g.
    // UserController). Without this, the catch-all below turned a plain
    // "not allowed" into a 500.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDto> handleAccessDenied(){
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorDto("You don't have access to this resource"));
    }

    // No controller or static resource matches the path. Must be handled
    // explicitly or the catch-all below would report a 500 for a simple 404.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorDto> handleNoResourceFound(){
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorDto("Not found"));
    }

    // A write that violates a database constraint (e.g. deleting a product
    // still referenced by a cart or order item, or a race on a unique
    // constraint). The transaction is rolled back by Spring either way; this
    // just turns the raw SQL/constraint exception into a clean 409 instead
    // of a 500.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDto> handleDataIntegrityViolation(DataIntegrityViolationException exception){
        log.warn("Data integrity violation: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorDto("This action conflicts with existing data and could not be completed"));
    }

    // Fallback for any exception not handled by a more specific handler
    // (either here or in a controller). Prevents the default Spring Boot
    // whitelabel error page / stack trace from ever reaching a client.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleUnexpectedError(Exception exception){
        log.error("Unhandled exception", exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorDto("An unexpected error occurred"));
    }
}
