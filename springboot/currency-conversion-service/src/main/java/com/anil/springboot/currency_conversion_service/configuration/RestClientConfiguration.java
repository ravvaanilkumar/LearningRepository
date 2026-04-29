package com.anil.springboot.currency_conversion_service.configuration;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfiguration {


    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestClientBuilder(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }



//    @Bean
//    @LoadBalanced
//    RestClient.Builder restClientBuilder() {  // ✅ @LoadBalanced on Builder
//        return RestClient.builder();
//    }
//
//    @Bean
//    RestClient restClient(RestClient.Builder builder) {  // ✅ Inject Builder
//        return builder.build();
//    }
}
