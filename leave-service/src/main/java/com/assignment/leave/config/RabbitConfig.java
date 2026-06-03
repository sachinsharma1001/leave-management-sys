package com.assignment.leave.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    DirectExchange leaveExchange(@Value("${app.notification.exchange}") String exchange) {
        return new DirectExchange(exchange);
    }

    @Bean
    Queue leaveNotificationQueue(@Value("${app.notification.queue}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    Binding binding(Queue leaveNotificationQueue, DirectExchange leaveExchange, @Value("${app.notification.routing-key}") String routingKey) {
        return BindingBuilder.bind(leaveNotificationQueue).to(leaveExchange).with(routingKey);
    }

    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
