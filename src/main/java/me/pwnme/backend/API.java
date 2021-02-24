package me.pwnme.backend;

import me.pwnme.backend.DTO.LoginBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class API {

    @PostMapping("/api/login")
    public void checkVersion(@RequestBody LoginBody body)
    {
        System.out.println("Works");
        System.out.println(body.email);
        System.out.println(body.password);
    }

}
