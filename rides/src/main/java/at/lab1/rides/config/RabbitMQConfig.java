package at.lab1.rides.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue createRideAssignmentQueue() {
        return new Queue("q.ride-assignment");
    }

    @Bean
    public Queue createRideCancellationQueue() {
        return new Queue("q.ride-cancellation");
    }

}