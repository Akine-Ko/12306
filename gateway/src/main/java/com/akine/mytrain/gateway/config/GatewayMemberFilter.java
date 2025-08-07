package com.akine.mytrain.gateway.config;

import com.akine.mytrain.gateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayMemberFilter implements GlobalFilter, Ordered {
    private static final Logger logger = LoggerFactory.getLogger(GatewayMemberFilter.class);
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (path.contains("/admin")
                || path.contains("/redis")
                || path.contains("/hello")
                || path.contains("/member/member/login")
                || path.contains("/member/member/send-code")
                || path.contains("/business/kaptcha")){
            logger.info("不需要登录验证:{}", path);
            return chain.filter(exchange);
        }
        else{
            logger.info("需要登录验证:{}", path);
        }

        //获取header的token参数
        String token = exchange.getRequest().getHeaders().getFirst("token");
        logger.info("会员登录验证开始:token:{}", token);

        if (token == null || token.isEmpty()) {
            logger.info( "token为空，请求被拦截" );
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        //校验token是否有效，包括token是否修改过，是否过期
        boolean validate = JwtUtil.validate(token);
        if (validate) {
            logger.info("token有效，放行该请求");
            return chain.filter(exchange);
        } else {
            logger.warn( "token无效，请求被拦截" );
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

    }


    @Override
    public int getOrder() {
        return 1;
    }
}
