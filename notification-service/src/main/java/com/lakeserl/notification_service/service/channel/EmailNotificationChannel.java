package com.lakeserl.notification_service.service.channel;

import java.nio.charset.StandardCharsets;
import java.time.Year;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.lakeserl.notification_service.config.properties.EmailProperties;
import com.lakeserl.notification_service.entity.Notification;
import com.lakeserl.notification_service.enums.NotificationCategory;
import com.lakeserl.notification_service.enums.NotificationChannel;
import com.lakeserl.notification_service.event.payload.RecipientContext;
import com.lakeserl.notification_service.service.UnsubscribeService;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends an HTML email through the configured provider SMTP. The body text is
 * wrapped in the branded {@code email/generic} Thymeleaf layout, which always
 * carries an unsubscribe / preferences footer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationChannel implements NotificationChannelHandler {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final EmailProperties emailProps;
    private final UnsubscribeService unsubscribeService;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void deliver(Notification notification, RecipientContext recipient) throws Exception {
        if (recipient == null || !recipient.hasEmail()) {
            throw new IllegalStateException("No email address for user " + notification.getUserId());
        }

        String subject = notification.getSubject() != null ? notification.getSubject() : "CalmSKIN";
        String html = renderHtml(notification, recipient, subject);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(emailProps.from());
        helper.setTo(recipient.email());
        helper.setSubject(subject);
        helper.setText(html, true);
        if (emailProps.replyTo() != null && !emailProps.replyTo().isBlank()) {
            helper.setReplyTo(emailProps.replyTo());
        }

        mailSender.send(message);
        log.info("Email sent to {} subject='{}' notificationId={}",
                recipient.email(), subject, notification.getId());
    }

    private String renderHtml(Notification notification, RecipientContext recipient, String subject) {
        Context context = new Context();
        context.setVariable("subject", subject);
        context.setVariable("bodyText", notification.getBody());
        context.setVariable("recipientName",
                recipient.fullName() != null && !recipient.fullName().isBlank()
                        ? recipient.fullName() : "bạn");
        context.setVariable("year", Year.now().getValue());
        context.setVariable("unsubscribeUrl", buildUnsubscribeUrl(notification));
        return templateEngine.process("email/generic", context);
    }

    /**
     * Promotional emails get a one-click tokenised unsubscribe link; every other
     * email links to the preference centre.
     */
    private String buildUnsubscribeUrl(Notification notification) {
        if (notification.getCategory() == NotificationCategory.PROMOTIONS) {
            String token = unsubscribeService.issueToken(
                    notification.getUserId(), NotificationCategory.PROMOTIONS.name());
            return emailProps.baseUrl() + "/api/v1/notifications/unsubscribe?token=" + token;
        }
        return emailProps.baseUrl() + "/account/notifications";
    }
}
