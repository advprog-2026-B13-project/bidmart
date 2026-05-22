package id.ac.ui.cs.advprog.bidmartcore.bidding.controller;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/bidding")
public class BiddingEventsController {

    private static final long STREAM_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();
    private static final long HEARTBEAT_SECONDS = 20;

    private final RedisMessageListenerContainer redisContainer;
    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sse-heartbeat");
                t.setDaemon(true);
                return t;
            });

    public BiddingEventsController(RedisMessageListenerContainer redisContainer) {
        this.redisContainer = redisContainer;
    }

    @GetMapping(path = "/auctions/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAuction(@PathVariable UUID id) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);

        String channel = "auction:" + id;
        ChannelTopic topic = new ChannelTopic(channel);
        MessageListener listener = (Message message, byte[] pattern) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(new String(message.getBody())));
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        };

        redisContainer.addMessageListener(listener, topic);
        log.debug("SSE client subscribed to {}", channel);

        // Heartbeat detects dead clients quickly so their connection slot is released
        // instead of lingering until the 30-minute timeout.
        ScheduledFuture<?> heartbeat = heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);

        Runnable cleanup = () -> {
            heartbeat.cancel(true);
            redisContainer.removeMessageListener(listener, topic);
            log.debug("SSE client unsubscribed from {}", channel);
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> emitter.complete());

        try {
            emitter.send(SseEmitter.event().name("open").data("connected"));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @PreDestroy
    public void shutdown() {
        heartbeatScheduler.shutdownNow();
    }
}
