package com.anil.springboot.currency_conversion_service.controller;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


//For calling the currency exchange URL is hardcoded.
//@FeignClient(name="currency-exchange", url="localhost:8000")
//For calling the currency exchange URL using service discovery
@FeignClient(name="currency-exchange")
public interface CurrencyExchangeProxy {

    @GetMapping("/currency-exchange/from/{from}/to/{to}")
    public CurrencyConversion retrieveExchangeValue(
            @PathVariable("from") String from,
            @PathVariable("to") String to);

}
