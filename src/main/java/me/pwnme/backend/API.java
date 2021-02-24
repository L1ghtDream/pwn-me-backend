package me.pwnme.backend;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class API {

    @PostMapping("/unity/api/check-version")
    public void checkVersion()
    {
        System.out.println("Works");
    }

}
