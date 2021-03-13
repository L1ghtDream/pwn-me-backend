package me.pwnme.backend;

import me.pwnme.backend.Database.Database;
import me.pwnme.backend.Utils.Utils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		//System.out.println(Utils.customEncode("12ab"));
		//System.out.println(Utils.customEncode("12abb"));
		//System.out.println(Utils.customEncode("TheCustomEncodeDecodeWorks"));
		//System.out.println(Utils.customDecode(Utils.customEncode("TheCustomEncodeDecodeWorks")));
		//System.out.println(Utils.customEncode("12ab"));
		System.out.println(Utils.customDecode(Utils.customEncode("12abb")));
		System.out.println(Utils.customDecode(Utils.customEncode("TheCustomEncodeDecodeWorks")));
		Database.sqlSetup();
		SpringApplication.run(BackendApplication.class, args);
	}

}
