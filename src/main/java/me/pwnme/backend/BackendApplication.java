package me.pwnme.backend;

import me.pwnme.backend.Database.Database;
import me.pwnme.backend.Utils.Utils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		System.out.println(Utils.encodeBase64("1"));
		System.out.println(Utils.customEncode("1"));
		System.out.println(Utils.customDecode(Utils.customEncode("1")));
		System.out.println(Utils.customDecode("Mw==?NDU2NTMz?2nN2nN"));
		Database.sqlSetup();
		SpringApplication.run(BackendApplication.class, args);
	}

}
