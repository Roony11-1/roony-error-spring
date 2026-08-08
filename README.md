# roony-error-spring

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![Java](https://img.shields.io/badge/Java-21%2B-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.roony11-1/roony-error-spring?style=flat-square)](https://search.maven.org/artifact/io.github.roony11-1/roony-error-spring)

Integración de `roony-error-core` para Spring Boot / Spring MVC. Registra automáticamente un `@RestControllerAdvice` (auto-configuración) que convierte tus `AppException` y errores HTTP comunes en respuestas JSON estructuradas, con `traceId` y `path`.

## Instalación

```xml
<dependency>
    <groupId>io.github.roony11-1</groupId>
    <artifactId>roony-error-spring</artifactId>
    <version>1.1.1</version>
</dependency>
```

O, si usas el BOM del ecosistema:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.roony11-1</groupId>
            <artifactId>roony-bom</artifactId>
            <version>1.0.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.github.roony11-1</groupId>
        <artifactId>roony-error-spring</artifactId>
        <!-- sin versión: la hereda del BOM -->
    </dependency>
</dependencies>
```

## Configuración

La integración es automática: Spring Boot descubre el `GlobalExceptionHandler` y lo registra sin configuración adicional.

Si quieres omitir campos nulos en el JSON de error (recomendado), añade en `application.properties` o `application.yml`:

```properties
spring.jackson.serialization-inclusion=NON_NULL
```

## Manejo de errores incluido

| Excepción | Resultado |
|---|---|
| `AppException` | HTTP según `HttpStatusRegistry` |
| `MethodArgumentNotValidException` (Bean Validation) | `400` — suma los `fieldErrors` como detalle |
| `MethodArgumentTypeMismatchException` | `400` |
| `HttpMessageNotReadableException` (JSON malformado) | `400` |
| `HttpRequestMethodNotSupportedException` | `405` |
| `NoResourceFoundException` (404 al despachar) | `404` |
| cualquier `Exception` | `500` |

Los campos `path` y `traceId` se rellenan automáticamente (el `traceId` se lee de `MDC`: pónlo con un filtro o con el observability estándar de Spring Boot 3 / Micrometer Tracing).

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
    "code": "ERR-0003",
    "message": "Producto no encontrado: 5",
    "timestamp": "2026-06-14T19:30:00Z",
    "traceId": "abc123...",
    "path": "/api/productos/5"
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

2. Regístrala con un código HTTP al iniciar la aplicación:

```java
import io.github.roony11_1.error.rest.HttpStatusRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErrorHttpConfig {
    @PostConstruct
    void register() {
        HttpStatusRegistry.register(MiCategoria.SALDO_INSUFICIENTE, 402);
    }
}
```

---

MIT License · [Roony11-1](https://github.com/roony11-1)