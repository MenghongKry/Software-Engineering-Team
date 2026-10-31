package edu.itc.se.shop.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(ShopProperties.class)
public class AppConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
