package me.pwnme.backend;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import me.pwnme.backend.Configuration.Config;
import me.pwnme.backend.Configuration.ResetPasswordEmailProperties;
import me.pwnme.backend.DTO.*;
import me.pwnme.backend.Database.Database;
import me.pwnme.backend.Services.MailService;
import me.pwnme.backend.Utils.Response;
import me.pwnme.backend.Utils.Utils;
import org.assertj.core.annotations.Beta;
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
@SuppressWarnings("unused")
public class API {

    private final ResetPasswordEmailProperties resetPasswordEmailProperties;
    private final MailService mailService;
    private final Config config;
    private final Configuration freemarkerConfiguration;

    //------------------------------ Legacy ------------------------------

    @Deprecated
    @PostMapping("/api/legacy/login/credentials")
    public String loginLegacy(@RequestBody LoginBody body) {

        try {
            String vulns = Utils.checkForVulns(Arrays.asList(body.password, body.email));
            if(!vulns.equals(Response.ok))
                return vulns;

            ResultSet result = Utils.getPreparedStatement("SELECT * FROM `?` WHERE EMAIL='?'", Arrays.asList(Database.usersTable, body.email)).executeQuery();
            ResultSet result1 = Utils.getPreparedStatement("SELECT COUNT(*) FROM `?` WHERE EMAIL='?'", Arrays.asList(Database.usersTable, body.email)).executeQuery();

            if(result.next() && result1.next()){
                if(result1.getInt("COUNT(*)")==1) {
                    if (result.getString("PASSWORD").equals(body.password)) {
                        long time = new Date().getTime();
                        return Response.ok + " " + Utils.encodeBase64(Utils.craftToken(body.email, String.valueOf(time), String.valueOf(time + 259200000L + Utils.getBonusTimeFromToken(body.password)), body.password));
                    }
                    return Response.invalid_credentials;
                }
                return Response.multiple_accounts_on_email;
            }
            return Response.email_does_not_exist;

        } catch (SQLException e) {
            e.printStackTrace();
            return Response.internal_error;
        }
    }

