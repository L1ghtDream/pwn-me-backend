package me.pwnme.backend;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import me.pwnme.backend.Configuration.Config;
import me.pwnme.backend.Configuration.ResetPasswordEmailProperties;
import me.pwnme.backend.DTO.*;
import me.pwnme.backend.Services.MailService;
import org.springframework.boot.json.GsonJsonParser;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class API {

    private final ResetPasswordEmailProperties resetPasswordEmailProperties;
    private final MailService mailService;
    private final Config config;
    private final Configuration freemarkerConfiguration;

    @PostMapping("/api/login/credentials")
    public String login(@RequestBody LoginBody body) {

        try {
            String vulns = Utils.checkForVulns(Arrays.asList(body.password, body.email));
            if(!vulns.equals(Response.ok))
                return vulns;

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

                        return Response.ok + " " + token;
                    }
                    return Response.invalid_credentials;
                }
                return Response.multiple_accounts_on_email;
            }
            return Response.email_does_not_exist;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Response.internal_error;
    }

    @PostMapping("/api/login/token")
    public String loginToken(@RequestBody LoginTokenBody body) {

        try {
            String vulns = Utils.checkForVulns(Arrays.asList(body.token));
            if(!vulns.equals(Response.ok))
                return vulns;

            Gson gson = new Gson();

            Token token = gson.fromJson(Utils.decodeBase64(body.token), Token.class);

            vulns = Utils.checkForVulns(Arrays.asList(token.password, token.email));
            if(!vulns.equals(Response.ok))
                return vulns;

            String query = "SELECT COUNT(*) FROM `{table}` WHERE EMAIL='{email}'";
            query = query.replace("{table}", Database.usersTable);
            query = query.replace("{email}", token.email);
            PreparedStatement st = Database.connection.prepareStatement(query);
            ResultSet result = st.executeQuery();

            if(result.next()){
                if(result.getInt("COUNT(*)") == 1){

                    //TODO: Check for the expire date

                    query = "SELECT PASSWORD FROM `{table}` WHERE EMAIL='{email}'";
                    query = query.replace("{table}", Database.usersTable);
                    query = query.replace("{email}", token.email);
                    st = Database.connection.prepareStatement(query);
                    result = st.executeQuery();
                    if(result.next()){
                        if(result.getString("PASSWORD").equals(token.password))
                            return Response.ok;
                        return Response.invalid_credentials;
                    }
                    return Response.email_does_not_exist;
                }
                return Response.multiple_accounts_on_email;
            }
            return Response.email_does_not_exist;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Response.internal_error;
    }

    @PostMapping("/api/register")
    public String register(@RequestBody RegisterBody body){

        try {

            String vulns = Utils.checkForVulns(Arrays.asList(body.email, body.password));
            if(!vulns.equals(Response.ok))
                return vulns;

            String query = "SELECT COUNT(*) FROM `{table}` WHERE EMAIL='{email}'";
            query = query.replace("{table}", Database.usersTable);
            query = query.replace("{email}", body.email);

            PreparedStatement st = Database.connection.prepareStatement(query);
            ResultSet result = st.executeQuery();

            if(result.next()){
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

                    return Response.ok + " " + token;
                }
                return Response.email_already_exists;
            }
            return Response.internal_error;



        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Response.internal_error;
    }

    //TODO: Add expire date atribute to reset password token
    @PostMapping("/api/forgot-password")
    public String forgotPassword(@RequestBody ForgotPasswordBody body){

        //TODO: Recode
        try {
            String vulns = Utils.checkForVulns(Arrays.asList(body.email));
            if(!vulns.equals(Response.ok))
                return vulns;

            String query = "SELECT COUNT(*) FROM `{table}` WHERE EMAIL='{email}'";
            query = query.replace("{table}", Database.usersTable);
            query = query.replace("{email}", body.email);
            PreparedStatement st = Database.connection.prepareStatement(query);
            ResultSet result = st.executeQuery();

            if(result.next()){
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

                        st = Database.connection.prepareStatement(query);
                        st.executeUpdate();
                    }
                    else
                        return Response.internal_error;

                    Map<String, Object> placeholders = new HashMap<>();
                    placeholders.put("name", "Anonymous");
                    placeholders.put("host", config.host);
                    placeholders.put("token", token);

                    mailService.sendMail(new Mail(resetPasswordEmailProperties.from, body.email, resetPasswordEmailProperties.subject), "reset-password-mail.ftl", placeholders);
                    return Response.ok;
                }
                return Response.multiple_accounts_on_email;
            }
            return Response.email_does_not_exist;


        } catch (SQLException | IOException | TemplateException | MessagingException e) {
            e.printStackTrace();
        }
        return Response.internal_error;
    }

    @GetMapping("/reset-password")
    public String resetPasswordMessage(@RequestParam String token){

        //TODO: Create the web portal for password reset

        try {
            String vulns = Utils.checkForVulns(Arrays.asList(token));
            if(!vulns.equals(Response.ok))
                return vulns;

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
