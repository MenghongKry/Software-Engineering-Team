package edu.itc.se.shop.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

public class HelloWorldController {

    @GetMapping("/hello")
    public String hello(@RequestParam(defaultValue = "World") String name) {
      return String.format("Hello %s!", name);
    }
    
}
