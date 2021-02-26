package me.pwnme.backend.Configuration;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "reset-email")
@Data
public class ResetPasswordEmailProperties {

    public String from;
    public String subject;
    public String content;

}
