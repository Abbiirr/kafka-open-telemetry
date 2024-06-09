package com.alammar.springobservabilityautoinstrumentkafka.sink;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.util.Optional;

@Component
public class OrderSink {

    @Autowired
    private Tracer tracer;

    @KafkaListener(topics = "my-topic", groupId = "my-group-id")
    public void listen(ConsumerRecord<String, String> data) {
        String correlationId = getHeader(data, "correlation-id");
        String traceId = getHeader(data, "trace-id");
        String spanId = getHeader(data, "span-id");

        if (traceId == null || spanId == null) {
            // Handle missing trace context appropriately
            return;
        }

        SpanContext spanContext = SpanContext.createFromRemoteParent(
                traceId,
                spanId,
                TraceFlags.getDefault(),
                TraceState.getDefault()
        );

        Context context = Context.current().with(Span.wrap(spanContext));
        try (Scope scope = context.makeCurrent()) {
            Span span = tracer.spanBuilder("kafka-consume")
                    .setParent(context)
                    .setAttribute("correlation.id", correlationId)
                    .startSpan();

            try {
                // Your business logic here
                System.out.println("Received message: " + data.value() + " with Correlation ID: " + correlationId);
            } finally {
                span.end();
            }
        }
    }

    private String getHeader(ConsumerRecord<String, String> data, String headerKey) {
        return Optional.ofNullable(data.headers().lastHeader(headerKey))
                .map(header -> new String(header.value()))
                .orElse(null);
    }
}
