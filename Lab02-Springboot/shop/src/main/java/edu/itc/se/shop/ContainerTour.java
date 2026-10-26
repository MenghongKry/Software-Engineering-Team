package edu.itc.se.shop;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import edu.itc.se.shop.service.ReportBuilder;
import edu.itc.se.shop.service.WelcomeService;

@Component
public class ContainerTour implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ContainerTour.class);

    private final ApplicationContext ctx;
    private final WelcomeService welcome;
    private final ObjectProvider<ReportBuilder> reports;

    public ContainerTour(ApplicationContext ctx, WelcomeService welcome,
                         ObjectProvider<ReportBuilder> reports) {
        this.ctx = ctx;
        this.welcome = welcome;
        this.reports = reports;
    }

    @Override
    public void run(String... args) {
        // Step 6: bean count and every bean name containing "Notifier"
        String[] notifiers = Arrays.stream(ctx.getBeanDefinitionNames())
                .filter(name -> name.toLowerCase().contains("notifier"))
                .toArray(String[]::new);
        log.info("{} beans; notifiers = {}", ctx.getBeanDefinitionCount(), Arrays.toString(notifiers));

        // Steps 2-3: normal welcome goes to the primary notifier, urgent one by SMS
        welcome.welcome("ann", false);
        welcome.welcome("bob", true);

        // Step 4: a prototype gives a new object on every getObject()
        ReportBuilder first = reports.getObject();
        ReportBuilder second = reports.getObject();
        log.info("prototype instances differ: {} ({} vs {})", first != second,
                System.identityHashCode(first), System.identityHashCode(second));
    }
}
