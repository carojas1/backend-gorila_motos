package com.projectBackend.GMotors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GMotorsApplication {

	public static void main(String[] args) {
		SpringApplication.run(GMotorsApplication.class, args);
	}

}
