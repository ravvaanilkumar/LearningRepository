package com.anil.springboot.currency_conversion_service.controller;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class CurrencyConversionController {
    private static final Logger logger = LoggerFactory.getLogger(CurrencyConversionController.class);
//    @Autowired
//    private RestClient restClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CurrencyExchangeProxy currencyExchangeProxy;

    @GetMapping("/currency-conversion/from/{from}/to/{to}/quantity/{quantity}")
    public CurrencyConversion calculateCurrencyConversion(
            @PathVariable String from,
            @PathVariable String to,
            @PathVariable BigDecimal quantity
    ) {

        logger.info("calculateCurrencyConversion Called.. From {} To{}", from, to);

        //Using restclient
        HashMap<String, String> uriVariables = new HashMap<>();
        uriVariables.put("from", from);
        uriVariables.put("to", to);
        CurrencyConversion currencyConversion = RestClient.create().get()
                .uri("http://localhost:8000/currency-exchange/from/{from}/to/{to}",
                        uriVariables)
                .retrieve()
                .body(CurrencyConversion.class);

        CurrencyConversion result = Optional.ofNullable(currencyConversion).orElseGet(CurrencyConversion::new);
        result.setQuantity(quantity);
        result.setTotalCalculatedAmount(quantity.multiply(currencyConversion.getConversionMultiple()));

        return  result;
//        return new CurrencyConversion(10001L,
//                from, to, quantity,
//                BigDecimal.ONE,
//                BigDecimal.ONE,
//                "");

    }


    @GetMapping("/currency-conversion/lb/from/{from}/to/{to}/quantity/{quantity}")
    public CurrencyConversion calculateCurrencyConversionWithLB(
            @PathVariable String from,
            @PathVariable String to,
            @PathVariable BigDecimal quantity
    ) {
        logger.info("calculateCurrencyConversionWithLB Called.. From {} To {} Quantity {}", from, to, quantity);
        //Using restclient
        HashMap<String, String> uriVariables = new HashMap<>();
        uriVariables.put("from", from);
        uriVariables.put("to", to);
        CurrencyConversion currencyConversion = restTemplate.getForObject(
                "http://currency-exchange/currency-exchange/from/{from}/to/{to}",CurrencyConversion.class,
                        uriVariables);

        CurrencyConversion result = Optional.ofNullable(currencyConversion).orElseGet(CurrencyConversion::new);
        result.setQuantity(quantity);
        result.setTotalCalculatedAmount(quantity.multiply(currencyConversion.getConversionMultiple()));

        return  result;
//        return new CurrencyConversion(10001L,
//                from, to, quantity,
//                BigDecimal.ONE,
//                BigDecimal.ONE,
//                "");

    }


    @GetMapping("/currency-conversion/feign/from/{from}/to/{to}/quantity/{quantity}")
    public CurrencyConversion calculateCurrencyConversionFeign(
            @PathVariable String from,
            @PathVariable String to,
            @PathVariable BigDecimal quantity
    ) {

        logger.info("calculateCurrencyConversionFeign Called.. From {} To {} Quantity {}", from, to, quantity);

        CurrencyConversion currencyConversion = currencyExchangeProxy.retrieveExchangeValue(from, to);

        CurrencyConversion result = Optional.ofNullable(currencyConversion).orElseGet(CurrencyConversion::new);
        result.setQuantity(quantity);
        result.setTotalCalculatedAmount(quantity.multiply(currencyConversion.getConversionMultiple()));
        result.setEnvironment(currencyConversion.getEnvironment() + " " + "feign");

        return  result;


    }



}
