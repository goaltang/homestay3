package com.homestay3.homestaybackend.exception;

import com.homestay3.homestaybackend.dto.PricingQuoteResponse;
import lombok.Getter;

/**
 * 价格变动异常
 * 下单时报价 token 过期或价格发生变化时抛出
 */
@Getter
public class PriceChangedException extends RuntimeException {

    private final PricingQuoteResponse latestQuote;

    public PriceChangedException(String message, PricingQuoteResponse latestQuote) {
        super(message);
        this.latestQuote = latestQuote;
    }
}
