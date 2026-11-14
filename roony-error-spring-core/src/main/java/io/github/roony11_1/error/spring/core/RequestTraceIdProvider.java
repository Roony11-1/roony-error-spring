package io.github.roony11_1.error.spring.core;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Implementación por defecto de {@link TraceIdProvider}.
 * <p>
 * Busca el traceId en el atributo de request {@value #TRACE_ID_ATTRIBUTE} y,
 * si no existe, en la cabecera {@value #TRACE_ID_HEADER}. No depende de SLF4J
 * ni de ningún otro framework de logging.
 * </p>
 */
public final class RequestTraceIdProvider implements TraceIdProvider
{
    public static final String TRACE_ID_ATTRIBUTE = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public String resolve(HttpServletRequest request)
    {
        Object attribute = request.getAttribute(TRACE_ID_ATTRIBUTE);
        if (attribute != null && !attribute.toString().isBlank())
            return attribute.toString();

        String header = request.getHeader(TRACE_ID_HEADER);
        return (header != null && !header.isBlank()) ? header : null;
    }
}
