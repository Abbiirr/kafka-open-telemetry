package com.alammar.springobservabilityautoinstrumentkafka.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private Tracer tracer;

    @GetMapping
    public String createOrder() {
        String orderId = UUID.randomUUID().toString();
        String correlationId = UUID.randomUUID().toString();

        Span span = tracer.spanBuilder("order-request")
                .setAttribute("correlation.id", correlationId)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            SpanContext spanContext = span.getSpanContext();

            ProducerRecord<String, String> record = new ProducerRecord<>("my-topic", orderId);
            record.headers().add(new RecordHeader("correlation-id", correlationId.getBytes()));
            record.headers().add(new RecordHeader("trace-id", spanContext.getTraceId().getBytes()));
            record.headers().add(new RecordHeader("span-id", spanContext.getSpanId().getBytes()));

            kafkaTemplate.send(record);
        } finally {
            span.end();
        }

        return orderId;
    }
}
