package edu.itc.se.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.itc.se.shop.web.HelloWorldController;

@SpringBootApplication
@RestController
public class ShopApplication {
	public static void main(String[] args) {
		SpringApplication.run(ShopApplication.class, args);
		HelloWorldController hwc = new HelloWorldController();
		hwc.hello("world");
	}
}
