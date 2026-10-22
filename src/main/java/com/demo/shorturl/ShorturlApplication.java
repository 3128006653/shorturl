package com.demo.shorturl;

import org.springframework.context.annotation.ComponentScan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ComponentScan("com.demo")
public class ShorturlApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShorturlApplication.class, args);
	}

}
