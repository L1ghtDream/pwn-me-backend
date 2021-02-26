package me.pwnme.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileNotFoundException;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		System.out.println("Backend Stared");
		Database.sqlSetup();
		SpringApplication.run(BackendApplication.class, args);
	}

}
