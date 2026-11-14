package io.github.roony11_1.error.spring.security;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.roony11_1.error.core.dto.ErrorInfo;
import io.github.roony11_1.error.core.handler.ErrorHandler;
import io.github.roony11_1.error.spring.core.TraceIdProvider;

/**
 * Handler MVC para excepciones de Spring Security.
 * <p>
 * Se registra solo si {@code spring-security-core} está en classpath
 * (condición declarada en {@link RoonySecurityAutoConfiguration}), por lo que
 * es un patch no rompiente: consumidores sin Security siguen funcionando sin
 * cambios.
 * </p>
 * Cubre method-security ({@code @PreAuthorize}) que sí llega a
 * {@code @RestControllerAdvice} (AccessDeniedException /
 * AuthorizationDeniedException vía herencia). El 401/403 de filter-chain
 * (ExceptionTranslationFilter) requiere además AuthenticationEntryPoint /
 * AccessDeniedHandler custom en SecurityConfig, pero este handler ya mejora
 * el flujo 403 de {@code @PreAuthorize}.
 */
@RestControllerAdvice
@Order(0)
public class SecurityExceptionHandler
{
    private final HttpServletRequest request;
    private final ErrorHandler errorHandler;
    private final TraceIdProvider traceIdProvider;

    public SecurityExceptionHandler(HttpServletRequest request,
                                    ErrorHandler errorHandler,
                                    TraceIdProvider traceIdProvider)
    {
        this.request = request;
        this.errorHandler = errorHandler;
        this.traceIdProvider = traceIdProvider;
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ProblemDetail> handleAccessDenied(Exception ex)
    {
        ErrorInfo info = errorHandler.toErrorInfo(ex);
        ProblemDetail body = toProblemDetail(
                info,
                HttpStatus.FORBIDDEN.value(),
                "HTTP-403",
                "Acceso denegado",
                "Acceso denegado: no tienes permisos para este recurso");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex)
    {
        ErrorInfo info = errorHandler.toErrorInfo(ex);
        ProblemDetail body = toProblemDetail(
                info,
                HttpStatus.UNAUTHORIZED.value(),
                "HTTP-401",
                "No autenticado",
                "No autenticado: token ausente, inválido o expirado");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    private ProblemDetail toProblemDetail(ErrorInfo info,
                                          int status,
                                          String code,
                                          String title,
                                          String detail)
    {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
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
