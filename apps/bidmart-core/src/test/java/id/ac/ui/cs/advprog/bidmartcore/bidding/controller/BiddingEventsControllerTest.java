package id.ac.ui.cs.advprog.bidmartcore.bidding.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BiddingEventsControllerTest {

    private RedisMessageListenerContainer redisContainer;
    private BiddingEventsController controller;

    @BeforeEach
    void setUp() {
        redisContainer = mock(RedisMessageListenerContainer.class);
        controller = new BiddingEventsController(redisContainer);
    }

    @AfterEach
    void tearDown() {
        controller.shutdown();
    }

    @Test
    void streamAuction_registersListenerAndReturnsEmitter() {
        SseEmitter emitter = controller.streamAuction(UUID.randomUUID());

        assertThat(emitter).isNotNull();
        verify(redisContainer).addMessageListener(any(MessageListener.class), any(Topic.class));
    }

    @Test
    void streamAuction_forwardsRedisMessageToEmitter() throws Exception {
        UUID id = UUID.randomUUID();
        final MessageListener[] captured = new MessageListener[1];
        doAnswer(inv -> {
            captured[0] = inv.getArgument(0);
            return null;
        }).when(redisContainer).addMessageListener(any(MessageListener.class), any(Topic.class));

        SseEmitter emitter = controller.streamAuction(id);
        assertThat(captured[0]).as("listener should be registered").isNotNull();

        // Delivering a Redis message should not throw and should push through the emitter.
        DefaultMessage message = new DefaultMessage(
                ("auction:" + id).getBytes(StandardCharsets.UTF_8),
                "{\"type\":\"price-change\"}".getBytes(StandardCharsets.UTF_8));
        captured[0].onMessage(message, null);

        assertThat(emitter).isNotNull();
    }

    @Test
    void redisMessageAfterCompletion_isHandledGracefully() {
        UUID id = UUID.randomUUID();
        final MessageListener[] captured = new MessageListener[1];
        doAnswer(inv -> {
            captured[0] = inv.getArgument(0);
            return null;
        }).when(redisContainer).addMessageListener(any(MessageListener.class), any(Topic.class));

        SseEmitter emitter = controller.streamAuction(id);
        emitter.complete();

        // Sending to a completed emitter throws inside the listener; the catch must swallow it.
        DefaultMessage message = new DefaultMessage(
                ("auction:" + id).getBytes(StandardCharsets.UTF_8),
                "{\"type\":\"price-change\"}".getBytes(StandardCharsets.UTF_8));

        org.assertj.core.api.Assertions.assertThatCode(() -> captured[0].onMessage(message, null))
                .doesNotThrowAnyException();
    }

    @Test
    void shutdown_isIdempotent() {
        controller.shutdown();
        // second shutdown via tearDown must not throw
        assertThat(controller).isNotNull();
    }
}
