package id.ac.ui.cs.advprog.bidmartcore.bidding.controller;

import java.util.UUID;

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

    private final RedisMessageListenerContainer redisContainer;

    public BiddingEventsController(RedisMessageListenerContainer redisContainer) {
        this.redisContainer = redisContainer;
    }

    @GetMapping(path = "/auctions/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAuction(@PathVariable UUID id) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        String channel = "auction:" + id;
        MessageListener listener = (Message message, byte[] pattern) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(new String(message.getBody())));
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        };

        redisContainer.addMessageListener(listener, new ChannelTopic(channel));
        log.debug("SSE client subscribed to {}", channel);

        try {
            emitter.send(SseEmitter.event().name("open").data("connected"));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        emitter.onCompletion(() -> {
            redisContainer.removeMessageListener(listener, new ChannelTopic(channel));
            log.debug("SSE client unsubscribed from {}", channel);
        });
        emitter.onTimeout(() -> {
            redisContainer.removeMessageListener(listener, new ChannelTopic(channel));
            log.debug("SSE client timed out for {}", channel);
        });

        return emitter;
    }
}
