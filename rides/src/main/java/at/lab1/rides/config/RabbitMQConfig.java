package at.lab1.rides.config;

import org.springframework.amqp.core.*;
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

}