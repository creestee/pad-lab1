package at.lab1.drivers.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue createRideAssignmentQueue() {
        return QueueBuilder.durable("q.ride-assignment").build();
    }

    @Bean
    public Queue createRideCancellationQueue() {
        return QueueBuilder.durable("q.ride-cancellation").build();
    }

    @Bean
    @Qualifier("ride-acceptance-queue")
    public Queue createRideAcceptanceQueue() {
        return QueueBuilder.durable("q.ride-acceptance")
                .build();
    }

    @Bean
    @Qualifier("ride-completion-queue")
    public Queue createRideCompletionQueue() {
        return QueueBuilder.durable("q.ride-completion")
                .build();
    }
}