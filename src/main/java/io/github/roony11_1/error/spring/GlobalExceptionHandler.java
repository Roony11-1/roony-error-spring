package io.github.roony11_1.error.spring;

import jakarta.servlet.http.HttpServletRequest;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.roony11_1.error.core.ErrorHandler;
import io.github.roony11_1.error.core.ErrorResponse;
import io.github.roony11_1.error.core.exceptions.AppException;
import io.github.roony11_1.error.core.exceptions.InvalidInputException;
import io.github.roony11_1.error.rest.HttpStatusRegistry;

@RestControllerAdvice
public class GlobalExceptionHandler 
{
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final HttpServletRequest request;

    public GlobalExceptionHandler(HttpServletRequest request) 
    {
        this.request = request;
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) 
    {
        log.warn("AppException: {} - {}", ex.getCode(), ex.getDisplayMessage());

        ErrorResponse body = buildEnrichedErrorResponse(ex);
        int status = HttpStatusRegistry.getStatus(ex.getCategory());

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) 
    {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        InvalidInputException appEx = new InvalidInputException("Datos inválidos: " + details);
        ErrorResponse body = buildEnrichedErrorResponse(appEx);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) 
    {
        log.error("Error inesperado", ex);

        ErrorResponse body = buildEnrichedErrorResponse(ex);
        return ResponseEntity.internalServerError().body(body);
    }

    private ErrorResponse buildEnrichedErrorResponse(Throwable throwable) 
    {
        ErrorResponse response = ErrorHandler.toErrorResponse(throwable);
        response.setPath(request.getRequestURI());
        response.setTraceId(MDC.get("traceId"));
        return response;
    }
}