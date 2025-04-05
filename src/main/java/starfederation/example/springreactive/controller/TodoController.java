package starfederation.example.springreactive.controller;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import starfederation.datastar.events.MergeFragments;
import starfederation.datastar.utils.DataStore;
import starfederation.example.springreactive.adapter.annotations.HandleSignals;
import starfederation.example.springreactive.model.Todo;
import starfederation.example.springreactive.repositories.TodoRepository;

import java.util.UUID;

@RestController
@RequestMapping("/todos")
public class TodoController {
    private final Sinks.Many<Todo> todoSink = Sinks.many().multicast().onBackpressureBuffer();
    private final TodoRepository repository;
    private final SpringTemplateEngine templateEngine;

    public TodoController(TodoRepository repository, SpringTemplateEngine templateEngine) {
        this.repository = repository;
        this.templateEngine = templateEngine;
    }

    @GetMapping
    public Flux<Todo> getAllTodos() {
        return repository.findAllSorted();
    }

    @GetMapping("/{username}")
    public Flux<Todo> getTodosByUser(@PathVariable String username) {
        return repository.findByUser(username);
    }

    @PostMapping
    public Mono<Todo> createTodo(@RequestBody Todo todo) {
        return repository.save(todo).doOnNext(todoSink::tryEmitNext);
    }
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<MergeFragments>> todoStream(@HandleSignals DataStore d ) {
        return todoSink.asFlux()
                .mergeWith(repository.findAll()) // on first contact, fetch all todos
                .flatMap(this::renderTodoItem).subscribeOn(Schedulers.boundedElastic())
                .map(html -> ServerSentEvent.<MergeFragments>builder()
                        .data(MergeFragments.builder()
                                .selector("#todolist")
                                .data(html)
                                .build())
                        .build())
                .cache(); // keep the sink open even when the last client goes away
    }

    private Mono<String> renderTodoItem(Todo todo) {
        return Mono.fromCallable(() -> {
            Context context = new Context();
            context.setVariable("todo", todo);
            return templateEngine.process("todo-item", context);
        });
    }
}