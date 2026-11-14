package io.github.roony11_1.error.spring.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import io.github.roony11_1.error.core.handler.ErrorHandler;
import io.github.roony11_1.error.spring.core.RoonyErrorAutoConfiguration;
import io.github.roony11_1.error.spring.core.TraceIdProvider;

@AutoConfiguration(after = RoonyErrorAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({AccessDeniedException.class, AuthenticationException.class})
@ConditionalOnBean(ErrorHandler.class)
public class RoonySecurityAutoConfiguration
{
    @Bean
    @ConditionalOnMissingBean
    public SecurityExceptionHandler securityExceptionHandler(HttpServletRequest request,
                                                             ErrorHandler errorHandler,
                                                             TraceIdProvider traceIdProvider)
    {
        return new SecurityExceptionHandler(request, errorHandler, traceIdProvider);
    }
}
