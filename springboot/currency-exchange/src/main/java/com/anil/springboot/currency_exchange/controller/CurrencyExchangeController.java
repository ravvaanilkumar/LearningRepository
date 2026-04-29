package com.anil.springboot.currency_exchange.controller;

import java.math.BigDecimal;

import com.anil.springboot.currency_exchange.currencybeans.CurrencyExchange;
import com.anil.springboot.currency_exchange.repositories.CurrencyExchangeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrencyExchangeController {
    private final Logger logger = LoggerFactory.getLogger(CurrencyExchangeController.class);
    @Autowired
    private Environment environment;

    @Autowired
    private CurrencyExchangeRepository repository;

    @GetMapping("/currency-exchange/from/{from}/to/{to}")
    public CurrencyExchange retrieveExchangeValue(
            @PathVariable String from,
            @PathVariable String to) {

        logger.info("currency-exchange called... From {} To{}", from, to);
        CurrencyExchange currencyExchange = new CurrencyExchange(1000L, from, to,
                BigDecimal.valueOf(50));
        String port = environment.getProperty("local.server.port");
        currencyExchange = repository.findByFromAndTo(from,to);
        System.out.println("currencyExchange: " + currencyExchange);
        currencyExchange.setEnvironment(port);
        return currencyExchange;

    }

}