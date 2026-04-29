package com.anil.springboot.currency_exchange.repositories;

import com.anil.springboot.currency_exchange.currencybeans.CurrencyExchange;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyExchangeRepository extends JpaRepository<CurrencyExchange, Long>
{
    CurrencyExchange findByFromAndTo(String from, String to);
}
