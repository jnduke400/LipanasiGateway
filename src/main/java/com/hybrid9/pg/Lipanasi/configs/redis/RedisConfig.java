package com.hybrid9.pg.Lipanasi.configs.redis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import org.apache.camel.component.redis.processor.idempotent.SpringRedisIdempotentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableRedisRepositories(basePackageClasses = RedisConfig.class)
public class RedisConfig {

    // Comma-separated list of cluster nodes e.g. "172.28.0.11:6379,172.28.0.12:6379,..."
    @Value("${redis.cluster.nodes}")
    private List<String> clusterNodes;

    @Value("${redis.password}")
    private String redisPassword;

    @Value("${redis.timeout:2000}")
    private int timeout;

    @Value("${redis.cluster.max-redirects:3}")
    private int maxRedirects;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        // 1. Cluster configuration
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(clusterNodes);
        clusterConfig.setPassword(RedisPassword.of(redisPassword));
        clusterConfig.setMaxRedirects(maxRedirects);

        // 2. Topology refresh — critical for cluster failover awareness
        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofSeconds(30))        // Poll cluster topology every 30s
                .enableAllAdaptiveRefreshTriggers()                    // React to MOVED/ASK/reconnects
                .adaptiveRefreshTriggersTimeout(Duration.ofSeconds(30))
                .build();

        // 3. Cluster client options
        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(topologyRefreshOptions)
                .autoReconnect(true)
                .disconnectedBehavior(io.lettuce.core.ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .validateClusterNodeMembership(false) // Set true in production if IPs are stable
                .build();

        // 4. Lettuce client configuration
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(timeout))
                .readFrom(ReadFrom.REPLICA_PREFERRED) // Still works in cluster mode
                .clientOptions(clusterClientOptions)
                .build();

        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        ObjectMapper objectMapper = createObjectMapper();
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = createObjectMapper();
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration)
                .transactionAware()
                .build();
    }

    @Bean(name = "depositIdempotentRepository")
    public SpringRedisIdempotentRepository depositIdempotentRepository(RedisTemplate<String, String> stringRedisTemplate) {
        return new SpringRedisIdempotentRepository(stringRedisTemplate, "deposit-idempotent");
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new Hibernate6Module()
                .disable(Hibernate6Module.Feature.USE_TRANSIENT_ANNOTATION)
                .disable(Hibernate6Module.Feature.FORCE_LAZY_LOADING));
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return objectMapper;
    }
}