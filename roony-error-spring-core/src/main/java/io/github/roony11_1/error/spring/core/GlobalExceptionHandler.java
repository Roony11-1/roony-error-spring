
package io.github.roony11_1.error.spring.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;


import io.github.roony11_1.error.core.dto.ErrorInfo;
import io.github.roony11_1.error.core.exceptions.AppException;
import io.github.roony11_1.error.core.exceptions.InvalidInputException;
import io.github.roony11_1.error.core.handler.ErrorHandler;
import io.github.roony11_1.error.rest.IHttpStatusMapper;

@RestControllerAdvice
public class GlobalExceptionHandler
{
    private static final String VALIDATION_ERROR =
            "Uno o más campos de la solicitud no son válidos.";

    private final HttpServletRequest request;
    private final ErrorHandler errorHandler;
    private final IHttpStatusMapper statusMapper;
    private final TraceIdProvider traceIdProvider;

    public GlobalExceptionHandler(
            HttpServletRequest request,
            ErrorHandler errorHandler,
            IHttpStatusMapper statusMapper,
            TraceIdProvider traceIdProvider)
    {
        this.request = request;
        this.errorHandler = errorHandler;
        this.statusMapper = statusMapper;
        this.traceIdProvider = traceIdProvider;
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ProblemDetail> handleAppException(AppException ex)
    {
        ErrorInfo info = errorHandler.toErrorInfo(ex);
        int status = statusMapper.getStatus(ex.getCategory());

        return ResponseEntity.status(status)
                .body(toProblemDetail(info, status));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex)
    {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        InvalidInputException appEx = new InvalidInputException(VALIDATION_ERROR);

        ErrorInfo info = errorHandler.toErrorInfo(appEx)
                .withError(errors);

        return badRequest(info);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex)
    {
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(error -> error.getPropertyPath() + ": " + error.getMessage())
                .toList();

        InvalidInputException appEx = new InvalidInputException(VALIDATION_ERROR);

        ErrorInfo info = errorHandler.toErrorInfo(appEx)
                .withError(errors);

        return badRequest(info);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableBody(
            HttpMessageNotReadableException ex)
    {
        InvalidInputException appEx = new InvalidInputException(
                "El cuerpo de la solicitud contiene datos inválidos o mal formados.");

        return badRequest(errorHandler.toErrorInfo(appEx));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex)
    {
        InvalidInputException appEx = new InvalidInputException(
                "Uno de los parámetros de la solicitud tiene un formato inválido.");

        return badRequest(errorHandler.toErrorInfo(appEx));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(
            MissingServletRequestParameterException ex)
    {
        InvalidInputException appEx = new InvalidInputException(
                "Falta un parámetro obligatorio en la solicitud.");

        return badRequest(errorHandler.toErrorInfo(appEx));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex)
    {
        ErrorInfo info = errorHandler.toErrorInfo(ex);

        ProblemDetail body = toProblemDetail(
                info,
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "HTTP-405",
                "Método no permitido",
                "Método HTTP no soportado: " + ex.getMethod());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            NoResourceFoundException ex)
    {
        ErrorInfo info = errorHandler.toErrorInfo(ex);

        ProblemDetail body = toProblemDetail(
                info,
                HttpStatus.NOT_FOUND.value(),
                "HTTP-404",
                "Recurso no encontrado",
                "No se encontró el recurso solicitado.");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex)
    {
        ErrorInfo info = errorHandler.toErrorInfo(ex);

        return ResponseEntity.internalServerError()
                .body(toProblemDetail(info, HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    private ResponseEntity<ProblemDetail> badRequest(ErrorInfo info)
    {
        return ResponseEntity.badRequest()
                .body(toProblemDetail(info, HttpStatus.BAD_REQUEST.value()));
    }

    private ProblemDetail toProblemDetail(ErrorInfo info, int status)
    {
        return toProblemDetail(
                info,
                status,
                info.code(),
                info.title(),
                info.detail());
    }

    private ProblemDetail toProblemDetail(
            ErrorInfo info,
            int status,
            String code,
            String title,
            String detail)
    {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);

        if (detail != null)
            problem.setDetail(detail);

        URI instance = requestInstance();

        if (instance != null)
            problem.setInstance(instance);

        info.attributes().forEach(problem::setProperty);

        problem.setProperty("code", code);
        problem.setProperty("timestamp", info.timestamp());

        String traceId = traceIdProvider.resolve(request);

        if (traceId != null)
            problem.setProperty("traceId", traceId);

        if (!info.errors().isEmpty())
            problem.setProperty("errors", info.errors());

        return problem;
    }

    private URI requestInstance()
    {
        try
        {
            return URI.create(request.getRequestURI());
        }
        catch (IllegalArgumentException ex)
        {
            return null;
        }
    }
}