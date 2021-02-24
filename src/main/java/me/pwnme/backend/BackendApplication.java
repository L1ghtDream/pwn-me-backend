package me.pwnme.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		System.out.println("Backend Stared");
		SpringApplication.run(BackendApplication.class, args);
	}

}
