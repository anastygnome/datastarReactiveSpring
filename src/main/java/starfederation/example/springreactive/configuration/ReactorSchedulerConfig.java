package starfederation.example.springreactive.configuration;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ReactorSchedulerConfig implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Environment environment = event.getEnvironment();

        String propertyValue = environment.getProperty("reactor.schedulers.defaultBoundedElasticOnVirtualThreads", "false");
        System.setProperty("reactor.schedulers.defaultBoundedElasticOnVirtualThreads", String.valueOf(Boolean.parseBoolean(propertyValue)));

    }
}
