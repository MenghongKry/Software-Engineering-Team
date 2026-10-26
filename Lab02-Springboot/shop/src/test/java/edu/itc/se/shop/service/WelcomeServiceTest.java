package edu.itc.se.shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Plain JUnit: no Spring context, we build the objects by hand. */
class WelcomeServiceTest {

    private final List<String> emails = new ArrayList<>();
    private final List<String> sms = new ArrayList<>();
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-25T09:15:02Z"), ZoneOffset.UTC);

    // the two lambdas are fake Notifiers that only remember what they were asked to send
    private final WelcomeService service = new WelcomeService(
            (to, text) -> emails.add(to + ":" + text),
            (to, text) -> sms.add(to + ":" + text),
            clock);

    @Test
    void normalWelcomeGoesThroughDefaultNotifier() {
        String text = service.welcome("ann", false);

        assertEquals("Welcome ann! It is 09:15:02", text);
        assertEquals(List.of("ann:Welcome ann! It is 09:15:02"), emails);
        assertEquals(List.of(), sms);
    }

    @Test
    void urgentWelcomeGoesThroughUrgentNotifier() {
        service.welcome("bob", true);

        assertEquals(List.of(), emails);
        assertEquals(List.of("bob:Welcome bob! It is 09:15:02"), sms);
    }
}
