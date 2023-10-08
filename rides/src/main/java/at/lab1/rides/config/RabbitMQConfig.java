package at.lab1.rides.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue createRideRequestsQueue() {
        return new Queue("q.ride-requests");
    }

    @Bean
    public Queue createRideCancelsQueue() {
        return new Queue("q.ride-cancels");
    }

}