package com.app.uctma.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.file-processing:file_processing_queue}")
    private String queueName;

    @Value("${rabbitmq.exchange.file-processing:file_processing_exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.file-processing:file_processing_routing_key}")
    private String routingKey;

    @Bean
    public Queue fileQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public DirectExchange fileExchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Binding fileBinding(Queue fileQueue, DirectExchange fileExchange) {
        return BindingBuilder
                .bind(fileQueue)
                .to(fileExchange)
                .with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
