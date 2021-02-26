package me.pwnme.backend.DTO;

import lombok.Data;

@Data
public class Mail {

    public String from;
    public String to;
    public String subject;


    public Mail(String from, String to, String subject) {
        this.from = from;
        this.to = to;
        this.subject = subject;
    }
}
