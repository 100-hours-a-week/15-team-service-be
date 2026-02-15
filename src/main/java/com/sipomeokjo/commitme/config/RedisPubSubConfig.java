package com.sipomeokjo.commitme.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sipomeokjo.commitme.domain.chat.pubsub.ChatBroadcastPayload;
import com.sipomeokjo.commitme.domain.chat.pubsub.ChatMessageSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class RedisPubSubConfig {

    @Bean
    public ChannelTopic chatMessageTopic() {
        return new ChannelTopic("chat:messages");
    }

    @Bean(name = "chatRedisObjectMapper")
    public ObjectMapper chatRedisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    @Bean
    public RedisTemplate<String, ChatBroadcastPayload> chatMessageRedisTemplate(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer chatMessageSerializer) {

        RedisTemplate<String, ChatBroadcastPayload> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(chatMessageSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(chatMessageSerializer);
        return template;
    }

    @Bean
    public GenericJackson2JsonRedisSerializer chatMessageSerializer(
            @Qualifier("chatRedisObjectMapper") ObjectMapper chatRedisObjectMapper) {
        return new GenericJackson2JsonRedisSerializer(chatRedisObjectMapper);
    }

    @Bean
    public RedisMessageListenerContainer chatMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ChatMessageSubscriber chatMessageSubscriber,
            ChannelTopic chatMessageTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(chatMessageSubscriber, chatMessageTopic);
        return container;
    }
}
