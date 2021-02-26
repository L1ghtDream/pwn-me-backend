package me.pwnme.backend.Services;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import me.pwnme.backend.DTO.Mail;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender emailSender;

    private final Configuration freemarkerConfiguration;


    public void sendMail(Mail mail, String templateName, Map<String, Object> placeholders) throws MessagingException, IOException, TemplateException {

        MimeMessage message = emailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name());

        Template template = freemarkerConfiguration.getTemplate(templateName);
        String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, placeholders);

        helper.setTo(mail.to);
        helper.setText(html, true);
        helper.setSubject(String.format(mail.subject, placeholders.get("email")));
        helper.setFrom(mail.from);
        emailSender.send(message);

    }


}
