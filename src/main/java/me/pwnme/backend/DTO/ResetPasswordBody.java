package me.pwnme.backend.DTO;

import lombok.Data;

@Data
public class ResetPasswordBody {

    public String token;
    public String email;
    public String newPassword;

}
