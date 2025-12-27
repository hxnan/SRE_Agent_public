package com.flashsale.backend.controller;

import com.flashsale.backend.logging.LoggerService;
import com.flashsale.backend.repository.SeckillGoodsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerNullSafetyTest {
    @Test
    void listActiveWithNullParamsAndRedisErrorDoesNotNpe() throws Exception {
        SeckillGoodsRepository goodsRepo = Mockito.mock(SeckillGoodsRepository.class);
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        LoggerService log = Mockito.mock(LoggerService.class);
        EntityManager em = Mockito.mock(EntityManager.class);
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        ProductController controller = new ProductController(goodsRepo, redis, log, em, mapper);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();

        ZSetOperations<String, String> zops = Mockito.mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(zops);
        when(zops.zCard(anyString())).thenThrow(new RuntimeException());
        ValueOperations<String, String> vops = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(vops);

        Query countQ = Mockito.mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(countQ);
        when(countQ.setParameter(anyString(), any())).thenReturn(countQ);
        when(countQ.getSingleResult()).thenReturn(0L);

        Query listQ = Mockito.mock(Query.class);
        when(em.createNativeQuery(anyString(), any(Class.class))).thenReturn(listQ);
        when(listQ.setParameter(anyString(), any())).thenReturn(listQ);
        when(listQ.getResultList()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/products")
                        .param("status", "active")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.products").exists());
    }
}
