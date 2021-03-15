package me.pwnme.backend;

import me.pwnme.backend.Database.Database;
import me.pwnme.backend.Utils.Utils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		Database.sqlSetup();
		SpringApplication.run(BackendApplication.class, args);
	}

}
