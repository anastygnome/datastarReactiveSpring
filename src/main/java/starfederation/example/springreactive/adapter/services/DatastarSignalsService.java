package starfederation.example.springreactive.adapter.services;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.session.WebSessionManager;
import reactor.core.publisher.Mono;
import starfederation.datastar.Consts;
import starfederation.datastar.utils.DataStore;

@Service
public class DatastarSignalsService {
    private final WebSessionManager sessionManager;

    public DatastarSignalsService(WebSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public Mono<DataStore> storeSignals(ServerWebExchange exchange, DataStore dataStore) {
        return sessionManager.getSession(exchange)
                .flatMap(session -> {
                    session.getAttributes().put(Consts.DATASTAR_KEY, dataStore);
                    return session.save().thenReturn(dataStore);
                });
    }
    public Mono<DataStore> getSignals(ServerWebExchange exchange) {
        return sessionManager.getSession(exchange)
                .flatMap(session -> Mono.justOrEmpty(session.getAttribute(Consts.DATASTAR_KEY)));
    }
    public Mono<DataStore> getSignalsAtRequestTime(ServerWebExchange exchange) {
        return exchange.getSession()
                .flatMap(session -> Mono.justOrEmpty(session.getAttribute(Consts.DATASTAR_KEY)));
    }
}
