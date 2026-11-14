package io.github.roony11_1.error.spring.core;

import java.util.Arrays;

import org.springframework.core.env.Environment;

import io.github.roony11_1.error.core.handler.IProfileResolver;

/**
 * {@link IProfileResolver} que obtiene los perfiles activos del
 * {@link Environment} de Spring.
 * <p>
 * Devuelve los perfiles activos separados por comas. Si no hay perfiles
 * activos, usa la propiedad {@code spring.profiles.active} y, como último
 * recurso, los perfiles por defecto del entorno.
 * </p>
 */
public final class SpringProfileResolver implements IProfileResolver
{
    private static final String ACTIVE_PROFILES_PROPERTY = "spring.profiles.active";

    private final Environment environment;

    public SpringProfileResolver(Environment environment)
    {
        this.environment = environment;
    }

    @Override
    public String resolve()
    {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0)
            return String.join(",", activeProfiles);

        String configured = environment.getProperty(ACTIVE_PROFILES_PROPERTY);
        if (configured != null && !configured.isBlank())
            return configured;

        String[] defaultProfiles = environment.getDefaultProfiles();
        return defaultProfiles.length > 0 ? String.join(",", defaultProfiles) : null;
    }

    @Override
    public String toString()
    {
        return "SpringProfileResolver" + Arrays.toString(environment.getActiveProfiles());
    }
}
