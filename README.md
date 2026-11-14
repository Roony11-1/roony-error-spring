# roony-error-spring

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![Java](https://img.shields.io/badge/Java-21%2B-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-green)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.roony11-1/roony-error-spring-starter?style=flat-square)](https://search.maven.org/artifact/io.github.roony11-1/roony-error-spring-starter)

Integración de `roony-error-core` para Spring Boot / Spring MVC. Registra automáticamente `@RestControllerAdvice` (vía auto-configuración) que convierte tus `AppException` y errores HTTP comunes en respuestas `ProblemDetail` (RFC 9457), con `code`, `traceId` e `instance`.

## Módulos

| Artefacto | Contenido |
|---|---|
| `roony-error-spring-core` | `GlobalExceptionHandler`, `SpringProfileResolver`, `TraceIdProvider` y auto-configuración base |
| `roony-error-spring-security` | `SecurityExceptionHandler` y auto-configuración de errores 401/403 |
| `roony-error-spring-starter` | Artefacto vacío que agrega `-core` (jar sin clases) |
| `roony-error-spring` | Marcador de migración del artefacto antiguo (relocation) |

Requisitos: Java 21 y Spring Boot 4.

## Instalación

```xml
<dependency>
    <groupId>io.github.roony11-1</groupId>
    <artifactId>roony-error-spring-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

Si usas Spring Security, añade también:

```xml
<dependency>
    <groupId>io.github.roony11-1</groupId>
    <artifactId>roony-error-spring-security</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Migración desde `roony-error-spring`

El artefacto antiguo se mantiene como marcador de migración. Al usar:

```xml
<dependency>
    <groupId>io.github.roony11-1</groupId>
    <artifactId>roony-error-spring</artifactId>
    <version>2.0.0</version>
</dependency>
```

Maven resuelve la `relocation` a `roony-error-spring-starter`. Aun así, se recomienda cambiar las coordenadas explícitamente:

- `roony-error-spring` → `roony-error-spring-starter`
- Si usas Spring Security, añadir `roony-error-spring-security`

> **Cambio de formato:** a partir de 2.0.0 las respuestas son `ProblemDetail` (RFC 9457) en lugar del antiguo `ErrorResponse` con `message`/`path`. Los campos `code`, `timestamp`, `traceId` y `errors` se mantienen como propiedades, y el path viaja en `instance`.

## Configuración

La integración es automática: Spring Boot descubre las auto-configuraciones y registra los handlers sin configuración adicional.

Si quieres omitir campos nulos en el JSON de error (recomendado), añade en `application.properties` o `application.yml`:

```properties
spring.jackson.serialization-inclusion=NON_NULL
```

## Manejo de errores incluido

| Excepción | Resultado |
|---|---|
| `AppException` | HTTP según `IHttpStatusMapper` |
| `AccessDeniedException` / `AuthorizationDeniedException` (Spring Security `@PreAuthorize`) | `403` — `code: HTTP-403` |
| `AuthenticationException` (token ausente/inválido) | `401` — `code: HTTP-401` |
| `MethodArgumentNotValidException` (Bean Validation) | `400` — suma los `fieldErrors` como detalle |
| `MethodArgumentTypeMismatchException` | `400` |
| `HttpMessageNotReadableException` (JSON malformado) | `400` |
| `HttpRequestMethodNotSupportedException` | `405` — `code: HTTP-405` |
| `NoResourceFoundException` (404 al despachar) | `404` — `code: HTTP-404` |
| cualquier `Exception` | `500` |

El `traceId` se resuelve con un `TraceIdProvider`. Por defecto se lee el atributo de request `traceId` y, si no existe, la cabecera `X-Trace-Id`. No hay dependencia de SLF4J: si usas MDC o Micrometer Tracing, declara tu propio bean:

```java
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;

@Bean
TraceIdProvider traceIdProvider() {
    return request -> MDC.get("traceId");
}
```

En perfiles de desarrollo se añade la traza de la excepción en `errors` (vía `SpringProfileResolver`). El logging es opcional: declara un bean `ErrorListener` si quieres registrar los errores; la librería no depende de ningún framework de logging.

## Uso

Lanza tus excepciones normalmente en la capa de servicio:

```java
throw new NotFoundException("Producto", id);
throw new AlreadyExistsException("Categoría duplicada");
throw new InternalErrorException("Fallo al comunicar con el proveedor");
```

La respuesta:

```json
{
    "type": "about:blank",
    "title": "Recurso no encontrado",
    "status": 404,
    "detail": "Producto no encontrado: 5",
    "instance": "/api/productos/5",
    "code": "ERR-0003",
    "timestamp": "2026-06-14T19:30:00Z",
    "traceId": "abc123..."
}
```

## Extender con categorías personalizadas

1. Define una categoría implementando `ErrorCategory`:

```java
import io.github.roony11_1.error.core.ErrorCategory;

public final class MiCategoria {
    public static final ErrorCategory SALDO_INSUFICIENTE = () -> "SALDO_INSUFICIENTE";
}
```

2. Declara un `IHttpStatusMapper` propio; la auto-configuración respeta `@ConditionalOnMissingBean`:

```java
import java.util.Map;

import io.github.roony11_1.error.rest.DefaultHttpStatusMapper;
import io.github.roony11_1.error.rest.IHttpStatusMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErrorHttpConfig {

    @Bean
    IHttpStatusMapper httpStatusMapper() {
        return new DefaultHttpStatusMapper(Map.of(
                MiCategoria.SALDO_INSUFICIENTE, 402));
    }
}
```

También puedes sustituir `ErrorHandler`, `IProfileResolver` o los handlers declarando tus propios beans.

---

MIT License · [Roony11-1](https://github.com/roony11-1)
