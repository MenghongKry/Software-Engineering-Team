package edu.itc.se.shop.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ReportBuilder {
    private final List<String> lines = new ArrayList<>();

    public ReportBuilder line(String line) {
        lines.add(line);
        return this;
    }

    public String build() {
        return String.join(System.lineSeparator(), lines);
    }
}
