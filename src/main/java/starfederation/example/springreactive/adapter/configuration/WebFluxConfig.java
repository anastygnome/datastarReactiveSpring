package starfederation.example.springreactive.adapter.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import starfederation.example.springreactive.adapter.annotations.DatastoreArgumentResolver;

@Configuration(proxyBeanMethods = false)
public class WebFluxConfig implements WebFluxConfigurer {
    DatastoreArgumentResolver signalStoreArgumentResolver;
    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.customCodecs().register(new AbstractDatastarEventHttpMessageWriter());
    }
    @Override
    public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
        configurer.addCustomResolver(signalStoreArgumentResolver);
    }
}
