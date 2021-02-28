package me.pwnme.backend;

import freemarker.core.ParseException;
import freemarker.template.*;
import lombok.RequiredArgsConstructor;
import me.pwnme.backend.Configuration.Config;
import me.pwnme.backend.Configuration.ResetPasswordEmailProperties;
import me.pwnme.backend.DTO.*;
import me.pwnme.backend.Services.MailService;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class API {

    private final ResetPasswordEmailProperties resetPasswordEmailProperties;
    private final MailService mailService;
    private final Config config;
    private final Configuration freemarkerConfiguration;


    /*
         0 -> OK
         1 -> Email already exists
         2 -> Invalid Credentials
         3 -> Email does not exist
         4 -> Data can not be null or empty
        -1 -> SQL injection is not allowed
        -2 -> Internal Error
        -3 -> String format exploit is not allowed
    */

    //TODO: With login and register send a base64 encoded token
    /*

    token = Base64(
    {
        "email": "{email}",
        "timeCreated": "{timeCreated}",
        "timeExpire": "{timeExpire}",
        "renewable": "{authToken}"
    }
    )

    timeCreated = new Date().now();
    timeExpire = timeCreated + 10m
    authToken=sha256(base65(sha256(sha256(password)))

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
            if(body.email.equals(""))
                return "4";
            if(body.password.equals(""))
                return "4";

            String query = "SELECT * FROM `{table}` WHERE EMAIL='{email}'";
            query = query.replace("{table}", Database.usersTable);
            query = query.replace("{email}", body.email);
            PreparedStatement st = Database.connection.prepareStatement(query);
            ResultSet result = st.executeQuery();

            query = "SELECT COUNT(*) FROM `{table}` WHERE EMAIL='{email}'";
            query = query.replace("{table}", Database.usersTable);
            query = query.replace("{email}", body.email);
            PreparedStatement st1 = Database.connection.prepareStatement(query);
            ResultSet result1 = st1.executeQuery();
            if(result.next() && result1.next()){
                if(result1.getInt("COUNT(*)")==1) {
                    if (result.getString("PASSWORD").equals(body.password)) {
                        String token = "{\"email\": \"{email}\",\"timeCreated\": \"{timeCreated}\",\"timeExpire\": \"{timeExpire}\",\"password\": \"{password}\"}";

                        long time = new Date().getTime();

                        token = token.replace("{email}", body.email);
                        token = token.replace("{timeCreated}", String.valueOf(time));
                        token = token.replace("{timeExpire}", String.valueOf(time + 600000L));
                        token = token.replace("{password}", body.password);

                        token = Utils.encodeBase64(token);

                        return "0 " + token;
                    }
                }
            }
            return "2";


        } catch (SQLException e) {
            e.printStackTrace();
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
            if(body.email.equals(""))
                return "4";
            if(body.password.equals(""))
                return "4";

            String query = "SELECT COUNT(*) FROM `{table}` WHERE EMAIL='{email}'";
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

                    String token = "{\"email\": \"{email}\",\"timeCreated\": \"{timeCreated}\",\"timeExpire\": \"{timeExpire}\",\"password\": \"{password}\"}";

                    long time = new Date().getTime();

                    token = token.replace("{email}", body.email);
                    token = token.replace("{timeCreated}", String.valueOf(time));
                    token = token.replace("{timeExpire}", String.valueOf(time + 600000L));
                    token = token.replace("{password}", body.password);

                    token = Utils.encodeBase64(token);

                    return "0 " + token;
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

            //TODO: [DONE] Check if the email is valid
            //TODO: Generate token
            //TODO: Add do database the token for password reset
            //TODO: [DONE] Send reset email

            String query = "SELECT COUNT(*) FROM `{table}` WHERE EMAIL='{email}'";
            query = query.replace("{table}", Database.usersTable);
            query = query.replace("{email}", body.email);
            PreparedStatement st = Database.connection.prepareStatement(query);
            ResultSet result = st.executeQuery();
            if(result.next())
                if(result.getInt("COUNT(*)")==1){

                    boolean generateToken = true;
                    String token = "";

                    while(generateToken) {
                        token = Utils.generateRandomString(32);

                        query = "SELECT COUNT(*) FROM `{table}` WHERE TOKEN='{token}'";
                        query = query.replace("{table}", Database.tokenTable);
                        query = query.replace("{token}", token);
                        st = Database.connection.prepareStatement(query);
                        result = st.executeQuery();
                        if(result.next())
                            if(result.getInt("COUNT(*)") == 0)
                                generateToken = false;
                        else
                            return "-2";
                    }



                    query = "SELECT COUNT(*) FROM `{table}` WHERE EMAIL='{email}'";
                    query = query.replace("{table}", Database.tokenTable);
                    query = query.replace("{email}", body.email);
                    st = Database.connection.prepareStatement(query);
                    result = st.executeQuery();
                    if(result.next()){
                        if(result.getInt("COUNT(*)")==0)
                            query = "INSERT INTO `{table}` VALUES ('{email}', '{token}', '{date}')";
                        else {
                            query = "UPDATE `{table}` SET DATE='{date}' WHERE EMAIL='{email}'";
                            query = query.replace("{table}", Database.tokenTable);
                            query = query.replace("{email}", body.email);
                            query = query.replace("{token}", token);
                            query = query.replace("{date}", String.valueOf(new Date().getTime()));

                            st = Database.connection.prepareStatement(query);
                            st.executeUpdate();

                            query = "UPDATE `{table}` SET TOKEN='{token}' WHERE EMAIL='{email}'";
                        }

                        query = query.replace("{table}", Database.tokenTable);
                        query = query.replace("{email}", body.email);
                        query = query.replace("{token}", token);
                        query = query.replace("{date}", String.valueOf(new Date().getTime()));

                        System.out.println(query);
                        st = Database.connection.prepareStatement(query);
                        st.executeUpdate();
                    }
                    else
                        return "-2";





                    Map<String, Object> placeholders = new HashMap<>();
                    placeholders.put("name", "Anonymous");
                    placeholders.put("host", config.host);
                    placeholders.put("token", token);

                    mailService.sendMail(new Mail(resetPasswordEmailProperties.from, body.email, resetPasswordEmailProperties.subject), "reset-password-mail.ftl", placeholders);
                    return "0";
                }
            return "3";


        } catch (SQLException | MessagingException | IOException | TemplateException e) {
            e.printStackTrace();
        }
        return "-2";
    }

    @GetMapping("/reset-password")
    public String resetPasswordMessage(@RequestParam String token){

        //TODO: Create the web portal for password reset

        try {
            if(token.contains(" "))
                return "-1";
            if(token.contains("%"))
                return "-3";

            String query = "SELECT COUNT(*) FROM `{table}` WHERE TOKEN='{token}'";
            query = query.replace("{table}", Database.tokenTable);
            query = query.replace("{token}", token);
            PreparedStatement st = Database.connection.prepareStatement(query);
            ResultSet result = st.executeQuery();
            if(result.next()){
                if(result.getInt("COUNT(*)") == 1){
                    Template template = freemarkerConfiguration.getTemplate("reset-password.ftl");
                    String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, new HashMap<String, Object>());
                    html = html.replace("{token}", token);
                    return html;
                }
                return FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate("error.ftl"), new HashMap<String, Object>());
            }
            return FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate("error.ftl"), new HashMap<String, Object>());

        } catch (SQLException | IOException | TemplateException e) {
            e.printStackTrace();
        }
        return "ERROR";
    }

    @PostMapping("/api/reset-password")
    public String resetPassword(@RequestBody ResetPasswordBody body){

        //try {
            if(body.token.contains(" "))
                return "-1";
            if(body.token.contains("%"))
                return "-3";
            if(body.email.contains(" "))
                return "-1";
            if(body.email.contains("%"))
                return "-3";
            if(body.newPassword.contains(" "))
                return "-1";
            if(body.newPassword.contains("%"))
                return "-3";

            return "0";

        //} catch (SQLException | IOException | TemplateException e) {
        //    e.printStackTrace();
        //}
        //return "-2";
    }
}
