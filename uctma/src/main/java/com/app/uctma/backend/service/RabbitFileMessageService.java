package com.app.uctma.backend.service;

import com.app.uctma.backend.dto.FileMessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Service
@RequiredArgsConstructor
public class RabbitFileMessageService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.file-processing}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.file-processing}")
    private String routingKey;

    public void sendFileProcessingMessage(FileMessageDTO message) {
        System.out.println("RabbitMQ'ya dosya işleme mesajı gönderiliyor: "+  message);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
    }

}
