package at.lab1.drivers.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue createRideAcceptanceQueue() {
        return new Queue("q.ride-acceptance");
    }

    @Bean
    public Queue createRideCompletionQueue() {
        return new Queue("q.ride-completion");
    }

}