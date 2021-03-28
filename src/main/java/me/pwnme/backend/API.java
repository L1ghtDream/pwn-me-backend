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
import java.util.regex.Pattern;

@RestController
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class API {

    private final ResetPasswordEmailProperties resetPasswordEmailProperties;
    private final MailService mailService;
    private final Config config;
    private final Configuration freemarkerConfiguration;

    //------------------------------ Secure ------------------------------

    @PostMapping("/api/secure/login/credentials")
    public String login(@RequestBody String data) {
        return Utils.customEncode(checkCredentials(new Gson().fromJson(Utils.customDecode(data), LoginBody.class)));
    }

    @PostMapping("/api/secure/login/token")
    public String loginToken(@RequestBody String data) {

        LoginTokenBody body = new Gson().fromJson(Utils.customDecode(data), LoginTokenBody.class);
        Token token = new Gson().fromJson(Utils.decodeBase64(body.token), Token.class);

        String vulns = Utils.checkForVulns(Arrays.asList(body.token, token.email, token.password, token.timeExpire, token.timeCreated));
        if (!vulns.equals(Response.ok))
            return Utils.customEncode(vulns);

        LoginBody loginBody = new LoginBody();
        loginBody.password = token.password;
        loginBody.email = token.email;

        if (Long.parseLong(token.timeExpire) < new Date().getTime())
            return Utils.customEncode(Response.token_expired);
        if (Long.parseLong(token.timeExpire) - Long.parseLong(token.timeCreated) != 259200000L + Utils.getBonusTimeFromToken(token.password))
            return Utils.customEncode(Response.invalid_token);

        return Utils.customEncode(checkCredentials(loginBody));
    }

    @PostMapping("/api/secure/register")
    public String register(@RequestBody String data) {

        try {
            RegisterBody body = new Gson().fromJson(Utils.customDecode(data), RegisterBody.class);

            String vulns = Utils.checkForVulns(Arrays.asList(body.email, body.password));
            if (!vulns.equals(Response.ok))
                return Utils.customEncode(vulns);

            ResultSet result = Utils.getPreparedStatement("SELECT COUNT(*) FROM %table% WHERE EMAIL=?", Database.usersTable, body.email).executeQuery();

            if (result.next()) {
                if (result.getInt("COUNT(*)") == 0) {
                    Utils.getPreparedStatement("INSERT INTO %table% VALUES (?, ?)", Database.usersTable, body.email, body.password).executeUpdate();
                    long time = new Date().getTime();
                    return Utils.customEncode(Response.ok + " " + Utils.encodeBase64(Utils.craftToken(body.email, String.valueOf(time), String.valueOf(time + 259200000L + Utils.getBonusTimeFromToken(body.password)), body.password)));
                }
                return Utils.customEncode(Response.email_already_exists);
            }
            return Utils.customEncode(Response.internal_error);
        } catch (SQLException e) {
            e.printStackTrace();
            return Utils.customEncode(Response.internal_error);
        }
    }

    @PostMapping("/api/secure/get-progress")
    public String getProgress(@RequestBody String data) {

        try {
            GetSaveDataRequestBody body = new Gson().fromJson(Utils.customDecode(data), GetSaveDataRequestBody.class);
            Token token = new Gson().fromJson(Utils.decodeBase64(body.token), Token.class);

            String vulns = Utils.checkForVulns(Arrays.asList(body.token, token.email, token.password, token.timeExpire, token.timeCreated));
            if (!vulns.equals(Response.ok))
                return Utils.customEncode(vulns);

            LoginBody loginBody = new LoginBody();
            loginBody.password = token.password;
            loginBody.email = token.email;

            String response = checkCredentials(loginBody);

            if (response.equals(Response.ok)) {
                ResultSet resultSet = Utils.getPreparedStatement("SELECT * FROM %table% WHERE EMAIL=?", Database.saveDataTable, loginBody.email).executeQuery();

                if (resultSet.next()) {

                    Progress progress = new Progress();
                    progress.level = Integer.parseInt(resultSet.getString("LEVEL"));
                    progress.points = Integer.parseInt(resultSet.getString("POINTS"));

                    return Utils.customEncode(Response.ok + " " + Utils.encodeBase64(new Gson().toJson(progress)));
                }
                return Utils.customEncode(Response.email_does_not_exist);
            } else
                return Utils.customEncode(response);
        } catch (SQLException e) {
            e.printStackTrace();
            return Utils.customEncode(Response.internal_error);
        }
    }

    @PostMapping("/api/secure/reset-progress")
    public String resetProgress(@RequestBody String data) {

        try {
            GetSaveDataRequestBody body = new Gson().fromJson(Utils.customDecode(data), GetSaveDataRequestBody.class);
            Token token = new Gson().fromJson(Utils.decodeBase64(body.token), Token.class);

            String vulns = Utils.checkForVulns(Arrays.asList(body.token, token.email, token.password, token.timeExpire, token.timeCreated));
            if (!vulns.equals(Response.ok))
                return Utils.customEncode(vulns);

            LoginBody loginBody = new LoginBody();
            loginBody.password = token.password;
            loginBody.email = token.email;

            String response = checkCredentials(loginBody);

            if (response.equals(Response.ok)) {
                ResultSet resultSet = Utils.getPreparedStatement("SELECT COUNT(*) FROM %table% WHERE EMAIL=?", Database.saveDataTable, loginBody.email).executeQuery();

                if (resultSet.next()) {
                    if (resultSet.getInt("COUNT(*)") == 0)
                        Utils.getPreparedStatement("INSERT INTO %table% VALUES(?, ?, ?)", Database.saveDataTable, loginBody.email, "0", "0").executeUpdate();
                    else
                        Utils.getPreparedStatement("UPDATE %table% SET LEVEL=?, POINTS=? WHERE EMAIL=?", Database.saveDataTable, "0", "0", loginBody.email).executeUpdate();
                    return Utils.customEncode(Response.ok);
                } else {
                    Utils.getPreparedStatement("UPDATE %table% SET LEVEL=?, POINTS=? WHERE EMAIL=?", Database.saveDataTable, "0", "0", loginBody.email).executeUpdate();
                    return Utils.customEncode(Response.ok);
                }
            } else
                return Utils.customEncode(response);
        } catch (SQLException e) {
            e.printStackTrace();
            return Utils.customEncode(Response.internal_error);
        }
    }

    @PostMapping("/api/secure/save-progress")
    public String saveProgress(@RequestBody String data) {

        try {
            LevelUpRequestBody body = new Gson().fromJson(Utils.customDecode(data), LevelUpRequestBody.class);
            Token token = new Gson().fromJson(Utils.decodeBase64(body.token), Token.class);

            String vulns = Utils.checkForVulns(Arrays.asList(body.token, token.email, token.password, token.timeExpire, token.timeCreated));
            if (!vulns.equals(Response.ok))
                return Utils.customEncode(vulns);

            LoginBody loginBody = new LoginBody();
            loginBody.password = token.password;
            loginBody.email = token.email;

            String response = checkCredentials(loginBody);

            if (response.equals(Response.ok)) {
                ResultSet resultSet = Utils.getPreparedStatement("SELECT COUNT(*) FROM %table% WHERE EMAIL=?", Database.saveDataTable, loginBody.email).executeQuery();

                if (resultSet.next()) {
                    ResultSet resultSet1 = Utils.getPreparedStatement("SELECT LEVEL FROM %table% WHERE EMAIL=?", Database.saveDataTable, loginBody.email).executeQuery();
                    int level;
                    if (resultSet1.next())
                        level = Integer.parseInt(resultSet1.getString("LEVEL"));
                    else
                        return Utils.customEncode(Response.email_does_not_exist);
                    Utils.getPreparedStatement("UPDATE %table% SET LEVEL=? WHERE EMAIL=?", Database.saveDataTable, String.valueOf(level), loginBody.email).executeUpdate();
                    return Utils.customEncode(Response.ok);
                }
                return Utils.customEncode(Response.email_does_not_exist);

            } else
                return Utils.customEncode(response);
        } catch (SQLException e) {
            e.printStackTrace();
            return Utils.customEncode(Response.internal_error);
        }
    }

    @PostMapping("/api/secure/forgot-password")
    public String forgotPassword(@RequestBody String data) {

        try {
            ForgotPasswordBody body = new Gson().fromJson(Utils.customDecode(data), ForgotPasswordBody.class);

            String vulns = Utils.checkForVulns(Collections.singletonList(body.email));
            if (!vulns.equals(Response.ok))
                return Utils.customEncode(vulns);

            ResultSet result = Utils.getPreparedStatement("SELECT COUNT(*) FROM %table% WHERE EMAIL=?", Database.usersTable, body.email).executeQuery();

            if (result.next()) {
                if (result.getInt("COUNT(*)") == 1) {
                    boolean generateToken = true;
                    String token = "";
                    while (generateToken) {
                        token = Utils.generateRandomString(32);
                        result = Utils.getPreparedStatement("SELECT COUNT(*) FROM %table% WHERE TOKEN=?", Database.usersTable, token).executeQuery();

                        if (result.next())
                            if (result.getInt("COUNT(*)") == 0)
                                generateToken = false;
                    }

                    result = Utils.getPreparedStatement("SELECT COUNT(*) FROM %table% WHERE EMAIL=?", Database.passwordResetTokenTable, body.email).executeQuery();

                    if (result.next()) {
                        if (result.getInt("COUNT(*)") == 0)
                            Utils.getPreparedStatement("INSERT INTO %table% VALUES (?, ?, ?)", Database.passwordResetTokenTable, body.email, token, String.valueOf(new Date().getTime())).executeUpdate();
                        else
                            Utils.getPreparedStatement("UPDATE `{table}` SET TOKEN='{token}' WHERE EMAIL='{email}'", Database.passwordResetTokenTable, token, body.email).executeUpdate();
                    } else
                        return Utils.customEncode(Response.internal_error);

                    Map<String, Object> placeholders = new HashMap<>();
                    placeholders.put("name", "Anonymous");
                    placeholders.put("host", config.host);
                    placeholders.put("token", token);

                    mailService.sendMail(new Mail(resetPasswordEmailProperties.from, body.email, resetPasswordEmailProperties.subject), "reset-password-mail.ftl", placeholders);
                    return Utils.customEncode(Response.ok);
                }
                return Utils.customEncode(Response.multiple_accounts_on_email);
            }
            return Utils.customEncode(Response.email_does_not_exist);


        } catch (SQLException | IOException | TemplateException | MessagingException e) {
            e.printStackTrace();
            return Utils.customEncode(Response.internal_error);
        }
    }


    //------------------------------ To Be Secured ------------------------------

    @Beta //TODO: Update to the new me.pwnme.backend.API
    @GetMapping("/reset-password")
    public String resetPasswordMessage(@RequestParam String token) {

        //TODO: Create the web portal for password reset

        try {
            String vulns = Utils.checkForVulns(Collections.singletonList(token));
            if (!vulns.equals(Response.ok))
                return vulns;

            String query = "SELECT COUNT(*) FROM `{table}` WHERE TOKEN='{token}'";
            query = query.replace("{table}", Database.passwordResetTokenTable);
            query = query.replace("{token}", token);
            PreparedStatement st = Database.connection.prepareStatement(query);
            ResultSet result = st.executeQuery();

            if (result.next()) {
                if (result.getInt("COUNT(*)") == 1) {
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
    public String resetPassword(@RequestBody ResetPasswordBody body) {

        //try {
        if (body.token.contains(" "))
            return "-1";
        if (body.token.contains("%"))
            return "-3";
        if (body.email.contains(" "))
            return "-1";
        if (body.email.contains("%"))
            return "-3";
        if (body.newPassword.contains(" "))
            return "-1";
        if (body.newPassword.contains("%"))
            return "-3";

        return "0";

    }


    //------------------------------ In Dev ------------------------------
    //NONE

    private String checkCredentials(LoginBody body) {
        try {

            String vulns = Utils.checkForVulns(Arrays.asList(body.password, body.email));
            if (!vulns.equals(Response.ok))
                return vulns;

            String emailRegex = "^(.+)@(.+)$";

            if (!Pattern.compile(emailRegex).matcher(body.email).matches())
                return Response.invalid_email_format;

            ResultSet result = Utils.getPreparedStatement("SELECT COUNT(*),PASSWORD FROM %table% WHERE EMAIL=?", Database.usersTable, body.email).executeQuery();

            if (result.next()) {
                if (result.getInt("COUNT(*)") == 1) {
                    if (result.getString("PASSWORD").equals(body.password)) {
                        long time = new Date().getTime();
                        return Response.ok + " " + Utils.encodeBase64(Utils.craftToken(body.email, String.valueOf(time), String.valueOf(time + 259200000L + Utils.getBonusTimeFromToken(body.password)), body.password));
                    }
                    return Response.invalid_credentials;
                }
                return Response.invalid_credentials;
            }
            return Response.invalid_credentials;

        } catch (SQLException e) {
            e.printStackTrace();
            return Response.internal_error;
        }
    }

}
