package com.flashsale.backend.kafka;

import com.flashsale.backend.logging.LoggerService;
import com.flashsale.backend.repository.SeckillOrderRepository;
import com.flashsale.backend.service.MetricsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class OrderConsumerNullSafetyTest {
    @Test
    void handleMissingRequestIdDoesNotThrow() {
        SeckillOrderRepository orderRepo = Mockito.mock(SeckillOrderRepository.class);
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        LoggerService log = Mockito.mock(LoggerService.class);
        KafkaTemplate<String, String> kafka = Mockito.mock(KafkaTemplate.class);
        MetricsService metrics = Mockito.mock(MetricsService.class);
        ValueOperations<String, String> vops = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(vops);
        when(vops.setIfAbsent(anyString(), anyString(), any(java.time.Duration.class))).thenReturn(Boolean.TRUE);
        OrderConsumer consumer = new OrderConsumer(orderRepo, redis, log, kafka, metrics);
        String json = "{\"order_id\":\"o-1\",\"goods_id\":123,\"price\":99.9,\"user_id\":456}";
        consumer.handle(json);
    }
}
