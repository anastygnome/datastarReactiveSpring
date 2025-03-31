package starfederation.example.springreactive.controller;

import starfederation.example.springreactive.adapter.annotations.DatastarController;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import starfederation.datastar.events.MergeFragments;
import starfederation.datastar.utils.DataStore;

import java.time.Duration;

@DatastarController
@RequestMapping
public class HelloWorldController {
    private static final Log logger = LogFactory.getLog(HelloWorldController.class);

    @GetMapping(value = "/hello", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<MergeFragments>> getHelloWorldSSE(DataStore datastore) {
        long delayMs = ((Number) datastore.getStore().getOrDefault("delay", 1000)).longValue();
        String message = "Hello, World!";

        return Flux.range(0, message.length())
                .delayElements(Duration.ofMillis(delayMs))
                .map(index -> {
                    String htmlFragment = String.format("<div id=\"message\">%s</div>", message.substring(0, index + 1));
                    MergeFragments event = MergeFragments.builder()
                            .selector("#message")
                            .data(htmlFragment)
                            .build();
                    return ServerSentEvent.builder(event).build();
                })
                .concatWith(Mono.fromRunnable(() -> logger.info("SSE stream completed"))); // Cleanup logic if needed
    }
}