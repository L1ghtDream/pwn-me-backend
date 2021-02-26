package me.pwnme.backend;

import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import me.pwnme.backend.Configuration.ResetPasswordEmailProperties;
import me.pwnme.backend.DTO.ForgotPasswordBody;
import me.pwnme.backend.DTO.LoginBody;
import me.pwnme.backend.DTO.Mail;
import me.pwnme.backend.DTO.RegisterBody;
import me.pwnme.backend.Services.MailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class API {

    private final ResetPasswordEmailProperties resetPasswordEmailProperties;
    private final MailService mailService;

    /*
         0 -> OK
         1 -> Email already exists
         2 -> Invalid Credentials
         3 -> Email does not exist
        -1 -> SQL injection is not allowed
        -2 -> Internal Error
        -3 -> String format exploit is not allowed

    */

    @PostMapping("/api/login")
    public String login(@RequestBody LoginBody body) {

        try {
            if(body.email.contains(" "))
                return "-1";
            if(body.password.contains(" "))
                return "-1";
            if(body.email.contains("%"))
                return "-3";
            if(body.password.contains("%"))
                return "-3";

            String query = "SELECT * FROM '{table}' WHERE EMAIL='{email}'";
            query = query.replace("{table}", Database.usersTable);
            query = query.replace("{email}", body.email);
            PreparedStatement st = Database.connection.prepareStatement(query);
            ResultSet result = st.executeQuery();
            if(result.next())
                if(result.getInt("COUNT(*)")==1)
                    if(result.getString("PASSWORD").equals(body.password))
                        return "0";
            return "2";


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return "-2";
    }

    @PostMapping("/api/register")
    public String register(@RequestBody RegisterBody body){

        try {
            if(body.email.contains(" "))
                return "-1";
            if(body.password.contains(" "))
                return "-1";
            if(body.email.contains("%"))
                return "-3";
            if(body.password.contains("%"))
                return "-3";

            String query = "SELECT COUNT(*) FROM '{table}' WHERE EMAIL='{email}'";
            query = query.replace("{table}", Database.usersTable);
            query = query.replace("{email}", body.email);

            PreparedStatement st = Database.connection.prepareStatement(query);
            ResultSet result = st.executeQuery();
            if(result.next())
                if(result.getInt("COUNT(*)")==0){
                    query = "INSERT INTO `{table}` VALUES ('{email}', '{password}')";
                    query = query.replace("{table}", Database.usersTable);
                    query = query.replace("{email}", body.email);
                    query = query.replace("{password}", body.password);

                    st = Database.connection.prepareStatement(query);
                    st.executeUpdate();
                    return "0";
                }
            return "1";


        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "-2";
    }

    @PostMapping("/api/forgot-password")
    public String forgotPassword(@RequestBody ForgotPasswordBody body){

        try {
            if(body.email.contains(" "))
                return "-1";
            if(body.email.contains("%"))
                return "-3";

            //TODO: Check if the email is valid
            //TODO: Send reset email
            //TODO: Add do database the token for password reset

            String query = "SELECT EMAIL FROM '{table}' WHERE EMAIL='{email}'";
            query = query.replace("{table}", Database.usersTable);
            query = query.replace("{email}", body.email);
            PreparedStatement st = Database.connection.prepareStatement(query);
            ResultSet result = st.executeQuery();
            if(result.next())
                if(result.getInt("COUNT(*)")==1){

                    Map<String, Object> placeholders = new HashMap<>();
                    placeholders.put("content", resetPasswordEmailProperties.content);

                    mailService.sendMail(new Mail(resetPasswordEmailProperties.from, body.email, resetPasswordEmailProperties.subject), "reset-password.ftl", placeholders);
                    return "0";
                }
            return "3";


        } catch (SQLException | MessagingException | IOException | TemplateException e) {
            e.printStackTrace();
        }
        return "-2";
    }

    //@RequestParam

}
