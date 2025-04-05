package starfederation.example.springreactive.repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import starfederation.example.springreactive.model.Todo;

import java.util.UUID;
@Repository
public interface TodoRepository extends ReactiveCrudRepository<Todo, UUID> {
    @Query("SELECT * FROM todo WHERE username = :username")
    Flux<Todo> findByUser(String username);

    @Query("SELECT * FROM todo ORDER BY description ")
    Flux<Todo> findAllSorted();}