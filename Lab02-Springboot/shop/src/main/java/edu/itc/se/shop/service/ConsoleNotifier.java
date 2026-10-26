package edu.itc.se.shop.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Primary
@Profile("dev")
public class ConsoleNotifier implements Notifier {
    private static final Logger log = LoggerFactory.getLogger(ConsoleNotifier.class);

    @Override
    public void send(String to, String text) {
        System.out.println("CONSOLE to=" + to + " text=" + text);
    }

    @PostConstruct
    void ready() { log.info("ConsoleNotifier ready"); }

    @PreDestroy
    void closing() { log.info("ConsoleNotifier closing"); }
}
