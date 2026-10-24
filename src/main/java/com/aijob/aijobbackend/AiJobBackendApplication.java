package com.aijob.aijobbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class AiJobBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiJobBackendApplication.class, args);
	}

}
