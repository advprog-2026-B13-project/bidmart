package id.ac.ui.cs.advprog.bidmartcore.catalog.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RabbitMQConfig {

    public static final String BID_PLACED_QUEUE = "bidmart.catalog.bidplaced.queue";
    public static final String BIDMART_EXCHANGE = "bidmart.exchange";
    public static final String BID_PLACED_ROUTING_KEY = "bid.placed";

    @Bean
    public Queue bidPlacedQueue() {
        return new Queue(BID_PLACED_QUEUE, true);
    }

    @Bean
    public TopicExchange bidmartExchange() {
        return new TopicExchange(BIDMART_EXCHANGE);
    }

    @Bean
    public Binding bidPlacedBinding(Queue bidPlacedQueue, TopicExchange bidmartExchange) {
        return BindingBuilder.bind(bidPlacedQueue)
                .to(bidmartExchange)
                .with(BID_PLACED_ROUTING_KEY);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}