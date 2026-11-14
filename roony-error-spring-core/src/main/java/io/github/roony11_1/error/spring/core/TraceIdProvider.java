package io.github.roony11_1.error.spring.core;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resuelve el identificador de traza que se añade a las respuestas de error.
 * <p>
 * La librería no depende de ninguna implementación de logging. El consumidor
 * puede declarar su propio bean {@code TraceIdProvider} (por ejemplo, para
 * leer de MDC o de Micrometer Tracing); si no lo hace, se usa
 * {@link RequestTraceIdProvider}.
 * </p>
 */
@FunctionalInterface
public interface TraceIdProvider
{
    /**
     * @param request petición HTTP actual
     * @return el traceId, o {@code null} si no está disponible
     */
    String resolve(HttpServletRequest request);
}