    @Deprecated
    @PostMapping("/api/legacy/login/token")
    public String loginTokenLegacy(@RequestBody LoginTokenBody body) {

        try {
            String vulns = Utils.checkForVulns(Collections.singletonList(body.token));
            if(!vulns.equals(Response.ok))
                return vulns;

            Gson gson = new Gson();

            Token token = gson.fromJson(Utils.decodeBase64(body.token), Token.class);

            vulns = Utils.checkForVulns(Arrays.asList(token.password, token.email));
            if(!vulns.equals(Response.ok))
                return vulns;

            ResultSet result = Utils.getPreparedStatement("SELECT COUNT(*) FROM `?` WHERE EMAIL='?'", Arrays.asList(Database.usersTable, token.email)).executeQuery();

            if(result.next()){
                if(result.getInt("COUNT(*)") == 1){

                    if(Long.parseLong(token.timeExpire) < new Date().getTime())
                        return Response.token_expired;
                    if(Long.parseLong(token.timeExpire)-Long.parseLong(token.timeCreated) != 259200000L + Utils.getBonusTimeFromToken(token.password))
                        return Response.invalid_token;

                    result = Utils.getPreparedStatement("SELECT PASSWORD FROM `?` WHERE EMAIL='?'", Arrays.asList(Database.usersTable, token.email)).executeQuery();

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
            return Response.internal_error;
        }
    }

    @Deprecated
    @PostMapping("/api/legacy/register")
    public String registerLegacy(@RequestBody RegisterBody body){

        try {

            String vulns = Utils.checkForVulns(Arrays.asList(body.email, body.password));
            if(!vulns.equals(Response.ok))
                return vulns;

            ResultSet result = Utils.getPreparedStatement("SELECT COUNT(*) FROM `?` WHERE EMAIL='?'", Arrays.asList(Database.usersTable, body.email)).executeQuery();

            if(result.next()){
                if(result.getInt("COUNT(*)")==0){
                    Utils.getPreparedStatement("INSERT INTO '?' VALUES ('?', '?')", Arrays.asList(Database.usersTable, body.email, body.password)).executeUpdate();
                    long time = new Date().getTime();
                    return Response.ok + " " + Utils.encodeBase64(Utils.craftToken(body.email, String.valueOf(time),  String.valueOf(time + 259200000L + Utils.getBonusTimeFromToken(body.password)), body.password));
                }
                return Response.email_already_exists;
            }
            return Response.internal_error;
        } catch (SQLException e) {
            e.printStackTrace();
            return Response.internal_error;
        }
    }


    //------------------------------ Secure ------------------------------

    @PostMapping("/api/secure/login/credentials")
    public String login(@RequestBody String data) {
        return Utils.customEncode(checkCredentials(new Gson().fromJson(Utils.customDecode(data), LoginBody.class)));
    }

    @PostMapping("/api/secure/login/token")
    public String loginToken(@RequestBody String data) {

        LoginTokenBody body = new Gson().fromJson(Utils.customDecode(data), LoginTokenBody.class);

        String vulns = Utils.checkForVulns(Collections.singletonList(body.token));
        if(!vulns.equals(Response.ok))
            return Utils.customEncode(vulns);

        Token token = new Gson().fromJson(Utils.decodeBase64(body.token), Token.class);

        LoginBody loginBody = new LoginBody();
        loginBody.password = token.password;
        loginBody.email = token.email;

        if(Long.parseLong(token.timeExpire) < new Date().getTime())
            return Utils.customEncode(Response.token_expired);
        if(Long.parseLong(token.timeExpire)-Long.parseLong(token.timeCreated) != 259200000L + Utils.getBonusTimeFromToken(token.password))
            return Utils.customEncode(Response.invalid_token);

        return Utils.customEncode(checkCredentials(loginBody));
    }

    @PostMapping("/api/secure/register")
    public String register(@RequestBody String data){

        try {
            RegisterBody body = new Gson().fromJson(Utils.customDecode(data), RegisterBody.class);

            String vulns = Utils.checkForVulns(Arrays.asList(body.email, body.password));
            if(!vulns.equals(Response.ok))
                return Utils.customEncode(vulns);

            ResultSet result = Utils.getPreparedStatement("SELECT COUNT(*) FROM `?` WHERE EMAIL='?'", Arrays.asList(Database.usersTable, body.email)).executeQuery();

            if(result.next()){
                if(result.getInt("COUNT(*)")==0){
                    Utils.getPreparedStatement("INSERT INTO '?' VALUES ('?', '?')", Arrays.asList(Database.usersTable, body.email, body.password)).executeUpdate();
                    long time = new Date().getTime();
                    return Utils.customEncode(Response.ok + " " + Utils.encodeBase64(Utils.craftToken(body.email, String.valueOf(time),  String.valueOf(time + 259200000L + Utils.getBonusTimeFromToken(body.password)), body.password)));
                }
                return Utils.customEncode(Response.email_already_exists);
            }
            return Utils.customEncode(Response.internal_error);
        } catch (SQLException e) {
            e.printStackTrace();
            return Utils.customEncode(Response.internal_error);
        }
    }

    @PostMapping("/api/secure/get-save-data")
    public String getSaveData(@RequestBody String data){

        try {
            SaveDataRequestCredentialsBody body  = new Gson().fromJson(Utils.customDecode(data), SaveDataRequestCredentialsBody.class);

            String vulns = Utils.checkForVulns(Collections.singletonList(body.email));
            if (!vulns.equals(Response.ok))
                return Utils.customEncode(vulns);

            LoginBody loginBody = new LoginBody();
            loginBody.password = body.password;
            loginBody.email = body.email;

            String response = checkCredentials(loginBody);

            if (response.equals(Response.ok)) {
                ResultSet resultSet = Utils.getPreparedStatement("SELECT * FROM `?` WHERE EMAIL='?'", Arrays.asList(Database.saveDataTable, body.email)).executeQuery();

                if (resultSet.next()) {
                    String output = "{\"level\": \"{1}\",\"points\": \"{2}\"}";

                    output = output.replace("{1}", resultSet.getString("LEVEL"));
                    output = output.replace("{2}", resultSet.getString("POINTS"));

                    return Utils.customEncode(Response.ok + " " + output);
                }
                return Utils.customEncode(Response.email_does_not_exist);
            } else
                return Utils.customEncode(response);
        }
        catch (SQLException e) {
            e.printStackTrace();
            return Utils.customEncode(Response.internal_error);
        }
    }



    //------------------------------ To Be Secured ------------------------------

    @Beta //TODO: Update to the new me.pwnme.backend.API || Add expire date atribute to reset password token
    @PostMapping("/api/forgot-password")
    public String forgotPassword(@RequestBody ForgotPasswordBody body){

        //TODO: Recode
        try {
            String vulns = Utils.checkForVulns(Collections.singletonList(body.email));
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
                        query = query.replace("{table}", Database.passwordResetTokenTable);
                        query = query.replace("{token}", token);
                        st = Database.connection.prepareStatement(query);
                        result = st.executeQuery();

                        if(result.next())
                            if(result.getInt("COUNT(*)") == 0)
                                generateToken = false;
                    }

                    query = "SELECT COUNT(*) FROM `{table}` WHERE EMAIL='{email}'";
                    query = query.replace("{table}", Database.passwordResetTokenTable);
                    query = query.replace("{email}", body.email);
                    st = Database.connection.prepareStatement(query);
                    result = st.executeQuery();

                    if(result.next()){
                        if(result.getInt("COUNT(*)")==0)
                            query = "INSERT INTO `{table}` VALUES ('{email}', '{token}', '{date}')";
                        else {
                            query = "UPDATE `{table}` SET DATE='{date}' WHERE EMAIL='{email}'";
                            query = query.replace("{table}", Database.passwordResetTokenTable);
                            query = query.replace("{email}", body.email);
                            query = query.replace("{token}", token);
                            query = query.replace("{date}", String.valueOf(new Date().getTime()));

                            st = Database.connection.prepareStatement(query);
                            st.executeUpdate();

                            query = "UPDATE `{table}` SET TOKEN='{token}' WHERE EMAIL='{email}'";
                        }

                        query = query.replace("{table}", Database.passwordResetTokenTable);
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

    @Beta //TODO: Update to the new me.pwnme.backend.API
    @GetMapping("/reset-password")
    public String resetPasswordMessage(@RequestParam String token){

        //TODO: Create the web portal for password reset

        try {
            String vulns = Utils.checkForVulns(Collections.singletonList(token));
            if(!vulns.equals(Response.ok))
                return vulns;

            String query = "SELECT COUNT(*) FROM `{table}` WHERE TOKEN='{token}'";
            query = query.replace("{table}", Database.passwordResetTokenTable);
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

    @Beta //TODO: Update to the new me.pwnme.backend.API
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


    //------------------------------ In Dev ------------------------------


    private String checkCredentials(LoginBody body) {
        try {

            String vulns = Utils.checkForVulns(Arrays.asList(body.password, body.email));
            if(!vulns.equals(Response.ok))
                return vulns;

            ResultSet result = Utils.getPreparedStatement("SELECT * FROM `?` WHERE EMAIL='?'", Arrays.asList(Database.usersTable, body.email)).executeQuery();
            ResultSet result1 = Utils.getPreparedStatement("SELECT COUNT(*) FROM `?` WHERE EMAIL='?'", Arrays.asList(Database.usersTable, body.email)).executeQuery();

            if(result.next() && result1.next()){
                if(result1.getInt("COUNT(*)")==1) {
                    if (result.getString("PASSWORD").equals(body.password)) {
                        long time = new Date().getTime();
                        return Response.ok + " " + Utils.encodeBase64(Utils.craftToken(body.email, String.valueOf(time), String.valueOf(time + 259200000L + Utils.getBonusTimeFromToken(body.password)), body.password));
                    }
                    return Response.invalid_credentials;
                }
                return Response.multiple_accounts_on_email;
            }
            return Response.email_does_not_exist;

        } catch (SQLException e) {
            e.printStackTrace();
            return Response.internal_error;
        }
    }

}
