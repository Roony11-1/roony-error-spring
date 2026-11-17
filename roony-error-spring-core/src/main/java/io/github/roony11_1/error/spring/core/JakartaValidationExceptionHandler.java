package io.github.roony11_1.error.spring.core;

import jakarta.validation.ConstraintViolationException;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.roony11_1.error.core.exceptions.InvalidInputException;
import io.github.roony11_1.error.core.handler.ErrorHandler;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@ConditionalOnClass(name = "jakarta.validation.ConstraintViolationException")
public class JakartaValidationExceptionHandler
{
    private static final String VALIDATION_ERROR =
            "Uno o más campos de la solicitud no son válidos.";

    private final ErrorHandler errorHandler;

    public JakartaValidationExceptionHandler(ErrorHandler errorHandler)
    {
        this.errorHandler = errorHandler;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex)
    {
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(error -> error.getPropertyPath() + ": " + error.getMessage())
                .toList();

        InvalidInputException appEx =
                new InvalidInputException(VALIDATION_ERROR);

        var info = errorHandler.toErrorInfo(appEx)
                .withError(errors);

        ProblemDetail problem = ProblemDetail.forStatus(400);
        problem.setTitle(info.title());
        problem.setDetail(info.detail());

        info.attributes().forEach(problem::setProperty);

        problem.setProperty("code", info.code());
        problem.setProperty("timestamp", info.timestamp());
        problem.setProperty("errors", info.errors());

        return ResponseEntity.badRequest().body(problem);
    }
}