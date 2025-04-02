package starfederation.example.springreactive.adapter.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import starfederation.datastar.Consts;
import starfederation.datastar.utils.DataStore;
import starfederation.example.springreactive.adapter.annotations.DatastarController;
import starfederation.example.springreactive.adapter.services.DatastarSignalsService;
import starfederation.example.springreactive.adapter.annotations.HandleSignals;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Component
public class DatastoreArgumentResolver implements HandlerMethodArgumentResolver {

    public static final Set<HttpMethod> NON_GET_METHODS = Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);

    private final Jackson2JsonDecoder decoder;
    private final DatastarSignalsService signalService;
    public DatastoreArgumentResolver(ObjectMapper objectMapper, DatastarSignalsService signalService) {
        this.decoder = new Jackson2JsonDecoder(objectMapper);
        this.signalService = signalService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean isAnnotatedController = AnnotationUtils.findAnnotation(parameter.getDeclaringClass(), DatastarController.class) != null;
        boolean hasParameterAnnotation = parameter.hasParameterAnnotation(HandleSignals.class);
        boolean isDataStoreType = DataStore.class.isAssignableFrom(parameter.getParameterType());

        return isDataStoreType && (isAnnotatedController || hasParameterAnnotation);
    }

    @Override
    @NonNull
    public Mono<Object> resolveArgument(@NonNull MethodParameter parameter, @NonNull BindingContext bindingContext, @NonNull ServerWebExchange exchange) {
        HttpMethod httpMethod = exchange.getRequest().getMethod();
        if (httpMethod == HttpMethod.GET) {
            return handleGetRequest(exchange).flatMap(dataStore -> signalService.storeSignals(exchange,dataStore));
        } else if (MediaType.APPLICATION_JSON.includes(exchange.getRequest().getHeaders().getContentType())
                && NON_GET_METHODS.contains(httpMethod)) {
            return handleNonGetRequest(exchange)
                    .flatMap(dataStore -> signalService.storeSignals(exchange,dataStore));
        }
        else {
           return signalService.getSignals(exchange)
                   .flatMap(dataStore -> signalService.storeSignals(exchange,dataStore));
        }
    }

    private Mono<DataStore> handleGetRequest(ServerWebExchange exchange) {
        String storeJson = exchange.getRequest().getQueryParams().getFirst(Consts.DATASTAR_KEY);

        if (storeJson == null) {
            return Mono.just(new DataStore());
        }

        return Mono.fromCallable(() -> {
                    DataStore dataStore = new DataStore();
                    Map<String, Object> parsedMap = decoder.getObjectMapper()
                            .readValue(storeJson, new TypeReference<>() {
                            });
                    dataStore.putAll(parsedMap);
                    return dataStore;
                })
                .subscribeOn(Schedulers.single())
                .onErrorMap(e -> new IllegalArgumentException("Invalid store JSON in query parameter", e));
    }

    @SuppressWarnings("unchecked")
    private Mono<DataStore> handleNonGetRequest(ServerWebExchange exchange) {
        ResolvableType bodyType = ResolvableType.forClassWithGenerics(Map.class, String.class, Object.class);
        ServerHttpRequest request = exchange.getRequest();

        return decoder.decodeToMono(request.getBody(), bodyType, request.getHeaders().getContentType(), Collections.emptyMap())
                .map(data -> {
                    DataStore dataStore = new DataStore();
                    dataStore.putAll((Map<String, Object>) data);
                    return dataStore;
                });
    }


}