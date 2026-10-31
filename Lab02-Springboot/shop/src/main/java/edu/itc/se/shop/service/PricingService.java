package edu.itc.se.shop.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import edu.itc.se.shop.config.ShopProperties;


@Service
public class PricingService {
    private final ShopProperties props;

    public PricingService(ShopProperties props) {
        this.props = props;
    }

    public BigDecimal gross(BigDecimal net) {
        if (net == null) {
            return null;
        }
        return net.multiply(BigDecimal.ONE.add(props.taxRate())).setScale(2, RoundingMode.HALF_UP);
    }
}
