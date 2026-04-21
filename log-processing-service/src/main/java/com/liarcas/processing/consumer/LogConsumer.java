package com.liarcas.processing.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.liarcas.models.LogEvent;
import com.liarcas.processing.document.LogEventDocument;
import com.liarcas.processing.repository.LogEventRepository;

@Component
public class LogConsumer {
    private final LogEventRepository logEventRepository;

    public LogConsumer(LogEventRepository logEventRepository) {
        this.logEventRepository = logEventRepository;
    }

    @KafkaListener(topics = "raw-logs")
    public void consume(LogEvent message) {   
        LogEventDocument document = new LogEventDocument(
            message.getId(),
            message.getServiceName(),
            message.getLevel(),
            message.getMessage(),
            message.getTimestamp()
        );
        logEventRepository.save(document);

        System.out.println("Consumed and saved log event: " + document.getId());
    }
}
