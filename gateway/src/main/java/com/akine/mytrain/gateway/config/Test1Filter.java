/*package com.akine.mytrain.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class Test1Filter implements GlobalFilter, Ordered {
    private static final Logger logger = LoggerFactory.getLogger(Test1Filter.class);
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.info("Test1Filter filter");
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 1;
    }
}*/
