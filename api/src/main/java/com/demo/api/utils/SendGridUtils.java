package com.demo.api.utils;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SendGrid Email API Utils.
 */
@Component
@Slf4j
public class SendGridUtils {

    private final SendGrid client;
    public SendGridUtils(@Value("${sendgrid.api-key}") String apiKey) {
        this.client = new SendGrid(apiKey);
    }
    @PreDestroy
    public void close() {}

    @Value("${sendgrid.from}")
    private String from;

    /**
     * Send HTML email.
     * @param to
     * @param subject
     * @param html
     */
    public void sendHtml(String to, String subject, String html) {
        Mail mail = new Mail();
        mail.setFrom(new Email(from));
        mail.setSubject(subject);

        Personalization p = new Personalization();
        p.addTo(new Email(to));
        mail.addPersonalization(p);

        mail.addContent(new Content("text/html", html));

        Request req = new Request();
        try {
            req.setMethod(Method.POST);
            req.setEndpoint("mail/send");
            req.setBody(mail.build());
            Response resp = client.api(req);
            int code = resp.getStatusCode();
            if (code >= 400) {
                throw new RuntimeException("SendGrid error: " + code + " - " + resp.getBody());
            }
        } catch (Exception e) {
            log.error("SendGrid sendHtml failed: to={}, subject={}", to, subject, e);
            throw new RuntimeException("Email send failed", e);
        }
    }

    /**
     * Send verify email.
     * @param to
     * @param verifyLink
     */
    public void sendVerifyEmail(String to, String verifyLink) {
        String subject = "Verify your email";
        String html = """
          <div style="font-family:sans-serif">
            <h2>Verify your email</h2>
            <p>Click the button below to verify your email address:</p>
            <p><a href="%s">Verify Email</a></p>
            <p>If the button doesn't work, copy this URL:<br/>%s</p>
            <p>This link expires in 2 hours.</p>
          </div>
        """.formatted(verifyLink, verifyLink);
        sendHtml(to, subject, html);
    }

    /**
     * Send reset password email.
     * @param to
     * @param resetLink
     */
    public void sendResetEmail(String to, String resetLink) {
        String subject = "Reset your password";
        String html = """
          <div style="font-family:sans-serif">
            <h2>Reset your password</h2>
            <p>If you didn't request this, you can ignore this email.</p>
            <p><a href="%s">Reset Password</a></p>
            <p>If the button doesn't work, copy this URL:<br/>%s</p>
            <p>This link expires in 30 minutes.</p>
          </div>
        """.formatted(resetLink, resetLink);
        sendHtml(to, subject, html);
    }

    /**
     * Send change email verification email.
     * @param to
     * @param changeLink
     */
    public void sendChangeEmail(String to, String changeLink) {
        String subject = "Change your email";
        String html = """
          <div style="font-family:sans-serif">
            <h2>Change your email</h2>
            <p>Click the button below to change your email address:</p>
            <p><a href="%s">Change Email</a></p>
            <p>If the button doesn't work, copy this URL:<br/>%s</p>
            <p>This link expires in 30 minutes.</p>
          </div>
        """.formatted(changeLink, changeLink);
        sendHtml(to, subject, html);
    }
}
