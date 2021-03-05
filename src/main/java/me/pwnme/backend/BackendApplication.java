package me.pwnme.backend;

import me.pwnme.backend.Database.Database;
import me.pwnme.backend.Utils.Utils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {

		System.out.println(Utils.encodeBase64("4"));
		System.out.println(Utils.decodeBase64("NA=="));
		System.out.println(Utils.decodeBase64("NA"));

		System.out.println(Utils.customEncode("12ab"));
		System.out.println(Utils.customDecode(Utils.customEncode("12ab")));

		System.out.println("Backend Stared");
		Database.sqlSetup();
		SpringApplication.run(BackendApplication.class, args);
	}

}
