package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.redis.RedisLuaScripts;

@Configuration
public class BiddingRedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    public RedisScript<List<Object>> placeBidScript() {
        return (RedisScript) RedisScript.of(RedisLuaScripts.PLACE_BID_LUA, List.class);
    }

    @Bean
    public RedisScript<Long> rollbackScript() {
        return RedisScript.of(RedisLuaScripts.ROLLBACK_LUA, Long.class);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
