package starfederation.example.springreactive.adapter.configuration;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.codec.ServerSentEventHttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import starfederation.datastar.events.AbstractDatastarEvent;

import java.util.List;
import java.util.Map;

public class AbstractDatastarEventHttpMessageWriter implements HttpMessageWriter<ServerSentEvent<AbstractDatastarEvent>> {

    private static final ServerSentEventHttpMessageWriter delegate = new ServerSentEventHttpMessageWriter();

    @Override
    public boolean canWrite(ResolvableType elementType, MediaType mediaType) {
        // Only handle ServerSentEvent<AbstractDatastarEvent>
        return ServerSentEvent.class.isAssignableFrom(elementType.toClass()) &&
                elementType.hasResolvableGenerics() &&
                AbstractDatastarEvent.class.isAssignableFrom(elementType.getGeneric(0).toClass()) &&
                (mediaType == null || MediaType.TEXT_EVENT_STREAM.includes(mediaType));
    }

    @Override
    public List<MediaType> getWritableMediaTypes() {
        return delegate.getWritableMediaTypes();
    }

    @Override
    public Mono<Void> write(
            Publisher<? extends ServerSentEvent<AbstractDatastarEvent>> inputStream,
            ResolvableType elementType,
            MediaType mediaType,
            ReactiveHttpOutputMessage message,
            Map<String, Object> hints
    ) {
        Flux<ServerSentEvent<String>> transformedStream = Flux.from(inputStream)
                .map(this::transformServerSentEvent);

        // Delegate the transformed stream to the original writer
        return delegate.write(transformedStream, ResolvableType.forClassWithGenerics(ServerSentEvent.class, String.class), mediaType, message, hints);
    }

    @Override
    public Mono<Void> write(
            Publisher<? extends ServerSentEvent<AbstractDatastarEvent>> inputStream,
            ResolvableType actualType,
            ResolvableType elementType,
            MediaType mediaType,
            ServerHttpRequest request,
            ServerHttpResponse response,
            Map<String, Object> hints
    ) {
        Flux<ServerSentEvent<String>> transformedStream = Flux.from(inputStream)
                .map(this::transformServerSentEvent);

        return delegate.write(transformedStream, actualType, ResolvableType.forClassWithGenerics(ServerSentEvent.class, String.class), mediaType, request, response, hints);
    }

    private ServerSentEvent<String> transformServerSentEvent(ServerSentEvent<AbstractDatastarEvent> originalEvent) {
        AbstractDatastarEvent eventData = originalEvent.data();

        String transformedData = String.join("\n", eventData.getDataLines());

        return ServerSentEvent.<String>builder()
                .id(originalEvent.id()) // Preserve the original ID, if any
                .event(eventData.getEventType().toString()) // Derive the event type
                .data(transformedData) // Set the transformed data
                .comment(originalEvent.comment()) // Preserve the original comment
                .retry(originalEvent.retry() ) // Preserve the original retry
                .build();
    }
}

