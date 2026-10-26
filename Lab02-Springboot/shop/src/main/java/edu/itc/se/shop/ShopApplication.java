package edu.itc.se.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

import edu.itc.se.shop.web.HelloController;

@SpringBootApplication
@RestController
public class ShopApplication {
	public static void main(String[] args) {
		SpringApplication.run(ShopApplication.class, args);
		HelloController hwc = new HelloController();
		hwc.hello("world");
	}
}
