package io.github.roony11_1.error.spring.core;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import io.github.roony11_1.error.core.handler.DevelopmentDetailsEnricher;
import io.github.roony11_1.error.core.handler.ErrorHandler;
import io.github.roony11_1.error.core.handler.ErrorInfoMapper;
import io.github.roony11_1.error.core.handler.ErrorListener;
import io.github.roony11_1.error.core.handler.IProfileResolver;
import io.github.roony11_1.error.rest.DefaultHttpStatusMapper;
import io.github.roony11_1.error.rest.IHttpStatusMapper;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
public class RoonyErrorAutoConfiguration
{
    @Bean
    @ConditionalOnMissingBean
    public IProfileResolver roonyProfileResolver(Environment environment)
    {
        return new SpringProfileResolver(environment);
    }

    @Bean
    @ConditionalOnMissingBean
    public TraceIdProvider roonyTraceIdProvider()
    {
        return new RequestTraceIdProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public IHttpStatusMapper roonyHttpStatusMapper()
    {
        return new DefaultHttpStatusMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorListener roonyErrorListener()
    {
        return ErrorListener.NOOP;
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorHandler roonyErrorHandler(IProfileResolver profileResolver,
                                          ErrorListener errorListener)
    {
        return new ErrorHandler(
                new DevelopmentDetailsEnricher(profileResolver),
                new ErrorInfoMapper(),
                errorListener);
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler(HttpServletRequest request,
                                                         ErrorHandler errorHandler,
                                                         IHttpStatusMapper statusMapper,
                                                         TraceIdProvider traceIdProvider)
    {
        return new GlobalExceptionHandler(request, errorHandler, statusMapper, traceIdProvider);
    }
}
