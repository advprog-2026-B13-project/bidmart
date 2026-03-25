package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
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

    @Bean
    public RedisScript<List> placeBidScript() {
        return RedisScript.of(RedisLuaScripts.PLACE_BID_LUA, List.class);
    }

    @Bean
    public RedisScript<Long> rollbackScript() {
        return RedisScript.of(RedisLuaScripts.ROLLBACK_LUA, Long.class);
    }
}
