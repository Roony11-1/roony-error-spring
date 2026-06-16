# roony-error-spring

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![Java](https://img.shields.io/badge/Java-21%2B-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)

Integración de `roony-error-core` para Spring Boot / Spring MVC.  
Registra automáticamente un `@RestControllerAdvice` que convierte tus `AppException` en respuestas JSON estructuradas, incluyendo `traceId` y `path`.

## Instalación

```xml
<dependency>
    <groupId>io.github.roony11-1</groupId>
    <artifactId>roony-error-spring</artifactId>
    <version>1.1.0</version>
</dependency>
```

O, si usas el BOM del ecosistema:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.roony11-1</groupId>
            <artifactId>roony-bom</artifactId>
            <version>1.0.0</version>
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
La integración es automática. Al añadir la dependencia, Spring Boot descubre el GlobalExceptionHandler y lo registra sin necesidad de configuración adicional.

Si quieres omitir campos nulos en el JSON de error (recomendado), añade en application.properties o application.yml:

```properties
spring.jackson.serialization-inclusion=NON_NULL
```

## Uso
Lanza tus excepciones normalmente en la capa de servicio:
```java
throw new NotFoundException("Producto", id);
throw new AlreadyExistsException("Categoría duplicada");
```

El @RestControllerAdvice las captura automáticamente y devuelve:

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

Crea una categoría nueva implementando ErrorCategory
```java
import io.github.roony11_1.error.core.ErrorCategory;

public class MiCategoria 
{
    public static final ErrorCategory MI_ERROR = () -> "MI_ERROR";
}
```
Regístrala con un código HTTP al iniciar la aplicación
```java
import io.github.roony11_1.error.rest.HttpStatusRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErrorHttpConfig 
{
    @PostConstruct
    void register() 
    {
        HttpStatusRegistry.register(MiCategoria.MI_ERROR, 418);
    }
}
```