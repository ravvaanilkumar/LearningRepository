package com.anil.springboot.api_gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("currency-exchange", r -> r
                        .path("/currency-exchange/**")
                        .uri("lb://currency-exchange"))
                .route("currency-conversion-service", r -> r
                        .path("/currency-conversion/**")
                        .uri("lb://currency-conversion-service"))
                .build();
    }
}
