package starfederation.example.springreactive.adapter.annotations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpMethod;
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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Component
public class DatastoreArgumentResolver implements HandlerMethodArgumentResolver {

    private static final Set<HttpMethod> NON_GET_METHODS = Set.of(HttpMethod.POST, HttpMethod.PUT,
            HttpMethod.DELETE, HttpMethod.PATCH);

    private final Jackson2JsonDecoder decoder;

    public DatastoreArgumentResolver(ObjectMapper objectMapper) {
        this.decoder = new Jackson2JsonDecoder(objectMapper);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean isAnnotatedController = parameter.getDeclaringClass().isAnnotationPresent(DatastarController.class);
        boolean hasParameterAnnotation = parameter.hasParameterAnnotation(HandleSignals.class);
        boolean isDataStoreType = DataStore.class.isAssignableFrom(parameter.getParameterType());

        return (isAnnotatedController || hasParameterAnnotation) && isDataStoreType;
    }

    @Override
    @NonNull
    public Mono<Object> resolveArgument(
            @NonNull MethodParameter parameter,
            @NonNull BindingContext bindingContext,
            @NonNull ServerWebExchange exchange
    ) {
        HttpMethod httpMethod = exchange.getRequest().getMethod();
        Mono<DataStore> dataStoreMono;

        if (httpMethod == HttpMethod.GET) {
            dataStoreMono = handleGetRequest(exchange);
        } else if (NON_GET_METHODS.contains(httpMethod)) {
            dataStoreMono = handleNonGetRequest(exchange);
        } else {
            return  exchange.getSession()
                    .flatMap(session -> Mono.justOrEmpty(session.getAttribute(Consts.DATASTAR_KEY)));
        }

        return dataStoreMono.flatMap(dataStore -> storeDataStoreInSession(exchange, dataStore));
    }

    private Mono<DataStore> handleGetRequest(ServerWebExchange exchange) {
        String storeJson = exchange.getRequest().getQueryParams().getFirst(Consts.DATASTAR_KEY);

        if (storeJson == null) {
            return Mono.just(new DataStore());
        }

        return Mono.fromCallable(() -> {
                    DataStore dataStore = new DataStore();
                    Map<String, Object> parsedMap = decoder.getObjectMapper()
                            .readValue(storeJson, new TypeReference<Map<String, Object>>() {});
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

        if (!decoder.canDecode(bodyType, request.getHeaders().getContentType())) {
            return Mono.error(new IllegalArgumentException("Unsupported content type: " +
                    request.getHeaders().getContentType()));
        }

        return decoder.decodeToMono(request.getBody(), bodyType, request.getHeaders().getContentType(), Collections.emptyMap())
                .map(data -> {
                    DataStore dataStore = new DataStore();
                    dataStore.putAll((Map<String, Object>) data);
                    return dataStore;
                });
    }

    private Mono<DataStore> storeDataStoreInSession(ServerWebExchange exchange, DataStore dataStore) {
        return exchange.getSession()
                .doOnNext(session -> session.getAttributes().put(Consts.DATASTAR_KEY, dataStore))
                .thenReturn(dataStore);
    }
}